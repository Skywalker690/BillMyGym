-- Development seed data anchored to the current business date in IST: 2026-05-24.
-- Existing user rows are preserved; additional org-admin users are appended for new seeded organizations.

INSERT INTO organizations (
    id, name, district, place, logo_url, map_location_link, address, admin_full_name, admin_phone_number, admin_email,
    monthly_membership_fee, admission_fee, organization_type, subscription_plan, status,
    trial_start_date, trial_end_date, subscription_due_date,
    paused_at, deactivated_at, deleted, deleted_at, created_at, updated_at, version,
    referral_code, gym_coin_balance, total_coins_earned, total_coins_used, six_month_reward_claimed
)
SELECT
    org_id,
    (ARRAY[
        'Iron Forge Fitness',
        'Pulse Arena',
        'Southside Strength',
        'Core Motion Club',
        'Summit Strength Gym',
        'Former Founders Fitness',
        'Demo Wellness Lab',
        'Renewal Wait Gym',
        'Starter House Gym',
        'Elite Barbell Club'
    ])[org_id],
    (ARRAY['Ernakulam', 'Thrissur', 'Kozhikode', 'Kannur', 'Kollam', 'Alappuzha', 'Kottayam', 'Palakkad', 'Malappuram', 'Idukki'])[org_id],
    (ARRAY['Kochi', 'Thrissur Town', 'Nadakkavu', 'Thalassery', 'Chinnakada', 'Cherthala', 'Pala', 'Ottapalam', 'Tirur', 'Munnar'])[org_id],
    'https://img.example.com/gymmax/org-' || LPAD(org_id::text, 2, '0') || '.png',
    'https://maps.example.com/gymmax/org-' || LPAD(org_id::text, 2, '0'),
    (100 + org_id) || ' Fitness Avenue, Kerala',
    (ARRAY['Ravi Kumar', 'Anita Sharma', 'Joseph Mathew', 'Neha Singh', 'Kiran Joseph', 'Meera Nair', 'Arjun Menon', 'Fatima Rahman', 'Vishnu Das', 'Leena Varghese'])[org_id],
    '+9191000000' || LPAD(org_id::text, 2, '0'),
    (ARRAY[
        'ravi@ironforge.local',
        'anita@pulsearena.local',
        'joseph@southside.local',
        'neha@coremotion.local',
        'kiran@summitstrength.local',
        'meera@formerfounders.local',
        'arjun@demowellness.local',
        'fatima@renewalwait.local',
        'vishnu@starterhouse.local',
        'leena@elitebarbell.local'
    ])[org_id],
    (ARRAY[1500.00, 1300.00, 1200.00, 1800.00, 900.00, 1100.00, 1000.00, 1400.00, 800.00, 2200.00])[org_id]::numeric(12, 2),
    (ARRAY[500.00, 400.00, 300.00, 700.00, 0.00, 350.00, 0.00, 450.00, 250.00, 900.00])[org_id]::numeric(12, 2),
    (ARRAY['REGULAR', 'REGULAR', 'REGULAR', 'REGULAR', 'REGULAR', 'REGULAR', 'DEMO', 'REGULAR', 'REGULAR', 'REGULAR'])[org_id],
    (ARRAY['BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM'])[org_id],
    (ARRAY['ACTIVE', 'PAUSED', 'TRIAL', 'PAUSED', 'ACTIVE', 'PAUSED', 'ACTIVE', 'PAUSED', 'ACTIVE', 'ACTIVE'])[org_id],
    CASE WHEN org_id = 3 THEN DATE '2026-05-18' WHEN org_id = 7 THEN NULL ELSE DATE '2026-03-01' END,
    CASE WHEN org_id = 3 THEN DATE '2026-07-16' WHEN org_id = 7 THEN NULL ELSE DATE '2026-04-29' END,
    (ARRAY[DATE '2026-06-22', DATE '2026-05-20', DATE '2026-07-16', DATE '2026-05-23', DATE '2026-07-08', DATE '2026-05-10', NULL, DATE '2026-05-19', DATE '2026-06-05', DATE '2026-05-27'])[org_id],
    CASE WHEN org_id IN (2, 4, 6, 8) THEN TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id % 5 + 1) || ' days')::interval ELSE NULL END,
    NULL,
    FALSE,
    NULL,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((45 + org_id) || ' days')::interval,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id % 5 + 1) || ' days')::interval,
    0,
    'ORG' || LPAD(org_id::text, 3, '0'),
    CASE WHEN org_id = 1 THEN 300.00 WHEN org_id = 4 THEN 600.00 ELSE 0.00 END::numeric(12, 2),
    CASE WHEN org_id = 1 THEN 300.00 WHEN org_id = 4 THEN 600.00 ELSE 0.00 END::numeric(12, 2),
    0.00::numeric(12, 2),
    FALSE
FROM generate_series(1, 10) AS org_id;

