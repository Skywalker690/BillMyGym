INSERT INTO organizations (id, name, district, logo_url, map_location_link, address, admin_full_name, admin_phone_number, admin_email, description, monthly_membership_fee, organization_type, status, trial_start_date, trial_end_date, pilot_start_date, pilot_end_date, subscription_due_date, paused_at, deactivated_at, deleted, created_at, updated_at) VALUES
(1, 'Gym Alpha', 'Central', 'https://img.example.com/alpha.png', 'https://maps.example.com/alpha', '101 Alpha Street', 'Aarav Mehta', '+919100000001', 'alpha.admin@billmygym.local', 'Premium strength and conditioning center', 1000.00, 'REGULAR', 'ACTIVE', '2026-01-01', '2026-01-15', NULL, NULL, '2026-06-15', NULL, NULL, FALSE, '2026-01-01T09:00:00Z', '2026-01-01T09:00:00Z'),
(2, 'Gym Bravo', 'North', 'https://img.example.com/bravo.png', 'https://maps.example.com/bravo', '102 Bravo Street', 'Ira Kapoor', '+919100000002', 'bravo.admin@billmygym.local', 'Community-driven fitness space', 1200.00, 'PILOT', 'ACTIVE', NULL, NULL, '2026-05-01', '2026-07-30', '2026-07-30', NULL, NULL, FALSE, '2026-01-02T09:00:00Z', '2026-01-02T09:00:00Z'),
(3, 'Gym Charlie', 'South', 'https://img.example.com/charlie.png', 'https://maps.example.com/charlie', '103 Charlie Street', 'Rohan Bhat', '+919100000003', 'charlie.admin@billmygym.local', 'Modern functional training gym', 900.00, 'REGULAR', 'PAUSED', '2026-01-03', '2026-01-17', NULL, NULL, '2026-04-15', '2026-04-16T09:00:00Z', NULL, FALSE, '2026-01-03T09:00:00Z', '2026-01-03T09:00:00Z');

