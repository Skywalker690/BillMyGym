UPDATE organizations
SET organization_type = 'REGULAR',
    subscription_plan = CASE
        WHEN subscription_plan = 'PILOT' OR subscription_plan IS NULL THEN 'STARTER'
        ELSE subscription_plan
    END,
    status = 'PAUSED',
    trial_start_date = NULL,
    trial_end_date = NULL,
    pilot_start_date = NULL,
    pilot_end_date = NULL,
    subscription_due_date = CURRENT_DATE,
    paused_at = COALESCE(paused_at, CURRENT_TIMESTAMP),
    deactivated_at = NULL
WHERE deleted = FALSE
  AND organization_type = 'PILOT'
  AND pilot_end_date IS NOT NULL
  AND pilot_end_date <= CURRENT_DATE;