INSERT INTO users (
    id, organization_id, email, google_id, avatar_url, password_hash, role, status, terms_accepted, privacy_accepted, policy_version,
    deleted, created_at, updated_at
) VALUES
    (1, NULL, 'sanjoksiby10@gmail.com', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'SUPER_ADMIN', 'ACTIVE', FALSE, FALSE, NULL,
        FALSE, TIMESTAMPTZ '2026-05-04 09:00:00+05:30', TIMESTAMPTZ '2026-05-22 09:00:00+05:30'),
    (3, 2, 'owner02@billmygym.local', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, FALSE, NULL,
        FALSE, TIMESTAMPTZ '2026-05-06 09:00:00+05:30', TIMESTAMPTZ '2026-05-22 09:00:00+05:30'),
    (4, 3, 'owner03@billmygym.local', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, FALSE, NULL,
        FALSE, TIMESTAMPTZ '2026-05-06 09:00:00+05:30', TIMESTAMPTZ '2026-05-22 09:00:00+05:30'),
    (2, 5, 'sanjoksiby0@gmail.com', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', FALSE, FALSE, NULL,
        FALSE, TIMESTAMPTZ '2026-05-06 09:00:00+05:30', TIMESTAMPTZ '2026-05-22 09:00:00+05:30'),
    (5, 1, 'owner01@billmygym.local', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', TRUE, TRUE, '2026-05',
        FALSE, TIMESTAMPTZ '2026-05-07 09:00:00+05:30', TIMESTAMPTZ '2026-05-22 09:00:00+05:30'),
    (6, 4, 'owner04@billmygym.local', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', TRUE, TRUE, '2026-05',
        FALSE, TIMESTAMPTZ '2026-05-07 09:00:00+05:30', TIMESTAMPTZ '2026-05-22 09:00:00+05:30'),
    (7, 6, 'owner06@billmygym.local', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', TRUE, TRUE, '2026-05',
        FALSE, TIMESTAMPTZ '2026-05-07 09:00:00+05:30', TIMESTAMPTZ '2026-05-22 09:00:00+05:30'),
    (8, 7, 'owner07@billmygym.local', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', TRUE, TRUE, '2026-05',
        FALSE, TIMESTAMPTZ '2026-05-07 09:00:00+05:30', TIMESTAMPTZ '2026-05-22 09:00:00+05:30'),
    (12, 7, 'demo@gymmax.local', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', TRUE, TRUE, '2026-05',
        FALSE, TIMESTAMPTZ '2026-05-07 09:00:00+05:30', TIMESTAMPTZ '2026-05-22 09:00:00+05:30'),
    (9, 8, 'owner08@billmygym.local', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', TRUE, TRUE, '2026-05',
        FALSE, TIMESTAMPTZ '2026-05-07 09:00:00+05:30', TIMESTAMPTZ '2026-05-22 09:00:00+05:30'),
    (10, 9, 'owner09@billmygym.local', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', TRUE, TRUE, '2026-05',
        FALSE, TIMESTAMPTZ '2026-05-07 09:00:00+05:30', TIMESTAMPTZ '2026-05-22 09:00:00+05:30'),
    (11, 10, 'owner10@billmygym.local', NULL, NULL, '$2b$12$n6ShiFFjgM4MFEgnFsSLneE2EbNBDRMKchwhKRNlTt54ueRkRGu/C', 'ORG_ADMIN', 'ACTIVE', TRUE, TRUE, '2026-05',
        FALSE, TIMESTAMPTZ '2026-05-07 09:00:00+05:30', TIMESTAMPTZ '2026-05-22 09:00:00+05:30');

INSERT INTO organization_settings (
    id, organization_id, whatsapp_enabled, email_enabled, branding_name,
    deleted, created_at, updated_at
)
SELECT
    org_id,
    org_id,
    org_id NOT IN (3, 9),
    TRUE,
    (ARRAY['Iron Forge', 'Pulse Arena', 'Southside', 'Core Motion', 'Summit Strength', 'Former Founders', 'Demo Wellness', 'Renewal Wait', 'Starter House', 'Elite Barbell'])[org_id],
    FALSE,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((30 + org_id) || ' days')::interval,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id % 6 + 1) || ' days')::interval
FROM generate_series(1, 10) AS org_id;

