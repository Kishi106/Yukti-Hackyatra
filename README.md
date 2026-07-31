# Smart Pothole Detection & Automated Citizen Reporting

> HackYatra 2026 — SW11

## Overview

This project is a full-stack pothole reporting platform for citizens and municipal authorities. Citizens can report potholes from a mobile app, while officials can view and manage reports from a web dashboard.

## Project Structure

```text
backend/                  # Person 1 — deploys to Render
├── src/
│   ├── routes/
│   │   └── potholes.js
│   ├── db.js
│   ├── models/
│   │   └── pothole.js
│   └── server.js
├── migrations/
│   └── 001_create_potholes.sql
├── .env.example
├── package.json
└── render.yaml

mobile-app/               # Persons 2 & 3 — built via EAS, installed as APK
├── App.js
├── src/
│   ├── screens/
│   │   ├── DetectScreen.js
│   │   ├── ReportScreen.js
│   │   └── StatusScreen.js
│   ├── services/
│   │   ├── accelerometer.js
│   │   └── api.js
│   └── config.js
├── app.json
├── .env.example
└── package.json

web-dashboard/            # Person 4 — deploys to Vercel
├── src/
│   ├── components/
│   │   ├── MapView.jsx
│   │   └── FilterBar.jsx
│   ├── services/
│   │   └── api.js
│   ├── App.jsx
│   └── config.js
├── .env.example
└── package.json

SCHEMA.md                 # Shared contract for all teams
README.md
```

## Stack Summary

| Layer | Technology | Deployment |
| --- | --- | --- |
| Backend | Node.js + Express | Render |
| Database | PostgreSQL | Render Postgres or Supabase |
| Mobile App | Expo (React Native) | EAS build as APK |
| Web Dashboard | React + Leaflet.js | Vercel |

## Suggested Deployment Notes

- Backend: deploy the Node.js service from the backend folder to Render.
- Database: use a PostgreSQL instance and set the DATABASE_URL environment value.
- Mobile app: build with Expo/EAS and distribute the APK.
- Web dashboard: deploy the React app from the web-dashboard folder to Vercel.

## Shared Contract

The API and data shape are documented in [SCHEMA.md](SCHEMA.md).

## Getting Started

1. Review [SCHEMA.md](SCHEMA.md) first.
2. Set up the backend and connect it to PostgreSQL.
3. Configure the mobile app and web dashboard to use the backend URL.
4. Run the app locally and test the report flow.

│
├── docs/
│
└── README.md
```

---

# Future Enhancements

* AI-based automatic pothole classification
* Severity estimation
* Duplicate report clustering
* Offline report storage and synchronization
* Push notifications
* Repair crew route optimization
* Rainfall and hotspot prediction
* Citizen reward system for verified reports

---

# Expected Impact

* Faster pothole identification
* Reduced dependence on manual inspections
* Improved reporting accuracy
* Better repair prioritization
* Increased transparency
* Data-driven road maintenance
* Enhanced public safety

---

# Team

**HackYatra 2026**

**Project:** Smart Pothole Detection & Automated Citizen Reporting

---

# License

This project is developed for educational and hackathon purposes. Future licensing can be added as the project evolves.
