ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS organization_id BIGINT REFERENCES organizations(id),
    ADD COLUMN IF NOT EXISTS actor_role VARCHAR(40),
    ADD COLUMN IF NOT EXISTS activity_scope VARCHAR(40),
    ADD COLUMN IF NOT EXISTS event_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS target_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64),
    ADD COLUMN IF NOT EXISTS device VARCHAR(120),
    ADD COLUMN IF NOT EXISTS browser VARCHAR(120);

UPDATE audit_logs
SET activity_scope = CASE category
    WHEN 'AUTHENTICATION' THEN 'GLOBAL_LOGIN'
    WHEN 'MEMBER' THEN 'MEMBER_ACTIVITY'
    WHEN 'ORGANIZATION' THEN 'ADMIN_ACTIVITY'
    ELSE 'ADMIN_ACTIVITY'
END
WHERE activity_scope IS NULL;

UPDATE audit_logs
SET event_status = CASE event
    WHEN 'LOGIN_FAILED' THEN 'FAILED'
    WHEN 'LOGIN_SUSPICIOUS' THEN 'SUSPICIOUS'
    WHEN 'PASSWORD_LOGIN' THEN 'SUCCESS'
    WHEN 'GOOGLE_LOGIN' THEN 'SUCCESS'
    ELSE 'INFO'
END
WHERE event_status IS NULL;

ALTER TABLE audit_logs
    ALTER COLUMN activity_scope SET NOT NULL,
    ALTER COLUMN event_status SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_org_scope_ts
    ON audit_logs(organization_id, activity_scope, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_audit_login_status_ts
    ON audit_logs(activity_scope, event_status, timestamp DESC);
