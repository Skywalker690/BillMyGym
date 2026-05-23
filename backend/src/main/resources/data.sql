INSERT INTO organizations (
    id, name, district, place, logo_url, map_location_link, address, admin_full_name, admin_phone_number, admin_email,
    description, monthly_membership_fee, admission_fee, organization_type, subscription_plan, status,
    trial_start_date, trial_end_date, pilot_start_date, pilot_end_date, subscription_due_date,
    paused_at, deactivated_at, deleted, deleted_at, created_at, updated_at, version
)
SELECT
    org_id,
    (ARRAY[
        'Iron Forge Fitness',
        'Pulse Arena',
        'Southside Strength',
        'Core Motion Club',
        'Pilot Peak Gym'
    ])[org_id],
    (ARRAY['Central', 'North', 'South', 'East', 'West'])[org_id],
    (ARRAY['Kochi', 'Thrissur', 'Kozhikode', 'Kannur', 'Kollam'])[org_id],
    'https://img.example.com/org-' || org_id || '.png',
    'https://maps.example.com/org-' || org_id,
    (100 + org_id) || ' Fitness Avenue',
    (ARRAY['Ravi Kumar', 'Anita Sharma', 'Joseph Mathew', 'Neha Singh', 'Kiran Joseph'])[org_id],
    '+91910000000' || org_id,
    (ARRAY[
        'ravi@ironforge.local',
        'anita@pulsearena.local',
        'joseph@southside.local',
        'neha@coremotion.local',
        'kiran@pilotpeak.local'
    ])[org_id],
    'Seed organization ' || org_id || ' with balanced member and subscription states',
    (ARRAY[1299.00, 1599.00, 999.00, 1899.00, 899.00])[org_id]::numeric(12, 2),
    (ARRAY[499.00, 699.00, 399.00, 799.00, 0.00])[org_id]::numeric(12, 2),
    CASE WHEN org_id = 5 THEN 'PILOT' ELSE 'REGULAR' END,
    (ARRAY['STARTER', 'GROWTH', 'STARTER', 'PRO', 'PILOT'])[org_id],
    (ARRAY['ACTIVE', 'PAUSED', 'TRIAL', 'ACTIVE', 'ACTIVE'])[org_id],
    CASE WHEN org_id = 5 THEN NULL ELSE CURRENT_DATE - (30 + org_id) END,
    CASE
        WHEN org_id = 3 THEN CURRENT_DATE + 7
        WHEN org_id = 5 THEN NULL
        ELSE CURRENT_DATE + (10 - org_id)
    END,
    CASE WHEN org_id = 5 THEN CURRENT_DATE - 12 ELSE NULL END,
    CASE WHEN org_id = 5 THEN CURRENT_DATE + 45 ELSE NULL END,
    CASE
        WHEN org_id = 1 THEN CURRENT_DATE + 12
        WHEN org_id = 2 THEN CURRENT_DATE - 4
        WHEN org_id = 3 THEN CURRENT_DATE + 7
        WHEN org_id = 4 THEN CURRENT_DATE + 18
        ELSE CURRENT_DATE + 45
    END,
    CASE WHEN org_id = 2 THEN CURRENT_TIMESTAMP - INTERVAL '4 days' ELSE NULL END,
    NULL,
    FALSE,
    NULL,
    CURRENT_TIMESTAMP - ((20 - org_id) || ' days')::interval,
    CURRENT_TIMESTAMP - ((5 - org_id) || ' days')::interval,
    0
FROM generate_series(1, 5) AS org_id;

