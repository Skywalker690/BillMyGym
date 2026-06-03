ALTER TABLE member_memberships
    ADD COLUMN IF NOT EXISTS version BIGINT;

UPDATE member_memberships
SET version = 0
WHERE version IS NULL;

ALTER TABLE member_memberships
    ALTER COLUMN version SET DEFAULT 0,
    ALTER COLUMN version SET NOT NULL;

CREATE TABLE IF NOT EXISTS member_credit_ledger (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    amount NUMERIC(12,2) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    reference_membership_id BIGINT REFERENCES member_memberships(id),
    notes VARCHAR(1000),
    created_by_user_id BIGINT,
    created_by_email VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS membership_operation_idempotency (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id BIGINT NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_member_memberships_active
    ON member_memberships(member_id)
    WHERE deleted = FALSE AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_member_credit_ledger_member
    ON member_credit_ledger(member_id, deleted);

CREATE INDEX IF NOT EXISTS idx_member_credit_ledger_organization_created
    ON member_credit_ledger(organization_id, created_at DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_member_credit_ledger_membership
    ON member_credit_ledger(reference_membership_id, deleted);

CREATE UNIQUE INDEX IF NOT EXISTS uk_membership_operation_idempotency
    ON membership_operation_idempotency(organization_id, member_id, operation_type, idempotency_key)
    WHERE deleted = FALSE;
