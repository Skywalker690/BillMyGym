DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'member_payments_payment_type_check'
    ) THEN
        ALTER TABLE member_payments
            DROP CONSTRAINT member_payments_payment_type_check;
    END IF;
END $$;

ALTER TABLE member_payments
    ADD CONSTRAINT member_payments_payment_type_check
    CHECK (
        payment_type IN (
            'ADMISSION',
            'MEMBERSHIP',
            'ADJUSTMENT',
            'NEW_MEMBERSHIP',
            'RENEWAL'
        )
    );
