ALTER TABLE orders DROP COLUMN reservationId;
ALTER TABLE orders ADD COLUMN reservation_id BIGINT NOT NULL UNIQUE REFERENCES reservations (id);