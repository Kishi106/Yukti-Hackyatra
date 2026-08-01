import React, { useEffect, useRef, useState } from 'react';
import { searchWards } from '../services/api';
import { INK, INK_LO, LINE, GOV, inter, display } from '../theme';

const SEARCH_DEBOUNCE_MS = 300;

export default function WardPickerPage({ officialName, onWardSelected, onLogout }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const debounceRef = useRef(null);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (!query.trim()) {
      setResults([]);
      return undefined;
    }
    setLoading(true);
    debounceRef.current = setTimeout(async () => {
      try {
        const data = await searchWards(query.trim());
        setResults(data);
      } catch (err) {
        console.error('Ward search failed', err);
        setResults([]);
      } finally {
        setLoading(false);
      }
    }, SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(debounceRef.current);
  }, [query]);

  return (
    <div
      style={{
        minHeight: '100vh',
        width: '100%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#F4F6F9',
        padding: 16
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: 460,
          background: 'white',
          borderRadius: 16,
          border: `1px solid ${LINE}`,
          boxShadow: '0 4px 24px rgba(18,33,47,0.08)',
          padding: '32px 32px 28px'
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: 20 }}>
          <img src="/logo.png" alt="GVMC logo" style={{ width: 48, height: 48, objectFit: 'contain', marginBottom: 8 }} />
          <h1 style={{ ...display, fontSize: 20, color: INK, margin: 0, textAlign: 'center' }}>
            Select your ward
          </h1>
          <p style={{ ...inter, fontSize: 12, color: INK_LO, marginTop: 4, textAlign: 'center' }}>
            {officialName ? `Welcome, ${officialName}. ` : ''}
            Choose the ward you're assigned to for this session.
          </p>
        </div>

        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search ward by name…"
          style={{
            ...inter,
            width: '100%',
            padding: '10px 12px',
            fontSize: 14,
            borderRadius: 8,
            border: `1px solid ${LINE}`,
            color: INK,
            boxSizing: 'border-box'
          }}
        />

        <div style={{ marginTop: 12, maxHeight: 280, overflowY: 'auto' }}>
          {loading && (
            <p style={{ ...inter, fontSize: 12.5, color: INK_LO, padding: '8px 2px' }}>Searching…</p>
          )}
          {!loading && query.trim() && results.length === 0 && (
            <p style={{ ...inter, fontSize: 12.5, color: INK_LO, padding: '8px 2px' }}>No wards match.</p>
          )}
          {results.map((ward) => (
            <button
              key={ward.ward_no}
              onClick={() => onWardSelected(ward.ward_no, ward.ward_name)}
              style={{
                ...inter,
                display: 'block',
                width: '100%',
                textAlign: 'left',
                padding: '10px 12px',
                marginBottom: 6,
                borderRadius: 8,
                border: `1px solid ${LINE}`,
                background: 'white',
                cursor: 'pointer',
                fontSize: 13.5,
                color: INK
              }}
              onMouseEnter={(e) => (e.currentTarget.style.background = '#F2F7FC')}
              onMouseLeave={(e) => (e.currentTarget.style.background = 'white')}
            >
              <span style={{ fontWeight: 600, color: GOV }}>Ward {ward.ward_no}</span>
              {'  '}— {ward.ward_name}
            </button>
          ))}
        </div>

        <button
          type="button"
          onClick={onLogout}
          style={{
            ...inter,
            marginTop: 18,
            width: '100%',
            padding: '9px 0',
            borderRadius: 8,
            border: `1px solid ${LINE}`,
            background: 'white',
            color: INK_LO,
            fontSize: 12.5,
            cursor: 'pointer'
          }}
        >
          Log out
        </button>
      </div>
    </div>
  );
}
