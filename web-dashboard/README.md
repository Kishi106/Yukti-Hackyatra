# Pothole Dashboard (Web)

React + Vite dashboard for government officials to monitor pothole reports on a Leaflet map.

## Setup

```bash
npm install
```

Set the backend URL in `src/config.js`:

```js
export const API_BASE_URL = 'https://your-backend.onrender.com';
```

## Run locally

```bash
npm run dev
```

## Build

```bash
npm run build
```

## Deploy to Vercel

1. Push this folder (or the monorepo) to GitHub.
2. In Vercel, create a new project pointing at `web-dashboard` as the root directory.
3. Framework preset: Vite. Build command: `npm run build`. Output directory: `dist`.
4. Update `src/config.js` with the deployed backend URL before building/deploying.
