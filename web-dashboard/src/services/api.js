const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:3000';

export const fetchPotholes = async () => {
  const response = await fetch(`${API_BASE_URL}/potholes`);
  return response.json();
};