INSERT INTO members (
    id, organization_id, full_name, phone, status, deleted, deleted_at, deactivated_at, deactivation_reason,
    created_at, updated_at, version
)
SELECT
    ((org_id - 1) * 10) + slot_id,
    org_id,
    (ARRAY['Aarav', 'Diya', 'Rahul', 'Sneha', 'Vivek', 'Nisha', 'Karthik', 'Amina', 'Joel', 'Meera'])[slot_id]
        || ' ' ||
    (ARRAY['Nair', 'Menon', 'Pillai', 'Das', 'Joseph', 'Thomas', 'Khan', 'Varghese', 'Mathew', 'Rajan'])[org_id],
    '+9198' || LPAD((((org_id - 1) * 10) + slot_id)::text, 8, '0'),
    CASE WHEN slot_id IN (4, 5) THEN 'DEACTIVATED' ELSE 'ACTIVE' END,
    FALSE,
    NULL,
    CASE
        WHEN slot_id = 4 THEN TIMESTAMPTZ '2026-05-22 18:00:00+05:30'
        WHEN slot_id = 5 THEN TIMESTAMPTZ '2026-04-19 18:00:00+05:30'
        ELSE NULL
    END,
    CASE
        WHEN slot_id = 4 THEN 'Automatically deactivated after membership stayed overdue for 30 days'
        WHEN slot_id = 5 THEN 'Manual deactivation after member stopped attending'
        ELSE NULL
    END,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - (((org_id * 4) + slot_id + 20) || ' days')::interval,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((slot_id % 5 + 1) || ' days')::interval,
    0
FROM generate_series(1, 10) AS org_id
CROSS JOIN generate_series(1, 10) AS slot_id;

INSERT INTO member_memberships (
    id, member_id, organization_id, duration_months, plan_amount, start_date, end_date, status,
    last_sent_at, deleted, deleted_at, version, created_at, updated_at
)
SELECT
    seeded.id,
    seeded.member_id,
    seeded.organization_id,
    seeded.duration_months,
    seeded.plan_amount,
    seeded.start_date,
    seeded.end_date,
    CASE
        WHEN seeded.end_date < DATE '2026-05-24' THEN 'OVERDUE'
        ELSE 'ACTIVE'
    END,
    CASE
        WHEN seeded.end_date < DATE '2026-05-24' AND seeded.slot_id IN (3, 8) THEN TIMESTAMPTZ '2026-05-23 09:00:00+05:30'
        ELSE NULL
    END,
    FALSE,
    NULL,
    0,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - (((seeded.organization_id * 3) + seeded.slot_id + 10) || ' days')::interval,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((seeded.slot_id % 5 + 1) || ' days')::interval
FROM (
    SELECT
        member.id,
        member.id AS member_id,
        member.organization_id,
        slot.slot_id,
        CASE WHEN slot.slot_id = 6 THEN 3 WHEN slot.slot_id = 7 THEN 12 ELSE 1 END AS duration_months,
        CASE
            WHEN slot.slot_id = 6 THEN organization.monthly_membership_fee * 3
            WHEN slot.slot_id = 7 THEN organization.monthly_membership_fee * 12
            ELSE organization.monthly_membership_fee
        END AS plan_amount,
        CASE
            WHEN slot.slot_id = 1 THEN DATE '2026-05-01'
            WHEN slot.slot_id = 2 THEN DATE '2026-04-28'
            WHEN slot.slot_id = 3 THEN DATE '2026-04-21'
            WHEN slot.slot_id = 4 THEN DATE '2026-03-19'
            WHEN slot.slot_id = 5 THEN DATE '2026-02-28'
            WHEN slot.slot_id = 6 THEN DATE '2026-04-15'
            WHEN slot.slot_id = 7 THEN DATE '2026-01-01'
            WHEN slot.slot_id = 8 THEN DATE '2026-04-11'
            WHEN slot.slot_id = 9 THEN DATE '2026-04-25'
            ELSE DATE '2026-05-16'
        END AS start_date,
        CASE
            WHEN slot.slot_id = 1 THEN DATE '2026-05-31'
            WHEN slot.slot_id = 2 THEN DATE '2026-05-27'
            WHEN slot.slot_id = 3 THEN DATE '2026-05-20'
            WHEN slot.slot_id = 4 THEN DATE '2026-04-18'
            WHEN slot.slot_id = 5 THEN DATE '2026-03-27'
            WHEN slot.slot_id = 6 THEN DATE '2026-07-14'
            WHEN slot.slot_id = 7 THEN DATE '2026-12-31'
            WHEN slot.slot_id = 8 THEN DATE '2026-05-10'
            WHEN slot.slot_id = 9 THEN DATE '2026-05-24'
            ELSE DATE '2026-06-15'
        END AS end_date
    FROM members member
    JOIN organizations organization ON organization.id = member.organization_id
    CROSS JOIN LATERAL (SELECT ((member.id - 1) % 10) + 1 AS slot_id) slot
) seeded;

