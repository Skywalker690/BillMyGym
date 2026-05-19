INSERT INTO organizations (
    id, name, district, logo_url, map_location_link, address, admin_full_name, admin_phone_number, admin_email,
    description, monthly_membership_fee, organization_type, subscription_plan, status, trial_start_date, trial_end_date,
    pilot_start_date, pilot_end_date, subscription_due_date, paused_at, deactivated_at, deleted, deleted_at,
    created_at, updated_at
)
SELECT
    gs,
    'Gym ' || LPAD(gs::text, 2, '0'),
    (ARRAY['Central', 'North', 'South', 'East', 'West'])[1 + ((gs - 1) % 5)],
    'https://img.example.com/gym-' || gs || '.png',
    'https://maps.example.com/gym-' || gs,
    (100 + gs) || ' Fitness Avenue',
    'Owner ' || LPAD(gs::text, 2, '0'),
    '+91910000' || LPAD(gs::text, 4, '0'),
    'owner' || LPAD(gs::text, 2, '0') || '@billmygym.local',
    'Demo organization ' || gs || ' for local development and dashboard testing',
    (800 + (gs * 40))::numeric(12, 2),
    CASE WHEN gs IN (6, 12, 18) THEN 'PILOT' ELSE 'REGULAR' END,
    CASE
        WHEN gs IN (6, 12, 18) THEN 'PILOT'
        WHEN gs % 7 = 0 THEN 'PRO'
        WHEN gs % 3 = 0 THEN 'GROWTH'
        ELSE 'STARTER'
    END,
    CASE
        WHEN gs IN (4, 9, 14, 19) THEN 'PAUSED'
        WHEN gs IN (5, 10, 15, 20) THEN 'TRIAL'
        WHEN gs IN (8, 16) THEN 'DEACTIVATED'
        ELSE 'ACTIVE'
    END,
    CASE WHEN gs IN (6, 12, 18) THEN NULL ELSE CURRENT_DATE - ((gs % 12) + 10) END,
    CASE WHEN gs IN (6, 12, 18) THEN NULL ELSE CURRENT_DATE + (CASE WHEN gs IN (5, 10, 15, 20) THEN (gs % 5) + 2 ELSE -((gs % 9) + 1) END) END,
    CASE WHEN gs IN (6, 12, 18) THEN CURRENT_DATE - 20 ELSE NULL END,
    CASE WHEN gs IN (6, 12, 18) THEN CURRENT_DATE + 70 ELSE NULL END,
    CASE
        WHEN gs IN (6, 12, 18) THEN CURRENT_DATE + 70
        WHEN gs IN (5, 10, 15, 20) THEN CURRENT_DATE + ((gs % 5) + 2)
        WHEN gs IN (4, 9, 14, 19) THEN CURRENT_DATE - ((gs % 6) + 2)
        WHEN gs IN (8, 16) THEN CURRENT_DATE - 45
        ELSE CURRENT_DATE + ((gs % 6) + 5)
    END,
    CASE WHEN gs IN (4, 9, 14, 19) THEN CURRENT_TIMESTAMP - (((gs % 4) + 1) || ' days')::interval ELSE NULL END,
    CASE WHEN gs IN (8, 16) THEN CURRENT_TIMESTAMP - (((gs % 7) + 15) || ' days')::interval ELSE NULL END,
    FALSE,
    NULL,
    CURRENT_TIMESTAMP - ((25 - gs) || ' days')::interval,
    CURRENT_TIMESTAMP - ((25 - gs) || ' days')::interval
FROM generate_series(1, 20) AS gs;

