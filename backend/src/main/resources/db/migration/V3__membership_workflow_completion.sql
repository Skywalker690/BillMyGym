ALTER TABLE members
    ADD COLUMN membership_duration VARCHAR(50) NOT NULL DEFAULT 'ONE_MONTH',
    ADD COLUMN custom_duration_days INT,
    ADD COLUMN overdue_marked_at TIMESTAMPTZ,
    ADD COLUMN deleted_at TIMESTAMPTZ;

UPDATE members
SET membership_duration = CASE
    WHEN joining_date + INTERVAL '1 month' = membership_end_date THEN 'ONE_MONTH'
    WHEN joining_date + INTERVAL '3 month' = membership_end_date THEN 'THREE_MONTHS'
    WHEN joining_date + INTERVAL '6 month' = membership_end_date THEN 'SIX_MONTHS'
    WHEN joining_date + INTERVAL '12 month' = membership_end_date THEN 'TWELVE_MONTHS'
    ELSE 'CUSTOM'
END,
custom_duration_days = CASE
    WHEN joining_date + INTERVAL '1 month' = membership_end_date THEN NULL
    WHEN joining_date + INTERVAL '3 month' = membership_end_date THEN NULL
    WHEN joining_date + INTERVAL '6 month' = membership_end_date THEN NULL
    WHEN joining_date + INTERVAL '12 month' = membership_end_date THEN NULL
    ELSE (membership_end_date - joining_date)
END;

UPDATE members
SET overdue_marked_at = updated_at
WHERE membership_status = 'OVERDUE';

ALTER TABLE notifications
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 0,
    ADD COLUMN failure_reason VARCHAR(1000),
    ADD COLUMN sent_at TIMESTAMPTZ;

UPDATE notifications
SET status = CASE
    WHEN status = 'QUEUED' THEN 'PENDING'
    WHEN status = 'FAILED' THEN 'FAILED'
    ELSE 'SENT'
END,
attempt_count = CASE
    WHEN status = 'QUEUED' THEN 0
    ELSE 1
END,
sent_at = CASE
    WHEN status = 'SENT' THEN updated_at
    ELSE NULL
END,
type = CASE
    WHEN type = 'OVERDUE_PAYMENT' THEN 'MEMBERSHIP_OVERDUE'
    ELSE type
END;

ALTER TABLE usage_metrics
    RENAME COLUMN whatsapp_messages_sent TO monthly_whatsapp_count;

ALTER TABLE usage_metrics
    RENAME COLUMN emails_sent TO monthly_email_count;

ALTER TABLE usage_metrics
    ADD COLUMN monthly_reminders_executed INT NOT NULL DEFAULT 0;
