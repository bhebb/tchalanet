-- =============================================================
-- core.seller — sellers, outlet assignments, commission policies
-- =============================================================

CREATE TABLE seller (
  id           uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id    uuid         NOT NULL REFERENCES tenant(id),
  user_id      uuid         NULL,
  code         varchar(80)  NOT NULL,
  display_name varchar(180) NOT NULL,
  status       varchar(24)  NOT NULL,
  created_at   timestamptz  NOT NULL DEFAULT now(),
  created_by   uuid         NULL,
  updated_at   timestamptz  NOT NULL DEFAULT now(),
  updated_by   uuid         NULL,
  deleted_at   timestamptz  NULL,
  deleted_by   uuid         NULL,
  version      bigint       NOT NULL DEFAULT 0,
  CONSTRAINT uq_seller_tenant_code UNIQUE (tenant_id, code),
  CONSTRAINT ck_seller_status CHECK (status IN ('ACTIVE','SUSPENDED','INACTIVE'))
);
CREATE INDEX idx_seller_tenant_status ON seller(tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_seller_tenant_user   ON seller(tenant_id, user_id) WHERE user_id IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE seller_outlet_assignment (
  id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id   uuid        NOT NULL REFERENCES tenant(id),
  seller_id   uuid        NOT NULL REFERENCES seller(id),
  outlet_id   uuid        NOT NULL REFERENCES outlet(id),
  starts_at   timestamptz NOT NULL,
  ends_at     timestamptz NULL,
  status      varchar(24) NOT NULL,
  created_at  timestamptz NOT NULL DEFAULT now(),
  created_by  uuid        NULL,
  updated_at  timestamptz NOT NULL DEFAULT now(),
  updated_by  uuid        NULL,
  deleted_at  timestamptz NULL,
  deleted_by  uuid        NULL,
  version     bigint      NOT NULL DEFAULT 0,
  CONSTRAINT ck_seller_assignment_status CHECK (status IN ('ACTIVE','ENDED','SUSPENDED'))
);
CREATE INDEX idx_seller_assignment_seller ON seller_outlet_assignment(tenant_id, seller_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_seller_assignment_outlet ON seller_outlet_assignment(tenant_id, outlet_id) WHERE status = 'ACTIVE' AND deleted_at IS NULL;

CREATE TABLE seller_commission_policy (
  id              uuid           PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id       uuid           NOT NULL REFERENCES tenant(id),
  seller_id       uuid           NOT NULL REFERENCES seller(id),
  commission_type varchar(40)    NOT NULL,
  commission_base varchar(40)    NOT NULL,
  rate_percent    numeric(8,4)   NULL,
  fixed_amount    numeric(18,4)  NULL,
  currency        varchar(8)     NOT NULL,
  starts_at       timestamptz    NOT NULL,
  ends_at         timestamptz    NULL,
  status          varchar(24)    NOT NULL,
  created_at      timestamptz    NOT NULL DEFAULT now(),
  created_by      uuid           NULL,
  updated_at      timestamptz    NOT NULL DEFAULT now(),
  updated_by      uuid           NULL,
  deleted_at      timestamptz    NULL,
  deleted_by      uuid           NULL,
  version         bigint         NOT NULL DEFAULT 0,
  CONSTRAINT ck_seller_commission_type CHECK (commission_type IN ('NONE','PERCENT','FIXED_PER_TICKET','FIXED_PLUS_PERCENT')),
  CONSTRAINT ck_seller_commission_base CHECK (commission_base IN ('GROSS_SALES','NET_SALES','PROFIT','TICKET_COUNT')),
  CONSTRAINT ck_seller_commission_status CHECK (status IN ('ACTIVE','ENDED','SUSPENDED'))
);
CREATE INDEX idx_seller_commission_seller ON seller_commission_policy(tenant_id, seller_id) WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- =============================================================
-- RLS — seller tables
-- =============================================================

ALTER TABLE seller ENABLE ROW LEVEL SECURITY;
ALTER TABLE seller FORCE ROW LEVEL SECURITY;
CREATE POLICY seller_rls_all ON seller FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active'  AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY seller_rls_select ON seller FOR SELECT
  USING (public.allow_platform_cross_tenant_select()
    OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE seller_outlet_assignment ENABLE ROW LEVEL SECURITY;
ALTER TABLE seller_outlet_assignment FORCE ROW LEVEL SECURITY;
CREATE POLICY seller_assignment_rls_all ON seller_outlet_assignment FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active'  AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY seller_assignment_rls_select ON seller_outlet_assignment FOR SELECT
  USING (public.allow_platform_cross_tenant_select()
    OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

ALTER TABLE seller_commission_policy ENABLE ROW LEVEL SECURITY;
ALTER TABLE seller_commission_policy FORCE ROW LEVEL SECURITY;
CREATE POLICY seller_commission_rls_all ON seller_commission_policy FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active'  AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY seller_commission_rls_select ON seller_commission_policy FOR SELECT
  USING (public.allow_platform_cross_tenant_select()
    OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));

-- =============================================================
-- Extend sales_ticket with seller/assignment snapshot
-- (nullable: seller resolution is async/optional)
-- =============================================================

ALTER TABLE sales_ticket
  ADD COLUMN seller_id            uuid NULL REFERENCES seller(id),
  ADD COLUMN seller_assignment_id uuid NULL REFERENCES seller_outlet_assignment(id);

CREATE INDEX idx_sales_ticket_tenant_seller     ON sales_ticket(tenant_id, seller_id)            WHERE seller_id IS NOT NULL;
CREATE INDEX idx_sales_ticket_tenant_assignment ON sales_ticket(tenant_id, seller_assignment_id) WHERE seller_assignment_id IS NOT NULL;
