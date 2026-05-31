INSERT INTO usage_metrics (
    organization_id,
    year,
    month,
    monthly_whatsapp_count,
    monthly_email_count,
    monthly_digest_count,
    active_members,
    active_admins,
    subscription_status,
    deleted,
    created_at,
    updated_at,
    version
)
SELECT
    stats.organization_id,
    stats.year,
    stats.month,
    stats.whatsapp_sent_count,
    stats.email_sent_count,
    0,
    0,
    0,
    'ACTIVE',
    FALSE,
    COALESCE(stats.created_at, NOW()),
    COALESCE(stats.updated_at, NOW()),
    0
FROM organization_usage_stats stats
LEFT JOIN usage_metrics metric
    ON metric.organization_id = stats.organization_id
   AND metric.year = stats.year
   AND metric.month = stats.month
   AND metric.deleted = FALSE
WHERE stats.deleted = FALSE
  AND metric.id IS NULL;

UPDATE usage_metrics metric
SET monthly_whatsapp_count = GREATEST(COALESCE(metric.monthly_whatsapp_count, 0), stats.whatsapp_sent_count),
    monthly_email_count = GREATEST(COALESCE(metric.monthly_email_count, 0), stats.email_sent_count),
    updated_at = NOW()
FROM organization_usage_stats stats
WHERE stats.deleted = FALSE
  AND metric.organization_id = stats.organization_id
  AND metric.year = stats.year
  AND metric.month = stats.month
  AND metric.deleted = FALSE;

DROP TABLE IF EXISTS organization_subscriptions;
DROP TABLE IF EXISTS organization_usage_stats;
