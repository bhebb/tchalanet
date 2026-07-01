-- V250: Backfill subscriptions for draft tenants created before subscription provisioning.
-- Draft tenants still need plan entitlements for setup pages such as theme, limits and promotions.

INSERT INTO tenant_subscription (
  id,
  tenant_id,
  plan_code,
  status,
  started_at,
  metadata_json,
  created_at,
  updated_at
)
SELECT
  gen_random_uuid(),
  t.id,
  p.code,
  'ACTIVE',
  now(),
  jsonb_build_object('source', 'V250__backfill_draft_tenant_subscriptions'),
  now(),
  now()
FROM tenant t
CROSS JOIN LATERAL (
  SELECT code
  FROM billing_plan
  WHERE is_default = true
    AND active = true
    AND deleted_at IS NULL
  ORDER BY code
  LIMIT 1
) p
WHERE t.deleted_at IS NULL
  AND t.status IN ('ACTIVE', 'DRAFT')
  AND NOT EXISTS (
    SELECT 1
    FROM tenant_subscription s
    WHERE s.tenant_id = t.id
      AND s.deleted_at IS NULL
      AND s.status IN ('ACTIVE', 'TRIAL')
      AND (s.ends_at IS NULL OR s.ends_at > now())
  );

DO $$ BEGIN
  IF EXISTS (
    SELECT 1
    FROM tenant t
    WHERE t.deleted_at IS NULL
      AND t.status IN ('ACTIVE', 'DRAFT')
      AND NOT EXISTS (
        SELECT 1
        FROM tenant_subscription s
        WHERE s.tenant_id = t.id
          AND s.deleted_at IS NULL
          AND s.status IN ('ACTIVE', 'TRIAL')
          AND (s.ends_at IS NULL OR s.ends_at > now())
      )
  ) THEN
    RAISE EXCEPTION 'V250 sanity: active/draft tenant without current subscription remains';
  END IF;

  RAISE NOTICE 'V250 OK: draft tenant subscriptions backfilled';
END $$;
