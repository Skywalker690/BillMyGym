ALTER TABLE organizations
    ADD COLUMN monthly_membership_fee NUMERIC(12,2) NOT NULL DEFAULT 0.00;

ALTER TABLE members
    ADD COLUMN payment_status VARCHAR(50) NOT NULL DEFAULT 'PAID',
    ADD COLUMN renewal_date DATE;
