CREATE TABLE IF NOT EXISTS processed_webhook_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_processed_webhook_events_event_id
    ON processed_webhook_events(event_id);

CREATE TABLE IF NOT EXISTS api_request_log (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT,
    request_id VARCHAR(255) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    response_hash VARCHAR(128) NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_api_request_log_org_request_endpoint
    ON api_request_log(organization_id, request_id, endpoint);

CREATE TABLE IF NOT EXISTS notification_delivery_logs (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT REFERENCES notifications(id),
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_attempt TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notification_delivery_org_type_attempt
    ON notification_delivery_logs(organization_id, type, last_attempt DESC, deleted);

ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uk_organizations_admin_email_active
    ON organizations(admin_email)
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_organizations_admin_phone_active
    ON organizations(admin_phone_number)
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_org_subscription_payment_payment_active
    ON organization_subscription_payments(razorpay_payment_id)
    WHERE razorpay_payment_id IS NOT NULL AND deleted = FALSE;

ALTER TABLE audit_logs
    ALTER COLUMN timestamp TYPE TIMESTAMPTZ
    USING timestamp AT TIME ZONE 'Asia/Kolkata';

ALTER TABLE otps
    ALTER COLUMN expires_at TYPE TIMESTAMPTZ
    USING expires_at AT TIME ZONE 'Asia/Kolkata',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
    USING created_at AT TIME ZONE 'Asia/Kolkata';
