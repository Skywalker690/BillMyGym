CREATE TABLE membership_plans (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    name VARCHAR(120) NOT NULL,
    price NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    default_plan BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_membership_plans_org_name
    ON membership_plans(organization_id, lower(name))
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX uk_membership_plans_default_per_org
    ON membership_plans(organization_id)
    WHERE deleted = FALSE AND default_plan = TRUE;

CREATE INDEX idx_membership_plans_org_deleted ON membership_plans(organization_id, deleted);
CREATE INDEX idx_membership_plans_org_active ON membership_plans(organization_id, active, deleted);
CREATE INDEX idx_membership_plans_org_default ON membership_plans(organization_id, default_plan, deleted);

INSERT INTO membership_plans (
    organization_id, name, price, active, default_plan, deleted, created_at, updated_at, version
)
SELECT
    organization.id,
    'Regular',
    COALESCE(organization.monthly_membership_fee, 0.00),
    TRUE,
    TRUE,
    FALSE,
    NOW(),
    NOW(),
    0
FROM organizations organization
WHERE organization.deleted = FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM membership_plans existing
      WHERE existing.organization_id = organization.id
        AND existing.deleted = FALSE
  );

ALTER TABLE member_memberships
    ADD COLUMN membership_plan_id BIGINT,
    ADD COLUMN plan_name VARCHAR(120) NOT NULL DEFAULT 'Regular',
    ADD COLUMN monthly_plan_price NUMERIC(12,2) NOT NULL DEFAULT 0.00;

UPDATE member_memberships membership
SET
    membership_plan_id = plan.id,
    plan_name = plan.name,
    monthly_plan_price = CASE
        WHEN membership.duration_months > 0 THEN ROUND(membership.plan_amount / membership.duration_months, 2)
        ELSE plan.price
    END
FROM membership_plans plan
WHERE plan.organization_id = membership.organization_id
  AND plan.default_plan = TRUE
  AND plan.deleted = FALSE;

ALTER TABLE member_memberships
    ALTER COLUMN membership_plan_id SET NOT NULL,
    ADD CONSTRAINT fk_member_memberships_membership_plan
        FOREIGN KEY (membership_plan_id) REFERENCES membership_plans(id);

CREATE INDEX idx_member_memberships_plan_deleted ON member_memberships(membership_plan_id, deleted);
CREATE INDEX idx_member_memberships_org_plan ON member_memberships(organization_id, membership_plan_id, deleted);
