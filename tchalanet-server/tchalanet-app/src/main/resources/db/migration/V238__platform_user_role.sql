-- Platform-scoped role assignments must not be attached to tenant membership.
CREATE TABLE IF NOT EXISTS platform_user_role (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES app_user(id),
  role_id uuid NOT NULL REFERENCES app_role(id),
  assigned_at timestamptz NOT NULL DEFAULT now(),
  assigned_by uuid NULL REFERENCES app_user(id),
  deleted_at timestamptz NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_platform_user_role__active
  ON platform_user_role (user_id, role_id)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_platform_user_role__user
  ON platform_user_role (user_id)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_platform_user_role__role
  ON platform_user_role (role_id)
  WHERE deleted_at IS NULL;

-- Local dev / E2E platform super admin seed.
INSERT INTO platform_user_role (id, user_id, role_id, assigned_at, assigned_by)
SELECT gen_random_uuid(), u.id, r.id, now(), null
FROM app_user u
JOIN app_role r
  ON r.code = 'SUPER_ADMIN'
 AND r.scope = 'PLATFORM'
 AND r.tenant_id IS NULL
WHERE u.id = '00000000-0000-0000-0000-000000010001'::uuid
  AND u.deleted_at IS NULL
  AND r.deleted_at IS NULL
ON CONFLICT DO NOTHING;
