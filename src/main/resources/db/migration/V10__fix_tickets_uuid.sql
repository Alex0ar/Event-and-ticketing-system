ALTER TABLE tickets ALTER COLUMN uuid_code TYPE VARCHAR(36);
ALTER TABLE tickets ALTER COLUMN uuid_code SET NOT NULL;
ALTER TABLE tickets ADD CONSTRAINT uq_tickets_uuid_code UNIQUE (uuid_code);