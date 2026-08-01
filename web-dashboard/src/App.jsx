import React, { useEffect, useState } from 'react';
import AuthPage from './pages/AuthPage';
import WardPickerPage from './pages/WardPickerPage';
import Dashboard from './components/Dashboard';
import { getStoredSession, validateSession, clearSession } from './services/auth';

export default function App() {
  const [session, setSession] = useState(undefined); // undefined = still checking
  const [ward, setWard] = useState(null); // { wardNo, wardLabel } | null — React state only, resets on refresh

  useEffect(() => {
    const stored = getStoredSession();
    if (!stored) {
      setSession(null);
      return;
    }
    // Trust the stored session immediately so refreshes don't force a re-login;
    // only clear it if the backend actively rejects the token.
    setSession(stored);
    validateSession(stored.token).catch(() => {
      clearSession();
      setSession(null);
    });
  }, []);

  function handleLogout() {
    clearSession();
    setSession(null);
    setWard(null);
  }

  if (session === undefined) {
    return null;
  }

  if (!session) {
    return <AuthPage onAuthenticated={setSession} />;
  }

  if (session.role === 'field_officer' && !ward) {
    return (
      <WardPickerPage
        officialName={session.name}
        onWardSelected={(wardNo, wardName) => setWard({ wardNo, wardLabel: `Ward ${wardNo} — ${wardName}` })}
        onLogout={handleLogout}
      />
    );
  }

  return (
    <Dashboard
      official={session}
      wardNo={session.role === 'field_officer' ? ward.wardNo : null}
      wardLabel={session.role === 'field_officer' ? ward.wardLabel : null}
      onChangeWard={session.role === 'field_officer' ? () => setWard(null) : null}
      onLogout={handleLogout}
    />
  );
}
