# Smart Pothole Detection & Automated Citizen Reporting

> **HackYatra 2026 — SW11**

A full-stack civic technology platform that enables citizens to report potholes accurately and allows municipal authorities to verify, prioritize, and monitor road repairs through an interactive GIS dashboard.

---

# Problem Statement

Road potholes are a major cause of vehicle damage, traffic congestion, and road accidents. Municipal corporations often rely on manual inspections and citizen complaints, which can delay the identification and repair of newly formed potholes.

This project provides a smart reporting platform that combines smartphone sensors, GPS, image capture, and GIS visualization to streamline pothole reporting, verification, and repair tracking for the Greater Visakhapatnam Municipal Corporation (GVMC).

---

# Project Goals

- Enable citizens to report potholes quickly and accurately.
- Reduce dependency on manual road inspections.
- Improve report authenticity using GPS and photo evidence.
- Provide municipal officials with a centralized monitoring dashboard.
- Increase transparency by allowing citizens to track report status.
- Identify pothole hotspots using GIS analytics for better maintenance planning.

---

# Core Features

## 📱 Citizen Mobile Application

- GPS-based pothole reporting
- Accelerometer-assisted pothole detection
- Camera integration for photo evidence
- User confirmation before report submission
- Live report status tracking
- Lightweight and easy-to-use interface

---

## 🖥️ Admin Dashboard

- Interactive OpenStreetMap
- Live pothole markers
- View complete report details
- Verify or reject reports
- Update repair status
- Search and filtering
- Ward/Zone-wise analytics
- Hotspot visualization

---

# Overall System Architecture

```text
                            ┌────────────────────────────┐
                            │   Citizen Mobile App       │
                            │  Expo (React Native)       │
                            └─────────────┬──────────────┘
                                          │
                                          │ HTTPS / REST API
                                          ▼
                    ┌────────────────────────────────────────────┐
                    │         Backend API (Node.js)              │
                    │              Express.js                    │
                    └───────────────┬───────────────┬────────────┘
                                    │               │
                    ┌───────────────┘               └──────────────┐
                    ▼                                              ▼
          PostgreSQL Database                          Firebase Storage
        (Reports, Users, Status)                    (Pothole Images)
                    │
                    │
                    ▼
         React Admin Dashboard (Vercel)
                    │
                    ▼
      Leaflet.js + OpenStreetMap + Nominatim
```

---

# Mobile Application Architecture

```text
                   Expo React Native Application

┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  Accelerometer Service                                       │
│      │                                                       │
│      ▼                                                       │
│ Detect sudden road impact                                    │
│                                                              │
│              GPS Location Service                            │
│      │                                                       │
│      ▼                                                       │
│ Get Latitude, Longitude & Accuracy                           │
│                                                              │
│              Camera Service                                  │
│      │                                                       │
│      ▼                                                       │
│ Capture pothole image                                        │
│                                                              │
│             User Confirmation Screen                         │
│      │                                                       │
│      ▼                                                       │
│ Optional description                                         │
│                                                              │
│              API Service                                     │
│      │                                                       │
│      ▼                                                       │
│ POST /api/potholes                                           │
└──────────────────────────────────────────────────────────────┘
```

---

# Backend Architecture

```text
                    Node.js + Express Backend

                 ┌──────────────────────────┐
                 │      Express Server      │
                 └────────────┬─────────────┘
                              │
       ┌──────────────────────┼───────────────────────┐
       │                      │                       │
       ▼                      ▼                       ▼
   Routes Layer         Middleware             Controllers
   /potholes            Validation            Business Logic
       │                                              │
       └──────────────────────┬───────────────────────┘
                              ▼
                        Database Models
                              │
                 ┌────────────┴─────────────┐
                 ▼                          ▼
           PostgreSQL                Firebase Storage
```

---

# Admin Dashboard Architecture

