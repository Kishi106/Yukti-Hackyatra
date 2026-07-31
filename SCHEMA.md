# Shared API and Data Contract

## Overview
This document defines the shared contract for the pothole reporting system.

## Pothole Object
```json
{
  "id": 1,
  "latitude": 17.6868,
  "longitude": 83.2185,
  "severity": "medium",
  "status": "reported",
  "description": "Large pothole near the intersection",
  "image_url": "https://example.com/pothole.jpg",
  "created_at": "2026-07-31T00:00:00.000Z",
  "updated_at": "2026-07-31T00:00:00.000Z"
}
```

## API Endpoints
### GET /potholes
Returns all potholes sorted by creation time.

### POST /potholes
Creates a new pothole report.

### PATCH /potholes/:id
Updates the status or severity of an existing pothole.

## Status Values
- reported
- verified
- assigned
- in_progress
- repaired
- closed

## Severity Values
- low
- medium
- high
