CREATE TABLE referrals (
    id BIGSERIAL PRIMARY KEY,
    referrer_org_id BIGINT NOT NULL REFERENCES organizations(id),
    referred_org_id BIGINT NOT NULL REFERENCES organizations(id),
    referral_code_used VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reward_type VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_referred_org UNIQUE(referred_org_id)
);

CREATE TABLE referral_rewards (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    reward_type VARCHAR(50) NOT NULL,
    amount NUMERIC(12, 2) DEFAULT 0,
    months_rewarded INT DEFAULT 0,
    reward_used BOOLEAN DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

ALTER TABLE organizations
ADD COLUMN referral_code VARCHAR(50),
ADD COLUMN gym_coin_balance NUMERIC(12, 2) DEFAULT 0,
ADD COLUMN total_coins_earned NUMERIC(12, 2) DEFAULT 0,
ADD COLUMN total_coins_used NUMERIC(12, 2) DEFAULT 0,
ADD COLUMN six_month_reward_claimed BOOLEAN DEFAULT FALSE;

ALTER TABLE organization_subscription_payments
ADD COLUMN gym_coins_used NUMERIC(12, 2) DEFAULT 0;

CREATE UNIQUE INDEX idx_org_referral_code ON organizations(referral_code);
CREATE INDEX idx_referrals_referrer ON referrals(referrer_org_id);
CREATE INDEX idx_referrals_status ON referrals(status);
CREATE INDEX idx_referral_rewards_org ON referral_rewards(org_id);
