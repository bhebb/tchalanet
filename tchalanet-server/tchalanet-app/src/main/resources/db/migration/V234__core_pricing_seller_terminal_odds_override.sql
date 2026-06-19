-- core.pricing: per-seller-terminal odds overrides
-- Resolution: seller_terminal override (active) → tenant default (catalog.pricing) → error

CREATE TABLE seller_terminal_pricing_odds_override
(
    id                 uuid          NOT NULL DEFAULT gen_random_uuid(),

    tenant_id          uuid          NOT NULL,
    seller_terminal_id uuid          NOT NULL,

    game_code          varchar(32)   NOT NULL,
    bet_type           varchar(32)   NOT NULL,
    bet_option         smallint      NULL,

    odds               numeric(12,4) NOT NULL,

    active             boolean       NOT NULL DEFAULT true,

    effective_from     timestamptz   NULL,
    effective_to       timestamptz   NULL,

    reason             varchar(500)  NULL,

    created_at         timestamptz   NOT NULL DEFAULT now(),
    created_by         uuid          NULL,
    updated_at         timestamptz   NULL,
    updated_by         uuid          NULL,
    deleted_at         timestamptz   NULL,
    deleted_by         uuid          NULL,
    version            bigint        NOT NULL DEFAULT 0,

    CONSTRAINT pk_st_pricing_odds_override
        PRIMARY KEY (id),

    CONSTRAINT ck_st_odds_positive
        CHECK (odds > 0),

    CONSTRAINT fk_st_pricing_override_seller_terminal
        FOREIGN KEY (seller_terminal_id)
        REFERENCES seller_terminal (id)
);

CREATE INDEX idx_st_pricing_odds_tenant
    ON seller_terminal_pricing_odds_override (tenant_id);

CREATE INDEX idx_st_pricing_odds_seller_terminal
    ON seller_terminal_pricing_odds_override (seller_terminal_id);

-- Unique active override per (tenant, seller_terminal, game, betType, betOption)
CREATE UNIQUE INDEX uq_st_pricing_odds_override_active
    ON seller_terminal_pricing_odds_override (
        tenant_id,
        seller_terminal_id,
        game_code,
        bet_type,
        COALESCE(bet_option, -1)
    )
    WHERE deleted_at IS NULL
      AND active = true;

-- limitpolicy: add SELLER_TERMINAL to scope/target enums
-- (existing scope_type and target_type columns in limit_assignment use varchar — no migration needed)

CREATE TABLE seller_terminal_pricing_odds_override_aud
(
    id                 uuid    NOT NULL,
    rev                integer NOT NULL,
    revtype            smallint,
    tenant_id          uuid,
    seller_terminal_id uuid,
    game_code          varchar(32),
    bet_type           varchar(32),
    bet_option         smallint,
    odds               numeric(12,4),
    active             boolean,
    effective_from     timestamptz,
    effective_to       timestamptz,
    reason             varchar(500),
    created_at         timestamptz,
    created_by         uuid,
    updated_at         timestamptz,
    updated_by         uuid,
    deleted_at         timestamptz,
    deleted_by         uuid,
    version            bigint,
    CONSTRAINT pk_st_pricing_odds_override_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_st_pricing_odds_override_aud__revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

ALTER TABLE seller_terminal_pricing_odds_override ENABLE ROW LEVEL SECURITY;
ALTER TABLE seller_terminal_pricing_odds_override FORCE ROW LEVEL SECURITY;
CREATE POLICY seller_terminal_pricing_odds_override_rls_all ON seller_terminal_pricing_odds_override
  FOR ALL
  USING (
    public.current_tenant() IS NOT NULL
    AND tenant_id = public.current_tenant()
    AND (public.deleted_visibility() = 'all'
      OR (public.deleted_visibility() = 'active' AND deleted_at IS NULL)
      OR (public.deleted_visibility() = 'deleted' AND deleted_at IS NOT NULL))
  )
  WITH CHECK (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant());
CREATE POLICY seller_terminal_pricing_odds_override_rls_select ON seller_terminal_pricing_odds_override
  FOR SELECT
  USING (public.allow_platform_cross_tenant_select() OR (public.current_tenant() IS NOT NULL AND tenant_id = public.current_tenant()));