INSERT INTO member_payments (
    id, member_id, membership_id, organization_id, amount, payment_date, billing_month, payment_status, payment_type,
    gateway_transaction_id, notes, deleted, deleted_at, created_at, updated_at, version
)
SELECT
    membership.id,
    membership.member_id,
    membership.id,
    membership.organization_id,
    membership.plan_amount,
    CASE
        WHEN slot.slot_id IN (1, 2, 9, 10) THEN DATE '2026-05-24' - ((slot.slot_id % 4) || ' days')::interval
        WHEN slot.slot_id = 6 THEN DATE '2026-05-05'
        WHEN slot.slot_id = 7 THEN DATE '2026-01-01'
        ELSE membership.start_date
    END::date,
    TO_CHAR(
        CASE
            WHEN slot.slot_id IN (1, 2, 9, 10) THEN DATE '2026-05-24' - ((slot.slot_id % 4) || ' days')::interval
            WHEN slot.slot_id = 6 THEN DATE '2026-05-05'
            WHEN slot.slot_id = 7 THEN DATE '2026-01-01'
            ELSE membership.start_date
        END::date,
        'YYYY-MM'
    ),
    'PAID',
    CASE
        WHEN slot.slot_id = 1 THEN 'NEW_MEMBERSHIP'
        WHEN slot.slot_id IN (2, 6, 9, 10) THEN 'RENEWAL'
        ELSE 'MEMBERSHIP'
    END,
    'mem_pay_' || LPAD(membership.id::text, 4, '0'),
    'Full membership payment; no partial payments in seed data',
    FALSE,
    NULL,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((slot.slot_id + 2) || ' days')::interval,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((slot.slot_id % 4 + 1) || ' days')::interval,
    0
FROM member_memberships membership
CROSS JOIN LATERAL (SELECT ((membership.member_id - 1) % 10) + 1 AS slot_id) slot;

INSERT INTO member_payments (
    id, member_id, membership_id, organization_id, amount, payment_date, billing_month, payment_status, payment_type,
    gateway_transaction_id, notes, deleted, deleted_at, created_at, updated_at, version
)
SELECT
    100 + ((membership.organization_id - 1) * 2) + CASE WHEN slot.slot_id = 1 THEN 1 ELSE 2 END,
    membership.member_id,
    membership.id,
    membership.organization_id,
    organization.admission_fee,
    (DATE '2026-05-24' - ((membership.organization_id % 4) || ' days')::interval)::date,
    TO_CHAR((DATE '2026-05-24' - ((membership.organization_id % 4) || ' days')::interval)::date, 'YYYY-MM'),
    'PAID',
    'ADMISSION',
    'adm_pay_' || LPAD(membership.organization_id::text, 2, '0') || '_' || slot.slot_id,
    'Full admission fee collected with membership onboarding',
    FALSE,
    NULL,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((membership.organization_id % 4 + 1) || ' days')::interval,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((membership.organization_id % 3 + 1) || ' days')::interval,
    0
FROM member_memberships membership
JOIN organizations organization ON organization.id = membership.organization_id
CROSS JOIN LATERAL (SELECT ((membership.member_id - 1) % 10) + 1 AS slot_id) slot
WHERE slot.slot_id IN (1, 9);

INSERT INTO member_credit_ledger (
    id, member_id, organization_id, reference_membership_id, amount, transaction_type, reason,
    notes, created_by_user_id, created_by_email, deleted, created_at, updated_at
) VALUES
    (1, 1, 1, 1, 150.00, 'CREDIT', 'PROMOTION', 'Founding member credit', 5, 'owner01@billmygym.local', FALSE, TIMESTAMPTZ '2026-05-10 09:00:00+05:30', TIMESTAMPTZ '2026-05-10 09:00:00+05:30'),
    (2, 12, 2, 12, 100.00, 'DEBIT', 'MANUAL_ADJUSTMENT', 'Applied against renewal', 3, 'owner02@billmygym.local', FALSE, TIMESTAMPTZ '2026-05-11 09:00:00+05:30', TIMESTAMPTZ '2026-05-11 09:00:00+05:30'),
    (3, 23, 3, 23, 200.00, 'CREDIT', 'CORRECTION', 'Billing correction credit', 4, 'owner03@billmygym.local', FALSE, TIMESTAMPTZ '2026-05-12 09:00:00+05:30', TIMESTAMPTZ '2026-05-12 09:00:00+05:30'),
    (4, 34, 4, 34, 300.00, 'CREDIT', 'REFUND', 'Refund wallet balance', 6, 'owner04@billmygym.local', FALSE, TIMESTAMPTZ '2026-05-13 09:00:00+05:30', TIMESTAMPTZ '2026-05-13 09:00:00+05:30'),
    (5, 45, 5, 45, 90.00, 'DEBIT', 'PACKAGE_SWITCH', 'Applied during package switch', 2, 'sanjoksiby0@gmail.com', FALSE, TIMESTAMPTZ '2026-05-14 09:00:00+05:30', TIMESTAMPTZ '2026-05-14 09:00:00+05:30'),
    (6, 56, 6, 56, 180.00, 'CREDIT', 'PROMOTION', 'Legacy workspace promo credit', 7, 'owner06@billmygym.local', FALSE, TIMESTAMPTZ '2026-05-15 09:00:00+05:30', TIMESTAMPTZ '2026-05-15 09:00:00+05:30'),
    (7, 67, 7, 67, 125.00, 'CREDIT', 'PROMOTION', 'Demo credit sample', 8, 'owner07@billmygym.local', FALSE, TIMESTAMPTZ '2026-05-16 09:00:00+05:30', TIMESTAMPTZ '2026-05-16 09:00:00+05:30'),
    (8, 78, 8, 78, 250.00, 'CREDIT', 'CORRECTION', 'Renewal wait correction', 9, 'owner08@billmygym.local', FALSE, TIMESTAMPTZ '2026-05-17 09:00:00+05:30', TIMESTAMPTZ '2026-05-17 09:00:00+05:30'),
    (9, 89, 9, 89, 75.00, 'DEBIT', 'MANUAL_ADJUSTMENT', 'Admin wallet debit', 10, 'owner09@billmygym.local', FALSE, TIMESTAMPTZ '2026-05-18 09:00:00+05:30', TIMESTAMPTZ '2026-05-18 09:00:00+05:30'),
    (10, 100, 10, 100, 500.00, 'CREDIT', 'PROMOTION', 'Elite annual promotion', 11, 'owner10@billmygym.local', FALSE, TIMESTAMPTZ '2026-05-19 09:00:00+05:30', TIMESTAMPTZ '2026-05-19 09:00:00+05:30');

