import React, { useMemo, useState } from 'react';
import { Search, MapPin, LocateFixed } from 'lucide-react';
import { INK, INK_LO, LINE, GOV, ORANGE, inter, STATUS_COLORS, STATUS_LABELS } from '../../theme';

const SLA_DAYS = 14;

function shortCode(id) {
  return `PH-${String(id).replace(/-/g, '').slice(0, 6).toUpperCase()}`;
}

function StatusBadge({ status }) {
  const color = STATUS_COLORS[status] || INK_LO;
  return (
    <span
      style={{
        ...inter,
        fontSize: 10.5,
        fontWeight: 600,
        color,
        background: `${color}1A`,
        border: `1px solid ${color}33`,
        borderRadius: 999,
        padding: '2px 8px',
        whiteSpace: 'nowrap'
      }}
    >
      {STATUS_LABELS[status] || status}
    </span>
  );
}

function AgeProgress({ createdAt }) {
  if (!createdAt) return null;
  const ageDays = Math.max(0, Math.floor((Date.now() - createdAt.getTime()) / 86400000));
  const pct = Math.min(100, (ageDays / SLA_DAYS) * 100);
  const overdue = ageDays >= SLA_DAYS;
  return (
    <div style={{ marginTop: 8 }}>
      <div style={{ height: 5, borderRadius: 999, background: '#EEF2F7', overflow: 'hidden' }}>
        <div
          style={{
            height: '100%',
            width: `${pct}%`,
            background: overdue ? ORANGE : '#9DB4CC',
            borderRadius: 999
          }}
        />
      </div>
      <p style={{ ...inter, fontSize: 9.5, color: overdue ? ORANGE : INK_LO, margin: '3px 0 0' }}>
        Day {ageDays} of {SLA_DAYS}
      </p>
    </div>
  );
}

function PotholeCard({ pothole, expanded, onToggle, onSelect, onLocate }) {
  const createdAt = pothole.created_at ? new Date(pothole.created_at) : null;
  return (
    <div
      style={{
        position: 'relative',
        flexShrink: 0,
        border: `1px solid ${LINE}`,
        borderRadius: 10,
        background: expanded ? '#F7FAFC' : 'white',
        overflow: 'hidden'
      }}
    >
      <button
        type="button"
        onClick={() => {
          onToggle();
          onSelect?.(pothole);
        }}
        style={{
          width: '100%',
          textAlign: 'left',
          background: 'none',
          border: 'none',
          padding: '10px 40px 10px 12px',
          cursor: 'pointer'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
          <span style={{ ...inter, fontSize: 12.5, fontWeight: 700, color: INK }}>{shortCode(pothole.id)}</span>
          <StatusBadge status={pothole.status} />
        </div>
        <p
          style={{
            ...inter,
            fontSize: 11.5,
            color: INK_LO,
            margin: '5px 0 0',
            display: 'flex',
            alignItems: 'center',
            gap: 4
          }}
        >
          <MapPin size={11} /> {pothole.ward || 'Unknown ward'}
        </p>
        <p style={{ ...inter, fontSize: 10.5, color: INK_LO, margin: '3px 0 0' }}>
          {createdAt ? createdAt.toLocaleString() : '—'}
        </p>
        <AgeProgress createdAt={createdAt} />
      </button>

      <button
        type="button"
        title="Locate on map"
        onClick={(e) => {
          e.stopPropagation();
          onLocate?.(pothole);
        }}
        style={{
          position: 'absolute',
          top: 8,
          right: 8,
          width: 26,
          height: 26,
          borderRadius: 8,
          border: `1px solid ${LINE}`,
          background: 'white',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          color: GOV
        }}
      >
        <LocateFixed size={13} />
      </button>

      {expanded && (
        <div style={{ padding: '0 12px 12px', borderTop: `1px solid ${LINE}` }}>
          <div style={{ display: 'flex', gap: 14, marginTop: 10, flexWrap: 'wrap' }}>
            <div>
              <p style={{ ...inter, fontSize: 10, color: INK_LO, margin: 0 }}>Severity</p>
              <p style={{ ...inter, fontSize: 12.5, fontWeight: 600, color: INK, margin: 0, textTransform: 'capitalize' }}>
                {pothole.severity}
              </p>
            </div>
            <div>
              <p style={{ ...inter, fontSize: 10, color: INK_LO, margin: 0 }}>Confidence score</p>
              <p style={{ ...inter, fontSize: 12.5, fontWeight: 600, color: GOV, margin: 0 }}>
                {pothole.confidence_score ?? 0}%
              </p>
            </div>
          </div>
          {pothole.photo_url && (
            <img
              src={pothole.photo_url}
              alt="Pothole"
              style={{ width: '100%', maxHeight: 140, objectFit: 'cover', borderRadius: 8, marginTop: 10 }}
            />
          )}
        </div>
      )}
    </div>
  );
}

export default function Sidebar({ potholes, onSelectPothole, onLocatePothole }) {
  const [query, setQuery] = useState('');
  const [expandedId, setExpandedId] = useState(null);

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return potholes;
    return potholes.filter((p) => {
      return (
        shortCode(p.id).toLowerCase().includes(needle) ||
        (p.ward || '').toLowerCase().includes(needle) ||
        (p.status || '').toLowerCase().includes(needle)
      );
    });
  }, [potholes, query]);

  return (
    <div
      style={{
        background: 'white',
        border: `1px solid ${LINE}`,
        borderRadius: 12,
        boxShadow: '0 1px 2px rgba(18,33,47,0.05)',
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        minHeight: 0
      }}
    >
      <div style={{ padding: 12, borderBottom: `1px solid ${LINE}` }}>
        <div style={{ position: 'relative' }}>
          <Search
            size={14}
            color={INK_LO}
            style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)' }}
          />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search reports…"
            style={{
              ...inter,
              width: '100%',
              padding: '8px 10px 8px 30px',
              fontSize: 12.5,
              borderRadius: 8,
              border: `1px solid ${LINE}`,
              color: INK,
              boxSizing: 'border-box'
            }}
          />
        </div>
      </div>

      <div style={{ flex: 1, minHeight: 0, overflowY: 'auto', padding: 10, display: 'flex', flexDirection: 'column', gap: 8 }}>
        {filtered.map((p) => (
          <PotholeCard
            key={p.id}
            pothole={p}
            expanded={expandedId === p.id}
            onToggle={() => setExpandedId((cur) => (cur === p.id ? null : p.id))}
            onSelect={onSelectPothole}
            onLocate={onLocatePothole}
          />
        ))}
        {filtered.length === 0 && (
          <p style={{ ...inter, fontSize: 12, color: INK_LO, textAlign: 'center', padding: '16px 4px' }}>
            No reports match.
          </p>
        )}
      </div>
    </div>
  );
}