INSERT INTO users (id, organization_id, email, password_hash, role, status, deleted, created_at, updated_at) VALUES
(1, NULL, 'sanjoksiby10@gmail.com', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'SUPER_ADMIN', 'ACTIVE', FALSE, '2026-01-01T10:00:00Z', '2026-01-01T10:00:00Z'),
(2, 1, 'sanjoksiby0@gmail.com', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, '2026-01-02T10:00:00Z', '2026-01-02T10:00:00Z'),
(3, 2, 'admin2@billmygym.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, '2026-01-03T10:00:00Z', '2026-01-03T10:00:00Z'),
(4, 3, 'admin3@billmygym.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, '2026-01-04T10:00:00Z', '2026-01-04T10:00:00Z');

INSERT INTO members (id, organization_id, full_name, phone, email, status, deleted, deleted_at, deactivated_at, deactivation_reason, notes, created_at, updated_at) VALUES
(1, 1, 'Raj Patel', '+919800000001', 'raj.patel@example.com', 'ACTIVE', FALSE, NULL, NULL, NULL, 'Prefers evening sessions', '2026-01-01T11:00:00Z', '2026-01-01T11:00:00Z'),
(2, 1, 'Nisha Kapoor', '+919800000002', 'nisha.kapoor@example.com', 'ACTIVE', FALSE, NULL, NULL, NULL, 'Will renew monthly', '2026-01-02T11:00:00Z', '2026-01-02T11:00:00Z'),
(3, 2, 'Kunal Singh', '+919800000003', 'kunal.singh@example.com', 'ACTIVE', FALSE, NULL, NULL, NULL, 'Pending renewal discussion', '2026-01-03T11:00:00Z', '2026-01-03T11:00:00Z'),
(4, 3, 'Dev Khanna', '+919800000004', NULL, 'DEACTIVATED', FALSE, NULL, '2026-04-10T09:00:00Z', 'Left gym', 'Historical ledger should remain', '2026-01-04T11:00:00Z', '2026-01-04T11:00:00Z');

INSERT INTO member_memberships (id, member_id, organization_id, plan_name, duration_months, plan_amount, start_date, end_date, status, last_sent_at, deleted, deleted_at, created_at, updated_at) VALUES
(1, 1, 1, 'Annual Strength', 12, 12000.00, '2026-05-01', '2027-04-30', 'ACTIVE', NULL, FALSE, NULL, '2026-05-01T11:00:00Z', '2026-05-01T11:00:00Z'),
(2, 2, 1, 'Quarterly Flex', 3, 3600.00, '2026-05-01', '2026-07-31', 'ACTIVE', NULL, FALSE, NULL, '2026-05-01T12:00:00Z', '2026-05-01T12:00:00Z'),
(3, 3, 2, 'Six Month Core', 6, 5400.00, '2025-11-15', '2026-05-14', 'OVERDUE', NULL, FALSE, NULL, '2025-11-15T11:00:00Z', '2026-05-16T11:00:00Z'),
(4, 4, 3, 'Annual Mobility', 12, 12000.00, '2025-04-01', '2026-03-31', 'COMPLETED', '2026-04-01T10:00:00Z', FALSE, NULL, '2025-04-01T11:00:00Z', '2026-04-10T11:00:00Z');

INSERT INTO member_payments (id, member_id, membership_id, organization_id, amount, payment_date, billing_month, payment_status, payment_type, gateway_transaction_id, notes, deleted, deleted_at, created_at, updated_at) VALUES
(1, 1, 1, 1, 12000.00, '2026-05-01', '2026-05', 'PAID', 'NEW_MEMBERSHIP', 'txn_annual_001', 'Annual prepaid plan', FALSE, NULL, '2026-05-01T16:00:00Z', '2026-05-01T16:00:00Z'),
(2, 2, 2, 1, 1000.00, '2026-05-01', '2026-05', 'PARTIAL', 'NEW_MEMBERSHIP', NULL, 'Partial upfront payment', FALSE, NULL, '2026-05-01T16:30:00Z', '2026-05-01T16:30:00Z'),
(3, 3, 3, 2, 0.00, '2026-05-16', '2026-05', 'FAILED', 'RENEWAL', 'txn_failed_003', 'Card charge failed', FALSE, NULL, '2026-05-16T09:30:00Z', '2026-05-16T09:30:00Z'),
(4, 4, 4, 3, 12000.00, '2025-04-01', '2025-04', 'PAID', 'NEW_MEMBERSHIP', 'txn_hist_004', 'Historical paid membership', FALSE, NULL, '2025-04-01T16:00:00Z', '2025-04-01T16:00:00Z');

INSERT INTO organization_settings (id, organization_id, whatsapp_enabled, email_enabled, reminder_days_before_expiry, branding_name, deleted, created_at, updated_at) VALUES
(1, 1, TRUE, TRUE, 3, 'Gym Alpha Pro', FALSE, '2026-01-01T12:00:00Z', '2026-01-01T12:00:00Z'),
(2, 2, TRUE, FALSE, 2, 'Gym Bravo Fit', FALSE, '2026-01-02T12:00:00Z', '2026-01-02T12:00:00Z'),
(3, 3, FALSE, TRUE, 1, 'Gym Charlie Core', FALSE, '2026-01-03T12:00:00Z', '2026-01-03T12:00:00Z');

INSERT INTO organization_subscriptions (id, organization_id, start_date, end_date, status, amount, razorpay_subscription_id, deleted, created_at, updated_at) VALUES
(1, 1, '2026-05-16', '2026-06-14', 'ACTIVE', 299.00, 'sub_alpha_001', FALSE, '2026-05-16T13:00:00Z', '2026-05-16T13:00:00Z'),
(2, 3, '2026-03-16', '2026-04-14', 'EXPIRED', 299.00, 'sub_charlie_003', FALSE, '2026-03-16T13:00:00Z', '2026-04-15T13:00:00Z');

INSERT INTO organization_subscription_payments (id, organization_id, amount, payment_date, subscription_start, subscription_end, payment_status, retry_count, next_retry_date, failure_reason, webhook_event_id, gateway_transaction_id, razorpay_order_id, razorpay_payment_id, razorpay_subscription_id, deleted, created_at, updated_at) VALUES
(1, 1, 299.00, '2026-05-16', '2026-05-16', '2026-06-14', 'PAID', 0, NULL, NULL, NULL, 'pay_alpha_001', 'order_alpha_001', 'pay_alpha_001', 'sub_alpha_001', FALSE, '2026-05-16T13:00:00Z', '2026-05-16T13:00:00Z'),
(2, 3, 299.00, '2026-04-15', '2026-04-15', '2026-05-14', 'FAILED', 1, '2026-04-17', 'Card charge failed', 'payment.failed:-:pay_charlie_failed:-', 'pay_charlie_failed', 'order_charlie_001', 'pay_charlie_failed', 'sub_charlie_003', FALSE, '2026-04-15T13:00:00Z', '2026-04-15T13:00:00Z');

INSERT INTO notifications (id, organization_id, member_id, member_count, type, channel, recipient, status, attempt_count, failure_reason, notification_date, sent_at, message, deleted, created_at, updated_at) VALUES
(1, 2, NULL, 1, 'ORGANIZATION_RENEWAL_DIGEST', 'WHATSAPP', '+919100000002', 'SENT', 1, NULL, '2026-05-16', '2026-05-16T08:00:00Z', 'BillMyGym Alert: overdue memberships', FALSE, '2026-05-16T08:00:00Z', '2026-05-16T08:00:00Z');

INSERT INTO usage_metrics (id, organization_id, year, month, monthly_whatsapp_count, monthly_email_count, monthly_digest_count, active_members, active_admins, subscription_status, deleted, created_at, updated_at) VALUES
(1, 1, 2026, 5, 40, 22, 8, 2, 1, 'ACTIVE', FALSE, '2026-05-01T15:00:00Z', '2026-05-01T15:00:00Z'),
(2, 2, 2026, 5, 18, 6, 4, 1, 1, 'ACTIVE', FALSE, '2026-05-01T15:00:00Z', '2026-05-01T15:00:00Z'),
(3, 3, 2026, 5, 4, 12, 2, 0, 1, 'PAUSED', FALSE, '2026-05-01T15:00:00Z', '2026-05-01T15:00:00Z');

SELECT setval('organizations_id_seq', COALESCE((SELECT MAX(id) FROM organizations), 1), TRUE);
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1), TRUE);
SELECT setval('members_id_seq', COALESCE((SELECT MAX(id) FROM members), 1), TRUE);
SELECT setval('member_memberships_id_seq', COALESCE((SELECT MAX(id) FROM member_memberships), 1), TRUE);
SELECT setval('organization_settings_id_seq', COALESCE((SELECT MAX(id) FROM organization_settings), 1), TRUE);
SELECT setval('organization_subscriptions_id_seq', COALESCE((SELECT MAX(id) FROM organization_subscriptions), 1), TRUE);
SELECT setval('organization_subscription_payments_id_seq', COALESCE((SELECT MAX(id) FROM organization_subscription_payments), 1), TRUE);
SELECT setval('member_payments_id_seq', COALESCE((SELECT MAX(id) FROM member_payments), 1), TRUE);
SELECT setval('notifications_id_seq', COALESCE((SELECT MAX(id) FROM notifications), 1), TRUE);
SELECT setval('usage_metrics_id_seq', COALESCE((SELECT MAX(id) FROM usage_metrics), 1), TRUE);