INSERT INTO organization_subscription_payments (
    id, organization_id, amount, subscription_plan, payment_date, subscription_start, subscription_end, payment_status,
    retry_count, next_retry_date, failure_reason, webhook_event_id, gateway_transaction_id, razorpay_order_id,
    razorpay_payment_id, razorpay_subscription_id, deleted, created_at, updated_at, version
)
SELECT
    org_id,
    org_id,
    (ARRAY[1499.00, 699.00, 299.00, 1499.00, 0.00, 299.00, 0.00, 699.00, 299.00, 1499.00])[org_id]::numeric(12, 2),
    (ARRAY['BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM', 'BILLMYGYM'])[org_id],
    CASE WHEN org_id IN (1, 5, 7, 9, 10) THEN (ARRAY[DATE '2026-05-23', DATE '2026-04-21', DATE '2026-05-18', DATE '2026-04-24', DATE '2026-05-12', DATE '2026-04-01', DATE '2026-05-01', DATE '2026-04-20', DATE '2026-05-07', DATE '2026-04-28'])[org_id] ELSE NULL END,
    (ARRAY[DATE '2026-05-23', DATE '2026-04-21', DATE '2026-05-18', DATE '2026-04-24', DATE '2026-05-12', DATE '2026-04-01', DATE '2026-05-01', DATE '2026-04-20', DATE '2026-05-07', DATE '2026-04-28'])[org_id],
    (ARRAY[DATE '2026-06-22', DATE '2026-05-20', DATE '2026-05-31', DATE '2026-05-23', DATE '2026-07-08', DATE '2026-05-10', DATE '2027-05-24', DATE '2026-05-19', DATE '2026-06-05', DATE '2026-05-27'])[org_id],
    (ARRAY['PAID', 'FAILED', 'PENDING', 'FAILED', 'PAID', 'FAILED', 'PAID', 'PENDING', 'PAID', 'PAID'])[org_id],
    CASE WHEN org_id IN (2, 4, 6, 8) THEN 2 WHEN org_id = 3 THEN 1 ELSE 0 END,
    CASE WHEN org_id IN (2, 3, 4, 6, 8) THEN DATE '2026-05-25' ELSE NULL END,
    CASE WHEN org_id IN (2, 4, 6) THEN 'Auto-debit failed; renewal required' ELSE NULL END,
    'evt_seed_sub_' || LPAD(org_id::text, 2, '0'),
    CASE WHEN org_id IN (1, 5, 7, 9, 10) THEN 'pay_seed_sub_' || LPAD(org_id::text, 2, '0') ELSE NULL END,
    'order_seed_sub_' || LPAD(org_id::text, 2, '0'),
    CASE WHEN org_id IN (1, 5, 7, 9, 10) THEN 'pay_seed_sub_' || LPAD(org_id::text, 2, '0') ELSE NULL END,
    'sub_seed_org_' || LPAD(org_id::text, 2, '0'),
    FALSE,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((18 + org_id) || ' days')::interval,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id % 4 + 1) || ' days')::interval,
    0
FROM generate_series(1, 10) AS org_id;

INSERT INTO referrals (
    id, referrer_org_id, referred_org_id, referral_code_used, status, reward_type, deleted, created_at, updated_at
) VALUES
    (1, 1, 2, 'ORG001', 'REWARDED', 'GYM_COIN', FALSE, TIMESTAMPTZ '2026-05-20 09:00:00+05:30', TIMESTAMPTZ '2026-05-20 09:00:00+05:30'),
    (2, 4, 3, 'ORG004', 'REWARDED', 'GYM_COIN', FALSE, TIMESTAMPTZ '2026-05-18 09:00:00+05:30', TIMESTAMPTZ '2026-05-18 09:00:00+05:30'),
    (3, 4, 6, 'ORG004', 'REWARDED', 'GYM_COIN', FALSE, TIMESTAMPTZ '2026-05-19 09:00:00+05:30', TIMESTAMPTZ '2026-05-19 09:00:00+05:30');

