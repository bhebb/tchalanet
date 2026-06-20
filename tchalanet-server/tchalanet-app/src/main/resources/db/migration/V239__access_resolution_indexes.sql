-- V239: Indexes to support scoped access resolution on every request.
--
-- resolvePlatform  → platform_user_role(user_id)    already covered by ix_platform_user_role__user (V238)
-- resolveTenant    → tenant_user_role(user_id, tenant_id) covered by uq_tenant_user_role__active prefix
-- effectiveTenant  → tenant_user_role(user_id) scan   ← missing, added below
-- resolveTenant overrides → user_permission_override(tenant_id, user_id) covered by uq_… prefix
-- resolveUserAccess (global) → user_permission_override(user_id)  ← missing, added below

-- ─── tenant_user_role ───────────────────────────────────────────────────────
-- Used by findDistinctActiveTenantIdsByUser — filters by user_id only.
-- The existing unique index starts with tenant_id, so it cannot serve this scan.
CREATE INDEX IF NOT EXISTS idx_tenant_user_role_active_user
    ON tenant_user_role (user_id)
    WHERE deleted_at IS NULL;

-- ─── tenant_user ────────────────────────────────────────────────────────────
-- Used by EffectiveTenantResolver.resolveSingleMembership when falling back to
-- a direct membership query. Composite beats the separate user/tenant indexes.
CREATE INDEX IF NOT EXISTS idx_tenant_user_active_user_tenant
    ON tenant_user (user_id, tenant_id)
    WHERE deleted_at IS NULL;

-- ─── role_permission ────────────────────────────────────────────────────────
-- LEFT JOIN anchor for both resolvePlatform and resolveTenant queries.
-- The composite PK (role_id, permission_code) already starts with role_id, so
-- Postgres can use it; this explicit index is here for clarity and optimizer hints.
CREATE INDEX IF NOT EXISTS idx_role_permission_role
    ON role_permission (role_id);

-- ─── user_permission_override ───────────────────────────────────────────────
-- Used by resolveUserAccess (global path) which queries by user_id across all
-- tenants. The existing unique index starts with tenant_id and cannot serve this.
CREATE INDEX IF NOT EXISTS idx_user_permission_override_active_user
    ON user_permission_override (user_id)
    WHERE deleted_at IS NULL;
