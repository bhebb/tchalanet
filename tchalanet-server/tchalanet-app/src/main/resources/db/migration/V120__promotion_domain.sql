-- Promotion domain tables.
-- BaseTenantEntity columns are physically present in every tenant table:
-- id, tenant_id, created_at, created_by, updated_at, updated_by, deleted_at, deleted_by, version.
-- Repositories must not filter tenant_id or deleted_at manually; RLS handles tenant isolation and deleted visibility.

CREATE TABLE promotion_campaign (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  code varchar(96) NOT NULL,
  name varchar(160) NOT NULL,
  status varchar(32) NOT NULL,
  priority integer NOT NULL DEFAULT 100,
  starts_at timestamptz NULL,
  ends_at timestamptz NULL,
  config_version varchar(48) NOT NULL DEFAULT 'v1',
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  deleted_by uuid NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_promotion_campaign_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE promotion_rule (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  campaign_id uuid NOT NULL REFERENCES promotion_campaign(id),
  rule_key varchar(96) NOT NULL,
  status varchar(32) NOT NULL,
  evaluation_phase varchar(48) NOT NULL,
  eligibility_json jsonb NOT NULL DEFAULT '{}'::jsonb,
  effects_json jsonb NOT NULL DEFAULT '{}'::jsonb,
  priority integer NOT NULL DEFAULT 100,
  quota_key varchar(96) NULL,
  max_uses integer NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  deleted_by uuid NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_promotion_rule_tenant_campaign_key UNIQUE (tenant_id, campaign_id, rule_key)
);

CREATE TABLE promotion_quota (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  campaign_id uuid NOT NULL REFERENCES promotion_campaign(id),
  rule_id uuid NOT NULL REFERENCES promotion_rule(id),
  quota_key varchar(128) NOT NULL,
  max_uses integer NOT NULL,
  used_count integer NOT NULL DEFAULT 0,
  window_starts_at timestamptz NULL,
  window_ends_at timestamptz NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  deleted_by uuid NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_promotion_quota_tenant_key UNIQUE (tenant_id, quota_key),
  CONSTRAINT ck_promotion_quota_counts CHECK (used_count >= 0 AND max_uses >= 0 AND used_count <= max_uses)
);

CREATE TABLE promotion_decision (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  decision_status varchar(32) NOT NULL,
  evaluation_phase varchar(48) NOT NULL,
  evaluated_at timestamptz NOT NULL,
  context_hash varchar(128) NOT NULL,
  engine_version varchar(48) NOT NULL,
  decision_json jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  deleted_by uuid NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_promotion_decision_tenant_hash_phase UNIQUE (tenant_id, context_hash, evaluation_phase)
);

CREATE TABLE applied_promotion_snapshot (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  ticket_id uuid NOT NULL,
  promotion_decision_id uuid NOT NULL REFERENCES promotion_decision(id),
  decision_status varchar(32) NOT NULL,
  applied_at timestamptz NOT NULL,
  snapshot_json jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_at timestamptz NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  deleted_by uuid NULL,
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT uq_applied_promotion_tenant_ticket_decision UNIQUE (tenant_id, ticket_id, promotion_decision_id)
);

CREATE INDEX idx_promotion_campaign_active ON promotion_campaign (tenant_id, status, starts_at, ends_at);
CREATE INDEX idx_promotion_rule_campaign ON promotion_rule (tenant_id, campaign_id, status, evaluation_phase);
CREATE INDEX idx_promotion_decision_lookup ON promotion_decision (tenant_id, context_hash, evaluation_phase);
CREATE INDEX idx_applied_promotion_ticket ON applied_promotion_snapshot (tenant_id, ticket_id);

-- Audit tables. Align columns with Envers revision strategy if the project uses REV/REVTYPE.
CREATE TABLE promotion_campaign_aud (
  id uuid NOT NULL,
  tenant_id uuid NOT NULL,
  rev integer NOT NULL,
  revtype smallint NULL,
  code varchar(96),
  name varchar(160),
  status varchar(32),
  priority integer,
  starts_at timestamptz,
  ends_at timestamptz,
  config_version varchar(48),
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint,
  PRIMARY KEY (id, rev)
);

CREATE TABLE promotion_rule_aud (
  id uuid NOT NULL,
  tenant_id uuid NOT NULL,
  rev integer NOT NULL,
  revtype smallint NULL,
  campaign_id uuid,
  rule_key varchar(96),
  status varchar(32),
  evaluation_phase varchar(48),
  eligibility_json jsonb,
  effect_type varchar(48),
  effect_json jsonb,
  quota_key varchar(96),
  max_uses integer,
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint,
  PRIMARY KEY (id, rev)
);

CREATE TABLE promotion_quota_aud (
  id uuid NOT NULL,
  tenant_id uuid NOT NULL,
  rev integer NOT NULL,
  revtype smallint NULL,
  campaign_id uuid,
  rule_id uuid,
  quota_key varchar(128),
  max_uses integer,
  used_count integer,
  window_starts_at timestamptz,
  window_ends_at timestamptz,
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint,
  PRIMARY KEY (id, rev)
);

CREATE TABLE promotion_decision_aud (
  id uuid NOT NULL,
  tenant_id uuid NOT NULL,
  rev integer NOT NULL,
  revtype smallint NULL,
  decision_status varchar(32),
  evaluation_phase varchar(48),
  evaluated_at timestamptz,
  context_hash varchar(128),
  engine_version varchar(48),
  decision_json jsonb,
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint,
  PRIMARY KEY (id, rev)
);

CREATE TABLE applied_promotion_snapshot_aud (
  id uuid NOT NULL,
  tenant_id uuid NOT NULL,
  rev integer NOT NULL,
  revtype smallint NULL,
  ticket_id uuid,
  promotion_decision_id uuid,
  decision_status varchar(32),
  applied_at timestamptz,
  snapshot_json jsonb,
  created_at timestamptz,
  created_by uuid,
  updated_at timestamptz,
  updated_by uuid,
  deleted_at timestamptz,
  deleted_by uuid,
  version bigint,
  PRIMARY KEY (id, rev)
);

-- RLS. Adapt helper function names if your project already wraps current_setting.
ALTER TABLE promotion_campaign ENABLE ROW LEVEL SECURITY;
ALTER TABLE promotion_rule ENABLE ROW LEVEL SECURITY;
ALTER TABLE promotion_quota ENABLE ROW LEVEL SECURITY;
ALTER TABLE promotion_decision ENABLE ROW LEVEL SECURITY;
ALTER TABLE applied_promotion_snapshot ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_promotion_campaign_tenant ON promotion_campaign
USING (
  tenant_id = nullif(current_setting('app.current_tenant', true), '')::uuid
  AND (deleted_at IS NULL OR current_setting('app.deleted_visibility', true) = 'include')
)
WITH CHECK (tenant_id = nullif(current_setting('app.current_tenant', true), '')::uuid);

CREATE POLICY rls_promotion_rule_tenant ON promotion_rule
USING (
  tenant_id = nullif(current_setting('app.current_tenant', true), '')::uuid
  AND (deleted_at IS NULL OR current_setting('app.deleted_visibility', true) = 'include')
)
WITH CHECK (tenant_id = nullif(current_setting('app.current_tenant', true), '')::uuid);

CREATE POLICY rls_promotion_quota_tenant ON promotion_quota
USING (
  tenant_id = nullif(current_setting('app.current_tenant', true), '')::uuid
  AND (deleted_at IS NULL OR current_setting('app.deleted_visibility', true) = 'include')
)
WITH CHECK (tenant_id = nullif(current_setting('app.current_tenant', true), '')::uuid);

CREATE POLICY rls_promotion_decision_tenant ON promotion_decision
USING (
  tenant_id = nullif(current_setting('app.current_tenant', true), '')::uuid
  AND (deleted_at IS NULL OR current_setting('app.deleted_visibility', true) = 'include')
)
WITH CHECK (tenant_id = nullif(current_setting('app.current_tenant', true), '')::uuid);

CREATE POLICY rls_applied_promotion_snapshot_tenant ON applied_promotion_snapshot
USING (
  tenant_id = nullif(current_setting('app.current_tenant', true), '')::uuid
  AND (deleted_at IS NULL OR current_setting('app.deleted_visibility', true) = 'include')
)
WITH CHECK (tenant_id = nullif(current_setting('app.current_tenant', true), '')::uuid);
