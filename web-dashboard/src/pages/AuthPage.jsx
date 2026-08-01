import React, { useState } from 'react';
import { signup, login, storeSession } from '../services/auth';
import { INK, INK_LO, LINE, GOV, RED, inter, display } from '../theme';

const ROLE_OPTIONS = [
  { value: 'field_officer', label: 'Field Officer' },
  { value: 'commissioner_analyst', label: "Commissioner's Office Analyst" }
];

function Field({ label, children }) {
  return (
    <label style={{ display: 'block', marginBottom: 14 }}>
      <span style={{ ...inter, fontSize: 12, fontWeight: 600, color: INK_LO, display: 'block', marginBottom: 6 }}>
        {label}
      </span>
      {children}
    </label>
  );
}

const inputStyle = {
  ...inter,
  width: '100%',
  padding: '10px 12px',
  fontSize: 14,
  borderRadius: 8,
  border: `1px solid ${LINE}`,
  color: INK,
  boxSizing: 'border-box'
};

export default function AuthPage({ onAuthenticated }) {
  const [mode, setMode] = useState('login');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState(ROLE_OPTIONS[0].value);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const session =
        mode === 'login' ? await login(phone, password) : await signup(name, phone, password, role);
      storeSession(session);
      onAuthenticated(session);
    } catch (err) {
      setError(err.message || 'Something went wrong');
    } finally {
      setSubmitting(false);
    }
  }

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
          maxWidth: 420,
          background: 'white',
          borderRadius: 16,
          border: `1px solid ${LINE}`,
          boxShadow: '0 4px 24px rgba(18,33,47,0.08)',
          padding: '32px 32px 28px'
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: 22 }}>
          <img src="/logo.png" alt="GVMC logo" style={{ width: 56, height: 56, objectFit: 'contain', marginBottom: 10 }} />
          <h1 style={{ ...display, fontSize: 22, color: INK, margin: 0, textAlign: 'center' }}>
            S.P.O.T — GVMC Roads &amp; Buildings
          </h1>
          <p style={{ ...inter, fontSize: 12, color: INK_LO, marginTop: 4, textAlign: 'center' }}>
            Staff sign-in for field officers &amp; commissioner analysts
          </p>
        </div>

        <div
          style={{
            display: 'flex',
            gap: 4,
            padding: 4,
            borderRadius: 10,
            background: '#EEF2F7',
            marginBottom: 20
          }}
        >
          {[
            ['login', 'Log in'],
            ['signup', 'Sign up']
          ].map(([key, label]) => (
            <button
              key={key}
              type="button"
              onClick={() => {
                setMode(key);
                setError('');
              }}
              style={{
                ...inter,
                flex: 1,
                padding: '8px 0',
                borderRadius: 8,
                border: 'none',
                fontSize: 13,
                fontWeight: 600,
                cursor: 'pointer',
                background: mode === key ? 'white' : 'transparent',
                color: mode === key ? GOV : INK_LO,
                boxShadow: mode === key ? '0 1px 2px rgba(18,33,47,.08)' : 'none'
              }}
            >
              {label}
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit}>
          {mode === 'signup' && (
            <Field label="Full name">
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                style={inputStyle}
                required
              />
            </Field>
          )}
          <Field label="Phone number">
            <input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              style={inputStyle}
              required
            />
          </Field>
          <Field label="Password">
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={inputStyle}
              minLength={6}
              required
            />
          </Field>
          {mode === 'signup' && (
            <Field label="Role">
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {ROLE_OPTIONS.map((opt) => (
                  <label
                    key={opt.value}
                    style={{
                      ...inter,
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                      fontSize: 13,
                      color: INK,
                      border: `1px solid ${role === opt.value ? GOV : LINE}`,
                      background: role === opt.value ? '#E7F0FA' : 'white',
                      borderRadius: 8,
                      padding: '8px 10px',
                      cursor: 'pointer'
                    }}
                  >
                    <input
                      type="radio"
                      name="role"
                      value={opt.value}
                      checked={role === opt.value}
                      onChange={() => setRole(opt.value)}
                    />
                    {opt.label}
                  </label>
                ))}
              </div>
            </Field>
          )}

          {error && (
            <p style={{ ...inter, fontSize: 12.5, color: RED, margin: '4px 0 12px' }}>{error}</p>
          )}

          <button
            type="submit"
            disabled={submitting}
            style={{
              ...inter,
              width: '100%',
              padding: '11px 0',
              marginTop: 6,
              borderRadius: 8,
              border: 'none',
              fontSize: 14,
              fontWeight: 600,
              color: 'white',
              background: GOV,
              cursor: submitting ? 'default' : 'pointer',
              opacity: submitting ? 0.7 : 1
            }}
          >
            {submitting ? 'Please wait…' : mode === 'login' ? 'Log in' : 'Create account'}
          </button>
        </form>
      </div>
    </div>
  );
}
