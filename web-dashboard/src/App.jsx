import React, { useCallback, useEffect, useMemo, useState } from 'react';
import RoadWatchApp from './components/RoadWatchApp';
import MapView from './components/MapView';
import { getPotholes, updatePotholeStatus } from './services/api';

const REFRESH_INTERVAL_MS = 15000;

export default function App() {
  const [potholes, setPotholes] = useState([]);
  const [selectedId, setSelectedId] = useState(null);

  const load = useCallback(async () => {
    try {
      const data = await getPotholes();
      setPotholes(data);
    } catch (err) {
      console.error('Failed to load potholes for map', err);
    }
  }, []);

  useEffect(() => {
    load();
    const interval = setInterval(load, REFRESH_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [load]);

  const selectedPothole = useMemo(
    () => potholes.find((p) => p.id === selectedId) || null,
    [potholes, selectedId]
  );

  async function handleStatusChange(id, newStatus) {
    try {
      await updatePotholeStatus(id, newStatus);
      await load();
    } catch (err) {
      console.error('Failed to update pothole status', err);
    }
  }

  return (
    <>
      <RoadWatchApp />
      <section className="map-section">
        <MapView
          potholes={potholes}
          selectedPothole={selectedPothole}
          onStatusChange={handleStatusChange}
        />
      </section>
    </>
  );
}
