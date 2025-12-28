-- V7: core identity & access control

CREATE TABLE IF NOT EXISTS app_user (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,
    keycloak_id uuid NOT NULL,
    username text NOT NULL,
    email citext,
    phone text,
    tenant_code text NOT NULL,
    tenant_id uuid NOT NULL REFERENCES tenant(id),
    first_name text,
    last_name text,
    display_name text,
    avatar_url text,
    locale varchar(8),
    time_zone varchar(64),
    status varchar(32) NOT NULL DEFAULT 'PENDING_APPROVAL',
    approved_at timestamptz,
    approved_by uuid,
    last_login_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
);

ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_status
        CHECK (status IN ('PENDING_APPROVAL', 'ACTIVE', 'SUSPENDED', 'INVITED'));

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_keycloak_id
    ON app_user (keycloak_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_email
    ON app_user (email) WHERE email IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_phone
    ON app_user (phone) WHERE phone IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_app_user_tenant_id ON app_user (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_app_user_tenant_code ON app_user (tenant_code) WHERE deleted_at IS NULL;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_app_user_updated_at') THEN
    CREATE TRIGGER trg_app_user_updated_at
      BEFORE UPDATE ON app_user
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS app_user_archive (
    id uuid PRIMARY KEY,
    archived_at timestamptz NOT NULL DEFAULT now(),
    archived_by uuid,
    version bigint NOT NULL,
    keycloak_id uuid NOT NULL,
    username text NOT NULL,
    email citext,
    phone text,
    tenant_code text NOT NULL,
    tenant_id uuid NOT NULL,
    first_name text,
    last_name text,
    display_name text,
    avatar_url text,
    locale varchar(8),
    time_zone varchar(64),
    status varchar(32) NOT NULL,
    approved_at timestamptz,
    approved_by uuid,
    last_login_at timestamptz,
    created_at timestamptz NOT NULL,
    created_by uuid,
    updated_at timestamptz NOT NULL,
    updated_by uuid,
    deleted_at timestamptz
);

ALTER TABLE app_user_archive
    ADD CONSTRAINT ck_app_user_archive_status
        CHECK (status IN ('PENDING_APPROVAL', 'ACTIVE', 'SUSPENDED', 'INVITED'));
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_archive_keycloak_id
    ON app_user_archive (keycloak_id);
CREATE INDEX IF NOT EXISTS ix_app_user_archive_tenant_id ON app_user_archive (tenant_id);

CREATE TABLE IF NOT EXISTS user_preference (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,
    user_id uuid NOT NULL REFERENCES app_user(id),
    theme_mode varchar(10),
    density smallint,
    locale varchar(8),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_user_preference_user_id
    ON user_preference (user_id) WHERE deleted_at IS NULL;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_user_preference_updated_at') THEN
    CREATE TRIGGER trg_user_preference_updated_at
      BEFORE UPDATE ON user_preference
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS app_role (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,
    code varchar(64) NOT NULL,
    name varchar(128) NOT NULL,
    description text,
    tenant_id uuid REFERENCES tenant(id),
    parent_role_id uuid REFERENCES app_role(id),
    is_system boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_role_tenant_code
    ON app_role (tenant_id, code) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_app_role_tenant ON app_role (tenant_id) WHERE deleted_at IS NULL;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_app_role_updated_at') THEN
    CREATE TRIGGER trg_app_role_updated_at
      BEFORE UPDATE ON app_role
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS permission (
    code varchar(128) PRIMARY KEY,
    name varchar(128) NOT NULL,
    description text,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_permission_updated_at') THEN
    CREATE TRIGGER trg_permission_updated_at
      BEFORE UPDATE ON permission
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS role_permission (
    role_id uuid NOT NULL REFERENCES app_role(id),
    permission_code varchar(128) NOT NULL REFERENCES permission(code),
    version bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (role_id, permission_code)
);

CREATE TABLE IF NOT EXISTS tenant_user (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,
    tenant_id uuid NOT NULL REFERENCES tenant(id),
    user_id uuid NOT NULL REFERENCES app_user(id),
    role_id uuid NOT NULL REFERENCES app_role(id),
    autonomy_level varchar(16) NOT NULL DEFAULT 'none',
    is_owner boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,
    UNIQUE (tenant_id, user_id)
);