```text
                    React Dashboard

┌─────────────────────────────────────────────────────┐
│ Dashboard                                            │
│                                                      │
│  ┌──────────────────────────────────────────────┐    │
│  │ Interactive Map (Leaflet + OSM)              │    │
│  │                                              │    │
│  │ • Display pothole markers                    │    │
│  │ • View report details                        │    │
│  │ • Navigate to location                       │    │
│  └──────────────────────────────────────────────┘    │
│                                                      │
│  ┌──────────────────────────────────────────────┐    │
│  │ Report Management                            │    │
│  │ • Verify report                              │    │
│  │ • Reject report                              │    │
│  │ • Update repair status                       │    │
│  └──────────────────────────────────────────────┘    │
│                                                      │
│  ┌──────────────────────────────────────────────┐    │
│  │ Analytics                                    │    │
│  │ • Total Reports                              │    │
│  │ • Pending Repairs                            │    │
│  │ • Hotspots                                   │    │
│  │ • Ward & Zone Analysis                       │    │
│  └──────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

---

# Database Architecture

```text
                    PostgreSQL

                 Users (Optional)
                       │
                       ▼
                Pothole Reports
───────────────────────────────────────────────
id
latitude
longitude
address
image_url
description
status
severity
created_at
updated_at

                       │
                       ▼

                Repair History
───────────────────────────────────────────────
report_id
verified_by
remarks
updated_at
```

---

# Complete Data Flow

```text
Citizen Driving
        │
        ▼
Accelerometer detects road impact
        │
        ▼
GPS captures coordinates
        │
        ▼
Camera captures pothole image
        │
        ▼
Citizen confirms report
        │
        ▼
REST API Request
        │
        ▼
Node.js Backend
        │
 ┌──────┴───────────┐
 │                  │
 ▼                  ▼
PostgreSQL     Firebase Storage
 │                  │
 └────────┬─────────┘
          ▼
React Admin Dashboard
          │
          ▼
Verification
          │
          ▼
Repair Status Updated
          │
          ▼
Citizen views updated status
```
---

# Repository Structure

```text
smart-pothole-reporting/
│
├── backend/
│   ├── src/
│   │   ├── controllers/
│   │   ├── middleware/
│   │   ├── models/
│   │   ├── routes/
│   │   │   └── potholes.js
│   │   ├── services/
│   │   ├── utils/
│   │   ├── db.js
│   │   └── server.js
│   │
│   ├── migrations/
│   ├── package.json
│   ├── render.yaml
│   └── .env.example
│
├── mobile-app/
│   ├── src/
│   │   ├── screens/
│   │   ├── components/
│   │   ├── services/
│   │   │   ├── api.js
│   │   │   ├── accelerometer.js
│   │   │   ├── location.js
│   │   │   ├── camera.js
│   │   │   └── storage.js
│   │   ├── hooks/
│   │   ├── navigation/
│   │   └── config.js
│   │
│   ├── App.js
│   ├── app.json
│   ├── package.json
│   └── .env.example
│
├── web-dashboard/
│   ├── src/
│   │   ├── pages/
│   │   ├── components/
│   │   │   ├── MapView.jsx
│   │   │   ├── ReportTable.jsx
│   │   │   ├── Analytics.jsx
│   │   │   └── FilterBar.jsx
│   │   ├── services/
│   │   │   └── api.js
│   │   ├── App.jsx
│   │   └── config.js
│   │
│   ├── package.json
│   └── .env.example
│
├── docs/
│   ├── API.md
│   ├── ARCHITECTURE.md
│   ├── SCHEMA.md
│   ├── SETUP.md
│   └── CONTRIBUTING.md
│
├── README.md
├── LICENSE
└── .gitignore
```

---

# Technology Stack

| Layer | Technology | Purpose |
|---------|------------|---------|
| Mobile | Expo (React Native) | Citizen reporting application |
| Backend | Node.js + Express | REST API |
| Database | PostgreSQL | Store pothole reports |
| Storage | Firebase Storage | Store uploaded images |
| Dashboard | React.js | Municipal dashboard |
| Maps | Leaflet.js | Interactive GIS map |
| Map Data | OpenStreetMap | Base map |
| Reverse Geocoding | Nominatim | Address lookup |
| Deployment | Render | Backend & Database |
| Deployment | Vercel | Dashboard |
| Mobile Build | Expo EAS | Android APK |

---

# Maps & Geolocation

The project uses a fully open-source mapping stack.

## OpenStreetMap (OSM)

- Interactive base maps
- Road network
- Ward and zone visualization
- Marker rendering

## Leaflet.js

- Display pothole markers
- Popup information
- Layer controls
- Heatmap support
- Polygon overlays

## Nominatim

Converts GPS coordinates into readable addresses.

Example:

Latitude:
17.7231

Longitude:
83.3012

↓

```
MVP Colony,
Visakhapatnam,
Andhra Pradesh
```

---

# REST API Overview

## Create Report

```
POST /api/potholes
```

Uploads a new pothole report.

---

## Get All Reports

```
GET /api/potholes
```

Returns every pothole report.

---

## Get Single Report

```
GET /api/potholes/:id
```

Returns report details.

---

## Update Status

```
PATCH /api/potholes/:id
```

Updates repair status.

---

## Delete Report

```
DELETE /api/potholes/:id
```

Deletes a report.

---

# Report Status Lifecycle

```text
Reported
     │
     ▼
