const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL || 'http://localhost:3000';

export const fetchPotholes = async () => {
  const response = await fetch(`${API_BASE_URL}/potholes`);
  return response.json();
};

export const createPothole = async (payload) => {
  const response = await fetch(`${API_BASE_URL}/potholes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return response.json();
};
