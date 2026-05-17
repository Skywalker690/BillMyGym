ALTER TABLE members
    ADD COLUMN renewed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN last_reminder_sent DATE,
    ADD COLUMN reminder_count INT NOT NULL DEFAULT 0;

ALTER TABLE notifications
    ALTER COLUMN member_id DROP NOT NULL;

ALTER TABLE notifications
    ADD COLUMN member_count INT NOT NULL DEFAULT 1;

UPDATE notifications
SET member_count = 1;

UPDATE members
SET membership_status = CASE
    WHEN membership_status = 'EXPIRED' AND membership_end_date < CURRENT_DATE THEN 'OVERDUE'
    WHEN membership_status = 'EXPIRED' AND membership_end_date = CURRENT_DATE THEN 'EXPIRED'
    WHEN membership_status = 'ACTIVE' AND membership_end_date = CURRENT_DATE THEN 'EXPIRED'
    ELSE membership_status
END;

ALTER TABLE usage_metrics
    RENAME COLUMN monthly_reminders_executed TO monthly_digest_count;

UPDATE notifications
SET type = 'ORGANIZATION_RENEWAL_DIGEST'
WHERE type <> 'ORGANIZATION_RENEWAL_DIGEST';
