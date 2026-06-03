UPDATE member_payments
SET payment_type = 'MEMBERSHIP'
WHERE payment_type IN ('NEW_MEMBERSHIP', 'RENEWAL');
