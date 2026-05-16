ALTER TABLE members
    ADD COLUMN IF NOT EXISTS phone VARCHAR(100),
    ADD COLUMN IF NOT EXISTS email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deactivation_reason VARCHAR(1000);

UPDATE members
SET phone = COALESCE(phone, 'MIGRATED-' || id)
WHERE phone IS NULL;

ALTER TABLE members
    ALTER COLUMN phone SET NOT NULL;

CREATE TABLE member_memberships (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    plan_name VARCHAR(255) NOT NULL,
    duration_months INT NOT NULL,
    plan_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    renewed BOOLEAN NOT NULL DEFAULT FALSE,
    pause_start DATE,
    pause_end DATE,
    paused_days INT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

INSERT INTO member_memberships (
    member_id,
    organization_id,
    plan_name,
    duration_months,
    plan_amount,
    start_date,
    end_date,
    status,
    renewed,
    pause_start,
    pause_end,
    paused_days,
    deleted,
    created_at,
    updated_at
)
SELECT
    m.id,
    m.organization_id,
    'Migrated Membership',
    COALESCE(m.plan_duration_months, 1),
    COALESCE(m.plan_total_amount, 0.00),
    COALESCE(m.membership_start_date, m.joining_date, CURRENT_DATE),
    COALESCE(m.membership_end_date, COALESCE(m.membership_start_date, m.joining_date, CURRENT_DATE) + INTERVAL '1 month'),
    CASE
        WHEN m.membership_status = 'RENEWED' THEN 'COMPLETED'
        ELSE COALESCE(m.membership_status, 'ACTIVE')
    END,
    COALESCE(m.renewed, FALSE),
    NULL,
    NULL,
    0,
    m.deleted,
    m.created_at,
    m.updated_at
FROM members m;

ALTER TABLE member_payments
    ADD COLUMN membership_id BIGINT;

UPDATE member_payments payment
SET membership_id = membership.id,
    payment_date = COALESCE(payment.payment_date, payment.created_at::date)
FROM member_memberships membership
WHERE payment.member_id = membership.member_id
  AND payment.organization_id = membership.organization_id
  AND payment.membership_id IS NULL;

ALTER TABLE member_payments
    ALTER COLUMN payment_date SET NOT NULL,
    ALTER COLUMN membership_id SET NOT NULL;

ALTER TABLE member_payments
    ADD CONSTRAINT fk_member_payments_membership
        FOREIGN KEY (membership_id) REFERENCES member_memberships(id);

ALTER TABLE member_payments
    DROP COLUMN expected_amount;

ALTER TABLE members
    DROP COLUMN joining_date,
    DROP COLUMN membership_start_date,
    DROP COLUMN membership_end_date,
    DROP COLUMN membership_duration,
    DROP COLUMN custom_duration_days,
    DROP COLUMN plan_duration_months,
    DROP COLUMN plan_total_amount,
    DROP COLUMN membership_status,
    DROP COLUMN payment_status,
    DROP COLUMN renewed,
    DROP COLUMN renewal_date,
    DROP COLUMN last_reminder_sent,
    DROP COLUMN reminder_count,
    DROP COLUMN overdue_marked_at;

CREATE UNIQUE INDEX uk_members_org_phone_active
    ON members (organization_id, phone)
    WHERE deleted = FALSE;

CREATE INDEX idx_member_memberships_org_status
    ON member_memberships (organization_id, status, deleted);

CREATE INDEX idx_member_payments_billing_month
    ON member_payments (organization_id, billing_month, deleted);
