CREATE EXTENSION IF NOT EXISTS pg_trgm;

DROP INDEX IF EXISTS uk_notifications_org_date_digest_active;

CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_org_date_digest_channel_sent
    ON notifications (organization_id, notification_date, type, channel)
    WHERE deleted = FALSE
      AND type = 'ORGANIZATION_RENEWAL_DIGEST'
      AND status = 'SENT';

CREATE INDEX IF NOT EXISTS idx_members_org_phone_deleted_btree
    ON members (organization_id, phone, deleted);

CREATE INDEX IF NOT EXISTS idx_members_org_name_deleted_btree
    ON members (organization_id, full_name, deleted);

CREATE INDEX IF NOT EXISTS idx_members_full_name_trgm
    ON members USING gin (lower(full_name) gin_trgm_ops)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_members_phone_trgm
    ON members USING gin (lower(phone) gin_trgm_ops)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_member_payments_org_status_date
    ON member_payments (organization_id, payment_status, payment_date DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_member_payments_org_type_date
    ON member_payments (organization_id, payment_type, payment_date DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_member_payments_gateway_txn
    ON member_payments (gateway_transaction_id, deleted);

CREATE INDEX IF NOT EXISTS idx_audit_org_event_ts
    ON audit_logs (organization_id, event, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_audit_org_scope_status_ts
    ON audit_logs (organization_id, activity_scope, event_status, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_org_status_created
    ON notifications (organization_id, status, created_at DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_notifications_org_channel_status_created
    ON notifications (organization_id, channel, status, created_at DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_notifications_org_date_type_channel
    ON notifications (organization_id, notification_date, type, channel, deleted);

CREATE INDEX IF NOT EXISTS idx_organizations_name_trgm
    ON organizations USING gin (lower(name) gin_trgm_ops)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_organizations_place_trgm
    ON organizations USING gin (lower(place) gin_trgm_ops)
    WHERE deleted = FALSE AND place IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_organizations_district_trgm
    ON organizations USING gin (lower(district) gin_trgm_ops)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_organizations_admin_email_trgm
    ON organizations USING gin (lower(admin_email) gin_trgm_ops)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_organizations_admin_name_trgm
    ON organizations USING gin (lower(admin_full_name) gin_trgm_ops)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_org_status_deleted
    ON organizations (status, deleted);

CREATE INDEX IF NOT EXISTS idx_org_type_status_due
    ON organizations (organization_type, status, subscription_due_date, deleted);

CREATE INDEX IF NOT EXISTS idx_org_created_deleted
    ON organizations (created_at DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_org_admin_email_deleted
    ON organizations (admin_email, deleted);

CREATE INDEX IF NOT EXISTS idx_org_admin_phone_deleted
    ON organizations (admin_phone_number, deleted);

CREATE INDEX IF NOT EXISTS idx_org_sub_org_end_deleted
    ON organization_subscriptions (organization_id, end_date DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_org_sub_status_end_deleted
    ON organization_subscriptions (status, end_date DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_org_subscription_payments_org_created
    ON organization_subscription_payments (organization_id, created_at DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_org_subscription_payment_order
    ON organization_subscription_payments (razorpay_order_id, deleted);

CREATE INDEX IF NOT EXISTS idx_org_subscription_payment_gateway
    ON organization_subscription_payments (gateway_transaction_id, deleted);

CREATE INDEX IF NOT EXISTS idx_org_subscription_payment_paid_date
    ON organization_subscription_payments (payment_status, payment_date, deleted);

CREATE INDEX IF NOT EXISTS idx_whatsapp_reminder_logs_org_status_sent
    ON whatsapp_reminder_logs (organization_id, status, sent_at DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_org_usage_stats_org_month
    ON organization_usage_stats (organization_id, year, month, deleted);

CREATE INDEX IF NOT EXISTS idx_usage_metrics_org_month
    ON usage_metrics (organization_id, year, month, deleted);

CREATE INDEX IF NOT EXISTS idx_users_org_role_deleted
    ON users (organization_id, role, deleted);

CREATE INDEX IF NOT EXISTS idx_users_status_deleted
    ON users (status, deleted);

CREATE INDEX IF NOT EXISTS idx_org_settings_org_deleted
    ON organization_settings (organization_id, deleted);

CREATE INDEX IF NOT EXISTS idx_membership_idempotency_org_key
    ON membership_operation_idempotency (organization_id, idempotency_key, deleted);

CREATE INDEX IF NOT EXISTS idx_membership_idempotency_member_operation
    ON membership_operation_idempotency (organization_id, member_id, operation_type, deleted);

CREATE INDEX IF NOT EXISTS idx_membership_idempotency_resource
    ON membership_operation_idempotency (resource_type, resource_id, deleted);

CREATE INDEX IF NOT EXISTS idx_processed_webhook_events_type_processed
    ON processed_webhook_events (event_type, processed_at DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_otps_email_code_verified
    ON otps (email, otp_code, verified);

CREATE INDEX IF NOT EXISTS idx_otps_email_created
    ON otps (email, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_otps_expires
    ON otps (expires_at);

CREATE INDEX IF NOT EXISTS idx_api_request_org_request_endpoint
    ON api_request_log (organization_id, request_id, endpoint);

CREATE INDEX IF NOT EXISTS idx_api_request_org_created
    ON api_request_log (organization_id, created_at DESC);
