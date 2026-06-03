DROP INDEX IF EXISTS uk_members_org_phone_active;

ALTER TABLE members
    ALTER COLUMN phone DROP NOT NULL;
