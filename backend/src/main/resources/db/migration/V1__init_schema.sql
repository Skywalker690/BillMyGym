CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    district VARCHAR(255) NOT NULL,
    logo_url VARCHAR(512),
    map_location_link VARCHAR(512),
    address VARCHAR(512),
    admin_full_name VARCHAR(255) NOT NULL,
    admin_phone_number VARCHAR(100) NOT NULL,
    admin_email VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    joining_date DATE NOT NULL,
    membership_start_date DATE NOT NULL,
    membership_end_date DATE NOT NULL,
    membership_status VARCHAR(50) NOT NULL,
    notes TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE organization_settings (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL UNIQUE REFERENCES organizations(id),
    whatsapp_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_days_before_expiry INT NOT NULL DEFAULT 1,
    branding_name VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE organization_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    razorpay_subscription_id VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    member_id BIGINT NOT NULL REFERENCES members(id),
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    message TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE usage_metrics (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    year INT NOT NULL,
    month INT NOT NULL,
    whatsapp_messages_sent INT NOT NULL DEFAULT 0,
    emails_sent INT NOT NULL DEFAULT 0,
    active_members INT NOT NULL DEFAULT 0,
    active_admins INT NOT NULL DEFAULT 0,
    subscription_status VARCHAR(50) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_usage_metrics_org_year_month UNIQUE (organization_id, year, month)
);

CREATE INDEX idx_members_org_deleted ON members(organization_id, deleted);
CREATE INDEX idx_members_expiry_deleted ON members(membership_end_date, deleted);
CREATE INDEX idx_users_org_deleted ON users(organization_id, deleted);
CREATE INDEX idx_org_sub_org_deleted ON organization_subscriptions(organization_id, deleted);
CREATE INDEX idx_notifications_org_deleted ON notifications(organization_id, deleted);
CREATE INDEX idx_org_deleted ON organizations(deleted);
