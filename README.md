# Smart Pothole Detection & Automated Citizen Reporting

> **HackYatra 2026 – SW11**
> **Problem Statement:** Pothole Detection and Automated Citizen Reporting

## Overview

Road potholes are a major cause of vehicle damage, traffic congestion, and accidents. Currently, the Greater Visakhapatnam Municipal Corporation (GVMC) relies on periodic inspections and citizen complaints to identify road damage. This reactive process often results in delayed repairs, especially during the monsoon season when potholes develop rapidly.

This project provides a **smart, semi-automated pothole reporting system** that enables citizens to report potholes accurately while giving municipal authorities a centralized platform to verify, prioritize, and monitor repairs.

---

# Features

## Citizen Mobile Application (Flutter)

* GPS-based pothole reporting
* Automatic location detection
* Smartphone accelerometer-assisted pothole detection
* User confirmation before submission
* Photo capture and upload
* AI-assisted image verification (optional)
* Report status tracking

---

## Admin Dashboard (React.js)

* Interactive GIS map
* View all reported potholes
* Verify or reject reports
* Track repair progress
* Report analytics
* Hotspot visualization
* Report filtering and search

---

# How It Works

```text
Vehicle Moving
      │
      ▼
Accelerometer Detects Sudden Impact
      │
      ▼
Collect High-Accuracy GPS
      │
      ▼
Google Roads API
(Snap Location to Road)
      │
      ▼
User Captures Photo
      │
      ▼
AI / User Verification
      │
      ▼
Upload Report
      │
      ▼
Backend
      │
      ▼
Admin Dashboard
      │
      ▼
Verification → Repair → Completion
```

---

# Technology Stack

## Mobile

* Flutter
* Dart

## Frontend

* React.js

## Backend

* Node.js
* Express.js

## Database

* Firebase Firestore *(or Supabase)*

## Maps & Location

* Google Maps SDK
* Google Roads API
* Geolocator (Flutter)

## Image Storage

* Firebase Storage

## AI (Optional)

* TensorFlow Lite / Google Vision API
* Custom pothole detection model

---

# System Architecture

```text
Flutter App
      │
      │
      ▼
Node.js + Express API
      │
      │
      ├──────── Firebase / Supabase
      │
      ├──────── Firebase Storage
      │
      ├──────── Google Maps API
      │
      └──────── Google Roads API
                    │
                    ▼
            React Admin Dashboard
```

---

# Accuracy Strategy

The application does **not** rely solely on accelerometer readings because sudden impacts can also occur due to:

* Speed breakers
* Rough roads
* Sudden braking
* Phone movement

To improve accuracy, the system combines:

* High-accuracy GPS
* Google Roads API (Snap to Roads)
* User confirmation
* Photo evidence
* AI image verification *(optional)*
* Duplicate report detection

---

# Report Lifecycle

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

# Folder Structure

```text
project-root/

├── mobile/
│   ├── lib/
│   ├── assets/
│   └── pubspec.yaml
│
├── backend/
│   ├── routes/
│   ├── controllers/
│   ├── models/
│   ├── middleware/
│   └── server.js
│
├── dashboard/
│   ├── src/
│   ├── public/
│   └── package.json
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
