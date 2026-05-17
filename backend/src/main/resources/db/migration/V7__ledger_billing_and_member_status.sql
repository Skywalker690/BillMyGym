ALTER TABLE members
    ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN plan_duration_months INT NOT NULL DEFAULT 1,
    ADD COLUMN plan_total_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN deactivated_at TIMESTAMPTZ,
    ADD COLUMN deactivation_reason VARCHAR(1000);

CREATE TABLE member_payments (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    expected_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    payment_date DATE,
    billing_month VARCHAR(7) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    notes TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_member_payments_org_deleted ON member_payments(organization_id, deleted);
CREATE INDEX idx_member_payments_member_billing ON member_payments(member_id, billing_month, deleted);
