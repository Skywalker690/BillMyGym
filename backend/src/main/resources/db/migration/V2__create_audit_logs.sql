CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event VARCHAR(80) NOT NULL,
    description TEXT NOT NULL,
    actor_name VARCHAR(255) NOT NULL,
    actor_email VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    zone VARCHAR(20),
    timestamp TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_ts ON audit_logs(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_zone ON audit_logs(zone);
CREATE INDEX IF NOT EXISTS idx_audit_category ON audit_logs(category);
