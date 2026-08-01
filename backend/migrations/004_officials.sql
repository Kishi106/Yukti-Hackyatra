CREATE TABLE IF NOT EXISTS officials (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  phone TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('field_officer', 'commissioner_analyst')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
