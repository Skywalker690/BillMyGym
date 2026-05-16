ALTER TABLE member_memberships
    ADD COLUMN IF NOT EXISTS last_sent_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE member_payments
    ADD COLUMN IF NOT EXISTS gateway_transaction_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uk_member_payments_gateway_transaction_active
    ON member_payments (gateway_transaction_id)
    WHERE gateway_transaction_id IS NOT NULL AND deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_member_memberships_member_deleted
    ON member_memberships (member_id, deleted);

CREATE INDEX IF NOT EXISTS idx_member_memberships_org_status_deleted
    ON member_memberships (organization_id, status, deleted);

CREATE INDEX IF NOT EXISTS idx_member_memberships_org_last_sent
    ON member_memberships (organization_id, last_sent_at, deleted);

CREATE INDEX IF NOT EXISTS idx_member_payments_member_deleted
    ON member_payments (member_id, deleted);

CREATE INDEX IF NOT EXISTS idx_member_payments_membership_deleted
    ON member_payments (membership_id, deleted);

CREATE INDEX IF NOT EXISTS idx_member_payments_billing_month_deleted
    ON member_payments (organization_id, billing_month, deleted);
