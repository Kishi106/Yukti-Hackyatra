import React, { useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';

const SEVERITY_COLORS = {
  low: '#2e7d32',
  medium: '#ef6c00',
  high: '#c62828'
};

const STATUS_OPTIONS = ['new', 'in_progress', 'fixed'];

const DEFAULT_CENTER = [20.5937, 78.9629]; // India centroid fallback
const DEFAULT_ZOOM = 5;

function markerIcon(severity) {
  const color = SEVERITY_COLORS[severity] || '#555';
  return L.divIcon({
    className: 'pothole-marker',
    html: `<span style="background:${color}" class="pothole-marker-dot"></span>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8]
  });
}

function FlyToSelected({ selectedPothole }) {
  const map = useMap();
  useEffect(() => {
    if (selectedPothole) {
      map.flyTo([selectedPothole.lat, selectedPothole.lng], 16, { duration: 0.75 });
    }
  }, [selectedPothole, map]);
  return null;
}

export default function MapView({ potholes, selectedPothole, onStatusChange }) {
  return (
    <MapContainer center={DEFAULT_CENTER} zoom={DEFAULT_ZOOM} className="map-container">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <FlyToSelected selectedPothole={selectedPothole} />
      {potholes.map((pothole) => (
        <Marker key={pothole.id} position={[pothole.lat, pothole.lng]} icon={markerIcon(pothole.severity)}>
          <Popup>
            <div className="popup-content">
              <p><strong>Severity:</strong> {pothole.severity}</p>
              <p><strong>Source:</strong> {pothole.source}</p>
              <p><strong>Status:</strong> {pothole.status}</p>
              <p><strong>Ward:</strong> {pothole.ward || 'Unknown'}</p>
              <p><strong>Reported:</strong> {new Date(pothole.created_at).toLocaleString()}</p>
              {pothole.photo_url && (
                <img src={pothole.photo_url} alt="Pothole" className="popup-photo" />
              )}
              <label className="popup-status-label">
                Update status:
                <select
                  defaultValue={pothole.status}
                  onChange={(e) => onStatusChange(pothole.id, e.target.value)}
                >
                  {STATUS_OPTIONS.map((option) => (
                    <option key={option} value={option}>{option}</option>
                  ))}
                </select>
              </label>
            </div>
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
