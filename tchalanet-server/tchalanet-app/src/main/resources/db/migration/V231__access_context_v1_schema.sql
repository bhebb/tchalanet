-- V231: Access-context V1 schema changes
-- 1. Remove RLS from tenant_user — required for IdentityBootstrapFilter to resolve actor
--    before tenant context exists. Protected by module boundaries, repository, status checks.
-- 2. Add partial unique index on tenant_user(user_id) WHERE status = 'ACTIVE'
--    to enforce one active membership per app_user before a full tenant resolve.

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. tenant_user — remove RLS
-- ─────────────────────────────────────────────────────────────────────────────
DROP POLICY IF EXISTS tenant_user_rls_all    ON tenant_user;
DROP POLICY IF EXISTS tenant_user_rls_select ON tenant_user;
ALTER TABLE tenant_user NO FORCE ROW LEVEL SECURITY;
ALTER TABLE tenant_user DISABLE ROW LEVEL SECURITY;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. tenant_user — partial unique index (one active membership per user)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE UNIQUE INDEX IF NOT EXISTS uq_tenant_user_one_active_per_user
    ON tenant_user (user_id)
    WHERE status = 'ACTIVE';

-- Sanity check: RLS is off
DO $$ BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_class WHERE relname = 'tenant_user' AND relrowsecurity
  ) THEN
    RAISE EXCEPTION 'V231 sanity: tenant_user still has RLS enabled';
  END IF;
  RAISE NOTICE 'V231 OK: tenant_user RLS removed, partial index created';
END $$;
