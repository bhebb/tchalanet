CREATE TABLE IF NOT EXISTS plans (
                                     id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code              VARCHAR(64) NOT NULL UNIQUE,      -- ex: BASIC, PRO, ENTERPRISE
    name              VARCHAR(128) NOT NULL,            -- optionnel (affichage back-office)
    description       TEXT,
    price_amount      NUMERIC(12,2) NOT NULL DEFAULT 0, -- 9999999999.99
    currency          CHAR(3)     NOT NULL DEFAULT 'EUR',
    billing_frequency VARCHAR(16) NOT NULL,             -- 'MONTH' | 'YEAR'
    public_plan       BOOLEAN     NOT NULL DEFAULT FALSE,
    features          JSONB,                             -- ex: ["plans.feat.analytics", ...]
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- Contrainte logique “enum” soft
ALTER TABLE plans
    ADD CONSTRAINT chk_plans_billing_frequency
        CHECK (billing_frequency IN ('MONTH','YEAR'));

CREATE INDEX IF NOT EXISTS idx_plans_public ON plans(public_plan);
