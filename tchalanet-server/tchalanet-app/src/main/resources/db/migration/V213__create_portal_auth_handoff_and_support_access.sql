-- Technical security/session tables for cross-origin portal authentication handoff.

CREATE TABLE portal_auth_handoff (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code_hash varchar(64) NOT NULL,
  subject_user_id uuid NOT NULL REFERENCES app_user(id),
  target_portal varchar(32) NOT NULL,
  entry_route varchar(512) NOT NULL,
  support_access_session_id uuid,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  expires_at timestamptz NOT NULL,
  consumed_at timestamptz,
  CONSTRAINT chk_portal_auth_handoff__target
    CHECK (target_portal IN ('ADMIN', 'PLATFORM')),
  CONSTRAINT chk_portal_auth_handoff__entry_relative
    CHECK (entry_route LIKE '/%' AND entry_route NOT LIKE '//%')
);

CREATE INDEX ix_portal_auth_handoff__expires_at
  ON portal_auth_handoff (expires_at);

CREATE INDEX ix_portal_auth_handoff__subject_user
  ON portal_auth_handoff (subject_user_id);

CREATE INDEX ix_portal_auth_handoff__unconsumed
  ON portal_auth_handoff (id, expires_at)
  WHERE consumed_at IS NULL;

GRANT SELECT, INSERT, UPDATE ON portal_auth_handoff TO app_user;

CREATE TABLE support_access_session (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  super_admin_user_id uuid NOT NULL REFERENCES app_user(id),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  tenant_code varchar(128) NOT NULL,
  tenant_name varchar(255) NOT NULL,
  mode varchar(32) NOT NULL,
  reason varchar(1024) NOT NULL,
  granted_at timestamptz NOT NULL,
  expires_at timestamptz NOT NULL,
  cleared_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT chk_support_access_session__mode
    CHECK (mode IN ('SUPPORT_OVERRIDE', 'SUPPORT_READONLY'))
);

CREATE INDEX ix_support_access_session__super_admin
  ON support_access_session (super_admin_user_id);

CREATE INDEX ix_support_access_session__tenant
  ON support_access_session (tenant_id);

CREATE INDEX ix_support_access_session__current
  ON support_access_session (super_admin_user_id, granted_at DESC)
  WHERE cleared_at IS NULL;

ALTER TABLE portal_auth_handoff
  ADD CONSTRAINT fk_portal_auth_handoff__support_access_session
  FOREIGN KEY (support_access_session_id)
  REFERENCES support_access_session(id);

GRANT SELECT, INSERT, UPDATE ON support_access_session TO app_user;
