INSERT INTO organizations (id, name, district, logo_url, map_location_link, address, admin_full_name, admin_phone_number, admin_email, description, status, deleted, created_at, updated_at) VALUES
(1, 'Gym Alpha', 'Central', 'https://img.example.com/alpha.png', 'https://maps.example.com/alpha', '101 Alpha Street', 'Aarav Mehta', '+919100000001', 'alpha.admin@gymflow.local', 'Premium strength and conditioning center', 'ACTIVE', FALSE, '2026-01-01T09:00:00Z', '2026-01-01T09:00:00Z'),
(2, 'Gym Bravo', 'North', 'https://img.example.com/bravo.png', 'https://maps.example.com/bravo', '102 Bravo Street', 'Ira Kapoor', '+919100000002', 'bravo.admin@gymflow.local', 'Community-driven fitness space', 'ACTIVE', FALSE, '2026-01-02T09:00:00Z', '2026-01-02T09:00:00Z'),
(3, 'Gym Charlie', 'South', 'https://img.example.com/charlie.png', 'https://maps.example.com/charlie', '103 Charlie Street', 'Rohan Bhat', '+919100000003', 'charlie.admin@gymflow.local', 'Modern functional training gym', 'ACTIVE', FALSE, '2026-01-03T09:00:00Z', '2026-01-03T09:00:00Z'),
(4, 'Gym Delta', 'East', 'https://img.example.com/delta.png', 'https://maps.example.com/delta', '104 Delta Street', 'Sneha Iyer', '+919100000004', 'delta.admin@gymflow.local', 'Focused on transformation programs', 'ACTIVE', FALSE, '2026-01-04T09:00:00Z', '2026-01-04T09:00:00Z'),
(5, 'Gym Echo', 'West', 'https://img.example.com/echo.png', 'https://maps.example.com/echo', '105 Echo Street', 'Karan Malhotra', '+919100000005', 'echo.admin@gymflow.local', 'Strength and mobility training', 'INACTIVE', FALSE, '2026-01-05T09:00:00Z', '2026-01-05T09:00:00Z'),
(6, 'Gym Foxtrot', 'Central', 'https://img.example.com/foxtrot.png', 'https://maps.example.com/foxtrot', '106 Foxtrot Street', 'Neha Sinha', '+919100000006', 'foxtrot.admin@gymflow.local', 'Athlete-focused conditioning', 'ACTIVE', FALSE, '2026-01-06T09:00:00Z', '2026-01-06T09:00:00Z'),
(7, 'Gym Golf', 'North', 'https://img.example.com/golf.png', 'https://maps.example.com/golf', '107 Golf Street', 'Arjun Sharma', '+919100000007', 'golf.admin@gymflow.local', 'Boutique fitness and nutrition', 'SUSPENDED', FALSE, '2026-01-07T09:00:00Z', '2026-01-07T09:00:00Z'),
(8, 'Gym Hotel', 'South', 'https://img.example.com/hotel.png', 'https://maps.example.com/hotel', '108 Hotel Street', 'Priya Nair', '+919100000008', 'hotel.admin@gymflow.local', 'Beginner-friendly personal coaching', 'ACTIVE', FALSE, '2026-01-08T09:00:00Z', '2026-01-08T09:00:00Z'),
(9, 'Gym India', 'East', 'https://img.example.com/india.png', 'https://maps.example.com/india', '109 India Street', 'Vikram Desai', '+919100000009', 'india.admin@gymflow.local', 'HIIT and endurance programs', 'ACTIVE', FALSE, '2026-01-09T09:00:00Z', '2026-01-09T09:00:00Z'),
(10, 'Gym Juliet', 'West', 'https://img.example.com/juliet.png', 'https://maps.example.com/juliet', '110 Juliet Street', 'Maya Joshi', '+919100000010', 'juliet.admin@gymflow.local', 'Premium wellness and recovery studio', 'ACTIVE', FALSE, '2026-01-10T09:00:00Z', '2026-01-10T09:00:00Z');

