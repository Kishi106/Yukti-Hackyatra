import React from 'react';
import { ClipboardList, Layers, Gauge } from 'lucide-react';
import { INK, INK_LO, LINE, GOV, inter, mono } from '../../theme';

function StatCard({ icon: Icon, label, value }) {
  return (
    <div
      style={{
        background: 'white',
        border: `1px solid ${LINE}`,
        borderRadius: 12,
        boxShadow: '0 1px 2px rgba(18,33,47,0.05)',
        padding: 14,
        flex: 1,
        minWidth: 0
      }}
    >
      <div
        style={{
          width: 30,
          height: 30,
          borderRadius: 8,
          background: '#E7F0FA',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: 8
        }}
      >
        <Icon size={15} color={GOV} />
      </div>
      <p style={{ ...mono, fontSize: 22, fontWeight: 600, color: INK, margin: 0 }}>{value}</p>
      <p style={{ ...inter, fontSize: 11, color: INK_LO, margin: '2px 0 0' }}>{label}</p>
    </div>
  );
}

export default function StatCards({ totalReports, secondary }) {
  return (
    <div style={{ display: 'flex', gap: 12 }}>
      <StatCard icon={ClipboardList} label="Total Reports" value={totalReports} />
      <StatCard icon={secondary.icon} label={secondary.label} value={secondary.value} />
    </div>
  );
}

export { Layers, Gauge };
