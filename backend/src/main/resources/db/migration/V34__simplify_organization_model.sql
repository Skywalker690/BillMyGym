UPDATE organizations
SET organization_type = 'REGULAR'
WHERE organization_type = 'PILOT';

UPDATE organizations
SET subscription_plan = CASE
    WHEN organization_type = 'DEMO' THEN 'PRO'
    WHEN subscription_plan = 'PILOT' OR subscription_plan IS NULL THEN 'STARTER'
    ELSE subscription_plan
END;

UPDATE organizations
SET subscription_due_date = COALESCE(subscription_due_date, trial_end_date)
WHERE organization_type = 'REGULAR';

UPDATE organizations
SET status = 'PAUSED',
    paused_at = COALESCE(paused_at, NOW())
WHERE organization_type = 'REGULAR'
  AND status = 'TRIAL'
  AND trial_end_date IS NOT NULL
  AND trial_end_date < CURRENT_DATE;

UPDATE organizations
SET trial_start_date = NULL,
    trial_end_date = NULL,
    subscription_due_date = NULL,
    paused_at = NULL
WHERE organization_type = 'DEMO';

ALTER TABLE organizations DROP COLUMN IF EXISTS pilot_start_date;
ALTER TABLE organizations DROP COLUMN IF EXISTS pilot_end_date;
