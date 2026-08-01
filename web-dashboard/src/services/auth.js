import { API_BASE_URL } from '../config';

const SESSION_KEY = 'gvmc_official_session';

async function handleResponse(response) {
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error || `Request failed with status ${response.status}`);
  }
  return response.json();
}

export async function signup(name, phone, password, role) {
  const response = await fetch(`${API_BASE_URL}/officials/signup`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, phone, password, role })
  });
  return handleResponse(response);
}

export async function login(phone, password) {
  const response = await fetch(`${API_BASE_URL}/officials/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ phone, password })
  });
  return handleResponse(response);
}

export async function validateSession(token) {
  const response = await fetch(`${API_BASE_URL}/officials/me`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  return handleResponse(response);
}

export function getStoredSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function storeSession(session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}
