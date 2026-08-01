import React, { useEffect, useMemo, useState } from 'react';
import { MapContainer, TileLayer, GeoJSON } from 'react-leaflet';
import { getWardBoundaries } from '../../services/api';
import { INK, INK_LO, LINE, ORANGE, inter } from '../../theme';

const DEFAULT_CENTER = [17.6868, 83.2185]; // Visakhapatnam
const DEFAULT_ZOOM = 10;

const PERIOD_OPTIONS = [
  { value: 'week', label: 'This Week' },
  { value: 'month', label: 'This Month' },
  { value: 'all', label: 'All Time' }
];

const LIGHT_FILL = [253, 231, 220]; // light tint of --accent-orange
const DARK_FILL = [242, 96, 61]; // --accent-orange

function periodStart(period) {
  const now = new Date();
  if (period === 'week') {
    const start = new Date(now);
    start.setHours(0, 0, 0, 0);
    start.setDate(start.getDate() - start.getDay());
    return start;
  }
  if (period === 'month') {
    return new Date(now.getFullYear(), now.getMonth(), 1);
  }
  return null; // all time
}

function interpolateColor(t) {
  const clamped = Math.max(0, Math.min(1, t));
  const rgb = LIGHT_FILL.map((c, i) => Math.round(c + (DARK_FILL[i] - c) * clamped));
  return `rgb(${rgb.join(',')})`;
}

export default function WardDensityMap({ potholes }) {
  const [period, setPeriod] = useState('week');
  const [boundaries, setBoundaries] = useState(null);

  useEffect(() => {
    let cancelled = false;
    getWardBoundaries()
      .then((collection) => {
        if (!cancelled) setBoundaries(collection);
      })
      .catch((err) => console.error('Failed to load ward boundaries', err));
    return () => {
      cancelled = true;
    };
  }, []);

  const scoped = useMemo(() => {
    const start = periodStart(period);
    if (!start) return potholes;
    return potholes.filter((p) => p.created_at && new Date(p.created_at) >= start);
  }, [potholes, period]);

  const wardCounts = useMemo(() => {
    const counts = {};
    scoped.forEach((p) => {
      if (p.ward_no == null) return;
      counts[p.ward_no] = (counts[p.ward_no] || 0) + 1;
    });
    return counts;
  }, [scoped]);

  const maxCount = useMemo(
    () => Object.values(wardCounts).reduce((max, c) => Math.max(max, c), 0),
    [wardCounts]
  );

  const totalReports = scoped.length;
  const resolutionRate = totalReports
    ? Math.round((scoped.filter((p) => p.status === 'fixed').length / totalReports) * 100)
    : 0;
  const avgConfidence = totalReports
    ? Math.round(scoped.reduce((sum, p) => sum + (p.confidence_score || 0), 0) / totalReports)
    : 0;

  return (
    <div
      style={{
        background: 'white',
        border: `1px solid ${LINE}`,
        borderRadius: 12,
        boxShadow: '0 1px 2px rgba(18,33,47,0.05)',
        overflow: 'hidden'
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 14px 0' }}>
        <p style={{ ...inter, fontSize: 13, fontWeight: 600, color: INK, margin: 0 }}>Ward Density Map</p>
        <select
          value={period}
          onChange={(e) => setPeriod(e.target.value)}
          style={{
            ...inter,
            fontSize: 11.5,
            color: INK,
            border: `1px solid ${LINE}`,
            borderRadius: 8,
            padding: '4px 8px',
            background: 'white'
          }}
        >
          {PERIOD_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>
      </div>

      <div style={{ height: 260, margin: '10px 14px 0', borderRadius: 8, overflow: 'hidden', border: `1px solid ${LINE}` }}>
        <MapContainer center={DEFAULT_CENTER} zoom={DEFAULT_ZOOM} className="density-map-container" zoomControl={false}>
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          {boundaries && (
            <GeoJSON
              key={period}
              data={boundaries}
              style={(feature) => {
                const count = wardCounts[feature.properties.ward_no] || 0;
                return {
                  color: ORANGE,
                  weight: 0.75,
                  fillColor: interpolateColor(maxCount ? count / maxCount : 0),
                  fillOpacity: 0.85
                };
              }}
              onEachFeature={(feature, layer) => {
                const count = wardCounts[feature.properties.ward_no] || 0;
                layer.bindTooltip(
                  `<strong>${feature.properties.ward_name}</strong><br/>${count} pothole${count === 1 ? '' : 's'}`,
                  { sticky: true, className: 'density-tooltip', direction: 'top' }
                );
              }}
            />
          )}
        </MapContainer>
      </div>

      <div style={{ display: 'flex', gap: 10, padding: 14 }}>
        {[
          ['Total Reports', totalReports],
          ['Resolution Rate', `${resolutionRate}%`],
          ['Avg. Confidence Score', `${avgConfidence}%`]
        ].map(([label, value]) => (
          <div
            key={label}
            style={{
              flex: 1,
              minWidth: 0,
              border: `1px solid ${LINE}`,
              borderRadius: 8,
              padding: '8px 10px'
            }}
          >
            <p style={{ ...inter, fontSize: 15, fontWeight: 700, color: INK, margin: 0 }}>{value}</p>
            <p style={{ ...inter, fontSize: 9.5, color: INK_LO, margin: '2px 0 0' }}>{label}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
