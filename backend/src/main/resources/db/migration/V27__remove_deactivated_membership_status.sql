UPDATE member_memberships
SET status = 'OVERDUE'
WHERE status = 'DEACTIVATED';

DROP INDEX IF EXISTS uk_member_memberships_one_active_per_member;
DROP INDEX IF EXISTS uk_member_memberships_active;

CREATE INDEX IF NOT EXISTS idx_member_memberships_member_end_created
    ON member_memberships(member_id, end_date DESC, created_at DESC, deleted);
