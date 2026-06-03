ALTER TABLE organization_subscription_payments
    ADD COLUMN IF NOT EXISTS retry_count INT,
    ADD COLUMN IF NOT EXISTS next_retry_date DATE,
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS webhook_event_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS razorpay_subscription_id VARCHAR(255);

UPDATE organization_subscription_payments
SET retry_count = 0
WHERE retry_count IS NULL;

ALTER TABLE organization_subscription_payments
    ALTER COLUMN retry_count SET DEFAULT 0,
    ALTER COLUMN retry_count SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_org_subscription_payment_webhook_event
    ON organization_subscription_payments(webhook_event_id)
    WHERE webhook_event_id IS NOT NULL AND deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_org_subscription_payment_retry
    ON organization_subscription_payments(payment_status, next_retry_date, deleted);

CREATE INDEX IF NOT EXISTS idx_org_subscription_payment_subscription
    ON organization_subscription_payments(razorpay_subscription_id, created_at DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_notifications_org_date_type
    ON notifications(organization_id, notification_date, type, deleted);

CREATE INDEX IF NOT EXISTS idx_notifications_last_sent
    ON member_memberships(last_sent_at, status, deleted);

CREATE INDEX IF NOT EXISTS idx_members_phone
    ON members(phone, deleted);

CREATE INDEX IF NOT EXISTS idx_member_memberships_billing_month
    ON member_payments(billing_month, deleted);
