CREATE INDEX IF NOT EXISTS idx_members_org_created_deleted
    ON members (organization_id, created_at DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_members_org_status_deleted
    ON members (organization_id, status, deleted);

CREATE INDEX IF NOT EXISTS idx_member_memberships_org_member_start
    ON member_memberships (organization_id, member_id, start_date DESC, created_at DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_member_memberships_org_status_end
    ON member_memberships (organization_id, status, end_date, deleted);

CREATE INDEX IF NOT EXISTS idx_member_payments_org_payment_date
    ON member_payments (organization_id, payment_date DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_notifications_org_type_created
    ON notifications (organization_id, type, created_at DESC, deleted);
