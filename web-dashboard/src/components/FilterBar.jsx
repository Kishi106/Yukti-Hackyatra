import React from 'react';

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'new', label: 'New' },
  { value: 'in_progress', label: 'In progress' },
  { value: 'fixed', label: 'Fixed' }
];

export default function FilterBar({ status, ward, wards, onStatusChange, onWardChange }) {
  return (
    <div className="filter-bar">
      <label className="filter-field">
        Status
        <select value={status} onChange={(e) => onStatusChange(e.target.value)}>
          {STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
      </label>

      <label className="filter-field">
        Ward
        <select value={ward} onChange={(e) => onWardChange(e.target.value)}>
          <option value="">All wards</option>
          {wards.map((w) => (
            <option key={w} value={w}>{w}</option>
          ))}
        </select>
      </label>
    </div>
  );
}
