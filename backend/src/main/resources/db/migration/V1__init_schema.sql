CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    district VARCHAR(255) NOT NULL,
    place VARCHAR(255),
    logo_url VARCHAR(512),
    map_location_link VARCHAR(512),
    address VARCHAR(512),
    admin_full_name VARCHAR(255) NOT NULL,
    admin_phone_number VARCHAR(100) NOT NULL,
    admin_email VARCHAR(255) NOT NULL,
    monthly_membership_fee NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    admission_fee NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    organization_type VARCHAR(50) NOT NULL DEFAULT 'REGULAR',
    subscription_plan VARCHAR(50) NOT NULL DEFAULT 'BILLMYGYM',
    status VARCHAR(50) NOT NULL DEFAULT 'TRIAL',
    trial_start_date DATE,
    trial_end_date DATE,
    subscription_due_date DATE,
    paused_at TIMESTAMPTZ,
    deactivated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    referral_code VARCHAR(50),
    gym_coin_balance NUMERIC(12,2) DEFAULT 0.00,
    total_coins_earned NUMERIC(12,2) DEFAULT 0.00,
    total_coins_used NUMERIC(12,2) DEFAULT 0.00,
    six_month_reward_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id),
    email VARCHAR(255) NOT NULL UNIQUE,
    google_id VARCHAR(255) UNIQUE,
    avatar_url VARCHAR(512),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    privacy_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    policy_accepted_at TIMESTAMPTZ,
    policy_version VARCHAR(32),
    policy_ip_address VARCHAR(64),
    policy_device_info VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMPTZ,
    deactivated_at TIMESTAMPTZ,
    deactivation_reason VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE organization_settings (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL UNIQUE REFERENCES organizations(id),
    whatsapp_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    branding_name VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE member_memberships (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    duration_months INT NOT NULL,
    plan_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    last_sent_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE member_payments (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    membership_id BIGINT NOT NULL REFERENCES member_memberships(id),
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    payment_date DATE NOT NULL,
    billing_month VARCHAR(7) NOT NULL,
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PAID',
    payment_type VARCHAR(50) NOT NULL DEFAULT 'MEMBERSHIP',
    gateway_transaction_id VARCHAR(255),
    notes VARCHAR(1000),
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT member_payments_payment_type_check
        CHECK (payment_type IN ('ADMISSION', 'MEMBERSHIP', 'ADJUSTMENT', 'NEW_MEMBERSHIP', 'RENEWAL'))
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    member_id BIGINT REFERENCES members(id),
    member_count INT NOT NULL DEFAULT 1,
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    failure_reason VARCHAR(1000),
    notification_date DATE NOT NULL,
    sent_at TIMESTAMPTZ,
    message VARCHAR(1000),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE usage_metrics (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    year INT NOT NULL,
    month INT NOT NULL,
    monthly_whatsapp_count INT NOT NULL DEFAULT 0,
    monthly_email_count INT NOT NULL DEFAULT 0,
    monthly_digest_count INT NOT NULL DEFAULT 0,
    active_members INT NOT NULL DEFAULT 0,
    active_admins INT NOT NULL DEFAULT 0,
    subscription_status VARCHAR(50) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_usage_metrics_org_year_month UNIQUE (organization_id, year, month)
);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event VARCHAR(80) NOT NULL,
    description TEXT NOT NULL,
    actor_name VARCHAR(255) NOT NULL,
    actor_email VARCHAR(255) NOT NULL,
    actor_role VARCHAR(40),
    organization_id BIGINT REFERENCES organizations(id),
    category VARCHAR(20) NOT NULL,
    activity_scope VARCHAR(40) NOT NULL DEFAULT 'ADMIN_ACTIVITY',
    event_status VARCHAR(20) NOT NULL DEFAULT 'INFO',
    target_name VARCHAR(255),
    zone VARCHAR(20),
    ip_address VARCHAR(64),
    device VARCHAR(120),
    browser VARCHAR(120),
    timestamp TIMESTAMPTZ NOT NULL
);

CREATE TABLE organization_subscription_payments (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    amount NUMERIC(12,2) NOT NULL,
    subscription_plan VARCHAR(50),
    gym_coins_used NUMERIC(12,2) DEFAULT 0.00,
    base_amount NUMERIC(12,2) DEFAULT 0.00,
    included_active_members INT,
    billed_active_members BIGINT,
    additional_active_members BIGINT,
    overage_rate NUMERIC(12,2) DEFAULT 0.00,
    overage_amount NUMERIC(12,2) DEFAULT 0.00,
    payment_date DATE,
    subscription_start DATE NOT NULL,
    subscription_end DATE NOT NULL,
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_date DATE,
    failure_reason VARCHAR(1000),
    webhook_event_id VARCHAR(255),
    gateway_transaction_id VARCHAR(255),
    razorpay_order_id VARCHAR(255),
    razorpay_payment_id VARCHAR(255),
    razorpay_subscription_id VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE member_credit_ledger (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    reference_membership_id BIGINT REFERENCES member_memberships(id),
    amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    transaction_type VARCHAR(50) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    notes VARCHAR(1000),
    created_by_user_id BIGINT,
    created_by_email VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE membership_operation_idempotency (
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

CREATE TABLE processed_webhook_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE api_request_log (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT,
    request_id VARCHAR(255) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    response_hash VARCHAR(128) NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE notification_delivery_logs (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT REFERENCES notifications(id),
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    last_attempt TIMESTAMPTZ,
    provider VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    template_name VARCHAR(100),
    recipient VARCHAR(255),
    provider_message_id VARCHAR(255),
    error_message VARCHAR(1000),
    next_retry_at TIMESTAMPTZ,
    request_payload TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE whatsapp_reminder_logs (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    member_id BIGINT NOT NULL REFERENCES members(id),
    message TEXT NOT NULL,
    sent_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE referrals (
    id BIGSERIAL PRIMARY KEY,
    referrer_org_id BIGINT NOT NULL REFERENCES organizations(id),
    referred_org_id BIGINT NOT NULL UNIQUE REFERENCES organizations(id),
    referral_code_used VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reward_type VARCHAR(50),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE referral_rewards (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    reward_type VARCHAR(50) NOT NULL,
    amount NUMERIC(12,2) DEFAULT 0.00,
    months_rewarded INT NOT NULL DEFAULT 0,
    reward_used BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE platform_settings (
    id BIGSERIAL PRIMARY KEY,
    referrals_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE otps (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_organizations_admin_email_active
    ON organizations(admin_email)
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX uk_organizations_admin_phone_active
    ON organizations(admin_phone_number)
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX idx_org_referral_code
    ON organizations(referral_code)
    WHERE referral_code IS NOT NULL;

CREATE INDEX idx_org_deleted ON organizations(deleted);
CREATE INDEX idx_org_status_deleted ON organizations(status, deleted);
CREATE INDEX idx_org_type_status_due ON organizations(organization_type, status, subscription_due_date, deleted);
CREATE INDEX idx_org_subscription_plan ON organizations(subscription_plan, deleted);
CREATE INDEX idx_org_created_deleted ON organizations(created_at DESC, deleted);
CREATE INDEX idx_org_admin_email_deleted ON organizations(admin_email, deleted);
CREATE INDEX idx_org_admin_phone_deleted ON organizations(admin_phone_number, deleted);
CREATE INDEX idx_organizations_name_trgm ON organizations USING gin (lower(name) gin_trgm_ops) WHERE deleted = FALSE;
CREATE INDEX idx_organizations_place_trgm ON organizations USING gin (lower(place) gin_trgm_ops) WHERE deleted = FALSE AND place IS NOT NULL;
CREATE INDEX idx_organizations_district_trgm ON organizations USING gin (lower(district) gin_trgm_ops) WHERE deleted = FALSE;
CREATE INDEX idx_organizations_admin_email_trgm ON organizations USING gin (lower(admin_email) gin_trgm_ops) WHERE deleted = FALSE;
CREATE INDEX idx_organizations_admin_name_trgm ON organizations USING gin (lower(admin_full_name) gin_trgm_ops) WHERE deleted = FALSE;

CREATE INDEX idx_users_org_deleted ON users(organization_id, deleted);
CREATE INDEX idx_users_org_role_deleted ON users(organization_id, role, deleted);
CREATE INDEX idx_users_status_deleted ON users(status, deleted);

CREATE INDEX idx_members_org_deleted ON members(organization_id, deleted);
CREATE INDEX idx_members_org_status_deleted ON members(organization_id, status, deleted);
CREATE INDEX idx_members_org_created_deleted ON members(organization_id, created_at DESC, deleted);
CREATE INDEX idx_members_org_phone_deleted_btree ON members(organization_id, phone, deleted);
CREATE INDEX idx_members_org_name_deleted_btree ON members(organization_id, full_name, deleted);
CREATE INDEX idx_members_phone ON members(phone, deleted);
CREATE INDEX idx_members_full_name_trgm ON members USING gin (lower(full_name) gin_trgm_ops) WHERE deleted = FALSE;
CREATE INDEX idx_members_phone_trgm ON members USING gin (lower(phone) gin_trgm_ops) WHERE deleted = FALSE;

CREATE INDEX idx_org_settings_org_deleted ON organization_settings(organization_id, deleted);

CREATE INDEX idx_member_memberships_member_deleted ON member_memberships(member_id, deleted);
CREATE INDEX idx_member_memberships_member_end_created ON member_memberships(member_id, end_date DESC, created_at DESC, deleted);
CREATE INDEX idx_member_memberships_org_status_deleted ON member_memberships(organization_id, status, deleted);
CREATE INDEX idx_member_memberships_org_status_end ON member_memberships(organization_id, status, end_date, deleted);
CREATE INDEX idx_member_memberships_org_member_start ON member_memberships(organization_id, member_id, start_date DESC, created_at DESC, deleted);
CREATE INDEX idx_member_memberships_org_last_sent ON member_memberships(organization_id, last_sent_at, status, deleted);
CREATE INDEX idx_notifications_last_sent ON member_memberships(last_sent_at, status, deleted);

CREATE UNIQUE INDEX uk_member_payments_gateway_transaction_active
    ON member_payments(gateway_transaction_id)
    WHERE gateway_transaction_id IS NOT NULL AND deleted = FALSE;

CREATE INDEX idx_member_payments_org_deleted ON member_payments(organization_id, deleted);
CREATE INDEX idx_member_payments_org_payment_date ON member_payments(organization_id, payment_date DESC, deleted);
CREATE INDEX idx_member_payments_org_status_date ON member_payments(organization_id, payment_status, payment_date DESC, deleted);
CREATE INDEX idx_member_payments_org_type_date ON member_payments(organization_id, payment_type, payment_date DESC, deleted);
CREATE INDEX idx_member_payments_member_billing ON member_payments(member_id, billing_month, deleted);
CREATE INDEX idx_member_payments_member_deleted ON member_payments(member_id, deleted);
CREATE INDEX idx_member_payments_membership_deleted ON member_payments(membership_id, deleted);
CREATE INDEX idx_member_payments_billing_month_deleted ON member_payments(organization_id, billing_month, deleted);
CREATE INDEX idx_member_memberships_billing_month ON member_payments(billing_month, deleted);
CREATE INDEX idx_member_payments_gateway_txn ON member_payments(gateway_transaction_id, deleted);

CREATE UNIQUE INDEX uk_notifications_org_date_digest_channel_sent
    ON notifications(organization_id, notification_date, type, channel)
    WHERE deleted = FALSE
      AND type = 'ORGANIZATION_RENEWAL_DIGEST'
      AND status = 'SENT';

CREATE INDEX idx_notifications_org_deleted ON notifications(organization_id, deleted);
CREATE INDEX idx_notifications_org_type_created ON notifications(organization_id, type, created_at DESC, deleted);
CREATE INDEX idx_notifications_org_status_created ON notifications(organization_id, status, created_at DESC, deleted);
CREATE INDEX idx_notifications_org_channel_status_created ON notifications(organization_id, channel, status, created_at DESC, deleted);
CREATE INDEX idx_notifications_org_date_type_channel ON notifications(organization_id, notification_date, type, channel, deleted);
CREATE INDEX idx_notifications_org_date_type ON notifications(organization_id, notification_date, type, deleted);

CREATE INDEX idx_usage_metrics_org_month ON usage_metrics(organization_id, year, month, deleted);
CREATE INDEX idx_usage_metrics_month ON usage_metrics(year, month, deleted);

CREATE INDEX idx_audit_ts ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_zone ON audit_logs(zone);
CREATE INDEX idx_audit_category ON audit_logs(category);
CREATE INDEX idx_audit_org_scope_ts ON audit_logs(organization_id, activity_scope, timestamp DESC);
CREATE INDEX idx_audit_org_event_ts ON audit_logs(organization_id, event, timestamp DESC);
CREATE INDEX idx_audit_org_scope_status_ts ON audit_logs(organization_id, activity_scope, event_status, timestamp DESC);
CREATE INDEX idx_audit_login_status_ts ON audit_logs(activity_scope, event_status, timestamp DESC);

CREATE UNIQUE INDEX uk_org_subscription_payment_order
    ON organization_subscription_payments(razorpay_order_id)
    WHERE razorpay_order_id IS NOT NULL AND deleted = FALSE;

CREATE UNIQUE INDEX uk_org_subscription_payment_payment
    ON organization_subscription_payments(razorpay_payment_id)
    WHERE razorpay_payment_id IS NOT NULL AND deleted = FALSE;

CREATE UNIQUE INDEX uk_org_subscription_payment_gateway_txn
    ON organization_subscription_payments(gateway_transaction_id)
    WHERE gateway_transaction_id IS NOT NULL AND deleted = FALSE;

CREATE UNIQUE INDEX uk_org_subscription_payment_webhook_event
    ON organization_subscription_payments(webhook_event_id)
    WHERE webhook_event_id IS NOT NULL AND deleted = FALSE;

CREATE INDEX idx_org_subscription_payments_org_status ON organization_subscription_payments(organization_id, payment_status, deleted);
CREATE INDEX idx_org_subscription_payments_org_created ON organization_subscription_payments(organization_id, created_at DESC, deleted);
CREATE INDEX idx_org_subscription_payment_retry ON organization_subscription_payments(payment_status, next_retry_date, deleted);
CREATE INDEX idx_org_subscription_payment_subscription ON organization_subscription_payments(razorpay_subscription_id, created_at DESC, deleted);
CREATE INDEX idx_org_subscription_payment_order ON organization_subscription_payments(razorpay_order_id, deleted);
CREATE INDEX idx_org_subscription_payment_gateway ON organization_subscription_payments(gateway_transaction_id, deleted);
CREATE INDEX idx_org_subscription_payment_paid_date ON organization_subscription_payments(payment_status, payment_date, deleted);

CREATE INDEX idx_member_credit_ledger_member ON member_credit_ledger(member_id, organization_id, deleted);
CREATE INDEX idx_member_credit_ledger_organization_created ON member_credit_ledger(organization_id, created_at DESC, deleted);
CREATE INDEX idx_member_credit_ledger_membership ON member_credit_ledger(reference_membership_id, transaction_type, deleted);

CREATE UNIQUE INDEX uk_membership_operation_idempotency
    ON membership_operation_idempotency(organization_id, member_id, operation_type, idempotency_key)
    WHERE deleted = FALSE;

CREATE INDEX idx_membership_idempotency_org_key ON membership_operation_idempotency(organization_id, idempotency_key, deleted);
CREATE INDEX idx_membership_idempotency_member_operation ON membership_operation_idempotency(organization_id, member_id, operation_type, deleted);
CREATE INDEX idx_membership_idempotency_resource ON membership_operation_idempotency(resource_type, resource_id, deleted);

CREATE INDEX idx_processed_webhook_events_type_processed ON processed_webhook_events(event_type, processed_at DESC, deleted);

CREATE UNIQUE INDEX uk_api_request_log_org_request_endpoint
    ON api_request_log(organization_id, request_id, endpoint);

CREATE INDEX idx_api_request_org_request_endpoint ON api_request_log(organization_id, request_id, endpoint);
CREATE INDEX idx_api_request_org_created ON api_request_log(organization_id, created_at DESC);

CREATE INDEX idx_notification_delivery_org_type_attempt ON notification_delivery_logs(organization_id, type, last_attempt, status);
CREATE INDEX idx_notification_delivery_provider_retry ON notification_delivery_logs(provider, status, next_retry_at);
CREATE INDEX idx_notification_delivery_provider_message ON notification_delivery_logs(provider_message_id, deleted);
CREATE INDEX idx_notification_delivery_notification ON notification_delivery_logs(notification_id);

CREATE INDEX idx_whatsapp_reminder_logs_org_sent ON whatsapp_reminder_logs(organization_id, sent_at DESC, deleted);
CREATE INDEX idx_whatsapp_reminder_logs_org_status_sent ON whatsapp_reminder_logs(organization_id, status, sent_at DESC, deleted);
CREATE INDEX idx_whatsapp_reminder_logs_member_created ON whatsapp_reminder_logs(member_id, created_at DESC, deleted);

CREATE INDEX idx_referrals_referrer ON referrals(referrer_org_id);
CREATE INDEX idx_referrals_status ON referrals(status);
CREATE INDEX idx_referral_rewards_org ON referral_rewards(org_id);

CREATE INDEX idx_otps_email_code_verified ON otps(email, otp_code, verified);
CREATE INDEX idx_otps_email_created ON otps(email, created_at DESC);
CREATE INDEX idx_otps_expires ON otps(expires_at);

INSERT INTO platform_settings (id, referrals_enabled, deleted, created_at, updated_at)
VALUES (1, TRUE, FALSE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

SELECT setval('platform_settings_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM platform_settings), 1), TRUE);
