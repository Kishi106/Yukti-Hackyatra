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
| detection_count   | integer                                | default 1, total merged raw detections (any source) — see geo merge below |
| report_count      | integer                                | default 1, subset of detection_count from `source: "citizen"` |
| last_seen_at      | timestamp                              | auto-updated on every merge; `created_at` is effectively "first seen" |
| red_zone_id       | uuid \| null                           | references `red_zones.id`, nullable              |

`lat`/`lng` are the *representative* coordinate — a running centroid recomputed on every merge, not necessarily any single report's exact GPS reading. See `raw_detections` for the untouched history.

## Raw detection record

Every detection/report ever received, whether or not it ended up merged into an existing pothole. Never updated or deleted — the audit trail behind `potholes.lat/lng/detection_count`.

| Field              | Type                       | Notes                                        |
|--------------------|-----------------------------|-----------------------------------------------|
| id                 | uuid                        | auto-generated                                 |
| source             | "auto" \| "citizen"        | required                                       |
| session_id         | string \| null              | optional dashcam trip/pass identifier; repeated pings from the same session within the merge radius collapse into one detection instead of inflating the count |
| lat                | float                       | required, the exact reading as received        |
| lng                | float                       | required                                       |
| confidence_score   | integer                     | snapshot at detection time                     |
| photo_url          | string \| null              | nullable                                       |
| reporter_id        | uuid \| null                | references `users.id`, nullable                |
| detected_at        | timestamp                   | auto-generated                                 |
| matched_pothole_id | uuid \| null                | references `potholes.id`                       |

## Red zone record

A density cluster of `potholes` (not raw detections) within ~20–30m of each other — its own entity, not another pin.

| Field         | Type      | Notes                                             |
|---------------|-----------|------------------------------------------------------|
| id            | uuid      | auto-generated                                        |
| centroid_lat  | float     |                                                        |
| centroid_lng  | float     |                                                        |
| radius_m      | float     | max member distance from centroid, floored at a minimum |
| pothole_count | integer   | cluster size (≥ `RED_ZONE_MIN_SAMPLES`)               |
| risk_score    | integer   | 0–100, simple v1 function of cluster size             |
| created_at    | timestamp |                                                        |
| updated_at    | timestamp |                                                        |

`red_zone_members` is the join table (`red_zone_id`, `pothole_id`).

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

- **Merged with an existing pothole** (new detection within `MERGE_RADIUS_METERS`, default 5m — see geo merge below): +15 (`MERGE_CONFIDENCE_BONUS`) to the matched record. Replaces the old ~30m bounding-box hack, which bumped nearby *unrelated* rows without actually merging them.
- **Photo attached** (at creation via `photo_url`, or added later via `PATCH /potholes/:id/photo`): +30, only once.
- **User confirmation** (`PATCH /potholes/:id/confirm`): +20, only once.

## Geo merge & red zones (backend/src/potholeMerge.js, redZones.js, geoConfig.js)

**Level 1 — duplicate merge.** Every `POST /potholes` searches for an existing pothole within `MERGE_RADIUS_METERS` (default 5m, haversine distance, bounding-box pre-filtered) before creating anything new. A match merges: `detection_count`/`report_count` increment, `lat`/`lng` recompute as a running centroid, `severity` takes the max of existing vs. new (never downgrades), `confidence_score` gets the merge bonus, `last_seen_at` updates, `created_at` stays as first-seen. No match creates a new pothole, same as before. Either way a `raw_detections` row is always inserted (except session-collapsed pings, below) — nothing is ever lost.

A `session_id` on the request body identifies one dashcam pass; a second ping from the same session within the merge radius is treated as the same physical detection (not a new one) so a single slow pass over a pothole can't inflate `detection_count`.

**Level 2 — red zones.** After every `POST /potholes`, a full DBSCAN pass (haversine distance, `RED_ZONE_EPS_METERS` default 25m, `RED_ZONE_MIN_SAMPLES` default 5) re-clusters all `potholes` and replaces `red_zones`/`red_zone_members`. This is a v1 tradeoff: a full-table batch recompute rather than a true incrementally-scoped one — simple and correct, fast enough at the current data volume (hundreds of rows), but O(n²) and worth revisiting (real incremental DBSCAN, or PostGIS) if the table grows much larger.

## API endpoints (backend/)

- `POST /potholes` — match-or-create. Requires `lat`, `lng`, `severity`, `source`. `ward`, `photo_url`, `reporter_id`, `session_id` are optional. Returns 201 with the created/merged record (`merged: true|false` included) on a new or merged detection, or 200 with `collapsedSessionDuplicate: true` if it was recognized as a repeat ping from the same `session_id`. 400 on missing/invalid fields.
- `GET /potholes` — returns all records as a JSON array. Supports `?status=`, `?ward=`, `?ward_no=` filters.
- `GET /potholes/:id/detections` — returns the full `raw_detections` history for one pothole, ordered oldest first.
- `PATCH /potholes/:id` — updates `status` only. 404 if id not found, 400 if status is not a valid enum value.
- `PATCH /potholes/:id/confirm` — marks `user_confirmed = true` and applies the confirmation confidence bonus (only once). 404 if not found.
- `PATCH /potholes/:id/photo` — body `{ photo_url }`. Sets `photo_url` and applies the photo confidence bonus if the record didn't already have a photo. 404 if not found, 400 if `photo_url` missing.
- `GET /red-zones` — returns all current red zones, ordered by `risk_score` descending.
- `POST /red-zones/recompute` — manually triggers the same full DBSCAN recompute that runs automatically after every `POST /potholes`. Returns the new zone list.
- `POST /users` — body `{ name, phone, ward }`. Creates a user account. Returns 201 with the created record; 409 with `{ error, existingUserId }` if `phone` is already registered.
- `GET /users/:id` — returns the user record, 404 if not found.
- `PATCH /users/:id` — body can include any subset of `{ name, phone, ward }`. Returns the updated record, 404 if not found.
- `POST /uploads` — multipart/form-data with a `photo` field (image files only, 8MB max). Uploads to Supabase Storage and returns 201 with `{ url }`. 400 on invalid/missing file, 500 on upload failure.
- `GET /health` — returns `{ "status": "ok" }`.
