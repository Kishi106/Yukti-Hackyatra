import React, { useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';

/* Mirrors the light-government design tokens defined at the top of
   RoadWatchApp.jsx (INK/INK_LO/LINE/GOV/AMBER/GREEN/RED), duplicated here so
   this file stays a standalone component instead of importing internals out
   of RoadWatchApp.jsx. */
const INK = '#12212F';
const INK_LO = '#5B6B7C';
const LINE = '#D8E0EA';
const GOV = '#12518A';
const AMBER = '#B07800';
const GREEN = '#1B7F5A';
const RED = '#C0392B';

const inter = { fontFamily: 'Inter, sans-serif' };

const SEVERITY_COLORS = {
  low: GREEN,
  medium: AMBER,
  high: RED
};

const STATUS_OPTIONS = ['new', 'in_progress', 'fixed'];

const DEFAULT_CENTER = [20.5937, 78.9629]; // India centroid fallback
const DEFAULT_ZOOM = 5;

function markerIcon(severity) {
  const color = SEVERITY_COLORS[severity] || INK_LO;
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
    <div
      style={{
        borderRadius: 12,
        border: `1px solid ${LINE}`,
        background: 'white',
        boxShadow: '0 1px 2px rgba(18,33,47,0.05)',
        overflow: 'hidden',
        height: '100%',
        display: 'flex',
        flexDirection: 'column'
      }}
    >
      <div style={{ padding: '10px 14px', borderBottom: `1px solid ${LINE}` }}>
        <p style={{ ...inter, fontSize: 13, fontWeight: 600, color: INK, margin: 0 }}>Live Map</p>
        <p style={{ ...inter, fontSize: 11, color: INK_LO, margin: '2px 0 0' }}>
          All reported potholes, plotted by location
        </p>
      </div>
      <div style={{ flex: 1, minHeight: 0 }}>
        <MapContainer center={DEFAULT_CENTER} zoom={DEFAULT_ZOOM} className="map-container">
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <FlyToSelected selectedPothole={selectedPothole} />
          {potholes.map((pothole) => (
            <Marker key={pothole.id} position={[pothole.lat, pothole.lng]} icon={markerIcon(pothole.severity)}>
              <Popup>
                <div className="popup-content" style={inter}>
                  <p style={{ color: INK }}><strong>Severity:</strong> {pothole.severity}</p>
                  <p style={{ color: INK }}><strong>Source:</strong> {pothole.source}</p>
                  <p style={{ color: INK }}><strong>Status:</strong> {pothole.status}</p>
                  <p style={{ color: INK }}><strong>Ward:</strong> {pothole.ward || 'Unknown'}</p>
                  <p style={{ color: INK_LO }}><strong>Reported:</strong> {new Date(pothole.created_at).toLocaleString()}</p>
                  {pothole.photo_url && (
                    <img src={pothole.photo_url} alt="Pothole" className="popup-photo" />
                  )}
                  <label className="popup-status-label" style={{ ...inter, color: INK_LO }}>
                    Update status:
                    <select
                      defaultValue={pothole.status}
                      onChange={(e) => onStatusChange(pothole.id, e.target.value)}
                      style={{ borderColor: LINE, color: GOV }}
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
      </div>
    </div>
  );
}
