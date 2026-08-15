-- V226: Grant setup quick-action permissions to tenant admin roles.
--
-- The admin setup screens gate draw-channel switches on draw_channel.manage
-- and game/matrix switches on game-pricing.update. These permissions must live
-- on the canonical roles so newly-created admins inherit them through normal
-- role assignment.

INSERT INTO role_permission (role_id, permission_code)
SELECT role.id, permission.code
FROM app_role role
CROSS JOIN permission
WHERE role.tenant_id IS NULL
  AND role.deleted_at IS NULL
  AND role.code IN ('SUPER_ADMIN', 'TENANT_OWNER', 'TENANT_ADMIN')
  AND permission.deleted_at IS NULL
  AND permission.code IN ('draw_channel.manage', 'game-pricing.update')
ON CONFLICT DO NOTHING;

DO $$
DECLARE
  missing_count int;
BEGIN
  SELECT count(*) INTO missing_count
  FROM app_role role
  CROSS JOIN permission
  WHERE role.tenant_id IS NULL
    AND role.deleted_at IS NULL
    AND role.code IN ('SUPER_ADMIN', 'TENANT_OWNER', 'TENANT_ADMIN')
    AND permission.deleted_at IS NULL
    AND permission.code IN ('draw_channel.manage', 'game-pricing.update')
    AND NOT EXISTS (
      SELECT 1
      FROM role_permission role_permission
      WHERE role_permission.role_id = role.id
        AND role_permission.permission_code = permission.code
    );

  IF missing_count > 0 THEN
    RAISE EXCEPTION 'V226 sanity: setup quick-action role permissions still missing: %', missing_count;
  END IF;

  RAISE NOTICE 'V226 OK: setup quick-action permissions granted to SUPER_ADMIN, TENANT_OWNER, TENANT_ADMIN';
END $$;
