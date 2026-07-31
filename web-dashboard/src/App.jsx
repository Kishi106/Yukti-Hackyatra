import React, { useCallback, useEffect, useMemo, useState } from 'react';
import MapView from './components/MapView';
import FilterBar from './components/FilterBar';
import { getPotholes, updatePotholeStatus } from './services/api';

const REFRESH_INTERVAL_MS = 15000;

export default function App() {
  const [potholes, setPotholes] = useState([]);
  const [wards, setWards] = useState([]);
  const [status, setStatus] = useState('');
  const [ward, setWard] = useState('');
  const [selectedId, setSelectedId] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      const data = await getPotholes({ status, ward });
      setPotholes(data);
      setWards((prev) => {
        const merged = new Set(prev);
        data.forEach((p) => {
          if (p.ward) merged.add(p.ward);
        });
        return Array.from(merged).sort();
      });
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [status, ward]);

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
      setError(err.message);
    }
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>Smart Pothole Dashboard</h1>
        <p className="subtitle">Live view of citizen and auto-detected pothole reports</p>
      </header>

      <FilterBar
        status={status}
        ward={ward}
        wards={wards}
        onStatusChange={setStatus}
        onWardChange={setWard}
      />

      {error && <p className="error-banner">{error}</p>}
      {loading && <p className="loading-banner">Loading reports...</p>}

      <main className="dashboard-layout">
        <section className="map-section">
          <MapView
            potholes={potholes}
            selectedPothole={selectedPothole}
            onStatusChange={handleStatusChange}
          />
        </section>
        <section className="list-section">
          {potholes.length === 0 ? (
            <p className="list-empty">No pothole reports match the current filters.</p>
          ) : (
            <ul className="pothole-list">
              {potholes.map((pothole) => (
                <li
                  key={pothole.id}
                  className={`pothole-row severity-${pothole.severity} ${pothole.id === selectedId ? 'selected' : ''}`}
                  onClick={() => setSelectedId(pothole.id)}
                >
                  <div className="pothole-row-top">
                    <span className="pothole-severity">{pothole.severity}</span>
                    <span className={`pothole-status status-${pothole.status}`}>{pothole.status}</span>
                  </div>
                  <div className="pothole-row-bottom">
                    <span>{pothole.ward || 'Unknown ward'}</span>
                    <span>{new Date(pothole.created_at).toLocaleString()}</span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  );
}
