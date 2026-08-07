-- Level 1 (duplicate merge, ~5m) and Level 2 (density clustering / red zones)
-- support. Extends the existing `potholes` table in place rather than
-- replacing it, so GET /potholes and the mobile app's POST /potholes keep
-- working unchanged for every existing consumer.
--
-- `potholes.lat`/`potholes.lng` become the *representative* coordinate
-- (running centroid of all linked raw_detections) once merging is active —
-- no column rename, to avoid touching every existing query/dashboard field.

-- Immutable audit trail — every detection/report ever received, whether or
-- not it ended up merged into an existing pothole. Never updated or deleted.
CREATE TABLE IF NOT EXISTS raw_detections (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  source TEXT NOT NULL CHECK (source IN ('auto', 'citizen')),
  session_id TEXT,
  lat DOUBLE PRECISION NOT NULL,
  lng DOUBLE PRECISION NOT NULL,
  confidence_score INTEGER NOT NULL DEFAULT 0,
  photo_url TEXT,
  reporter_id UUID REFERENCES users(id),
  detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  matched_pothole_id UUID REFERENCES potholes(id)
);

CREATE INDEX IF NOT EXISTS idx_raw_detections_pothole ON raw_detections (matched_pothole_id);
CREATE INDEX IF NOT EXISTS idx_raw_detections_session ON raw_detections (session_id);
CREATE INDEX IF NOT EXISTS idx_raw_detections_lat_lng ON raw_detections (lat, lng);

ALTER TABLE potholes ADD COLUMN IF NOT EXISTS detection_count INTEGER NOT NULL DEFAULT 1;
ALTER TABLE potholes ADD COLUMN IF NOT EXISTS report_count INTEGER NOT NULL DEFAULT 1;
ALTER TABLE potholes ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE potholes ADD COLUMN IF NOT EXISTS red_zone_id UUID;

CREATE INDEX IF NOT EXISTS idx_potholes_lat_lng ON potholes (lat, lng);
CREATE INDEX IF NOT EXISTS idx_potholes_red_zone_id ON potholes (red_zone_id);

CREATE TABLE IF NOT EXISTS red_zones (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  centroid_lat DOUBLE PRECISION NOT NULL,
  centroid_lng DOUBLE PRECISION NOT NULL,
  radius_m DOUBLE PRECISION NOT NULL,
  pothole_count INTEGER NOT NULL,
  risk_score INTEGER NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS red_zone_members (
  red_zone_id UUID NOT NULL REFERENCES red_zones(id) ON DELETE CASCADE,
  pothole_id UUID NOT NULL REFERENCES potholes(id) ON DELETE CASCADE,
  PRIMARY KEY (red_zone_id, pothole_id)
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_potholes_red_zone'
  ) THEN
    ALTER TABLE potholes
      ADD CONSTRAINT fk_potholes_red_zone
      FOREIGN KEY (red_zone_id) REFERENCES red_zones(id) ON DELETE SET NULL;
  END IF;
END $$;

-- Backfill: one raw_detection per existing pothole row, so pre-existing
-- reports aren't treated as having no history. Safe to re-run — skips rows
-- that already have a linked raw_detection.
--
-- A handful of pre-existing potholes rows have a reporter_id left over from
-- a deleted/never-valid user (a pre-existing data gap, unrelated to this
-- migration) — LEFT JOIN + NULL those out rather than let a dangling FK
-- block the whole backfill.
INSERT INTO raw_detections (source, lat, lng, confidence_score, photo_url, reporter_id, detected_at, matched_pothole_id)
SELECT p.source, p.lat, p.lng, p.confidence_score, p.photo_url,
       CASE WHEN u.id IS NULL THEN NULL ELSE p.reporter_id END,
       p.created_at, p.id
FROM potholes p
LEFT JOIN users u ON u.id = p.reporter_id
WHERE NOT EXISTS (SELECT 1 FROM raw_detections WHERE matched_pothole_id = p.id);
