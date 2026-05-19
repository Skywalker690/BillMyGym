ALTER TABLE notification_delivery_logs
    ADD COLUMN IF NOT EXISTS provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS template_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS recipient VARCHAR(255),
    ADD COLUMN IF NOT EXISTS provider_message_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS error_message VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS request_payload TEXT;

UPDATE notification_delivery_logs
SET provider = COALESCE(provider, 'SYSTEM')
WHERE provider IS NULL;

ALTER TABLE notification_delivery_logs
    ALTER COLUMN provider SET DEFAULT 'SYSTEM',
    ALTER COLUMN provider SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_notification_delivery_provider_retry
    ON notification_delivery_logs(provider, status, next_retry_at, attempt_count, deleted);

CREATE INDEX IF NOT EXISTS idx_notification_delivery_provider_message
    ON notification_delivery_logs(provider_message_id, deleted)
    WHERE provider_message_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS organization_usage_stats (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    year INT NOT NULL,
    month INT NOT NULL,
    whatsapp_sent_count INT NOT NULL DEFAULT 0,
    email_sent_count INT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_org_usage_stats_org_year_month UNIQUE (organization_id, year, month)
);

INSERT INTO organization_usage_stats (
    organization_id,
    year,
    month,
    whatsapp_sent_count,
    email_sent_count,
    deleted,
    created_at,
    updated_at
)
SELECT
    organization_id,
    year,
    month,
    monthly_whatsapp_count,
    monthly_email_count,
    deleted,
    created_at,
    updated_at
FROM usage_metrics source
WHERE NOT EXISTS (
    SELECT 1
    FROM organization_usage_stats target
    WHERE target.organization_id = source.organization_id
      AND target.year = source.year
      AND target.month = source.month
);

CREATE INDEX IF NOT EXISTS idx_org_usage_stats_month
    ON organization_usage_stats(year, month, deleted);

UPDATE organizations
SET
    trial_end_date = trial_start_date + 13,
    subscription_due_date = trial_start_date + 13
WHERE organization_type = 'REGULAR'
  AND status = 'TRIAL'
  AND trial_start_date IS NOT NULL;

UPDATE organization_subscription_payments
SET
    subscription_start = payment_date,
    subscription_end = (payment_date + INTERVAL '1 month' - INTERVAL '1 day')::date
WHERE payment_status = 'PAID'
  AND payment_date IS NOT NULL;

UPDATE organization_subscriptions
SET
    end_date = (start_date + INTERVAL '1 month' - INTERVAL '1 day')::date
WHERE status = 'ACTIVE';

UPDATE organizations target
SET subscription_due_date = latest.end_date + 1
FROM (
    SELECT DISTINCT ON (organization_id)
        organization_id,
        end_date
    FROM organization_subscriptions
    WHERE deleted = FALSE
      AND status = 'ACTIVE'
    ORDER BY organization_id, end_date DESC
) latest
WHERE target.id = latest.organization_id
  AND target.organization_type = 'REGULAR'
  AND target.status = 'ACTIVE';
