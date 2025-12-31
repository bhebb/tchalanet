-- V7: core identity & access control (v1: global user status + approval)

-- ---------------------------------------------------------------------
-- USER (global identity)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_user (
                                        id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    keycloak_sub uuid NOT NULL,

    username text,
    email citext,
    phone text,

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
        CHECK (status IN ('INVITED','PENDING_APPROVAL','ACTIVE','SUSPENDED'));

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_keycloak_sub
    ON app_user (keycloak_sub)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_email
    ON app_user (email)
    WHERE email IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_phone
    ON app_user (phone)
    WHERE phone IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_app_user_status
    ON app_user (status)
    WHERE deleted_at IS NULL;

DROP TRIGGER IF EXISTS trg_app_user_updated_at ON app_user;
CREATE TRIGGER trg_app_user_updated_at
    BEFORE UPDATE ON app_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ---------------------------------------------------------------------
-- USER PREFERENCE (1-1 with app_user)
-- ---------------------------------------------------------------------
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
    deleted_at timestamptz,

    UNIQUE (user_id)
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_preference_user_id
    ON user_preference (user_id)
    WHERE deleted_at IS NULL;

DROP TRIGGER IF EXISTS trg_user_preference_updated_at ON user_preference;
CREATE TRIGGER trg_user_preference_updated_at
    BEFORE UPDATE ON user_preference
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ---------------------------------------------------------------------
-- PERMISSIONS
-- ---------------------------------------------------------------------
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

DROP TRIGGER IF EXISTS trg_permission_updated_at ON permission;
CREATE TRIGGER trg_permission_updated_at
    BEFORE UPDATE ON permission
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ---------------------------------------------------------------------
-- ROLES (system roles: tenant_id NULL)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_role (
                                        id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    version bigint NOT NULL DEFAULT 0,

    tenant_id uuid REFERENCES tenant(id),

    code varchar(64) NOT NULL,
    name varchar(128) NOT NULL,
    description text,

    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_role_system_code
    ON app_role (code)
    WHERE tenant_id IS NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_role_tenant_code
    ON app_role (tenant_id, code)
    WHERE tenant_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_app_role_tenant
    ON app_role (tenant_id)
    WHERE deleted_at IS NULL;

DROP TRIGGER IF EXISTS trg_app_role_updated_at ON app_role;
CREATE TRIGGER trg_app_role_updated_at
    BEFORE UPDATE ON app_role
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ---------------------------------------------------------------------
-- ROLE <-> PERMISSION mapping
-- Choose ONE of the two variants below.
-- ---------------------------------------------------------------------

-- Variant A (RECOMMENDED): simple mapping table (no audit/version columns)
CREATE TABLE IF NOT EXISTS role_permission (
                                               role_id uuid NOT NULL REFERENCES app_role(id),
    permission_code varchar(128) NOT NULL REFERENCES permission(code),
    PRIMARY KEY (role_id, permission_code)
    );

-- Variant B (if you REALLY want AuditableEntity on AppRolePermissionEntity):
-- (Uncomment this instead of Variant A, and keep your entity extends AuditableEntity)
-- CREATE TABLE IF NOT EXISTS role_permission (
--   role_id uuid NOT NULL REFERENCES app_role(id),
--   permission_code varchar(128) NOT NULL REFERENCES permission(code),
--   version bigint NOT NULL DEFAULT 0,
--   created_at timestamptz NOT NULL DEFAULT now(),
--   created_by uuid,
--   updated_at timestamptz NOT NULL DEFAULT now(),
--   updated_by uuid,
--   deleted_at timestamptz,
--   PRIMARY KEY (role_id, permission_code)
-- );
-- DROP TRIGGER IF EXISTS trg_role_permission_updated_at ON role_permission;
-- CREATE TRIGGER trg_role_permission_updated_at
--   BEFORE UPDATE ON role_permission
--   FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ---------------------------------------------------------------------
-- TENANT MEMBERSHIP (kept for role/autonomy even if user status is global in v1)
-- ---------------------------------------------------------------------
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

ALTER TABLE tenant_user
    ADD CONSTRAINT ck_tenant_user_autonomy_level
        CHECK (autonomy_level IN ('none', 'partial', 'full'));

CREATE INDEX IF NOT EXISTS ix_tenant_user_tenant
    ON tenant_user (tenant_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_tenant_user_user
    ON tenant_user (user_id)
    WHERE deleted_at IS NULL;

DROP TRIGGER IF EXISTS trg_tenant_user_updated_at ON tenant_user;
CREATE TRIGGER trg_tenant_user_updated_at
    BEFORE UPDATE ON tenant_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
