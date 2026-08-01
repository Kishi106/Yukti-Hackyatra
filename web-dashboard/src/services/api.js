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
  if (filters.ward_no != null) params.set('ward_no', filters.ward_no);
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

export async function searchWards(q) {
  const response = await fetch(`${API_BASE_URL}/wards/search?q=${encodeURIComponent(q)}`);
  return handleResponse(response);
}

export async function getWardBoundary(wardNo) {
  const response = await fetch(`${API_BASE_URL}/wards/boundaries?ward_no=${encodeURIComponent(wardNo)}`);
  return handleResponse(response);
}

// Full FeatureCollection across all wards, used for the choropleth layer.
export async function getWardBoundaries() {
  const response = await fetch(`${API_BASE_URL}/wards/boundaries`);
  return handleResponse(response);
}
