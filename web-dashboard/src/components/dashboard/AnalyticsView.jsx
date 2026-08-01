import React from 'react';
import StatusOverview from './StatusOverview';
import ResolutionChart from './ResolutionChart';
import WardDensityMap from './WardDensityMap';

export default function AnalyticsView({ potholes }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <StatusOverview potholes={potholes} />
      <ResolutionChart potholes={potholes} />
      <WardDensityMap potholes={potholes} />
    </div>
  );
}
