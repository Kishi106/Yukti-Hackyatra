import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, StyleSheet, FlatList, RefreshControl } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { getPotholes } from '../services/api';

const SESSION_IDS_KEY = 'pothole_session_report_ids';

export default function StatusScreen() {
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [allPotholes, storedIds] = await Promise.all([
        getPotholes(),
        AsyncStorage.getItem(SESSION_IDS_KEY)
      ]);
      const sessionIds = new Set(storedIds ? JSON.parse(storedIds) : []);
      const mine = allPotholes.filter((p) => sessionIds.has(p.id));
      mine.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
      setReports(mine);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>My Reports</Text>
      {error && <Text style={styles.error}>{error}</Text>}
      {!loading && reports.length === 0 && !error && (
        <Text style={styles.empty}>No reports submitted this session yet.</Text>
      )}
      <FlatList
        data={reports}
        keyExtractor={(item) => item.id}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={load} />}
        renderItem={({ item }) => (
          <View style={styles.row}>
            <View style={styles.rowTop}>
              <Text style={styles.severity}>{item.severity.toUpperCase()}</Text>
              <Text style={styles.status}>{item.status}</Text>
            </View>
            <Text style={styles.time}>{new Date(item.created_at).toLocaleString()}</Text>
          </View>
        )}
      />
    </View>
  );
}

export async function trackSubmittedReportId(id) {
  const stored = await AsyncStorage.getItem(SESSION_IDS_KEY);
  const ids = stored ? JSON.parse(stored) : [];
  ids.push(id);
  await AsyncStorage.setItem(SESSION_IDS_KEY, JSON.stringify(ids));
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24 },
  title: { fontSize: 22, fontWeight: '700', marginBottom: 16 },
  empty: { color: '#888', textAlign: 'center', marginTop: 40 },
  error: { color: 'red', marginBottom: 12 },
  row: {
    backgroundColor: '#f2f2f2',
    borderRadius: 8,
    padding: 16,
    marginBottom: 10
  },
  rowTop: { flexDirection: 'row', justifyContent: 'space-between' },
  severity: { fontWeight: '700', fontSize: 16 },
  status: { fontSize: 14, color: '#2563eb', textTransform: 'uppercase' },
  time: { fontSize: 12, color: '#666', marginTop: 6 }
});
