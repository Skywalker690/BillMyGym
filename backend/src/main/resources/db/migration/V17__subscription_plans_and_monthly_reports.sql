ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS subscription_plan VARCHAR(50) NOT NULL DEFAULT 'STARTER';

UPDATE organizations
SET subscription_plan = 'PILOT'
WHERE organization_type = 'PILOT';

UPDATE organizations
SET subscription_plan = 'STARTER'
WHERE organization_type <> 'PILOT'
  AND subscription_plan = 'PILOT';

CREATE INDEX IF NOT EXISTS idx_org_subscription_plan
    ON organizations(subscription_plan, deleted);
