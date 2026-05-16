ALTER TABLE member_memberships
    DROP COLUMN IF EXISTS renewed,
    DROP COLUMN IF EXISTS pause_start,
    DROP COLUMN IF EXISTS pause_end,
    DROP COLUMN IF EXISTS paused_days;

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS notification_date DATE;

UPDATE notifications
SET notification_date = COALESCE(notification_date, COALESCE(sent_at::date, created_at::date));

ALTER TABLE notifications
    ALTER COLUMN notification_date SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_org_date_type_active
    ON notifications (organization_id, notification_date, type)
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_member_memberships_one_active_per_member
    ON member_memberships (member_id)
    WHERE deleted = FALSE AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_members_org_phone_deleted
    ON members (organization_id, phone, deleted);

DROP INDEX IF EXISTS idx_member_memberships_org_status_deleted;

CREATE INDEX IF NOT EXISTS idx_member_memberships_org_status_deleted
    ON member_memberships (organization_id, status, deleted);
