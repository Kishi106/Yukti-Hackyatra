ALTER TABLE potholes ADD COLUMN IF NOT EXISTS ward_no INTEGER;

CREATE INDEX IF NOT EXISTS idx_potholes_ward_no ON potholes (ward_no);