INSERT INTO referral_rewards (
    id, org_id, reward_type, amount, months_rewarded, reward_used, deleted, created_at, updated_at
) VALUES
    (1, 1, 'GYM_COIN', 300.00, 0, FALSE, FALSE, TIMESTAMPTZ '2026-05-20 09:00:00+05:30', TIMESTAMPTZ '2026-05-20 09:00:00+05:30'),
    (2, 4, 'GYM_COIN', 300.00, 0, FALSE, FALSE, TIMESTAMPTZ '2026-05-18 09:00:00+05:30', TIMESTAMPTZ '2026-05-18 09:00:00+05:30'),
    (3, 4, 'GYM_COIN', 300.00, 0, FALSE, FALSE, TIMESTAMPTZ '2026-05-19 09:00:00+05:30', TIMESTAMPTZ '2026-05-19 09:00:00+05:30');

INSERT INTO notifications (
    id, organization_id, member_id, member_count, type, channel, recipient, status, attempt_count, failure_reason,
    notification_date, sent_at, message, deleted, created_at, updated_at
)
SELECT
    org_id,
    org_id,
    ((org_id - 1) * 10) + 3,
    2,
    (ARRAY['ORGANIZATION_RENEWAL_DIGEST', 'SUBSCRIPTION_PAYMENT_FAILED', 'TRIAL_ENDING', 'SUBSCRIPTION_PAUSED', 'MONTHLY_ORGANIZATION_REPORT', 'SUBSCRIPTION_DEACTIVATED', 'INDIVIDUAL_WHATSAPP_REMINDER', 'SUBSCRIPTION_REMINDER', 'ORGANIZATION_WELCOME', 'SUBSCRIPTION_PAYMENT_SUCCESS'])[org_id],
    CASE WHEN org_id IN (5, 7, 10) THEN 'WHATSAPP' ELSE 'EMAIL' END,
    CASE WHEN org_id IN (5, 7, 10) THEN '+9191000000' || LPAD(org_id::text, 2, '0') ELSE (SELECT admin_email FROM organizations WHERE id = org_id) END,
    (ARRAY['SENT', 'FAILED', 'SENT', 'SENT', 'SENT', 'FAILED', 'SENT', 'PENDING', 'SENT', 'SENT'])[org_id],
    CASE WHEN org_id IN (2, 6, 8) THEN 2 ELSE 1 END,
    CASE WHEN org_id IN (2, 6) THEN 'Delivery provider rejected the message' ELSE NULL END,
    (DATE '2026-05-24' - ((org_id % 3) || ' days')::interval)::date,
    CASE WHEN org_id IN (2, 6, 8) THEN NULL ELSE TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id % 3 + 1) || ' hours')::interval END,
    'Seed notification for organization ' || org_id,
    FALSE,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id + 1) || ' days')::interval,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id % 3 + 1) || ' hours')::interval
FROM generate_series(1, 10) AS org_id;

INSERT INTO notification_delivery_logs (
    id, notification_id, organization_id, type, status, attempt_count, last_attempt, provider, template_name,
    recipient, provider_message_id, error_message, next_retry_at, request_payload, deleted, created_at, updated_at
)
SELECT
    org_id,
    org_id,
    org_id,
    (SELECT type FROM notifications WHERE id = org_id),
    (ARRAY['SENT', 'FAILED', 'SENT', 'SENT', 'SENT', 'FAILED', 'SENT', 'PENDING', 'SENT', 'SENT'])[org_id],
    CASE WHEN org_id IN (2, 6, 8) THEN 2 ELSE 1 END,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id % 5 + 1) || ' hours')::interval,
    CASE WHEN org_id IN (5, 7, 10) THEN 'WHATSAPP_CLOUD' ELSE 'SYSTEM' END,
    LOWER((SELECT type FROM notifications WHERE id = org_id)),
    (SELECT recipient FROM notifications WHERE id = org_id),
    CASE WHEN org_id IN (2, 6, 8) THEN NULL ELSE 'provider_msg_' || LPAD(org_id::text, 2, '0') END,
    CASE WHEN org_id IN (2, 6) THEN 'Provider delivery failure' ELSE NULL END,
    CASE WHEN org_id = 8 THEN TIMESTAMPTZ '2026-05-24 13:00:00+05:30' ELSE NULL END,
    '{"seed":true,"channel":"reporting"}',
    FALSE,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id + 2) || ' days')::interval,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id % 4 + 1) || ' hours')::interval
FROM generate_series(1, 10) AS org_id;