INSERT INTO users (
    id, organization_id, email, google_id, avatar_url, password_hash, role, status, terms_accepted, policy_version,
    deleted, created_at, updated_at
) VALUES
    (1, NULL, 'sanjoksiby10@gmail.com', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'SUPER_ADMIN', 'ACTIVE', NULL, NULL,
        FALSE, CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3, 2, 'owner02@billmygym.local', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', NULL, NULL,
        FALSE, CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (4, 3, 'owner03@billmygym.local', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', NULL, NULL,
        FALSE, CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (2, 5, 'sanjoksiby0@gmail.com', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', NULL, NULL,
        FALSE, CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '2 days');

INSERT INTO organization_settings (
    id, organization_id, whatsapp_enabled, email_enabled, reminder_days_before_expiry, branding_name,
    deleted, created_at, updated_at
)
SELECT
    org_id,
    org_id,
    org_id <> 5,
    TRUE,
    (ARRAY[3, 2, 5, 4, 6])[org_id],
    (ARRAY['Iron Forge', 'Pulse Arena', 'Southside', 'Core Motion', 'Pilot Peak'])[org_id],
    FALSE,
    CURRENT_TIMESTAMP - ((18 - org_id) || ' days')::interval,
    CURRENT_TIMESTAMP - ((4 - org_id) || ' days')::interval
FROM generate_series(1, 5) AS org_id;

INSERT INTO members (
    id, organization_id, full_name, phone, status, deleted, deleted_at, deactivated_at, deactivation_reason,
    created_at, updated_at, version
)
SELECT
    ((org_id - 1) * 10) + slot_id,
    org_id,
    'Member ' || org_id || '-' || LPAD(slot_id::text, 2, '0'),
    '+9198' || LPAD((((org_id - 1) * 10) + slot_id)::text, 8, '0'),
    CASE
        WHEN slot_id = 5 THEN 'DEACTIVATED'
        WHEN slot_id = 6 THEN 'ACTIVE'
        ELSE 'ACTIVE'
    END,
    FALSE,
    NULL,
    CASE WHEN slot_id = 5 THEN CURRENT_TIMESTAMP - INTERVAL '3 days' ELSE NULL END,
    CASE WHEN slot_id = 5 THEN 'Temporarily inactive member' ELSE NULL END,
    CURRENT_TIMESTAMP - (((30 - org_id) + slot_id) || ' days')::interval,
    CURRENT_TIMESTAMP - (((10 - org_id) + (slot_id % 4)) || ' days')::interval,
    0
FROM generate_series(1, 5) AS org_id
CROSS JOIN generate_series(1, 10) AS slot_id;

INSERT INTO member_memberships (
    id, member_id, organization_id, duration_months, plan_amount, start_date, end_date, status,
    last_sent_at, deleted, deleted_at, version, created_at, updated_at
)
SELECT
    member_id,
    member_id,
    org_id,
    CASE
        WHEN slot_id IN (5, 6, 8) THEN 3
        WHEN slot_id = 7 THEN 12
        ELSE 1
    END,
    CASE
        WHEN slot_id IN (1, 10) THEN 1200.00
        WHEN slot_id = 2 THEN 1500.00
        WHEN slot_id IN (3, 4, 9) THEN 1800.00
        WHEN slot_id IN (5, 6, 8) THEN 3600.00
        ELSE 12000.00
    END::numeric(12, 2),
    CASE
        WHEN slot_id IN (3, 9) THEN CURRENT_DATE - 35
        WHEN slot_id = 4 THEN CURRENT_DATE - 31
        WHEN slot_id = 5 THEN CURRENT_DATE - 95
        WHEN slot_id = 8 THEN CURRENT_DATE - 120
        ELSE CURRENT_DATE - (8 + (slot_id % 4))
    END,
    CASE
        WHEN slot_id IN (1, 2, 6, 7, 10) THEN CURRENT_DATE + (10 + slot_id)
        WHEN slot_id = 3 THEN CURRENT_DATE - 4
        WHEN slot_id = 4 THEN CURRENT_DATE
        WHEN slot_id = 5 THEN CURRENT_DATE - 6
        WHEN slot_id = 8 THEN CURRENT_DATE - 20
        WHEN slot_id = 9 THEN CURRENT_DATE - 2
        ELSE CURRENT_DATE + 15
    END,
    CASE
        WHEN slot_id IN (3, 9) THEN 'OVERDUE'
        WHEN slot_id = 4 THEN 'OVERDUE'
        WHEN slot_id IN (5, 8) THEN 'OVERDUE'
        ELSE 'ACTIVE'
    END,
    CASE
        WHEN slot_id IN (3, 4, 9) THEN NULL
        WHEN slot_id IN (2, 10) THEN CURRENT_TIMESTAMP - INTERVAL '1 day'
        ELSE CURRENT_TIMESTAMP - INTERVAL '2 days'
    END,
    FALSE,
    NULL,
    0,
    CURRENT_TIMESTAMP - (((28 - org_id) + slot_id) || ' days')::interval,
    CURRENT_TIMESTAMP - (((8 - org_id) + (slot_id % 3)) || ' days')::interval
FROM (
    SELECT
        org_id,
        slot_id,
        ((org_id - 1) * 10) + slot_id AS member_id
    FROM generate_series(1, 5) AS org_id
    CROSS JOIN generate_series(1, 10) AS slot_id
) seeded;

INSERT INTO member_payments (
    id, member_id, membership_id, organization_id, amount, payment_date, billing_month, payment_status, payment_type,
    gateway_transaction_id, notes, deleted, deleted_at, created_at, updated_at, version
)
SELECT
    membership_id,
    membership_id,
    membership_id,
    org_id,
    CASE
        WHEN slot_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10) THEN plan_amount
        ELSE plan_amount
    END::numeric(12, 2),
    CURRENT_DATE - (slot_id + org_id),
    TO_CHAR(CURRENT_DATE - (slot_id + org_id), 'YYYY-MM'),
    'PAID',
    CASE
        WHEN slot_id IN (1, 6) THEN 'MEMBERSHIP'
        WHEN slot_id IN (2, 3, 4, 9, 10) THEN 'MEMBERSHIP'
        ELSE 'ADJUSTMENT'
    END,
    'txn_member_' || LPAD(membership_id::text, 4, '0'),
    'Fully paid membership seed',
    FALSE,
    NULL,
    CURRENT_TIMESTAMP - (((26 - org_id) + slot_id) || ' days')::interval,
    CURRENT_TIMESTAMP - (((7 - org_id) + (slot_id % 3)) || ' days')::interval,
    0
FROM (
    SELECT
        org_id,
        slot_id,
        ((org_id - 1) * 10) + slot_id AS membership_id,
        CASE
            WHEN slot_id IN (1, 10) THEN 1200.00
            WHEN slot_id = 2 THEN 1500.00
            WHEN slot_id IN (3, 4, 9) THEN 1800.00
            WHEN slot_id IN (5, 6, 8) THEN 3600.00
            ELSE 12000.00
        END::numeric(12, 2) AS plan_amount
    FROM generate_series(1, 5) AS org_id
    CROSS JOIN generate_series(1, 10) AS slot_id
) payment_seed;

INSERT INTO member_credit_ledger (
    id, member_id, organization_id, reference_membership_id, amount, transaction_type, reason,
    notes, created_by_user_id, created_by_email, deleted, created_at, updated_at
)
SELECT
    ledger_id,
    member_id,
    organization_id,
    reference_membership_id,
    amount,
    transaction_type,
    reason,
    notes,
    NULL,
    'system@billmygym.local',
    FALSE,
    CURRENT_TIMESTAMP - ((15 - ledger_id) || ' days')::interval,
    CURRENT_TIMESTAMP - ((5 - (ledger_id % 5)) || ' days')::interval
FROM (
    VALUES
        (1, 2, 1, 2, 200.00::numeric(12,2), 'CREDIT', 'PROMOTION', 'Promo credit for partial payer'),
        (2, 10, 1, 10, 150.00::numeric(12,2), 'DEBIT', 'MANUAL_ADJUSTMENT', 'Wallet debit for renewal'),
        (3, 13, 2, 13, 300.00::numeric(12,2), 'CREDIT', 'CORRECTION', 'Correction on overdue account'),
        (4, 22, 3, 22, 250.00::numeric(12,2), 'CREDIT', 'PROMOTION', 'Trial org promotional credit'),
        (5, 31, 4, 31, 125.00::numeric(12,2), 'DEBIT', 'MANUAL_ADJUSTMENT', 'Debit applied to active member'),
        (6, 45, 5, 45, 100.00::numeric(12,2), 'CREDIT', 'REFUND', 'Pilot member refund balance'),
        (7, 49, 5, 49, 180.00::numeric(12,2), 'CREDIT', 'PROMOTION', 'Pilot overdue credit'),
        (8, 6, 1, 6, 90.00::numeric(12,2), 'CREDIT', 'CORRECTION', 'Paused member correction'),
        (9, 28, 3, 28, 220.00::numeric(12,2), 'DEBIT', 'MANUAL_ADJUSTMENT', 'Debit against completed plan'),
        (10, 37, 4, 37, 500.00::numeric(12,2), 'CREDIT', 'PROMOTION', 'Annual plan bonus credit')
) AS ledger(ledger_id, member_id, organization_id, reference_membership_id, amount, transaction_type, reason, notes);

INSERT INTO organization_subscriptions (
    id, organization_id, start_date, end_date, status, amount, razorpay_subscription_id,
    deleted, created_at, updated_at
) VALUES
    (1, 1, CURRENT_DATE - 18, CURRENT_DATE + 12, 'ACTIVE', 299.00, 'sub_org_001', FALSE, CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (2, 2, CURRENT_DATE - 30, CURRENT_DATE - 4, 'FAILED', 699.00, 'sub_org_002', FALSE, CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (3, 3, CURRENT_DATE - 10, CURRENT_DATE + 7, 'PENDING', 299.00, 'sub_org_003', FALSE, CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (4, 4, CURRENT_DATE - 12, CURRENT_DATE + 18, 'ACTIVE', 1499.00, 'sub_org_004', FALSE, CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (5, 5, CURRENT_DATE - 12, CURRENT_DATE + 45, 'ACTIVE', 0.00, 'sub_org_005', FALSE, CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '2 days');

INSERT INTO organization_subscription_payments (
    id, organization_id, amount, subscription_plan, payment_date, subscription_start, subscription_end, payment_status,
    retry_count, next_retry_date, failure_reason, webhook_event_id, gateway_transaction_id, razorpay_order_id,
    razorpay_payment_id, razorpay_subscription_id, deleted, created_at, updated_at, version
) VALUES
    (1, 1, 299.00, 'STARTER', CURRENT_DATE - 18, CURRENT_DATE - 18, CURRENT_DATE + 12, 'PAID',
        0, NULL, NULL, 'evt_sub_001', 'pay_sub_001', 'order_sub_001', 'pay_sub_001', 'sub_org_001',
        FALSE, CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '2 days', 0),
    (2, 2, 699.00, 'GROWTH', NULL, CURRENT_DATE - 30, CURRENT_DATE - 4, 'FAILED',
        2, CURRENT_DATE + 2, 'Card auto-debit failed', 'evt_sub_002', NULL, 'order_sub_002', NULL, 'sub_org_002',
        FALSE, CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '1 day', 0),
    (3, 3, 299.00, 'STARTER', NULL, CURRENT_DATE - 10, CURRENT_DATE + 7, 'PENDING',
        1, CURRENT_DATE + 1, NULL, 'evt_sub_003', NULL, 'order_sub_003', NULL, 'sub_org_003',
        FALSE, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '1 day', 0),
    (4, 4, 1499.00, 'PRO', CURRENT_DATE - 12, CURRENT_DATE - 12, CURRENT_DATE + 18, 'PAID',
        0, NULL, NULL, 'evt_sub_004', 'pay_sub_004', 'order_sub_004', 'pay_sub_004', 'sub_org_004',
        FALSE, CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '2 days', 0),
    (5, 5, 0.00, 'PILOT', CURRENT_DATE - 12, CURRENT_DATE - 12, CURRENT_DATE + 45, 'PAID',
        0, NULL, NULL, 'evt_sub_005', 'pay_sub_005', 'order_sub_005', 'pay_sub_005', 'sub_org_005',
        FALSE, CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '2 days', 0);

INSERT INTO notifications (
    id, organization_id, member_id, member_count, type, channel, recipient, status, attempt_count, failure_reason,
    notification_date, sent_at, message, deleted, created_at, updated_at
) VALUES
    (1, 1, 3, 10, 'ORGANIZATION_RENEWAL_DIGEST', 'EMAIL', 'ravi@ironforge.local', 'SENT', 1, NULL,
        CURRENT_DATE - 1, CURRENT_TIMESTAMP - INTERVAL '12 hours', 'Renewal digest for Iron Forge', FALSE, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '12 hours'),
    (2, 2, 13, 10, 'SUBSCRIPTION_PAYMENT_FAILED', 'EMAIL', 'anita@pulsearena.local', 'FAILED', 3, 'SMTP rejected payload',
        CURRENT_DATE, NULL, 'Payment failed notification for Pulse Arena', FALSE, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '6 hours'),
    (3, 3, 22, 10, 'TRIAL_ENDING', 'EMAIL', 'joseph@southside.local', 'SENT', 1, NULL,
        CURRENT_DATE, CURRENT_TIMESTAMP - INTERVAL '10 hours', 'Trial ending reminder for Southside Strength', FALSE, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '10 hours'),
    (4, 4, 37, 10, 'MONTHLY_ORGANIZATION_REPORT', 'WHATSAPP', '+919100000004', 'RETRYING', 2, 'Provider timeout',
        CURRENT_DATE - 2, NULL, 'Monthly report delivery for Core Motion Club', FALSE, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '3 hours'),
    (5, 5, NULL, 10, 'ORGANIZATION_WELCOME', 'EMAIL', 'kiran@pilotpeak.local', 'SENT', 1, NULL,
        CURRENT_DATE - 3, CURRENT_TIMESTAMP - INTERVAL '1 day', 'Welcome to Pilot Peak Gym', FALSE, CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '1 day');

INSERT INTO notification_delivery_logs (
    id, notification_id, organization_id, type, status, attempt_count, last_attempt, provider, template_name,
    recipient, provider_message_id, error_message, next_retry_at, request_payload, deleted, created_at, updated_at
) VALUES
    (1, 1, 1, 'ORGANIZATION_RENEWAL_DIGEST', 'SENT', 1, CURRENT_TIMESTAMP - INTERVAL '12 hours', 'SYSTEM', 'renewal_digest',
        'ravi@ironforge.local', 'msg_001', NULL, NULL, '{"channel":"EMAIL"}', FALSE, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '12 hours'),
    (2, 2, 2, 'SUBSCRIPTION_PAYMENT_FAILED', 'FAILED', 3, CURRENT_TIMESTAMP - INTERVAL '6 hours', 'SYSTEM', 'payment_failed',
        'anita@pulsearena.local', NULL, 'SMTP rejected payload', NULL, '{"channel":"EMAIL"}', FALSE, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '6 hours'),
    (3, 3, 3, 'TRIAL_ENDING', 'SENT', 1, CURRENT_TIMESTAMP - INTERVAL '10 hours', 'SYSTEM', 'trial_ending',
        'joseph@southside.local', 'msg_003', NULL, NULL, '{"channel":"EMAIL"}', FALSE, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '10 hours'),
    (4, 4, 4, 'MONTHLY_ORGANIZATION_REPORT', 'RETRYING', 2, CURRENT_TIMESTAMP - INTERVAL '3 hours', 'WHATSAPP_CLOUD', 'monthly_report',
        '+919100000004', NULL, 'Provider timeout', CURRENT_TIMESTAMP + INTERVAL '2 hours', '{"channel":"WHATSAPP"}', FALSE, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '3 hours'),
    (5, 5, 5, 'ORGANIZATION_WELCOME', 'SENT', 1, CURRENT_TIMESTAMP - INTERVAL '1 day', 'SYSTEM', 'organization_welcome',
        'kiran@pilotpeak.local', 'msg_005', NULL, NULL, '{"channel":"EMAIL"}', FALSE, CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '1 day');

INSERT INTO usage_metrics (
    id, organization_id, year, month, monthly_whatsapp_count, monthly_email_count, monthly_digest_count,
    active_members, active_admins, subscription_status, deleted, created_at, updated_at
)
SELECT
    org_id,
    org_id,
    EXTRACT(YEAR FROM CURRENT_DATE)::int,
    EXTRACT(MONTH FROM CURRENT_DATE)::int,
    8 + (org_id * 2),
    6 + org_id,
    1 + (org_id % 3),
    8,
    1,
    (ARRAY['ACTIVE', 'PAUSED', 'TRIAL', 'ACTIVE', 'ACTIVE'])[org_id],
    FALSE,
    CURRENT_TIMESTAMP - ((6 - org_id) || ' days')::interval,
    CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM generate_series(1, 5) AS org_id;

INSERT INTO organization_usage_stats (
    id, organization_id, year, month, whatsapp_sent_count, email_sent_count, deleted, created_at, updated_at
)
SELECT
    org_id,
    org_id,
    EXTRACT(YEAR FROM CURRENT_DATE)::int,
    EXTRACT(MONTH FROM CURRENT_DATE)::int,
    8 + (org_id * 2),
    6 + org_id,
    FALSE,
    CURRENT_TIMESTAMP - ((6 - org_id) || ' days')::interval,
    CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM generate_series(1, 5) AS org_id;

INSERT INTO membership_operation_idempotency (
    id, organization_id, member_id, idempotency_key, operation_type, resource_type, resource_id,
    deleted, created_at, updated_at
)
SELECT
    row_id,
    organization_id,
    member_id,
    idempotency_key,
    operation_type,
    resource_type,
    resource_id,
    FALSE,
    CURRENT_TIMESTAMP - ((12 - row_id) || ' hours')::interval,
    CURRENT_TIMESTAMP - ((12 - row_id) || ' hours')::interval
FROM (
    VALUES
        (1, 1, 1, 'seed-membership-create-001', 'MEMBERSHIP_CREATE', 'MEMBERSHIP', 1),
        (2, 1, 2, 'seed-membership-switch-002', 'MEMBERSHIP_SWITCH', 'MEMBERSHIP', 2),
        (3, 2, 0, 'seed-payment-record-003', 'PAYMENT_RECORD', 'PAYMENT', 13),
        (4, 2, 15, 'seed-credit-adjust-004', 'CREDIT_ADJUSTMENT', 'CREDIT_LEDGER_ENTRY', 3),
        (5, 3, 0, 'seed-sub-create-005', 'SUBSCRIPTION_CREATE', 'SUBSCRIPTION_PAYMENT', 3),
        (6, 3, 0, 'seed-sub-verify-006', 'SUBSCRIPTION_VERIFY', 'SUBSCRIPTION_PAYMENT', 3),
        (7, 4, 31, 'seed-membership-create-007', 'MEMBERSHIP_CREATE', 'MEMBERSHIP', 31),
        (8, 4, 37, 'seed-debit-adjust-008', 'DEBIT_ADJUSTMENT', 'CREDIT_LEDGER_ENTRY', 10),
        (9, 5, 45, 'seed-membership-switch-009', 'MEMBERSHIP_SWITCH', 'MEMBERSHIP', 45),
        (10, 5, 0, 'seed-payment-record-010', 'PAYMENT_RECORD', 'PAYMENT', 45)
) AS seed(row_id, organization_id, member_id, idempotency_key, operation_type, resource_type, resource_id);

INSERT INTO processed_webhook_events (
    id, event_id, event_type, processed_at, deleted, created_at, updated_at
) VALUES
    (1, 'webhook-event-001', 'payment.captured', CURRENT_TIMESTAMP - INTERVAL '20 hours', FALSE, CURRENT_TIMESTAMP - INTERVAL '20 hours', CURRENT_TIMESTAMP - INTERVAL '20 hours'),
    (2, 'webhook-event-002', 'payment.failed', CURRENT_TIMESTAMP - INTERVAL '16 hours', FALSE, CURRENT_TIMESTAMP - INTERVAL '16 hours', CURRENT_TIMESTAMP - INTERVAL '16 hours'),
    (3, 'webhook-event-003', 'order.paid', CURRENT_TIMESTAMP - INTERVAL '12 hours', FALSE, CURRENT_TIMESTAMP - INTERVAL '12 hours', CURRENT_TIMESTAMP - INTERVAL '12 hours'),
    (4, 'webhook-event-004', 'subscription.charged', CURRENT_TIMESTAMP - INTERVAL '8 hours', FALSE, CURRENT_TIMESTAMP - INTERVAL '8 hours', CURRENT_TIMESTAMP - INTERVAL '8 hours'),
    (5, 'webhook-event-005', 'payment.failed', CURRENT_TIMESTAMP - INTERVAL '4 hours', FALSE, CURRENT_TIMESTAMP - INTERVAL '4 hours', CURRENT_TIMESTAMP - INTERVAL '4 hours');

INSERT INTO api_request_log (
    id, organization_id, request_id, endpoint, response_hash, response_body, created_at
) VALUES
    (1, 1, 'req-sub-create-001', 'POST:/api/subscription/create', md5('seed-response-1'), '{"status":"ok","kind":"create"}', CURRENT_TIMESTAMP - INTERVAL '10 hours'),
    (2, 1, 'req-sub-verify-002', 'POST:/api/subscription/verify', md5('seed-response-2'), '{"status":"ok","kind":"verify"}', CURRENT_TIMESTAMP - INTERVAL '9 hours'),
    (3, 2, 'req-payment-003', 'POST:/api/payments', md5('seed-response-3'), '{"status":"ok","kind":"payment"}', CURRENT_TIMESTAMP - INTERVAL '8 hours'),
    (4, 4, 'req-membership-004', 'POST:/api/members/31/memberships', md5('seed-response-4'), '{"status":"ok","kind":"membership"}', CURRENT_TIMESTAMP - INTERVAL '7 hours'),
    (5, 5, 'req-reminder-005', 'POST:/api/reminders/run', md5('seed-response-5'), '{"status":"ok","kind":"reminder"}', CURRENT_TIMESTAMP - INTERVAL '6 hours');

INSERT INTO audit_logs (
    id, event, description, actor_name, actor_email, actor_role, organization_id, category, activity_scope,
    event_status, target_name, zone, ip_address, device, browser, timestamp
) VALUES
    (1, 'PASSWORD_LOGIN', 'Super admin signed in successfully', 'Sanjo K Siby', 'sanjoksiby10@gmail.com', 'SUPER_ADMIN', NULL, 'AUTHENTICATION', 'GLOBAL_LOGIN',
        'SUCCESS', 'GymMax Admin Console', 'IST', '10.10.10.1', 'MacBook Pro', 'Chrome 136', CURRENT_TIMESTAMP - INTERVAL '18 hours'),
    (2, 'ORG_CREATED', 'Created organization profile for Iron Forge Fitness', 'Sanjo K Siby', 'sanjoksiby10@gmail.com', 'SUPER_ADMIN', 1, 'ORGANIZATION', 'ADMIN_ACTIVITY',
        'INFO', 'Iron Forge Fitness', 'IST', '10.10.10.2', 'Windows Laptop', 'Edge 136', CURRENT_TIMESTAMP - INTERVAL '16 hours'),
    (3, 'MEMBER_IMPORTED', 'Imported members for Pulse Arena onboarding', 'Anita Sharma', 'owner02@billmygym.local', 'ORG_ADMIN', 2, 'MEMBER', 'MEMBER_ACTIVITY',
        'SUCCESS', 'Pulse Arena Batch Import', 'IST', '10.10.10.3', 'Office Desktop', 'Chrome 136', CURRENT_TIMESTAMP - INTERVAL '12 hours'),
    (4, 'PAYMENT_RETRY_FAILED', 'Automatic subscription retry failed for Southside Strength', 'System Scheduler', 'system@billmygym.local', 'SYSTEM', 3, 'SYSTEM', 'ADMIN_ACTIVITY',
        'FAILED', 'Southside Strength Subscription', 'IST', '10.10.10.4', 'Backend Worker', 'Spring Scheduler', CURRENT_TIMESTAMP - INTERVAL '8 hours'),
    (5, 'MONTHLY_REPORT_GENERATED', 'Generated monthly organization report for Core Motion Club', 'System Scheduler', 'system@billmygym.local', 'SYSTEM', 4, 'SYSTEM', 'ADMIN_ACTIVITY',
        'INFO', 'Core Motion Club Monthly Report', 'IST', '10.10.10.5', 'Backend Worker', 'Spring Scheduler', CURRENT_TIMESTAMP - INTERVAL '4 hours');

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'otps'
    ) THEN
        INSERT INTO otps (
            id, email, otp_code, expires_at, verified, created_at
        ) VALUES
            (1, 'sanjoksiby10@gmail.com', '412583', CURRENT_TIMESTAMP + INTERVAL '10 minutes', FALSE, CURRENT_TIMESTAMP - INTERVAL '15 minutes'),
            (2, 'owner02@billmygym.local', '904221', CURRENT_TIMESTAMP + INTERVAL '8 minutes', FALSE, CURRENT_TIMESTAMP - INTERVAL '12 minutes'),
            (3, 'owner03@billmygym.local', '781144', CURRENT_TIMESTAMP - INTERVAL '2 minutes', FALSE, CURRENT_TIMESTAMP - INTERVAL '9 minutes'),
            (4, 'sanjoksiby0@gmail.com', '635902', CURRENT_TIMESTAMP - INTERVAL '20 minutes', TRUE, CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
            (5, 'audit.seed@billmygym.local', '118407', CURRENT_TIMESTAMP + INTERVAL '5 minutes', FALSE, CURRENT_TIMESTAMP - INTERVAL '3 minutes');
    END IF;
END $$;

SELECT setval('organizations_id_seq', COALESCE((SELECT MAX(id) FROM organizations), 1), TRUE);
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1), TRUE);
SELECT setval('organization_settings_id_seq', COALESCE((SELECT MAX(id) FROM organization_settings), 1), TRUE);
SELECT setval('members_id_seq', COALESCE((SELECT MAX(id) FROM members), 1), TRUE);
SELECT setval('member_memberships_id_seq', COALESCE((SELECT MAX(id) FROM member_memberships), 1), TRUE);
SELECT setval('member_payments_id_seq', COALESCE((SELECT MAX(id) FROM member_payments), 1), TRUE);
SELECT setval('organization_subscriptions_id_seq', COALESCE((SELECT MAX(id) FROM organization_subscriptions), 1), TRUE);
SELECT setval('organization_subscription_payments_id_seq', COALESCE((SELECT MAX(id) FROM organization_subscription_payments), 1), TRUE);
SELECT setval('notifications_id_seq', COALESCE((SELECT MAX(id) FROM notifications), 1), TRUE);
SELECT setval('notification_delivery_logs_id_seq', COALESCE((SELECT MAX(id) FROM notification_delivery_logs), 1), TRUE);
SELECT setval('usage_metrics_id_seq', COALESCE((SELECT MAX(id) FROM usage_metrics), 1), TRUE);
SELECT setval('organization_usage_stats_id_seq', COALESCE((SELECT MAX(id) FROM organization_usage_stats), 1), TRUE);
SELECT setval('member_credit_ledger_id_seq', COALESCE((SELECT MAX(id) FROM member_credit_ledger), 1), TRUE);
SELECT setval('membership_operation_idempotency_id_seq', COALESCE((SELECT MAX(id) FROM membership_operation_idempotency), 1), TRUE);
SELECT setval('processed_webhook_events_id_seq', COALESCE((SELECT MAX(id) FROM processed_webhook_events), 1), TRUE);
SELECT setval('api_request_log_id_seq', COALESCE((SELECT MAX(id) FROM api_request_log), 1), TRUE);
SELECT setval('audit_logs_id_seq', COALESCE((SELECT MAX(id) FROM audit_logs), 1), TRUE);
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.sequences
        WHERE sequence_schema = current_schema()
          AND sequence_name = 'otps_id_seq'
    ) THEN
        PERFORM setval('otps_id_seq', COALESCE((SELECT MAX(id) FROM otps), 1), TRUE);
    END IF;
END $$;
