import React, { useState } from 'react';
import { LayoutDashboard, List, Map, BarChart3, Bell, ChevronDown, LogOut } from 'lucide-react';
import { INK, INK_LO, LINE, GOV, ORANGE, inter, display, ROLE_LABELS } from '../../theme';

const NAV_ITEMS = [
  { key: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { key: 'reports', label: 'Reports', icon: List },
  { key: 'wards', label: 'Wards', icon: Map },
  { key: 'analytics', label: 'Analytics', icon: BarChart3 }
];

export default function Header({ official, wardLabel, onChangeWard, onLogout, activeView, onChangeView }) {
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <header
      style={{
        background: 'white',
        borderBottom: `1px solid ${LINE}`,
        padding: '10px 20px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 16,
        flexWrap: 'wrap'
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
        <img src="/logo.png" alt="GVMC logo" style={{ width: 34, height: 34, objectFit: 'contain', borderRadius: 6 }} />
        <div style={{ minWidth: 0 }}>
          <p style={{ ...display, fontSize: 17, color: INK, margin: 0, lineHeight: 1.1, whiteSpace: 'nowrap' }}>
            S.P.O.T — GVMC Roads &amp; Buildings
          </p>
          {wardLabel && (
            <p style={{ ...inter, fontSize: 10.5, color: INK_LO, margin: '2px 0 0' }}>
              Scoped to {wardLabel}
              {onChangeWard && (
                <button
                  type="button"
                  onClick={onChangeWard}
                  style={{
                    ...inter,
                    marginLeft: 8,
                    fontSize: 10.5,
                    color: GOV,
                    background: 'none',
                    border: 'none',
                    padding: 0,
                    cursor: 'pointer',
                    textDecoration: 'underline'
                  }}
                >
                  Change ward
                </button>
              )}
            </p>
          )}
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
        {NAV_ITEMS.map(({ key, label, icon: Icon }) => {
          const active = activeView === key;
          return (
            <button
              key={key}
              type="button"
              title={label}
              onClick={() => onChangeView?.(key)}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 2,
                padding: '5px 10px 6px',
                borderRadius: 9,
                border: 'none',
                cursor: 'pointer',
                background: active ? 'rgba(242,96,61,0.1)' : 'transparent',
                color: active ? ORANGE : INK_LO
              }}
            >
              <Icon size={16} />
              <span
                style={{
                  ...inter,
                  fontSize: 9,
                  fontWeight: 600,
                  borderBottom: active ? `2px solid ${ORANGE}` : '2px solid transparent',
                  paddingBottom: 1
                }}
              >
                {label}
              </span>
            </button>
          );
        })}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
        <span
          style={{
            width: 34,
            height: 34,
            borderRadius: 9,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: INK_LO,
            position: 'relative'
          }}
        >
          <Bell size={17} />
        </span>

        <div style={{ position: 'relative' }}>
          <button
            type="button"
            onClick={() => setMenuOpen((v) => !v)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              padding: 0
            }}
          >
            <span
              style={{
                width: 32,
                height: 32,
                borderRadius: '50%',
                background: GOV,
                color: 'white',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 13,
                fontWeight: 700,
                ...inter
              }}
            >
              {(official?.name || '?').slice(0, 1).toUpperCase()}
            </span>
            <span style={{ textAlign: 'left' }}>
              <p style={{ ...inter, fontSize: 12.5, fontWeight: 600, color: INK, margin: 0, lineHeight: 1.2 }}>
                {official?.name}
              </p>
              <p style={{ ...inter, fontSize: 10.5, color: INK_LO, margin: 0 }}>
                {ROLE_LABELS[official?.role] || official?.role}
              </p>
            </span>
            <ChevronDown size={14} color={INK_LO} />
          </button>

          {menuOpen && (
            <div
              style={{
                position: 'absolute',
                right: 0,
                top: '100%',
                marginTop: 8,
                background: 'white',
                border: `1px solid ${LINE}`,
                borderRadius: 10,
                boxShadow: '0 8px 20px rgba(18,33,47,0.12)',
                minWidth: 160,
                zIndex: 1100,
                overflow: 'hidden'
              }}
            >
              <button
                type="button"
                onClick={() => {
                  setMenuOpen(false);
                  onLogout();
                }}
                style={{
                  ...inter,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  width: '100%',
                  padding: '10px 14px',
                  border: 'none',
                  background: 'white',
                  color: '#C0392B',
                  fontSize: 13,
                  cursor: 'pointer'
                }}
              >
                <LogOut size={14} /> Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
