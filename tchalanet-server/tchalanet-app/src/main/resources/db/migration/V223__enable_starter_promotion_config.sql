-- V223: allow the default starter tenant plan to configure base promotions.
-- The V0 admin setup flow exposes Maryaj gratis as a tenant configuration surface.

UPDATE billing_plan
SET features_json = COALESCE(features_json, '{}'::jsonb)
        || jsonb_build_object('promotion.campaigns.config', true),
    limits_json = COALESCE(limits_json, '{}'::jsonb)
        || jsonb_build_object('limits.promotion_rules.max', 5),
    updated_at = now()
WHERE code = 'STARTER'
  AND deleted_at IS NULL;
