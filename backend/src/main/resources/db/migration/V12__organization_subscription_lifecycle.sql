ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS organization_type VARCHAR(50) NOT NULL DEFAULT 'REGULAR',
    ADD COLUMN IF NOT EXISTS trial_start_date DATE,
    ADD COLUMN IF NOT EXISTS trial_end_date DATE,
    ADD COLUMN IF NOT EXISTS pilot_start_date DATE,
    ADD COLUMN IF NOT EXISTS pilot_end_date DATE,
    ADD COLUMN IF NOT EXISTS subscription_due_date DATE,
    ADD COLUMN IF NOT EXISTS paused_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMPTZ;

UPDATE organizations
SET status = 'DEACTIVATED'
WHERE status = 'INACTIVE';

UPDATE organizations
SET status = 'PAUSED'
WHERE status = 'SUSPENDED';

UPDATE organizations
SET
    organization_type = COALESCE(organization_type, 'REGULAR'),
    trial_start_date = COALESCE(trial_start_date, created_at::date),
    trial_end_date = COALESCE(trial_end_date, created_at::date + 14),
    subscription_due_date = COALESCE(subscription_due_date, created_at::date + 30)
WHERE organization_type = 'REGULAR';

CREATE TABLE IF NOT EXISTS organization_subscription_payments (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    amount NUMERIC(12,2) NOT NULL,
    payment_date DATE,
    subscription_start DATE NOT NULL,
    subscription_end DATE NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    gateway_transaction_id VARCHAR(255),
    razorpay_order_id VARCHAR(255),
    razorpay_payment_id VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

INSERT INTO organization_subscription_payments (
    organization_id,
    amount,
    payment_date,
    subscription_start,
    subscription_end,
    payment_status,
    gateway_transaction_id,
    razorpay_order_id,
    razorpay_payment_id,
    deleted,
    created_at,
    updated_at
)
SELECT
    organization_id,
    amount,
    start_date,
    start_date,
    end_date,
    CASE
        WHEN status = 'ACTIVE' THEN 'PAID'
        WHEN status = 'EXPIRED' THEN 'FAILED'
        ELSE 'PENDING'
    END,
    razorpay_subscription_id,
    razorpay_subscription_id,
    razorpay_subscription_id,
    deleted,
    created_at,
    updated_at
FROM organization_subscriptions source
WHERE NOT EXISTS (
    SELECT 1
    FROM organization_subscription_payments target
    WHERE target.organization_id = source.organization_id
      AND target.subscription_start = source.start_date
      AND target.subscription_end = source.end_date
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_org_subscription_payment_order
    ON organization_subscription_payments(razorpay_order_id)
    WHERE razorpay_order_id IS NOT NULL AND deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_org_subscription_payment_payment
    ON organization_subscription_payments(razorpay_payment_id)
    WHERE razorpay_payment_id IS NOT NULL AND deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_org_subscription_payment_gateway_txn
    ON organization_subscription_payments(gateway_transaction_id)
    WHERE gateway_transaction_id IS NOT NULL AND deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_org_subscription_payments_org_status
    ON organization_subscription_payments(organization_id, payment_status, deleted);

CREATE INDEX IF NOT EXISTS idx_organizations_subscription_lifecycle
    ON organizations(organization_type, status, subscription_due_date, deleted);

CREATE INDEX IF NOT EXISTS idx_usage_metrics_month
    ON usage_metrics(year, month, deleted);
