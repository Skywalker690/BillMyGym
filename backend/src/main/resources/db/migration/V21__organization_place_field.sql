ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS place VARCHAR(255);

UPDATE organizations
SET place = district
WHERE place IS NULL;
