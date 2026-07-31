import { API_BASE_URL } from '../config';

async function handleResponse(response) {
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error || `Request failed with status ${response.status}`);
  }
  return response.json();
}

export async function getPotholes() {
  const response = await fetch(`${API_BASE_URL}/potholes`);
  return handleResponse(response);
}

export async function submitReport(data) {
  const response = await fetch(`${API_BASE_URL}/potholes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  return handleResponse(response);
}