INSERT INTO users (id, organization_id, email, password_hash, role, status, deleted, created_at, updated_at) VALUES
(1, NULL, 'sanjoksiby10@gmail.com', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'SUPER_ADMIN', 'ACTIVE', FALSE, CURRENT_TIMESTAMP - INTERVAL '40 days', CURRENT_TIMESTAMP - INTERVAL '40 days'),
(2, 1, 'owner01@billmygym.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '30 days'),
(3, 2, 'owner02@billmygym.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, CURRENT_TIMESTAMP - INTERVAL '29 days', CURRENT_TIMESTAMP - INTERVAL '29 days'),
(4, 3, 'owner03@billmygym.local', '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, CURRENT_TIMESTAMP - INTERVAL '28 days', CURRENT_TIMESTAMP - INTERVAL '28 days');

INSERT INTO organization_settings (
    id, organization_id, whatsapp_enabled, email_enabled, reminder_days_before_expiry, branding_name, deleted, created_at, updated_at
)
SELECT
    gs,
    gs,
    gs % 4 <> 0,
    gs % 3 <> 0,
    1 + (gs % 4),
    'Gym ' || LPAD(gs::text, 2, '0') || ' Studio',
    FALSE,
    CURRENT_TIMESTAMP - ((24 - gs) || ' days')::interval,
    CURRENT_TIMESTAMP - ((24 - gs) || ' days')::interval
FROM generate_series(1, 20) AS gs;

INSERT INTO members (
    id, organization_id, full_name, phone, email, status, deleted, deleted_at, deactivated_at, deactivation_reason, notes, created_at, updated_at
)
SELECT
    gs,
    gs,
    (ARRAY[
        'Aarush Nair', 'Diya Sharma', 'Kabir Menon', 'Meera Joshi', 'Ishaan Verma',
        'Anaya Rao', 'Ritvik Das', 'Sana Ali', 'Vihaan Gupta', 'Kiara Thomas',
        'Yash Malhotra', 'Tara Kapoor', 'Arjun Pillai', 'Mira Sethi', 'Rohan George',
        'Naina Arora', 'Devansh Jain', 'Ira Bose', 'Kunal Reddy', 'Aditi Bhat'
    ])[gs],
    '+91980000' || LPAD(gs::text, 4, '0'),
    CASE WHEN gs % 6 = 0 THEN NULL ELSE 'member' || LPAD(gs::text, 2, '0') || '@example.com' END,
    CASE
        WHEN gs IN (8, 16) THEN 'DEACTIVATED'
        WHEN gs IN (4, 12, 20) THEN 'PAUSED'
        ELSE 'ACTIVE'
    END,
    FALSE,
    NULL,
    CASE WHEN gs IN (8, 16) THEN CURRENT_TIMESTAMP - (((gs % 5) + 3) || ' days')::interval ELSE NULL END,
    CASE WHEN gs IN (8, 16) THEN 'Inactive member kept for historical reporting' ELSE NULL END,
    CASE
        WHEN gs % 5 = 0 THEN 'Joined through current local demo campaign'
        WHEN gs % 2 = 0 THEN 'Prefers morning training slots'
        ELSE 'Regular floor training member'
    END,
    CURRENT_TIMESTAMP - ((23 - gs) || ' days')::interval,
    CURRENT_TIMESTAMP - ((23 - gs) || ' days')::interval
FROM generate_series(1, 20) AS gs;

INSERT INTO member_memberships (
    id, member_id, organization_id, plan_name, duration_months, plan_amount, start_date, end_date, status, last_sent_at,
    deleted, deleted_at, version, created_at, updated_at
)
SELECT
    gs,
    gs,
    gs,
    (ARRAY['Monthly Core', 'Quarterly Flex', 'Half-Year Build', 'Annual Strength'])[1 + ((gs - 1) % 4)],
    (ARRAY[1, 3, 6, 12])[1 + ((gs - 1) % 4)],
    (ARRAY[1200.00, 3600.00, 6600.00, 12000.00])[1 + ((gs - 1) % 4)]::numeric(12, 2),
    CASE
        WHEN gs IN (2, 6, 10, 14, 18) THEN CURRENT_DATE - 15
        WHEN gs IN (3, 7, 11, 15, 19) THEN CURRENT_DATE - 95
        WHEN gs IN (4, 8, 12, 16, 20) THEN CURRENT_DATE - 370
        ELSE CURRENT_DATE - 5
    END,
    CASE
        WHEN gs IN (2, 6, 10, 14, 18) THEN CURRENT_DATE + 15
        WHEN gs IN (3, 7, 11, 15, 19) THEN CURRENT_DATE - 2
        WHEN gs IN (4, 8, 12, 16, 20) THEN CURRENT_DATE - 30
        ELSE CURRENT_DATE + 25
    END,
    CASE
        WHEN gs IN (3, 7, 11, 15, 19) THEN 'OVERDUE'
        WHEN gs IN (4, 8, 12, 16, 20) THEN 'COMPLETED'
        ELSE 'ACTIVE'
    END,
    CASE WHEN gs % 3 = 0 THEN CURRENT_TIMESTAMP - (((gs % 6) + 1) || ' days')::interval ELSE NULL END,
    FALSE,
    NULL,
    0,
    CURRENT_TIMESTAMP - ((22 - gs) || ' days')::interval,
    CURRENT_TIMESTAMP - ((22 - gs) || ' days')::interval
FROM generate_series(1, 20) AS gs;

INSERT INTO member_payments (
    id, member_id, membership_id, organization_id, amount, payment_date, billing_month, payment_status, payment_type,
    gateway_transaction_id, notes, deleted, deleted_at, created_at, updated_at
)
SELECT
    gs,
    gs,
    gs,
    gs,
    CASE
        WHEN gs IN (3, 7, 11, 15, 19) THEN 0.00
        WHEN gs IN (2, 6, 10, 14, 18) THEN 1000.00
        ELSE (ARRAY[1200.00, 3600.00, 6600.00, 12000.00])[1 + ((gs - 1) % 4)]::numeric(12, 2)
    END,
    CURRENT_DATE - (gs % 12),
    TO_CHAR(CURRENT_DATE - (gs % 12), 'YYYY-MM'),
    CASE
        WHEN gs IN (3, 7, 11, 15, 19) THEN 'FAILED'
        WHEN gs IN (2, 6, 10, 14, 18) THEN 'PARTIAL'
        ELSE 'PAID'
    END,
    CASE WHEN gs % 4 = 0 THEN 'RENEWAL' WHEN gs % 5 = 0 THEN 'ADJUSTMENT' ELSE 'NEW_MEMBERSHIP' END,
    CASE WHEN gs IN (2, 6, 10, 14, 18) THEN NULL ELSE 'txn_member_' || LPAD(gs::text, 3, '0') END,
    CASE
        WHEN gs IN (3, 7, 11, 15, 19) THEN 'Gateway charge failed during retry flow'
        WHEN gs IN (2, 6, 10, 14, 18) THEN 'Collected partial amount for active plan'
        ELSE 'Seed payment for local reporting'
    END,
    FALSE,
    NULL,
    CURRENT_TIMESTAMP - ((21 - gs) || ' days')::interval,
    CURRENT_TIMESTAMP - ((21 - gs) || ' days')::interval
FROM generate_series(1, 20) AS gs;

-- Additional per-organization member dataset to make each gym feel realistic in dashboard and reminder flows:
-- 1. Approaching renewal with full payment
-- 2. Active membership with partial payment and pending revenue
-- 3. Overdue renewal with unpaid overdue balance
INSERT INTO members (
    id, organization_id, full_name, phone, email, status, deleted, deleted_at, deactivated_at, deactivation_reason, notes, created_at, updated_at
)
SELECT
    1000 + ((org_id - 1) * 3) + scenario_id,
    org_id,
    CASE scenario_id
        WHEN 1 THEN 'Renewal Watch Member ' || LPAD(org_id::text, 2, '0')
        WHEN 2 THEN 'Pending Revenue Member ' || LPAD(org_id::text, 2, '0')
        ELSE 'Overdue Renewal Member ' || LPAD(org_id::text, 2, '0')
    END,
    '+919700' || LPAD((((org_id - 1) * 3) + scenario_id)::text, 6, '0'),
    CASE scenario_id
        WHEN 3 THEN 'overdue.member.' || LPAD(org_id::text, 2, '0') || '@example.com'
        WHEN 2 THEN 'pending.member.' || LPAD(org_id::text, 2, '0') || '@example.com'
        ELSE 'renewal.member.' || LPAD(org_id::text, 2, '0') || '@example.com'
    END,
    'ACTIVE',
    FALSE,
    NULL,
    NULL,
    NULL,
    CASE scenario_id
        WHEN 1 THEN 'Seed member ending soon to trigger expiring-soon dashboards'
        WHEN 2 THEN 'Seed member with partial collection and pending revenue'
        ELSE 'Seed member overdue to populate pending renewal and overdue revenue views'
    END,
    CURRENT_TIMESTAMP - (((40 - org_id) + scenario_id) || ' days')::interval,
    CURRENT_TIMESTAMP - (((40 - org_id) + scenario_id) || ' days')::interval
FROM generate_series(1, 20) AS org_id
CROSS JOIN generate_series(1, 3) AS scenario_id;

INSERT INTO member_memberships (
    id, member_id, organization_id, plan_name, duration_months, plan_amount, start_date, end_date, status, last_sent_at,
    deleted, deleted_at, version, created_at, updated_at
)
SELECT
    2000 + ((org_id - 1) * 3) + scenario_id,
    1000 + ((org_id - 1) * 3) + scenario_id,
    org_id,
    CASE scenario_id
        WHEN 1 THEN 'Renewal Sprint Plan'
        WHEN 2 THEN 'Flex Growth Plan'
        ELSE 'Recovery Renewal Plan'
    END,
    1,
    CASE scenario_id
        WHEN 1 THEN 1800.00
        WHEN 2 THEN 2400.00
        ELSE 2100.00
    END::numeric(12, 2),
    CASE scenario_id
        WHEN 1 THEN CURRENT_DATE - 29
        WHEN 2 THEN CURRENT_DATE - 23
        ELSE CURRENT_DATE - 36
    END,
    CASE scenario_id
        WHEN 1 THEN CURRENT_DATE + 1
        WHEN 2 THEN CURRENT_DATE + 6
        ELSE CURRENT_DATE - ((org_id % 4) + 1)
    END,
    CASE scenario_id
        WHEN 3 THEN 'OVERDUE'
        ELSE 'ACTIVE'
    END,
    CASE
        WHEN scenario_id = 3 THEN NULL
        WHEN scenario_id = 2 THEN CURRENT_TIMESTAMP - ((org_id % 3) || ' days')::interval
        ELSE CURRENT_TIMESTAMP - (((org_id % 2) + 1) || ' days')::interval
    END,
    FALSE,
    NULL,
    0,
    CURRENT_TIMESTAMP - (((30 - org_id) + scenario_id) || ' days')::interval,
    CURRENT_TIMESTAMP - (((30 - org_id) + scenario_id) || ' days')::interval
FROM generate_series(1, 20) AS org_id
CROSS JOIN generate_series(1, 3) AS scenario_id;

INSERT INTO member_payments (
    id, member_id, membership_id, organization_id, amount, payment_date, billing_month, payment_status, payment_type,
    gateway_transaction_id, notes, deleted, deleted_at, created_at, updated_at
)
SELECT
    3000 + ((org_id - 1) * 3) + scenario_id,
    1000 + ((org_id - 1) * 3) + scenario_id,
    2000 + ((org_id - 1) * 3) + scenario_id,
    org_id,
    CASE scenario_id
        WHEN 1 THEN 1800.00
        WHEN 2 THEN 1200.00
        ELSE 0.00
    END::numeric(12, 2),
    CASE scenario_id
        WHEN 1 THEN CURRENT_DATE - 28
        WHEN 2 THEN CURRENT_DATE - 18
        ELSE CURRENT_DATE - ((org_id % 4) + 2)
    END,
    TO_CHAR(
        CASE scenario_id
            WHEN 1 THEN CURRENT_DATE - 28
            WHEN 2 THEN CURRENT_DATE - 18
            ELSE CURRENT_DATE - ((org_id % 4) + 2)
        END,
        'YYYY-MM'
    ),
    CASE scenario_id
        WHEN 1 THEN 'PAID'
        WHEN 2 THEN 'PARTIAL'
        ELSE 'FAILED'
    END,
    'RENEWAL',
    CASE scenario_id
        WHEN 1 THEN 'txn_renewal_watch_' || LPAD(org_id::text, 3, '0')
        WHEN 2 THEN NULL
        ELSE 'txn_overdue_fail_' || LPAD(org_id::text, 3, '0')
    END,
    CASE scenario_id
        WHEN 1 THEN 'Fully paid member approaching renewal date'
        WHEN 2 THEN 'Partially collected renewal to create pending revenue'
        ELSE 'Failed renewal collection to create overdue pending revenue'
    END,
    FALSE,
    NULL,
    CURRENT_TIMESTAMP - (((28 - org_id) + scenario_id) || ' days')::interval,
    CURRENT_TIMESTAMP - (((28 - org_id) + scenario_id) || ' days')::interval
FROM generate_series(1, 20) AS org_id
CROSS JOIN generate_series(1, 3) AS scenario_id;

INSERT INTO organization_subscriptions (
    id, organization_id, start_date, end_date, status, amount, razorpay_subscription_id, deleted, created_at, updated_at
)
SELECT
    gs,
    gs,
    CURRENT_DATE - 29,
    CURRENT_DATE + 1,
    CASE
        WHEN gs IN (4, 9, 14, 19) THEN 'FAILED'
        WHEN gs IN (5, 10, 15, 20) THEN 'PENDING'
        WHEN gs IN (8, 16) THEN 'EXPIRED'
        ELSE 'ACTIVE'
    END,
    299.00,
    'sub_org_' || LPAD(gs::text, 3, '0'),
    FALSE,
    CURRENT_TIMESTAMP - ((20 - gs) || ' days')::interval,
    CURRENT_TIMESTAMP - ((20 - gs) || ' days')::interval
FROM generate_series(1, 20) AS gs;

INSERT INTO organization_subscription_payments (
    id, organization_id, amount, payment_date, subscription_start, subscription_end, payment_status, retry_count,
    next_retry_date, failure_reason, webhook_event_id, gateway_transaction_id, razorpay_order_id, razorpay_payment_id,
    razorpay_subscription_id, deleted, created_at, updated_at
)
SELECT
    gs,
    gs,
    299.00,
    CASE WHEN gs IN (5, 10, 15, 20) THEN NULL ELSE CURRENT_DATE - (gs % 9) END,
    CURRENT_DATE - 29,
    CURRENT_DATE + 1,
    CASE
        WHEN gs IN (4, 9, 14, 19) THEN 'FAILED'
        WHEN gs IN (5, 10, 15, 20) THEN 'PENDING'
        WHEN gs IN (8, 16) THEN 'REFUNDED'
        ELSE 'PAID'
    END,
    CASE WHEN gs IN (4, 9, 14, 19) THEN 2 WHEN gs IN (5, 10, 15, 20) THEN 1 ELSE 0 END,
    CASE WHEN gs IN (4, 5, 9, 10, 14, 15, 19, 20) THEN CURRENT_DATE + ((gs % 3) + 1) ELSE NULL END,
    CASE
        WHEN gs IN (4, 9, 14, 19) THEN 'Card auto-debit failed'
        WHEN gs IN (8, 16) THEN 'Charge refunded in local seed set'
        ELSE NULL
    END,
    CASE WHEN gs % 2 = 0 THEN 'evt_sub_' || LPAD(gs::text, 3, '0') ELSE NULL END,
    CASE WHEN gs IN (5, 10, 15, 20) THEN NULL ELSE 'pay_sub_' || LPAD(gs::text, 3, '0') END,
    'order_sub_' || LPAD(gs::text, 3, '0'),
    CASE WHEN gs IN (5, 10, 15, 20) THEN NULL ELSE 'pay_sub_' || LPAD(gs::text, 3, '0') END,
    'sub_org_' || LPAD(gs::text, 3, '0'),
    FALSE,
    CURRENT_TIMESTAMP - ((19 - gs) || ' days')::interval,
    CURRENT_TIMESTAMP - ((19 - gs) || ' days')::interval
FROM generate_series(1, 20) AS gs;

INSERT INTO notifications (
    id, organization_id, member_id, member_count, type, channel, recipient, status, attempt_count, failure_reason,
    notification_date, sent_at, message, deleted, created_at, updated_at
)
SELECT
    gs,
    gs,
    gs,
    1,
    (ARRAY[
        'ORGANIZATION_RENEWAL_DIGEST', 'SUBSCRIPTION_REMINDER', 'TRIAL_ENDING', 'SUBSCRIPTION_PAYMENT_SUCCESS',
        'SUBSCRIPTION_PAYMENT_FAILED', 'SUBSCRIPTION_PAUSED', 'SUBSCRIPTION_DEACTIVATED'
    ])[1 + ((gs - 1) % 7)],
    CASE WHEN gs % 2 = 0 THEN 'EMAIL' ELSE 'WHATSAPP' END,
    CASE WHEN gs % 2 = 0 THEN 'owner' || LPAD(gs::text, 2, '0') || '@billmygym.local' ELSE '+91910000' || LPAD(gs::text, 4, '0') END,
    CASE
        WHEN gs IN (4, 8, 12, 16, 20) THEN 'FAILED'
        WHEN gs IN (3, 7, 11, 15, 19) THEN 'RETRYING'
        ELSE 'SENT'
    END,
    CASE
        WHEN gs IN (4, 8, 12, 16, 20) THEN 3
        WHEN gs IN (3, 7, 11, 15, 19) THEN 2
        ELSE 1
    END,
    CASE WHEN gs IN (4, 8, 12, 16, 20) THEN 'Delivery provider rejected payload' ELSE NULL END,
    CURRENT_DATE - (gs % 7),
    CASE WHEN gs IN (4, 8, 12, 16, 20) OR gs IN (3, 7, 11, 15, 19) THEN NULL ELSE CURRENT_TIMESTAMP - ((gs % 10) || ' hours')::interval END,
    'Demo notification message ' || gs,
    FALSE,
    CURRENT_TIMESTAMP - ((18 - gs) || ' days')::interval,
    CURRENT_TIMESTAMP - ((18 - gs) || ' days')::interval
FROM generate_series(1, 20) AS gs;

INSERT INTO usage_metrics (
    id, organization_id, year, month, monthly_whatsapp_count, monthly_email_count, monthly_digest_count,
    active_members, active_admins, subscription_status, deleted, created_at, updated_at
)
SELECT
    gs,
    gs,
    EXTRACT(YEAR FROM CURRENT_DATE)::int,
    EXTRACT(MONTH FROM CURRENT_DATE)::int,
    5 + gs,
    3 + (gs * 2),
    1 + (gs % 6),
    CASE WHEN gs IN (8, 16) THEN 0 ELSE 1 END,
    1,
    CASE
        WHEN gs IN (4, 9, 14, 19) THEN 'PAUSED'
        WHEN gs IN (8, 16) THEN 'DEACTIVATED'
        WHEN gs IN (5, 10, 15, 20) THEN 'TRIAL'
        ELSE 'ACTIVE'
    END,
    FALSE,
    CURRENT_TIMESTAMP - ((17 - gs) || ' days')::interval,
    CURRENT_TIMESTAMP - ((17 - gs) || ' days')::interval
FROM generate_series(1, 20) AS gs;

INSERT INTO organization_usage_stats (
    id, organization_id, year, month, whatsapp_sent_count, email_sent_count, deleted, created_at, updated_at
)
SELECT
    gs,
    gs,
    EXTRACT(YEAR FROM CURRENT_DATE)::int,
    EXTRACT(MONTH FROM CURRENT_DATE)::int,
    5 + gs,
    3 + (gs * 2),
    FALSE,
    CURRENT_TIMESTAMP - ((17 - gs) || ' days')::interval,
    CURRENT_TIMESTAMP - ((17 - gs) || ' days')::interval
FROM generate_series(1, 20) AS gs;

INSERT INTO member_credit_ledger (
    id, member_id, organization_id, amount, transaction_type, reason, reference_membership_id,
    notes, created_by_user_id, created_by_email, deleted, created_at, updated_at
)
SELECT
    gs,
    gs,
    gs,
    CASE WHEN gs % 4 = 0 THEN 300.00 ELSE 150.00 END,
    CASE WHEN gs % 4 = 0 THEN 'CREDIT' ELSE 'DEBIT' END,
    CASE WHEN gs % 5 = 0 THEN 'PROMOTION' WHEN gs % 3 = 0 THEN 'PACKAGE_SWITCH' ELSE 'MANUAL_ADJUSTMENT' END,
    gs,
    'Ledger seed row ' || gs,
    CASE WHEN gs <= 3 THEN gs + 1 ELSE NULL END,
    CASE WHEN gs <= 3 THEN 'owner' || LPAD(gs::text, 2, '0') || '@billmygym.local' ELSE 'system@billmygym.local' END,
    FALSE,
    CURRENT_TIMESTAMP - ((16 - gs) || ' days')::interval,
    CURRENT_TIMESTAMP - ((16 - gs) || ' days')::interval
FROM generate_series(1, 20) AS gs;

INSERT INTO membership_operation_idempotency (
    id, organization_id, member_id, idempotency_key, operation_type, resource_type, resource_id, deleted, created_at, updated_at
)
SELECT
    gs,
    gs,
    gs,
    'membership-op-' || LPAD(gs::text, 3, '0'),
    (ARRAY['MEMBERSHIP_CREATE', 'PAYMENT_RECORD', 'CREDIT_ADJUSTMENT', 'DEBIT_ADJUSTMENT', 'MEMBERSHIP_SWITCH'])[1 + ((gs - 1) % 5)],
    (ARRAY['MEMBERSHIP', 'PAYMENT', 'CREDIT_LEDGER_ENTRY'])[1 + ((gs - 1) % 3)],
    gs,
    FALSE,
    CURRENT_TIMESTAMP - ((15 - gs) || ' days')::interval,
    CURRENT_TIMESTAMP - ((15 - gs) || ' days')::interval
FROM generate_series(1, 20) AS gs;

INSERT INTO processed_webhook_events (
    id, event_id, event_type, processed_at, deleted, created_at, updated_at
)
SELECT
    gs,
    'webhook-event-' || LPAD(gs::text, 3, '0'),
    (ARRAY['payment.captured', 'order.paid', 'payment.failed', 'subscription.charged'])[1 + ((gs - 1) % 4)],
    CURRENT_TIMESTAMP - ((14 - gs) || ' hours')::interval,
    FALSE,
    CURRENT_TIMESTAMP - ((14 - gs) || ' hours')::interval,
    CURRENT_TIMESTAMP - ((14 - gs) || ' hours')::interval
FROM generate_series(1, 20) AS gs;

INSERT INTO api_request_log (
    id, organization_id, request_id, endpoint, response_hash, response_body, created_at
)
SELECT
    gs,
    gs,
    'req-' || LPAD(gs::text, 3, '0'),
    CASE WHEN gs % 2 = 0 THEN 'POST:/api/subscription/create' ELSE 'POST:/api/subscription/verify' END,
    md5('response-' || gs),
    '{"request":"seed-' || gs || '","status":"ok"}',
    CURRENT_TIMESTAMP - ((13 - gs) || ' hours')::interval
FROM generate_series(1, 20) AS gs;

INSERT INTO notification_delivery_logs (
    id, notification_id, organization_id, type, status, attempt_count, last_attempt, deleted, created_at, updated_at
)
SELECT
    gs,
    gs,
    gs,
    (ARRAY[
        'ORGANIZATION_RENEWAL_DIGEST', 'SUBSCRIPTION_REMINDER', 'TRIAL_ENDING', 'SUBSCRIPTION_PAYMENT_SUCCESS',
        'SUBSCRIPTION_PAYMENT_FAILED', 'SUBSCRIPTION_PAUSED', 'SUBSCRIPTION_DEACTIVATED'
    ])[1 + ((gs - 1) % 7)],
    CASE
        WHEN gs IN (4, 8, 12, 16, 20) THEN 'FAILED'
        WHEN gs IN (3, 7, 11, 15, 19) THEN 'RETRYING'
        WHEN gs IN (5, 10) THEN 'PENDING'
        ELSE 'SENT'
    END,
    CASE
        WHEN gs IN (4, 8, 12, 16, 20) THEN 3
        WHEN gs IN (3, 7, 11, 15, 19) THEN 2
        ELSE 1
    END,
    CURRENT_TIMESTAMP - ((12 - gs) || ' hours')::interval,
    FALSE,
    CURRENT_TIMESTAMP - ((12 - gs) || ' hours')::interval,
    CURRENT_TIMESTAMP - ((12 - gs) || ' hours')::interval
FROM generate_series(1, 20) AS gs;

SELECT setval('organizations_id_seq', COALESCE((SELECT MAX(id) FROM organizations), 1), TRUE);
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1), TRUE);
SELECT setval('organization_settings_id_seq', COALESCE((SELECT MAX(id) FROM organization_settings), 1), TRUE);
SELECT setval('members_id_seq', COALESCE((SELECT MAX(id) FROM members), 1), TRUE);
SELECT setval('member_memberships_id_seq', COALESCE((SELECT MAX(id) FROM member_memberships), 1), TRUE);
SELECT setval('member_payments_id_seq', COALESCE((SELECT MAX(id) FROM member_payments), 1), TRUE);
SELECT setval('organization_subscriptions_id_seq', COALESCE((SELECT MAX(id) FROM organization_subscriptions), 1), TRUE);
SELECT setval('organization_subscription_payments_id_seq', COALESCE((SELECT MAX(id) FROM organization_subscription_payments), 1), TRUE);
SELECT setval('notifications_id_seq', COALESCE((SELECT MAX(id) FROM notifications), 1), TRUE);
SELECT setval('usage_metrics_id_seq', COALESCE((SELECT MAX(id) FROM usage_metrics), 1), TRUE);
SELECT setval('organization_usage_stats_id_seq', COALESCE((SELECT MAX(id) FROM organization_usage_stats), 1), TRUE);
SELECT setval('member_credit_ledger_id_seq', COALESCE((SELECT MAX(id) FROM member_credit_ledger), 1), TRUE);
SELECT setval('membership_operation_idempotency_id_seq', COALESCE((SELECT MAX(id) FROM membership_operation_idempotency), 1), TRUE);
SELECT setval('processed_webhook_events_id_seq', COALESCE((SELECT MAX(id) FROM processed_webhook_events), 1), TRUE);
SELECT setval('api_request_log_id_seq', COALESCE((SELECT MAX(id) FROM api_request_log), 1), TRUE);
SELECT setval('notification_delivery_logs_id_seq', COALESCE((SELECT MAX(id) FROM notification_delivery_logs), 1), TRUE);
