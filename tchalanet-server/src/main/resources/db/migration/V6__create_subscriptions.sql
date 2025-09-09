CREATE TABLE IF NOT EXISTS subscriptions (
                                             id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              VARCHAR(128) NOT NULL,  -- ton identifiant tenant (string)
    plan_id                UUID NOT NULL REFERENCES plans(id),
    status                 VARCHAR(16) NOT NULL,   -- 'ACTIVE','TRIALING','CANCELED','PAST_DUE','SUSPENDED'
    current_period_start   TIMESTAMPTZ,
    current_period_end     TIMESTAMPTZ,
    cancel_at_period_end   BOOLEAN NOT NULL DEFAULT FALSE,
    billing_provider       VARCHAR(16),           -- 'STRIPE','ADYEN','NONE'
    billing_external_id    VARCHAR(128),
    meta                   JSONB,
    version                BIGINT NOT NULL DEFAULT 0, -- pour optimistic locking si tu le veux côté SQL
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- Un abonnement “courant” max par tenant (ACTIVE/TRIALING non annulé fin de période)
CREATE UNIQUE INDEX IF NOT EXISTS ux_sub_active_per_tenant
    ON subscriptions(tenant_id)
    WHERE status IN ('ACTIVE','TRIALING') AND cancel_at_period_end = FALSE;

-- Aide perfs
CREATE INDEX IF NOT EXISTS idx_sub_tenant ON subscriptions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sub_status ON subscriptions(status);

-- Checks “enum” soft
ALTER TABLE subscriptions
    ADD CONSTRAINT chk_sub_status
        CHECK (status IN ('ACTIVE','TRIALING','CANCELED','PAST_DUE','SUSPENDED'));

ALTER TABLE subscriptions
    ADD CONSTRAINT chk_billing_provider
        CHECK (billing_provider IS NULL OR billing_provider IN ('STRIPE','ADYEN','NONE'));
