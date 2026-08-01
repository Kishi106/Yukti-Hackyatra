# Shared Data Contract

This document defines the pothole record shape shared by `backend/`, `mobile-app/`, and `web-dashboard/`. All three must stay in sync with this contract.

## Pothole record

| Field            | Type                                  | Notes                                          |
|-------------------|----------------------------------------|-------------------------------------------------|
| id                | uuid                                   | auto-generated                                   |
| lat               | float                                   | required                                         |
| lng               | float                                   | required                                         |
| severity          | "low" \| "medium" \| "high"            | required                                         |
| source            | "auto" \| "citizen"                    | required                                         |
| photo_url         | string \| null                         | nullable                                         |
| status            | "new" \| "in_progress" \| "fixed"      | default "new"                                    |
| ward              | string \| null                         | nullable                                         |
| created_at        | timestamp                              | auto-generated                                   |
| reporter_id       | uuid \| null                           | references `users.id`, nullable                  |
| confidence_score  | integer                                | 0–100, default 0, see scoring rules below        |
| user_confirmed    | boolean                                | default false                                    |

## User record

| Field       | Type      | Notes                          |
|-------------|-----------|---------------------------------|
| id          | uuid      | auto-generated                  |
| name        | string    | required                        |
| phone       | string    | required, unique                |
| ward        | string \| null | nullable                   |
| created_at  | timestamp | auto-generated                  |

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
  "created_at": "2026-07-31T00:00:00.000Z",
  "reporter_id": null,
  "confidence_score": 0,
  "user_confirmed": false
}
```

## Confidence scoring

`confidence_score` starts at 30 for `source: "auto"` reports (accelerometer detection), or 0 for `source: "citizen"` reports, and is capped at 100 throughout:

- **Duplicate nearby report** (another pothole within ~30m and reported in the last 30 days): +20 to the new record, and +20 to every matching existing record.
- **Photo attached** (at creation via `photo_url`, or added later via `PATCH /potholes/:id/photo`): +30, only once.
- **User confirmation** (`PATCH /potholes/:id/confirm`): +20, only once.

## API endpoints (backend/)

- `POST /potholes` — create a record. Requires `lat`, `lng`, `severity`, `source`. `ward`, `photo_url`, `reporter_id` are optional. Computes `confidence_score` per the rules above. Returns 201 with the created record, 400 on missing/invalid fields.
- `GET /potholes` — returns all records as a JSON array. Supports `?status=` and `?ward=` filters.
- `PATCH /potholes/:id` — updates `status` only. 404 if id not found, 400 if status is not a valid enum value.
- `PATCH /potholes/:id/confirm` — marks `user_confirmed = true` and applies the confirmation confidence bonus (only once). 404 if not found.
- `PATCH /potholes/:id/photo` — body `{ photo_url }`. Sets `photo_url` and applies the photo confidence bonus if the record didn't already have a photo. 404 if not found, 400 if `photo_url` missing.
- `POST /users` — body `{ name, phone, ward }`. Creates a user account. Returns 201 with the created record; 409 with `{ error, existingUserId }` if `phone` is already registered.
- `GET /users/:id` — returns the user record, 404 if not found.
- `PATCH /users/:id` — body can include any subset of `{ name, phone, ward }`. Returns the updated record, 404 if not found.
- `POST /uploads` — multipart/form-data with a `photo` field (image files only, 8MB max). Uploads to Supabase Storage and returns 201 with `{ url }`. 400 on invalid/missing file, 500 on upload failure.
- `GET /health` — returns `{ "status": "ok" }`.
