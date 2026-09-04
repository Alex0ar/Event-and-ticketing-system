INSERT INTO users (email, password_hash, full_name, enabled, created_at, updated_at)
VALUES ('admin@ticketflow.com', '$2a$10$aQBXaqcyS30Lmvc233vDY.jHggGcEygwWASKJ6kygICTSutizPj0G', 'Admin', true, now(), now())
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE email = 'admin@ticketflow.com'
ON CONFLICT DO NOTHING;