INSERT INTO whatsapp_reminder_logs (
    id, organization_id, member_id, message, sent_at, status, deleted, created_at, updated_at
)
SELECT
    org_id,
    org_id,
    ((org_id - 1) * 10) + CASE WHEN org_id % 2 = 0 THEN 8 ELSE 3 END,
    'Hello, your gym membership is overdue. Due Amount: INR ' || (SELECT monthly_membership_fee::int FROM organizations WHERE id = org_id) || '.',
    CASE WHEN org_id IN (3, 8) THEN NULL ELSE TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id % 6 + 1) || ' hours')::interval END,
    CASE WHEN org_id = 8 THEN 'PENDING' WHEN org_id = 3 THEN 'FAILED' ELSE 'SENT' END,
    FALSE,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id % 7 + 1) || ' hours')::interval,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id % 5 + 1) || ' hours')::interval
FROM generate_series(1, 10) AS org_id;

INSERT INTO usage_metrics (
    id, organization_id, year, month, monthly_whatsapp_count, monthly_email_count, monthly_digest_count,
    active_members, active_admins, subscription_status, deleted, created_at, updated_at
)
SELECT
    org_id,
    org_id,
    2026,
    5,
    6 + org_id,
    4 + org_id,
    1 + (org_id % 3),
    8,
    1,
    (ARRAY['ACTIVE', 'PAUSED', 'TRIAL', 'PAUSED', 'ACTIVE', 'PAUSED', 'ACTIVE', 'PAUSED', 'ACTIVE', 'ACTIVE'])[org_id],
    FALSE,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id + 5) || ' days')::interval,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - INTERVAL '1 day'
FROM generate_series(1, 10) AS org_id;


INSERT INTO membership_operation_idempotency (
    id, organization_id, member_id, idempotency_key, operation_type, resource_type, resource_id,
    deleted, created_at, updated_at
)
SELECT
    org_id,
    org_id,
    ((org_id - 1) * 10) + 1,
    'seed-operation-' || LPAD(org_id::text, 2, '0'),
    (ARRAY['MEMBERSHIP_CREATE', 'PAYMENT_RECORD', 'SUBSCRIPTION_CREATE', 'SUBSCRIPTION_VERIFY', 'MEMBERSHIP_SWITCH', 'CREDIT_ADJUSTMENT', 'DEBIT_ADJUSTMENT', 'PAYMENT_RECORD', 'MEMBERSHIP_CREATE', 'SUBSCRIPTION_VERIFY'])[org_id],
    (ARRAY['MEMBERSHIP', 'PAYMENT', 'SUBSCRIPTION_PAYMENT', 'SUBSCRIPTION_PAYMENT', 'MEMBERSHIP', 'CREDIT_LEDGER_ENTRY', 'CREDIT_LEDGER_ENTRY', 'PAYMENT', 'MEMBERSHIP', 'SUBSCRIPTION_PAYMENT'])[org_id],
    CASE WHEN org_id IN (6, 7) THEN org_id ELSE ((org_id - 1) * 10) + 1 END,
    FALSE,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id + 1) || ' hours')::interval,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id + 1) || ' hours')::interval
FROM generate_series(1, 10) AS org_id;

INSERT INTO processed_webhook_events (
    id, event_id, event_type, processed_at, deleted, created_at, updated_at
)
SELECT
    org_id,
    'webhook-seed-event-' || LPAD(org_id::text, 2, '0'),
    (ARRAY['payment.captured', 'payment.failed', 'subscription.charged', 'order.paid', 'payment.captured', 'payment.failed', 'subscription.activated', 'subscription.pending', 'payment.captured', 'subscription.charged'])[org_id],
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id + 2) || ' hours')::interval,
    FALSE,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id + 2) || ' hours')::interval,
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id + 2) || ' hours')::interval
FROM generate_series(1, 10) AS org_id;

INSERT INTO api_request_log (
    id, organization_id, request_id, endpoint, response_hash, response_body, created_at
)
SELECT
    org_id,
    org_id,
    'req-seed-' || LPAD(org_id::text, 2, '0'),
    (ARRAY['GET:/api/reports/dashboard', 'GET:/api/reports/financial', 'POST:/api/payments', 'POST:/api/subscription/create', 'GET:/api/members', 'POST:/api/memberships', 'POST:/api/whatsapp-reminder/send', 'GET:/api/tracking/export', 'GET:/api/subscription/status', 'GET:/api/reports/reminders'])[org_id],
    md5('seed-response-' || org_id),
    '{"status":"ok","seed":true}',
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id + 1) || ' hours')::interval
FROM generate_series(1, 10) AS org_id;

