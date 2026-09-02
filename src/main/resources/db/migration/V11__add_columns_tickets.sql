ALTER TABLE tickets ADD COLUMN checked_in_at TIMESTAMPTZ;
ALTER TABLE tickets ADD COLUMN checked_in_by BIGINT REFERENCES users (id);