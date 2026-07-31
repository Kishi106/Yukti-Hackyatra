# Shared Data Contract

This document defines the pothole record shape shared by `backend/`, `mobile-app/`, and `web-dashboard/`. All three must stay in sync with this contract.

## Pothole record

| Field       | Type                                  | Notes                          |
|-------------|----------------------------------------|---------------------------------|
| id          | uuid                                   | auto-generated                  |
| lat         | float                                   | required                        |
| lng         | float                                   | required                        |
| severity    | "low" \| "medium" \| "high"            | required                        |
| source      | "auto" \| "citizen"                    | required                        |
| photo_url   | string \| null                         | nullable                        |
| status      | "new" \| "in_progress" \| "fixed"      | default "new"                   |
| ward        | string \| null                         | nullable                        |
| created_at  | timestamp                              | auto-generated                  |

## Example JSON

```json
{
  "id": "3f2a1c1e-4b8a-4f0a-9c3d-1a2b3c4d5e6f",
  "lat": 17.6868,
  "lng": 83.2185,
  "severity": "medium",
  "source": "citizen",
  "photo_url": null,
  "status": "new",
  "ward": null,
  "created_at": "2026-07-31T00:00:00.000Z"
}
```

## API endpoints (backend/)

- `POST /potholes` — create a record. Requires `lat`, `lng`, `severity`, `source`. `ward` and `photo_url` are optional. Returns 201 with the created record, 400 on missing/invalid fields.
- `GET /potholes` — returns all records as a JSON array. Supports `?status=` and `?ward=` filters.
- `PATCH /potholes/:id` — updates `status` only. 404 if id not found, 400 if status is not a valid enum value.
- `GET /health` — returns `{ "status": "ok" }`.