Verified
     │
     ▼
Assigned
     │
     ▼
Repair In Progress
     │
     ▼
Repaired
     │
     ▼
Closed
```

---

# Environment Variables

## Backend

```env
PORT=5000

DATABASE_URL=

FIREBASE_API_KEY=

FIREBASE_PROJECT_ID=

FIREBASE_STORAGE_BUCKET=

JWT_SECRET=
```

---

## Mobile App

```env
EXPO_PUBLIC_API_URL=http://localhost:5000/api
```

---

## Dashboard

```env
VITE_API_URL=http://localhost:5000/api
```

---

# Local Development

## Clone Repository

```bash
git clone https://github.com/your-org/smart-pothole-reporting.git

cd smart-pothole-reporting
```

---

## Backend

```bash
cd backend

npm install

npm run dev
```

---

## Mobile

```bash
cd mobile-app

npm install

npx expo start
```

---

## Dashboard

```bash
cd web-dashboard

npm install

npm run dev
```

---

# Deployment

## Backend

Deploy to **Render**

- Configure PostgreSQL
- Configure Environment Variables
- Enable HTTPS

---

## Dashboard

Deploy to **Vercel**

Configure:

```
VITE_API_URL
```

---

## Mobile

Build APK

```bash
eas build -p android
```

Install APK on Android devices for testing.

---

# Future Enhancements

- AI-based pothole verification
- Automatic severity estimation
- Duplicate report clustering
- Offline report synchronization
- Push notifications
- Crew assignment dashboard
- Route optimization
- Predictive maintenance
- Citizen reward points
- ML-powered hotspot prediction

---

# Expected Impact

The proposed solution provides measurable benefits for both citizens and municipal authorities.

### Citizens

- Faster reporting
- Transparent complaint tracking
- Improved road safety
- Reduced vehicle damage

### GVMC Officials

- Centralized report management
- GIS-based monitoring
- Better resource allocation
- Reduced manual inspections

### Municipality

- Faster repair cycles
- Data-driven maintenance planning
- Ward-wise analytics
- Improved civic engagement

---

# Roadmap

```text
✔ Problem Analysis

✔ Solution Design

✔ System Architecture

⬜ Backend Development

⬜ PostgreSQL Integration

⬜ Mobile Application

⬜ Dashboard Development

⬜ GIS Integration

⬜ Testing

⬜ Deployment

⬜ Final Presentation
```

---

# Team Responsibilities

| Member | Responsibility |
|----------|----------------|
| Member 1 | Backend API & PostgreSQL |
| Member 2 | Mobile Application |
| Member 3 | Sensors, GPS & Camera Integration |
| Member 4 | Dashboard, GIS & Analytics |

---

# Contributing

1. Create a feature branch.

2. Commit changes using meaningful commit messages.

3. Open a Pull Request.

4. Ensure API contracts remain compatible.

5. Update documentation when required.

---

# License

This project is developed for **HackYatra 2026** under the **SW11 – Smart Pothole Detection & Automated Citizen Reporting** problem statement.

It is intended for educational, demonstration, and hackathon purposes.

---

# Acknowledgements

Special thanks to:

- Greater Visakhapatnam Municipal Corporation (GVMC)
- HackYatra 2026 Organizers
- OpenStreetMap Community
- Leaflet.js Contributors
- Expo Team
- Node.js Community
- PostgreSQL Community