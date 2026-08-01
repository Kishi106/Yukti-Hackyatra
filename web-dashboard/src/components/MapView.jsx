import React, { useEffect, useRef, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, GeoJSON, useMap } from 'react-leaflet';
import L from 'leaflet';
import { searchWards, getWardBoundary } from '../services/api';

/* Mirrors the light-government design tokens in src/theme.js
   (INK/INK_LO/LINE/GOV/AMBER/GREEN/RED), duplicated here so this file stays a
   standalone component instead of importing internals from theme.js. */
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

const WARD_BOUNDARY_STYLE = {
  color: GOV,
  weight: 3,
  fillColor: GOV,
  fillOpacity: 0.12
};

const SEARCH_DEBOUNCE_MS = 300;
// Gives flyTo time to finish its pan/zoom animation before the popup opens.
const POPUP_OPEN_DELAY_MS = 800;

function markerIcon(severity) {
  const color = SEVERITY_COLORS[severity] || INK_LO;
  return L.divIcon({
    className: 'pothole-marker',
    html: `<span style="background:${color}" class="pothole-marker-dot"></span>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8]
  });
}

function FlyToSelected({ selectedPothole, markerRefs }) {
  const map = useMap();
  useEffect(() => {
    if (!selectedPothole) return undefined;
    map.flyTo([selectedPothole.lat, selectedPothole.lng], 16, { duration: 0.75 });
    const timer = setTimeout(() => {
      markerRefs.current[selectedPothole.id]?.openPopup();
    }, POPUP_OPEN_DELAY_MS);
    return () => clearTimeout(timer);
  }, [selectedPothole, map, markerRefs]);
  return null;
}

function FitToBoundary({ boundary }) {
  const map = useMap();
  useEffect(() => {
    if (!boundary) return;
    try {
      const bounds = L.geoJSON(boundary).getBounds();
      if (bounds.isValid()) {
        map.fitBounds(bounds, { padding: [24, 24] });
      }
    } catch (err) {
      console.error('Failed to fit map to ward boundary', err);
    }
  }, [boundary, map]);
  return null;
}

export default function MapView({ potholes, selectedPothole, onStatusChange, selectedWardNo }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [highlightedWardNo, setHighlightedWardNo] = useState(null);
  const [boundary, setBoundary] = useState(null);
  const debounceRef = useRef(null);
  const markerRefs = useRef({});

  // Let an external ward selection (e.g. the "Assigned ward" gate, or the
  // sidebar's "locate" button) drive the same highlight this component's own
  // search does.
  useEffect(() => {
    if (selectedWardNo != null) {
      setHighlightedWardNo(selectedWardNo);
    }
  }, [selectedWardNo]);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (!query.trim()) {
      setResults([]);
      return undefined;
    }
    debounceRef.current = setTimeout(async () => {
      try {
        const data = await searchWards(query.trim());
        setResults(data);
      } catch (err) {
        console.error('Ward search failed', err);
        setResults([]);
      }
    }, SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(debounceRef.current);
  }, [query]);

  useEffect(() => {
    if (highlightedWardNo == null) {
      setBoundary(null);
      return undefined;
    }
    let cancelled = false;
    getWardBoundary(highlightedWardNo)
      .then((feature) => {
        if (!cancelled) setBoundary(feature);
      })
      .catch((err) => {
        console.error('Failed to load ward boundary', err);
        if (!cancelled) setBoundary(null);
      });
    return () => {
      cancelled = true;
    };
  }, [highlightedWardNo]);

  function handleSelectWard(ward) {
    setHighlightedWardNo(ward.ward_no);
    setQuery(ward.ward_name);
    setResults([]);
  }

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
        <div style={{ marginTop: 8, position: 'relative' }}>
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search ward…"
            style={{
              ...inter,
              width: '100%',
              padding: '6px 10px',
              fontSize: 12,
              borderRadius: 6,
              border: `1px solid ${LINE}`,
              color: INK,
              boxSizing: 'border-box'
            }}
          />
          {results.length > 0 && (
            <ul
              style={{
                ...inter,
                listStyle: 'none',
                margin: '4px 0 0',
                padding: 4,
                position: 'absolute',
                top: '100%',
                left: 0,
                right: 0,
                background: 'white',
                border: `1px solid ${LINE}`,
                borderRadius: 6,
                boxShadow: '0 4px 10px rgba(18,33,47,0.12)',
                zIndex: 1000,
                maxHeight: 200,
                overflowY: 'auto'
              }}
            >
              {results.map((ward) => (
                <li
                  key={ward.ward_no}
                  onClick={() => handleSelectWard(ward)}
                  style={{ padding: '6px 8px', fontSize: 12, color: INK, cursor: 'pointer', borderRadius: 4 }}
                >
                  Ward {ward.ward_no} — {ward.ward_name}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
      <div style={{ flex: 1, minHeight: 0 }}>
        <MapContainer center={DEFAULT_CENTER} zoom={DEFAULT_ZOOM} className="map-container">
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <FlyToSelected selectedPothole={selectedPothole} markerRefs={markerRefs} />
          {boundary && (
            <>
              <GeoJSON key={highlightedWardNo} data={boundary} style={() => WARD_BOUNDARY_STYLE} />
              <FitToBoundary boundary={boundary} />
            </>
          )}
          {potholes.map((pothole) => (
            <Marker
              key={pothole.id}
              position={[pothole.lat, pothole.lng]}
              icon={markerIcon(pothole.severity)}
              ref={(el) => {
                if (el) markerRefs.current[pothole.id] = el;
              }}
            >
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
