import React, { useMemo, useState } from 'react';
import { INK, INK_LO, LINE, inter, STATUS_COLORS, STATUS_LABELS } from '../../theme';

const STATUS_ORDER = ['new', 'in_progress', 'fixed'];

const PERIOD_OPTIONS = [
  { value: 'week', label: 'This Week' },
  { value: 'month', label: 'This Month' },
  { value: 'all', label: 'All Time' }
];

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
  return null;
}

export default function StatusOverview({ potholes }) {
  const [period, setPeriod] = useState('all');

  const scoped = useMemo(() => {
    const start = periodStart(period);
    if (!start) return potholes;
    return potholes.filter((p) => p.created_at && new Date(p.created_at) >= start);
  }, [potholes, period]);

  const breakdown = useMemo(() => {
    const total = scoped.length;
    return STATUS_ORDER.map((status) => {
      const count = scoped.filter((p) => p.status === status).length;
      const pct = total ? Math.round((count / total) * 100) : 0;
      return { status, count, pct };
    });
  }, [scoped]);

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
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <p style={{ ...inter, fontSize: 13, fontWeight: 600, color: INK, margin: 0 }}>Status Overview</p>
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
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 10 }}>
        {breakdown.map(({ status, count, pct }) => (
          <div key={status}>
            <p style={{ ...inter, fontSize: 18, fontWeight: 700, color: INK, margin: 0 }}>{pct}%</p>
            <div style={{ height: 6, borderRadius: 999, background: '#EEF2F7', overflow: 'hidden', margin: '6px 0' }}>
              <div
                style={{
                  height: '100%',
                  width: `${pct}%`,
                  background: STATUS_COLORS[status],
                  borderRadius: 999,
                  transition: 'width 0.3s ease'
                }}
              />
            </div>
            <p style={{ ...inter, fontSize: 10.5, color: INK_LO, margin: 0 }}>{STATUS_LABELS[status]}</p>
            <p style={{ ...inter, fontSize: 9.5, color: INK_LO, margin: '1px 0 0' }}>{count} report{count === 1 ? '' : 's'}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
