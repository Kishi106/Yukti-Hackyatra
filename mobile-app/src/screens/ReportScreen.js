import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, ActivityIndicator } from 'react-native';
import * as Location from 'expo-location';
import * as ImagePicker from 'expo-image-picker';
import { submitReport } from '../services/api';
import { uploadPhoto } from '../services/upload';
import { trackSubmittedReportId } from './StatusScreen';

const SEVERITIES = ['low', 'medium', 'high'];

export default function ReportScreen() {
  const [severity, setSeverity] = useState('medium');
  const [photoUri, setPhotoUri] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  async function handleReport() {
    setError(null);
    setSuccess(false);
    setSubmitting(true);

    try {
      const { status: locationStatus } = await Location.requestForegroundPermissionsAsync();
      if (locationStatus !== 'granted') {
        throw new Error('Location permission is required to submit a report.');
      }
      const location = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });

      const { status: cameraStatus } = await ImagePicker.requestCameraPermissionsAsync();
      if (cameraStatus !== 'granted') {
        throw new Error('Camera permission is required to submit a report.');
      }
      const photoResult = await ImagePicker.launchCameraAsync({ quality: 0.6 });

      let photoUrl = null;
      if (!photoResult.canceled && photoResult.assets && photoResult.assets.length > 0) {
        const localUri = photoResult.assets[0].uri;
        setPhotoUri(localUri);
        photoUrl = await uploadPhoto(localUri);
      }

      const created = await submitReport({
        lat: location.coords.latitude,
        lng: location.coords.longitude,
        severity,
        source: 'citizen',
        photo_url: photoUrl,
        ward: null
      });
      await trackSubmittedReportId(created.id);

      setSuccess(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Report a Pothole</Text>

      <Text style={styles.label}>Severity</Text>
      <View style={styles.severityRow}>
        {SEVERITIES.map((option) => (
          <TouchableOpacity
            key={option}
            style={[styles.severityOption, severity === option && styles.severityOptionSelected]}
            onPress={() => setSeverity(option)}
          >
            <Text style={severity === option ? styles.severityTextSelected : styles.severityText}>
              {option}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {photoUri && <Image source={{ uri: photoUri }} style={styles.preview} />}

      <TouchableOpacity style={styles.button} onPress={handleReport} disabled={submitting}>
        {submitting ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>Report a pothole</Text>
        )}
      </TouchableOpacity>

      {success && <Text style={styles.success}>Report submitted successfully.</Text>}
      {error && <Text style={styles.error}>{error}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, justifyContent: 'center' },
  title: { fontSize: 22, fontWeight: '700', marginBottom: 24, textAlign: 'center' },
  label: { fontSize: 14, color: '#555', marginBottom: 8 },
  severityRow: { flexDirection: 'row', marginBottom: 20 },
  severityOption: {
    flex: 1,
    padding: 12,
    marginHorizontal: 4,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ccc',
    alignItems: 'center'
  },
  severityOptionSelected: { backgroundColor: '#2563eb', borderColor: '#2563eb' },
  severityText: { color: '#333' },
  severityTextSelected: { color: '#fff', fontWeight: '600' },
  preview: { width: '100%', height: 200, borderRadius: 8, marginBottom: 20 },
  button: {
    backgroundColor: '#2563eb',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center'
  },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  success: { color: 'green', marginTop: 16, textAlign: 'center' },
  error: { color: 'red', marginTop: 16, textAlign: 'center' }
});
