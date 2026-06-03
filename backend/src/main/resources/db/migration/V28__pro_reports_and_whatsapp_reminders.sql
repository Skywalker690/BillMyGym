DROP INDEX IF EXISTS uk_notifications_org_date_type_active;

CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_org_date_digest_active
    ON notifications (organization_id, notification_date, type)
    WHERE deleted = FALSE
      AND type = 'ORGANIZATION_RENEWAL_DIGEST';

CREATE TABLE IF NOT EXISTS whatsapp_reminder_logs (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    member_id BIGINT NOT NULL REFERENCES members(id),
    message TEXT NOT NULL,
    sent_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_whatsapp_reminder_logs_org_sent
    ON whatsapp_reminder_logs(organization_id, sent_at DESC, deleted);

CREATE INDEX IF NOT EXISTS idx_whatsapp_reminder_logs_member_created
    ON whatsapp_reminder_logs(member_id, created_at DESC, deleted);
