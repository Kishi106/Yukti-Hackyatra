const DEFAULT_API_BASE_URL = import.meta.env.DEV
  ? 'http://localhost:3000'
  : 'https://pothole-backend.onrender.com';

const rawApiBaseUrl = import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || DEFAULT_API_BASE_URL;

export const API_BASE_URL = rawApiBaseUrl.replace(/\/$/, '');
