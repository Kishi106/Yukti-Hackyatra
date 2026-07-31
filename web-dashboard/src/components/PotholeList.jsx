import React from 'react';

export default function PotholeList({ potholes, selectedId, onSelect }) {
  if (potholes.length === 0) {
    return <p className="list-empty">No pothole reports match the current filters.</p>;
  }

  return (
    <ul className="pothole-list">
      {potholes.map((pothole) => (
        <li
          key={pothole.id}
          className={`pothole-row severity-${pothole.severity} ${pothole.id === selectedId ? 'selected' : ''}`}
          onClick={() => onSelect(pothole)}
        >
          <div className="pothole-row-top">
            <span className="pothole-severity">{pothole.severity}</span>
            <span className={`pothole-status status-${pothole.status}`}>{pothole.status}</span>
          </div>
          <div className="pothole-row-bottom">
            <span>{pothole.ward || 'Unknown ward'}</span>
            <span>{new Date(pothole.created_at).toLocaleString()}</span>
          </div>
        </li>
      ))}
    </ul>
  );
}