INSERT INTO users (id, organization_id, email, password_hash, role, status, deleted, created_at, updated_at) VALUES
(1, NULL, 'superadmin1@gymflow.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'SUPER_ADMIN', 'ACTIVE', FALSE, '2026-01-01T10:00:00Z', '2026-01-01T10:00:00Z'),
(2, NULL, 'superadmin2@gymflow.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'SUPER_ADMIN', 'ACTIVE', FALSE, '2026-01-02T10:00:00Z', '2026-01-02T10:00:00Z'),
(3, 1, 'admin1@gymflow.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, '2026-01-03T10:00:00Z', '2026-01-03T10:00:00Z'),
(4, 2, 'admin2@gymflow.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, '2026-01-04T10:00:00Z', '2026-01-04T10:00:00Z'),
(5, 3, 'admin3@gymflow.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, '2026-01-05T10:00:00Z', '2026-01-05T10:00:00Z'),
(6, 4, 'admin4@gymflow.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, '2026-01-06T10:00:00Z', '2026-01-06T10:00:00Z'),
(7, 5, 'admin5@gymflow.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'INACTIVE', FALSE, '2026-01-07T10:00:00Z', '2026-01-07T10:00:00Z'),
(8, 6, 'admin6@gymflow.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, '2026-01-08T10:00:00Z', '2026-01-08T10:00:00Z'),
(9, 7, 'admin7@gymflow.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, '2026-01-09T10:00:00Z', '2026-01-09T10:00:00Z'),
(10, 8, 'admin8@gymflow.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, '2026-01-10T10:00:00Z', '2026-01-10T10:00:00Z');

INSERT INTO members (id, organization_id, full_name, phone_number, email, joining_date, membership_start_date, membership_end_date, membership_duration, custom_duration_days, membership_status, overdue_marked_at, notes, deleted, deleted_at, created_at, updated_at) VALUES
(1, 1, 'Raj Patel', '+919800000001', 'raj.patel@example.com', '2025-10-01', '2025-10-01', '2026-10-01', 'TWELVE_MONTHS', NULL, 'ACTIVE', NULL, 'Prefers evening sessions', FALSE, NULL, '2026-01-01T11:00:00Z', '2026-01-01T11:00:00Z'),
(2, 2, 'Ananya Rao', '+919800000002', 'ananya.rao@example.com', '2025-11-01', '2025-11-01', '2026-05-30', 'CUSTOM', 210, 'ACTIVE', NULL, 'Yoga and cardio plan', FALSE, NULL, '2026-01-02T11:00:00Z', '2026-01-02T11:00:00Z'),
(3, 3, 'Kunal Singh', '+919800000003', 'kunal.singh@example.com', '2025-09-15', '2025-09-15', '2026-03-15', 'SIX_MONTHS', NULL, 'OVERDUE', '2026-03-16T09:00:00Z', 'Pending renewal discussion', FALSE, NULL, '2026-01-03T11:00:00Z', '2026-01-03T11:00:00Z'),
(4, 4, 'Ishita Jain', '+919800000004', 'ishita.jain@example.com', '2025-08-20', '2025-08-20', '2026-08-19', 'CUSTOM', 364, 'ACTIVE', NULL, 'PT sessions twice weekly', FALSE, NULL, '2026-01-04T11:00:00Z', '2026-01-04T11:00:00Z'),
(5, 5, 'Dev Khanna', '+919800000005', 'dev.khanna@example.com', '2025-04-01', '2025-04-01', '2026-04-01', 'TWELVE_MONTHS', NULL, 'EXPIRED', NULL, 'Paused due to travel', FALSE, NULL, '2026-01-05T11:00:00Z', '2026-01-05T11:00:00Z'),
(6, 6, 'Meera Nanda', '+919800000006', 'meera.nanda@example.com', '2025-07-12', '2025-07-12', '2026-07-12', 'TWELVE_MONTHS', NULL, 'ACTIVE', NULL, 'Strength progression program', FALSE, NULL, '2026-01-06T11:00:00Z', '2026-01-06T11:00:00Z'),
(7, 7, 'Amit Verma', '+919800000007', 'amit.verma@example.com', '2025-06-01', '2025-06-01', '2026-02-01', 'CUSTOM', 245, 'OVERDUE', '2026-02-02T09:00:00Z', 'Requires payment reminder', FALSE, NULL, '2026-01-07T11:00:00Z', '2026-01-07T11:00:00Z'),
(8, 8, 'Riya Das', '+919800000008', 'riya.das@example.com', '2025-12-10', '2025-12-10', '2026-12-10', 'TWELVE_MONTHS', NULL, 'ACTIVE', NULL, 'Morning group classes', FALSE, NULL, '2026-01-08T11:00:00Z', '2026-01-08T11:00:00Z'),
(9, 9, 'Sahil Arora', '+919800000009', 'sahil.arora@example.com', '2025-03-05', '2025-03-05', '2026-03-04', 'CUSTOM', 364, 'EXPIRED', NULL, 'Needs follow-up call', FALSE, NULL, '2026-01-09T11:00:00Z', '2026-01-09T11:00:00Z'),
(10, 10, 'Pooja Kulkarni', '+919800000010', 'pooja.kulkarni@example.com', '2025-10-21', '2025-10-21', '2026-10-21', 'TWELVE_MONTHS', NULL, 'ACTIVE', NULL, 'Includes nutrition package', FALSE, NULL, '2026-01-10T11:00:00Z', '2026-01-10T11:00:00Z');

INSERT INTO organization_settings (id, organization_id, whatsapp_enabled, email_enabled, reminder_days_before_expiry, branding_name, deleted, created_at, updated_at) VALUES
(1, 1, TRUE, TRUE, 1, 'Gym Alpha Pro', FALSE, '2026-01-01T12:00:00Z', '2026-01-01T12:00:00Z'),
(2, 2, TRUE, TRUE, 2, 'Gym Bravo Fit', FALSE, '2026-01-02T12:00:00Z', '2026-01-02T12:00:00Z'),
(3, 3, TRUE, FALSE, 3, 'Gym Charlie Core', FALSE, '2026-01-03T12:00:00Z', '2026-01-03T12:00:00Z'),
(4, 4, FALSE, TRUE, 1, 'Gym Delta Plus', FALSE, '2026-01-04T12:00:00Z', '2026-01-04T12:00:00Z'),
(5, 5, TRUE, TRUE, 2, 'Gym Echo Active', FALSE, '2026-01-05T12:00:00Z', '2026-01-05T12:00:00Z'),
(6, 6, TRUE, TRUE, 1, 'Gym Foxtrot Prime', FALSE, '2026-01-06T12:00:00Z', '2026-01-06T12:00:00Z'),
(7, 7, TRUE, FALSE, 1, 'Gym Golf Flex', FALSE, '2026-01-07T12:00:00Z', '2026-01-07T12:00:00Z'),
(8, 8, FALSE, TRUE, 2, 'Gym Hotel Pulse', FALSE, '2026-01-08T12:00:00Z', '2026-01-08T12:00:00Z'),
(9, 9, TRUE, TRUE, 1, 'Gym India One', FALSE, '2026-01-09T12:00:00Z', '2026-01-09T12:00:00Z'),
(10, 10, TRUE, TRUE, 3, 'Gym Juliet Elite', FALSE, '2026-01-10T12:00:00Z', '2026-01-10T12:00:00Z');

INSERT INTO organization_subscriptions (id, organization_id, start_date, end_date, status, amount, razorpay_subscription_id, deleted, created_at, updated_at) VALUES
(1, 1, '2026-01-01', '2026-12-31', 'ACTIVE', 9999.00, 'sub_alpha_001', FALSE, '2026-01-01T13:00:00Z', '2026-01-01T13:00:00Z'),
(2, 2, '2026-01-01', '2026-12-31', 'ACTIVE', 8999.00, 'sub_bravo_002', FALSE, '2026-01-02T13:00:00Z', '2026-01-02T13:00:00Z'),
(3, 3, '2026-01-01', '2026-12-31', 'ACTIVE', 7999.00, 'sub_charlie_003', FALSE, '2026-01-03T13:00:00Z', '2026-01-03T13:00:00Z'),
(4, 4, '2026-01-01', '2026-12-31', 'ACTIVE', 6999.00, 'sub_delta_004', FALSE, '2026-01-04T13:00:00Z', '2026-01-04T13:00:00Z'),
(5, 5, '2025-01-01', '2025-12-31', 'EXPIRED', 5999.00, 'sub_echo_005', FALSE, '2026-01-05T13:00:00Z', '2026-01-05T13:00:00Z'),
(6, 6, '2026-01-01', '2026-12-31', 'ACTIVE', 9999.00, 'sub_foxtrot_006', FALSE, '2026-01-06T13:00:00Z', '2026-01-06T13:00:00Z'),
(7, 7, '2025-01-01', '2025-12-31', 'CANCELED', 6999.00, 'sub_golf_007', FALSE, '2026-01-07T13:00:00Z', '2026-01-07T13:00:00Z'),
(8, 8, '2026-01-01', '2026-12-31', 'ACTIVE', 7499.00, 'sub_hotel_008', FALSE, '2026-01-08T13:00:00Z', '2026-01-08T13:00:00Z'),
(9, 9, '2025-01-01', '2025-12-31', 'EXPIRED', 8499.00, 'sub_india_009', FALSE, '2026-01-09T13:00:00Z', '2026-01-09T13:00:00Z'),
(10, 10, '2026-01-01', '2026-12-31', 'ACTIVE', 10999.00, 'sub_juliet_010', FALSE, '2026-01-10T13:00:00Z', '2026-01-10T13:00:00Z');

INSERT INTO notifications (id, organization_id, member_id, type, channel, recipient, status, attempt_count, failure_reason, sent_at, message, deleted, created_at, updated_at) VALUES
(1, 1, 1, 'MEMBERSHIP_EXPIRY', 'WHATSAPP', '+919800000001', 'SENT', 1, NULL, '2026-01-01T14:00:00Z', 'Your membership will expire soon. Renew now.', FALSE, '2026-01-01T14:00:00Z', '2026-01-01T14:00:00Z'),
(2, 2, 2, 'MEMBERSHIP_EXPIRY', 'EMAIL', 'ananya.rao@example.com', 'PENDING', 0, NULL, NULL, 'Reminder: membership expiry in 7 days.', FALSE, '2026-01-02T14:00:00Z', '2026-01-02T14:00:00Z'),
(3, 3, 3, 'MEMBERSHIP_OVERDUE', 'WHATSAPP', '+919800000003', 'SENT', 1, NULL, '2026-01-03T14:00:00Z', 'Payment overdue. Please clear dues.', FALSE, '2026-01-03T14:00:00Z', '2026-01-03T14:00:00Z'),
(4, 4, 4, 'WELCOME_MESSAGE', 'EMAIL', 'ishita.jain@example.com', 'SENT', 1, NULL, '2026-01-04T14:00:00Z', 'Welcome to Gym Delta!', FALSE, '2026-01-04T14:00:00Z', '2026-01-04T14:00:00Z'),
(5, 5, 5, 'MEMBERSHIP_EXPIRY', 'WHATSAPP', '+919800000005', 'FAILED', 3, 'Provider timeout', NULL, 'Membership expired. Please renew.', FALSE, '2026-01-05T14:00:00Z', '2026-01-05T14:00:00Z'),
(6, 6, 6, 'WELCOME_MESSAGE', 'EMAIL', 'meera.nanda@example.com', 'SENT', 1, NULL, '2026-01-06T14:00:00Z', 'Welcome to Gym Foxtrot!', FALSE, '2026-01-06T14:00:00Z', '2026-01-06T14:00:00Z'),
(7, 7, 7, 'MEMBERSHIP_OVERDUE', 'WHATSAPP', '+919800000007', 'PENDING', 0, NULL, NULL, 'Kind reminder for overdue subscription.', FALSE, '2026-01-07T14:00:00Z', '2026-01-07T14:00:00Z'),
(8, 8, 8, 'MEMBERSHIP_EXPIRY', 'EMAIL', 'riya.das@example.com', 'SENT', 1, NULL, '2026-01-08T14:00:00Z', 'Membership expiry approaching soon.', FALSE, '2026-01-08T14:00:00Z', '2026-01-08T14:00:00Z'),
(9, 9, 9, 'MEMBERSHIP_OVERDUE', 'EMAIL', 'sahil.arora@example.com', 'FAILED', 3, 'Mailbox unavailable', NULL, 'Overdue alert: please contact support.', FALSE, '2026-01-09T14:00:00Z', '2026-01-09T14:00:00Z'),
(10, 10, 10, 'WELCOME_MESSAGE', 'WHATSAPP', '+919800000010', 'SENT', 1, NULL, '2026-01-10T14:00:00Z', 'Welcome aboard Gym Juliet!', FALSE, '2026-01-10T14:00:00Z', '2026-01-10T14:00:00Z');

INSERT INTO usage_metrics (id, organization_id, year, month, monthly_whatsapp_count, monthly_email_count, monthly_reminders_executed, active_members, active_admins, subscription_status, deleted, created_at, updated_at) VALUES
(1, 1, 2026, 1, 110, 80, 42, 95, 1, 'ACTIVE', FALSE, '2026-01-01T15:00:00Z', '2026-01-01T15:00:00Z'),
(2, 2, 2026, 2, 95, 70, 38, 88, 1, 'ACTIVE', FALSE, '2026-01-02T15:00:00Z', '2026-01-02T15:00:00Z'),
(3, 3, 2026, 3, 120, 65, 47, 76, 1, 'ACTIVE', FALSE, '2026-01-03T15:00:00Z', '2026-01-03T15:00:00Z'),
(4, 4, 2026, 4, 85, 90, 35, 102, 1, 'ACTIVE', FALSE, '2026-01-04T15:00:00Z', '2026-01-04T15:00:00Z'),
(5, 5, 2026, 5, 50, 45, 21, 40, 1, 'EXPIRED', FALSE, '2026-01-05T15:00:00Z', '2026-01-05T15:00:00Z'),
(6, 6, 2026, 6, 140, 100, 50, 118, 1, 'ACTIVE', FALSE, '2026-01-06T15:00:00Z', '2026-01-06T15:00:00Z'),
(7, 7, 2026, 7, 60, 25, 18, 35, 1, 'CANCELED', FALSE, '2026-01-07T15:00:00Z', '2026-01-07T15:00:00Z'),
(8, 8, 2026, 8, 90, 95, 39, 99, 1, 'ACTIVE', FALSE, '2026-01-08T15:00:00Z', '2026-01-08T15:00:00Z'),
(9, 9, 2026, 9, 55, 40, 20, 52, 1, 'EXPIRED', FALSE, '2026-01-09T15:00:00Z', '2026-01-09T15:00:00Z'),
(10, 10, 2026, 10, 150, 115, 57, 130, 1, 'ACTIVE', FALSE, '2026-01-10T15:00:00Z', '2026-01-10T15:00:00Z');

SELECT setval('organizations_id_seq', COALESCE((SELECT MAX(id) FROM organizations), 1), TRUE);
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1), TRUE);
SELECT setval('members_id_seq', COALESCE((SELECT MAX(id) FROM members), 1), TRUE);
SELECT setval('organization_settings_id_seq', COALESCE((SELECT MAX(id) FROM organization_settings), 1), TRUE);
SELECT setval('organization_subscriptions_id_seq', COALESCE((SELECT MAX(id) FROM organization_subscriptions), 1), TRUE);
SELECT setval('notifications_id_seq', COALESCE((SELECT MAX(id) FROM notifications), 1), TRUE);
SELECT setval('usage_metrics_id_seq', COALESCE((SELECT MAX(id) FROM usage_metrics), 1), TRUE);
