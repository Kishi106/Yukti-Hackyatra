import React, { useMemo, useState } from 'react';
import { BarChart, Bar, Cell, XAxis, YAxis, CartesianGrid, Tooltip, LabelList, ResponsiveContainer } from 'recharts';
import { INK, INK_LO, LINE, ORANGE, MUTED_BAR, inter, STATUS_COLORS, STATUS_LABELS } from '../../theme';

const WEEKS_TO_SHOW = 8;
const MONTHS_TO_SHOW = 6;
const LEGEND_STATUSES = ['new', 'in_progress', 'fixed'];

function startOfWeek(date) {
  const d = new Date(date);
  const day = d.getDay();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() - day);
  return d;
}

function buildBuckets(potholes, period) {
  const now = new Date();
  const buckets = [];

  if (period === 'week') {
    for (let i = WEEKS_TO_SHOW - 1; i >= 0; i--) {
      const start = startOfWeek(now);
      start.setDate(start.getDate() - i * 7);
      buckets.push({
        key: start.toISOString().slice(0, 10),
        label: start.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }),
        start,
        end: new Date(start.getTime() + 7 * 86400000),
        total: 0
      });
    }
  } else {
    for (let i = MONTHS_TO_SHOW - 1; i >= 0; i--) {
      const start = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const end = new Date(now.getFullYear(), now.getMonth() - i + 1, 1);
      buckets.push({
        key: start.toISOString().slice(0, 7),
        label: start.toLocaleDateString(undefined, { month: 'short', year: '2-digit' }),
        start,
        end,
        total: 0
      });
    }
  }

  potholes.forEach((p) => {
    if (!p.created_at) return;
    const created = new Date(p.created_at);
    const bucket = buckets.find((b) => created >= b.start && created < b.end);
    if (bucket) bucket.total += 1;
  });

  return buckets;
}

function CalloutLabel({ x, y, width, value, index, dataLength }) {
  if (index !== dataLength - 1 || !value) return null;
  const cx = x + width / 2;
  const bubbleY = y - 26;
  return (
    <g>
      <rect x={cx - 16} y={bubbleY} width={32} height={20} rx={6} fill={ORANGE} />
      <polygon points={`${cx - 4},${bubbleY + 20} ${cx + 4},${bubbleY + 20} ${cx},${bubbleY + 26}`} fill={ORANGE} />
      <text x={cx} y={bubbleY + 14} textAnchor="middle" fontSize={11} fontWeight={700} fill="white">
        {value}
      </text>
    </g>
  );
}

export default function ResolutionChart({ potholes }) {
  const [period, setPeriod] = useState('week');
  const data = useMemo(() => buildBuckets(potholes, period), [potholes, period]);

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
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4, flexWrap: 'wrap', gap: 8 }}>
        <p style={{ ...inter, fontSize: 13, fontWeight: 600, color: INK, margin: 0 }}>Resolution Status</p>
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
          <option value="week">Week</option>
          <option value="month">Month</option>
        </select>
      </div>

      <div style={{ display: 'flex', gap: 12, marginBottom: 10 }}>
        {LEGEND_STATUSES.map((status) => (
          <span key={status} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <span style={{ width: 8, height: 8, borderRadius: 2, background: STATUS_COLORS[status], display: 'inline-block' }} />
            <span style={{ ...inter, fontSize: 10.5, color: INK_LO }}>{STATUS_LABELS[status]}</span>
          </span>
        ))}
      </div>

      <div style={{ width: '100%', height: 200 }}>
        <ResponsiveContainer>
          <BarChart data={data} margin={{ top: 30, right: 4, left: -20, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke={LINE} vertical={false} />
            <XAxis dataKey="label" tick={{ fontSize: 10, fill: INK_LO }} axisLine={{ stroke: LINE }} tickLine={false} />
            <YAxis tick={{ fontSize: 10, fill: INK_LO }} axisLine={false} tickLine={false} allowDecimals={false} />
            <Tooltip
              contentStyle={{ ...inter, fontSize: 11.5, borderRadius: 8, border: `1px solid ${LINE}` }}
              labelStyle={{ color: INK }}
            />
            <Bar dataKey="total" radius={[4, 4, 0, 0]}>
              {data.map((entry, index) => (
                <Cell key={entry.key} fill={index === data.length - 1 ? ORANGE : MUTED_BAR} />
              ))}
              <LabelList
                dataKey="total"
                content={(props) => <CalloutLabel {...props} dataLength={data.length} />}
              />
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
