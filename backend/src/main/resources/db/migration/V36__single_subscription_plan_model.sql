ALTER TABLE organizations
    ALTER COLUMN subscription_plan SET DEFAULT 'BILLMYGYM';

UPDATE organizations
SET subscription_plan = 'BILLMYGYM'
WHERE subscription_plan IS NULL
   OR subscription_plan IN ('STARTER', 'GROWTH', 'PRO', 'PILOT');

UPDATE organization_subscription_payments
SET subscription_plan = 'BILLMYGYM'
WHERE subscription_plan IS NULL
   OR subscription_plan IN ('STARTER', 'GROWTH', 'PRO', 'PILOT');

UPDATE referrals
SET reward_type = 'SIX_MONTH_BILLMYGYM'
WHERE reward_type = 'SIX_MONTH_PRO';

UPDATE referrals
SET reward_type = 'GYM_COIN'
WHERE reward_type = 'GYM_COINS';

UPDATE referrals
SET status = 'REWARDED'
WHERE status = 'COMPLETED';

UPDATE referral_rewards
SET reward_type = 'SIX_MONTH_BILLMYGYM'
WHERE reward_type = 'SIX_MONTH_PRO';

UPDATE referral_rewards
SET reward_type = 'GYM_COIN'
WHERE reward_type = 'GYM_COINS';

ALTER TABLE organization_subscription_payments
    ADD COLUMN IF NOT EXISTS base_amount NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS included_active_members INTEGER,
    ADD COLUMN IF NOT EXISTS billed_active_members BIGINT,
    ADD COLUMN IF NOT EXISTS additional_active_members BIGINT,
    ADD COLUMN IF NOT EXISTS overage_rate NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS overage_amount NUMERIC(12, 2);

UPDATE organization_subscription_payments
SET base_amount = COALESCE(base_amount, 299.00),
    included_active_members = COALESCE(included_active_members, 100),
    overage_rate = COALESCE(overage_rate, 4.00),
    overage_amount = COALESCE(overage_amount, GREATEST(amount - 299.00, 0.00)),
    additional_active_members = COALESCE(
        additional_active_members,
        CEIL(GREATEST(amount - 299.00, 0.00) / 4.00)::BIGINT
    ),
    billed_active_members = COALESCE(
        billed_active_members,
        100 + CEIL(GREATEST(amount - 299.00, 0.00) / 4.00)::BIGINT
    );
