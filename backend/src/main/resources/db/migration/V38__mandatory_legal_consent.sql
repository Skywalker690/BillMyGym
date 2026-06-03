ALTER TABLE users
    ADD COLUMN IF NOT EXISTS terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS privacy_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS policy_accepted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS policy_version VARCHAR(32),
    ADD COLUMN IF NOT EXISTS policy_ip_address VARCHAR(64),
    ADD COLUMN IF NOT EXISTS policy_device_info VARCHAR(255);

UPDATE users
SET terms_accepted = COALESCE(terms_accepted, FALSE),
    privacy_accepted = COALESCE(privacy_accepted, FALSE)
WHERE terms_accepted IS NULL
   OR privacy_accepted IS NULL;