INSERT INTO audit_logs (
    id, event, description, actor_name, actor_email, actor_role, organization_id, category, activity_scope,
    event_status, target_name, zone, ip_address, device, browser, timestamp
)
SELECT
    org_id,
    CASE WHEN org_id <= 8 THEN 'MEMBER_DEACTIVATED' WHEN org_id = 9 THEN 'MEMBER_REACTIVATED' ELSE 'MONTHLY_REPORT_GENERATED' END,
    CASE WHEN org_id <= 8 THEN 'Seed deactivation cycle recorded for monthly deactivation reporting' WHEN org_id = 9 THEN 'Seed reactivation after member returned' ELSE 'Seed monthly report generated' END,
    CASE WHEN org_id = 10 THEN 'System Scheduler' ELSE (SELECT admin_full_name FROM organizations WHERE id = org_id) END,
    CASE WHEN org_id = 10 THEN 'system@billmygym.local' ELSE (SELECT admin_email FROM organizations WHERE id = org_id) END,
    CASE WHEN org_id = 10 THEN 'SYSTEM' ELSE 'ORG_ADMIN' END,
    org_id,
    CASE WHEN org_id <= 9 THEN 'MEMBER' ELSE 'SYSTEM' END,
    CASE WHEN org_id <= 9 THEN 'MEMBER_STATUS_ACTIVITY' ELSE 'ADMIN_ACTIVITY' END,
    'INFO',
    CASE WHEN org_id <= 8 THEN (SELECT full_name FROM members WHERE id = ((org_id - 1) * 10) + 4) ELSE (SELECT name FROM organizations WHERE id = org_id) END,
    'IST',
    '10.10.10.' || org_id,
    (ARRAY['Windows Laptop', 'MacBook Pro', 'Office Desktop', 'Android Phone', 'iPad', 'Backend Worker', 'ChromeBook', 'Windows Laptop', 'Android Phone', 'Backend Worker'])[org_id],
    (ARRAY['Chrome 136', 'Edge 136', 'Chrome 136', 'Chrome Mobile', 'Safari', 'Spring Scheduler', 'Chrome 136', 'Edge 136', 'Chrome Mobile', 'Spring Scheduler'])[org_id],
    TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((org_id + 1) || ' hours')::interval
FROM generate_series(1, 10) AS org_id;

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
        )
        SELECT
            otp_id,
            (ARRAY[
                'sanjoksiby10@gmail.com',
                'owner01@billmygym.local',
                'owner02@billmygym.local',
                'owner03@billmygym.local',
                'owner04@billmygym.local',
                'sanjoksiby0@gmail.com',
                'owner06@billmygym.local',
                'owner07@billmygym.local',
                'owner08@billmygym.local',
                'owner10@billmygym.local'
            ])[otp_id],
            LPAD((410000 + otp_id * 137)::text, 6, '0'),
            TIMESTAMPTZ '2026-05-24 09:30:00+05:30' + ((otp_id % 4 + 1) || ' minutes')::interval,
            otp_id IN (4, 8),
            TIMESTAMPTZ '2026-05-24 09:00:00+05:30' - ((otp_id + 1) || ' minutes')::interval
        FROM generate_series(1, 10) AS otp_id;
    END IF;
END $$;

SELECT setval('organizations_id_seq', COALESCE((SELECT MAX(id) FROM organizations), 1), TRUE);
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1), TRUE);
SELECT setval('organization_settings_id_seq', COALESCE((SELECT MAX(id) FROM organization_settings), 1), TRUE);
SELECT setval('members_id_seq', COALESCE((SELECT MAX(id) FROM members), 1), TRUE);
SELECT setval('member_memberships_id_seq', COALESCE((SELECT MAX(id) FROM member_memberships), 1), TRUE);
SELECT setval('member_payments_id_seq', COALESCE((SELECT MAX(id) FROM member_payments), 1), TRUE);
SELECT setval('organization_subscription_payments_id_seq', COALESCE((SELECT MAX(id) FROM organization_subscription_payments), 1), TRUE);
SELECT setval('notifications_id_seq', COALESCE((SELECT MAX(id) FROM notifications), 1), TRUE);
SELECT setval('notification_delivery_logs_id_seq', COALESCE((SELECT MAX(id) FROM notification_delivery_logs), 1), TRUE);
SELECT setval('whatsapp_reminder_logs_id_seq', COALESCE((SELECT MAX(id) FROM whatsapp_reminder_logs), 1), TRUE);
SELECT setval('usage_metrics_id_seq', COALESCE((SELECT MAX(id) FROM usage_metrics), 1), TRUE);
SELECT setval('member_credit_ledger_id_seq', COALESCE((SELECT MAX(id) FROM member_credit_ledger), 1), TRUE);
SELECT setval('membership_operation_idempotency_id_seq', COALESCE((SELECT MAX(id) FROM membership_operation_idempotency), 1), TRUE);
SELECT setval('processed_webhook_events_id_seq', COALESCE((SELECT MAX(id) FROM processed_webhook_events), 1), TRUE);
SELECT setval('api_request_log_id_seq', COALESCE((SELECT MAX(id) FROM api_request_log), 1), TRUE);
SELECT setval('audit_logs_id_seq', COALESCE((SELECT MAX(id) FROM audit_logs), 1), TRUE);
SELECT setval('referrals_id_seq', COALESCE((SELECT MAX(id) FROM referrals), 1), TRUE);
SELECT setval('referral_rewards_id_seq', COALESCE((SELECT MAX(id) FROM referral_rewards), 1), TRUE);
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
