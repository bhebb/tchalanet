-- SellerTerminal V0: operational field-seller model
-- Replaces Terminal + SalesSession + Seller as the selling actor.
-- Address: FK to shared address table (not inline).
-- External identity: normalized to seller_terminal_external_identity (same pattern as app_user_external_identity).

CREATE TABLE seller_terminal
(
    id              uuid         NOT NULL,
    tenant_id       uuid         NOT NULL,

    -- identity
    terminal_code   varchar(64)  NOT NULL,
    first_name      varchar(120),
    last_name       varchar(120),
    display_name    varchar(180) NOT NULL,
    phone_number    varchar(64),

    -- address (optional FK)
    address_id      uuid,

    -- control
    status          varchar(32)   NOT NULL DEFAULT 'PENDING',
    commission_rate numeric(5, 2) NOT NULL DEFAULT 15.00,

    -- lifecycle timestamps
    last_seen_at   timestamptz,
    activated_at   timestamptz,
    blocked_at     timestamptz,
    blocked_by     uuid,
    blocked_reason varchar(500),
    disabled_at    timestamptz,

    -- standard audit columns
    created_at timestamptz NOT NULL,
    created_by uuid,
    updated_at timestamptz NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    deleted_by uuid,
    version    bigint       NOT NULL DEFAULT 0,

    CONSTRAINT pk_seller_terminal PRIMARY KEY (id),
    CONSTRAINT uq_seller_terminal_code UNIQUE (tenant_id, terminal_code),
    CONSTRAINT fk_seller_terminal__tenant  FOREIGN KEY (tenant_id)  REFERENCES tenant (id),
    CONSTRAINT fk_seller_terminal__address FOREIGN KEY (address_id) REFERENCES address (id)
);

CREATE INDEX idx_seller_terminal_tenant_status ON seller_terminal (tenant_id, status);
CREATE INDEX idx_seller_terminal_tenant_code   ON seller_terminal (tenant_id, terminal_code);
CREATE INDEX idx_seller_terminal_tenant_name   ON seller_terminal (tenant_id, display_name);

-- ─── External identity (same pattern as app_user_external_identity) ───────────

CREATE TABLE seller_terminal_external_identity
(
    id                  uuid        NOT NULL,
    seller_terminal_id  uuid        NOT NULL,
    provider            varchar(32) NOT NULL,
    issuer              varchar(512) NOT NULL,
    external_subject    varchar(255) NOT NULL,

    created_at timestamptz NOT NULL,
    created_by uuid,
    updated_at timestamptz NOT NULL,
    updated_by uuid,
    deleted_at timestamptz,
    deleted_by uuid,
    version    bigint NOT NULL DEFAULT 0,

    CONSTRAINT pk_seller_terminal_external_identity PRIMARY KEY (id),
    CONSTRAINT uq_seller_terminal_ext_identity UNIQUE (provider, issuer, external_subject),
    CONSTRAINT chk_seller_terminal_ext_provider
        CHECK (provider IN ('FIREBASE')),
    CONSTRAINT fk_seller_terminal_ext__terminal
        FOREIGN KEY (seller_terminal_id) REFERENCES seller_terminal (id)
);

CREATE INDEX idx_seller_terminal_ext_terminal ON seller_terminal_external_identity (seller_terminal_id);

-- ─── Envers audit table (partial — control and financial fields only) ─────────

CREATE TABLE seller_terminal_aud
(
    id               uuid    NOT NULL,
    rev              integer NOT NULL,
    revtype          smallint,

    terminal_code    varchar(64),
    status           varchar(32),
    commission_rate  numeric(5, 2),
    blocked_at       timestamptz,
    blocked_by       uuid,
    blocked_reason   varchar(500),
    disabled_at      timestamptz,

    -- base audit
    created_at timestamptz,
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    deleted_at timestamptz,
    deleted_by uuid,
    version    bigint,

    CONSTRAINT pk_seller_terminal_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_seller_terminal_aud__revinfo FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ─── Ticket additions — commission snapshot ───────────────────────────────────

ALTER TABLE sales_ticket
    ADD COLUMN seller_terminal_id                uuid,
    ADD COLUMN seller_commission_rate_snapshot   numeric(5, 2),
    ADD COLUMN seller_commission_amount_snapshot numeric(12, 2);

CREATE INDEX idx_sales_ticket_seller_terminal ON sales_ticket (seller_terminal_id) WHERE seller_terminal_id IS NOT NULL;

-- Allow SellerTerminal path: outlet/terminal/user/session are null when actor is SELLER_TERMINAL
ALTER TABLE sales_ticket
    ALTER COLUMN outlet_id DROP NOT NULL,
    ALTER COLUMN terminal_id DROP NOT NULL,
    ALTER COLUMN seller_user_id DROP NOT NULL,
    ALTER COLUMN sales_session_id DROP NOT NULL;
