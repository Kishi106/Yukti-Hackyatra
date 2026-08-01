import React, { useEffect, useMemo, useState } from 'react';
import { getWardBoundaries, getWardBoundary } from '../../services/api';
import { INK, INK_LO, LINE, GOV, inter } from '../../theme';

export default function WardsView({ potholes, wardNo }) {
  const [wards, setWards] = useState([]);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        if (wardNo != null) {
          const feature = await getWardBoundary(wardNo);
          if (!cancelled) setWards([feature.properties]);
        } else {
          const collection = await getWardBoundaries();
          if (!cancelled) setWards(collection.features.map((f) => f.properties));
        }
      } catch (err) {
        console.error('Failed to load wards', err);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [wardNo]);

  const counts = useMemo(() => {
    const map = {};
    potholes.forEach((p) => {
      if (p.ward_no == null) return;
      map[p.ward_no] = (map[p.ward_no] || 0) + 1;
    });
    return map;
  }, [potholes]);

  const rows = useMemo(
    () =>
      [...wards]
        .map((w) => ({ ...w, count: counts[w.ward_no] || 0 }))
        .sort((a, b) => b.count - a.count),
    [wards, counts]
  );

  return (
    <div
      style={{
        background: 'white',
        border: `1px solid ${LINE}`,
        borderRadius: 12,
        boxShadow: '0 1px 2px rgba(18,33,47,0.05)',
        padding: 14
      }}
    >
      <p style={{ ...inter, fontSize: 14, fontWeight: 600, color: INK, margin: '0 0 12px' }}>
        Wards in scope ({rows.length})
      </p>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 10 }}>
        {rows.map((w) => (
          <div key={w.ward_no} style={{ border: `1px solid ${LINE}`, borderRadius: 10, padding: 12 }}>
            <p style={{ ...inter, fontSize: 12.5, fontWeight: 700, color: GOV, margin: 0 }}>Ward {w.ward_no}</p>
            <p style={{ ...inter, fontSize: 11.5, color: INK, margin: '3px 0 0' }}>{w.ward_name}</p>
            <p style={{ ...inter, fontSize: 20, fontWeight: 700, color: INK, margin: '8px 0 0' }}>{w.count}</p>
            <p style={{ ...inter, fontSize: 10, color: INK_LO, margin: '2px 0 0' }}>pothole{w.count === 1 ? '' : 's'}</p>
          </div>
        ))}
        {rows.length === 0 && (
          <p style={{ ...inter, fontSize: 12, color: INK_LO, gridColumn: '1 / -1' }}>No wards to show.</p>
        )}
      </div>
    </div>
  );
}
