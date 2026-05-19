ALTER TABLE organization_subscription_payments
    ADD COLUMN IF NOT EXISTS subscription_plan VARCHAR(50);

UPDATE organization_subscription_payments osp
SET subscription_plan = COALESCE(o.subscription_plan, 'STARTER')
FROM organizations o
WHERE osp.organization_id = o.id
  AND osp.subscription_plan IS NULL;

