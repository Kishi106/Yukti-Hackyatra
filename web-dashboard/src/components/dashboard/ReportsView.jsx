import React, { useMemo, useState } from 'react';
import { ArrowUpDown } from 'lucide-react';
import { INK, INK_LO, LINE, inter, STATUS_COLORS, STATUS_LABELS } from '../../theme';

const COLUMNS = [
  { key: 'id', label: 'ID' },
  { key: 'status', label: 'Status' },
  { key: 'severity', label: 'Severity' },
  { key: 'ward', label: 'Ward' },
  { key: 'confidence_score', label: 'Confidence' },
  { key: 'created_at', label: 'Reported' }
];

function shortCode(id) {
  return `PH-${String(id).replace(/-/g, '').slice(0, 6).toUpperCase()}`;
}

export default function ReportsView({ potholes }) {
  const [sortKey, setSortKey] = useState('created_at');
  const [sortDir, setSortDir] = useState('desc');

  const sorted = useMemo(() => {
    const list = [...potholes];
    list.sort((a, b) => {
      let av = a[sortKey];
      let bv = b[sortKey];
      if (sortKey === 'created_at') {
        av = av ? new Date(av).getTime() : 0;
        bv = bv ? new Date(bv).getTime() : 0;
      }
      if (sortKey === 'confidence_score') {
        av = av || 0;
        bv = bv || 0;
      }
      if (typeof av === 'string') av = av.toLowerCase();
      if (typeof bv === 'string') bv = bv.toLowerCase();
      if (av < bv) return sortDir === 'asc' ? -1 : 1;
      if (av > bv) return sortDir === 'asc' ? 1 : -1;
      return 0;
    });
    return list;
  }, [potholes, sortKey, sortDir]);

  function handleSort(key) {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
  }

  return (
    <div
      style={{
        background: 'white',
        border: `1px solid ${LINE}`,
        borderRadius: 12,
        boxShadow: '0 1px 2px rgba(18,33,47,0.05)',
        padding: 14,
        overflow: 'auto'
      }}
    >
      <p style={{ ...inter, fontSize: 14, fontWeight: 600, color: INK, margin: '0 0 12px' }}>
        Reports ({potholes.length})
      </p>
      <table style={{ width: '100%', borderCollapse: 'collapse', ...inter }}>
        <thead>
          <tr style={{ borderBottom: `1px solid ${LINE}` }}>
            {COLUMNS.map((col) => (
              <th
                key={col.key}
                onClick={() => handleSort(col.key === 'id' ? 'id' : col.key)}
                style={{
                  textAlign: 'left',
                  padding: '8px 10px',
                  fontSize: 11,
                  color: INK_LO,
                  cursor: 'pointer',
                  userSelect: 'none',
                  whiteSpace: 'nowrap'
                }}
              >
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                  {col.label}
                  <ArrowUpDown size={10} />
                </span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {sorted.map((p) => (
            <tr key={p.id} style={{ borderBottom: `1px solid ${LINE}` }}>
              <td style={{ padding: '8px 10px', fontSize: 12, color: INK, fontWeight: 600 }}>{shortCode(p.id)}</td>
              <td style={{ padding: '8px 10px', fontSize: 12 }}>
                <span style={{ color: STATUS_COLORS[p.status] || INK_LO, fontWeight: 600 }}>
                  {STATUS_LABELS[p.status] || p.status}
                </span>
              </td>
              <td style={{ padding: '8px 10px', fontSize: 12, color: INK, textTransform: 'capitalize' }}>{p.severity}</td>
              <td style={{ padding: '8px 10px', fontSize: 12, color: INK }}>{p.ward || '—'}</td>
              <td style={{ padding: '8px 10px', fontSize: 12, color: INK }}>{p.confidence_score ?? 0}%</td>
              <td style={{ padding: '8px 10px', fontSize: 12, color: INK_LO }}>
                {p.created_at ? new Date(p.created_at).toLocaleString() : '—'}
              </td>
            </tr>
          ))}
          {sorted.length === 0 && (
            <tr>
              <td colSpan={COLUMNS.length} style={{ padding: '18px 10px', textAlign: 'center', fontSize: 12, color: INK_LO }}>
                No reports in scope.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
