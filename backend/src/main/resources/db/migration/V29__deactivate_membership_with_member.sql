UPDATE member_memberships membership
SET status = 'DEACTIVATED',
    last_sent_at = NULL
FROM members member
WHERE membership.member_id = member.id
  AND membership.deleted = FALSE
  AND member.deleted = FALSE
  AND member.status = 'DEACTIVATED'
  AND membership.status <> 'DEACTIVATED'
  AND NOT EXISTS (
      SELECT 1
      FROM member_memberships newer
      WHERE newer.member_id = membership.member_id
        AND newer.deleted = FALSE
        AND (
            newer.end_date > membership.end_date
            OR (newer.end_date = membership.end_date AND newer.created_at > membership.created_at)
        )
  );
