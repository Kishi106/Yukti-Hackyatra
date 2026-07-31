import React from 'react';
import MapView from './components/MapView';
import FilterBar from './components/FilterBar';

export default function App() {
  return (
    <div>
      <h1>Smart Pothole Dashboard</h1>
      <FilterBar />
      <MapView />
    </div>
  );
}
