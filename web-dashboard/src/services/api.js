import { API_BASE_URL } from '../config';

async function handleResponse(response) {
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error || `Request failed with status ${response.status}`);
  }
  return response.json();
}

export async function getPotholes(filters = {}) {
  const params = new URLSearchParams();
  if (filters.status) params.set('status', filters.status);
  if (filters.ward) params.set('ward', filters.ward);
  const query = params.toString();

  const response = await fetch(`${API_BASE_URL}/potholes${query ? `?${query}` : ''}`);
  return handleResponse(response);
}

export async function updatePotholeStatus(id, status) {
  const response = await fetch(`${API_BASE_URL}/potholes/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status })
  });
  return handleResponse(response);
}
