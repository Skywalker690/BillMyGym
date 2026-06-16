ALTER TABLE notifications ALTER COLUMN message TYPE TEXT;
ALTER TABLE notifications ALTER COLUMN failure_reason TYPE TEXT;
ALTER TABLE notification_delivery_logs ALTER COLUMN error_message TYPE TEXT;
