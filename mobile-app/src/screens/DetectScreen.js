import React, { useEffect, useRef, useState } from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { Accelerometer } from 'expo-sensors';
import * as Location from 'expo-location';
import { submitReport } from '../services/api';
import { trackSubmittedReportId } from './StatusScreen';
import {
  MIN_SPEED_MPS,
  ROLLING_BUFFER_SIZE,
  DETECTION_THRESHOLD,
  computeMagnitude,
  computeDelta,
  estimateSeverity
} from '../services/accelerometer';

// Minimum time between two auto-submitted detections, so a single big bump
// doesn't get reported multiple times as the accelerometer settles.
const DETECTION_COOLDOWN_MS = 3000;

export default function DetectScreen() {
  const [permissionError, setPermissionError] = useState(null);
  const [tracking, setTracking] = useState(false);
  const [currentMagnitude, setCurrentMagnitude] = useState(0);
  const [lastDetectionTime, setLastDetectionTime] = useState(null);
  const [detectionCount, setDetectionCount] = useState(0);
  const [currentSpeed, setCurrentSpeed] = useState(0);

  const bufferRef = useRef([]);
  const locationRef = useRef(null);
  const lastDetectionRef = useRef(0);
  const submittingRef = useRef(false);
  const accelSubRef = useRef(null);
  const locationSubRef = useRef(null);

  useEffect(() => {
    let isMounted = true;

    async function start() {
      const { status: locationStatus } = await Location.requestForegroundPermissionsAsync();
      if (locationStatus !== 'granted') {
        setPermissionError('Location permission is required for auto-detection.');
        return;
      }

      locationSubRef.current = await Location.watchPositionAsync(
        { accuracy: Location.Accuracy.High, timeInterval: 1000, distanceInterval: 1 },
        (loc) => {
          if (!isMounted) return;
          locationRef.current = loc;
          setCurrentSpeed(loc.coords.speed && loc.coords.speed > 0 ? loc.coords.speed : 0);
        }
      );

      Accelerometer.setUpdateInterval(100);
      accelSubRef.current = Accelerometer.addListener((reading) => {
        if (!isMounted) return;
        handleReading(reading);
      });

      setTracking(true);
    }

    start();

    return () => {
      isMounted = false;
      if (accelSubRef.current) accelSubRef.current.remove();
      if (locationSubRef.current) locationSubRef.current.remove();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleReading(reading) {
    const magnitude = computeMagnitude(reading);
    const delta = computeDelta(magnitude);

    setCurrentMagnitude(magnitude);

    const buffer = bufferRef.current;
    buffer.push(delta);
    if (buffer.length > ROLLING_BUFFER_SIZE) buffer.shift();

    const now = Date.now();
    const cooldownElapsed = now - lastDetectionRef.current > DETECTION_COOLDOWN_MS;
    const speed = locationRef.current?.coords?.speed || 0;
    const movingFastEnough = speed >= MIN_SPEED_MPS;

    if (delta >= DETECTION_THRESHOLD && movingFastEnough && cooldownElapsed && !submittingRef.current) {
      lastDetectionRef.current = now;
      submitDetection(delta);
    }
  }

  async function submitDetection(delta) {
    const loc = locationRef.current;
    if (!loc) return;

    submittingRef.current = true;
    try {
      const created = await submitReport({
        lat: loc.coords.latitude,
        lng: loc.coords.longitude,
        severity: estimateSeverity(delta),
        source: 'auto',
        photo_url: null,
        ward: null
      });
      await trackSubmittedReportId(created.id);
      setLastDetectionTime(new Date());
      setDetectionCount((count) => count + 1);
    } catch (err) {
      console.warn('Auto-detection submit failed:', err.message);
    } finally {
      submittingRef.current = false;
    }
  }

  if (permissionError) {
    return (
      <View style={styles.container}>
        <Text style={styles.error}>{permissionError}</Text>
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>Auto-Detect Mode</Text>
      <Text style={styles.status}>{tracking ? 'Tracking active' : 'Starting sensors...'}</Text>

      <View style={styles.card}>
        <Text style={styles.label}>Current magnitude</Text>
        <Text style={styles.value}>{currentMagnitude.toFixed(3)} g</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.label}>Current speed</Text>
        <Text style={styles.value}>{currentSpeed.toFixed(2)} m/s</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.label}>Detections this session</Text>
        <Text style={styles.value}>{detectionCount}</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.label}>Last detection</Text>
        <Text style={styles.value}>
          {lastDetectionTime ? lastDetectionTime.toLocaleTimeString() : 'None yet'}
        </Text>
      </View>

      <Text style={styles.hint}>
        Keep the app open and your phone mounted while walking or driving. Detections require
        movement above {MIN_SPEED_MPS} m/s.
      </Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flexGrow: 1, padding: 24, alignItems: 'stretch' },
  title: { fontSize: 22, fontWeight: '700', marginBottom: 4 },
  status: { fontSize: 14, color: '#555', marginBottom: 20 },
  card: {
    backgroundColor: '#f2f2f2',
    borderRadius: 8,
    padding: 16,
    marginBottom: 12
  },
  label: { fontSize: 13, color: '#666' },
  value: { fontSize: 20, fontWeight: '600', marginTop: 4 },
  hint: { marginTop: 12, fontSize: 12, color: '#888' },
  error: { color: 'red', fontSize: 16, textAlign: 'center', marginTop: 40 }
});
