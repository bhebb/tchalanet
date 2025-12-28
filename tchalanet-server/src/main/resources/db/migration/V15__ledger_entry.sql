CREATE TABLE ledger_entry
(
    -- Identité
    id          uuid PRIMARY KEY        DEFAULT gen_random_uuid(),
    tenant_id   uuid           NOT NULL,

    -- Référence métier
    ref_type    varchar(64)    NOT NULL,
    ref_id      uuid           NOT NULL,

    -- Mouvement
    amount      numeric(18, 2) NOT NULL CHECK (amount > 0),
    direction   varchar(8)     NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),

    -- Temps métier
    occurred_at timestamptz    NOT NULL DEFAULT now(),

    -- Auditing standard
    created_at  timestamptz    NOT NULL DEFAULT now(),
    created_by  uuid,
    updated_at  timestamptz,
    updated_by  uuid,
    deleted_at  timestamptz,

    -- Optimistic locking (technique, rarement utilisé ici)
    version     bigint         NOT NULL DEFAULT 0
);

-- Index essentiels
CREATE INDEX idx_ledger_entry_tenant
    ON ledger_entry (tenant_id);

CREATE INDEX idx_ledger_entry_ref
    ON ledger_entry (ref_type, ref_id);

CREATE INDEX idx_ledger_entry_occurred_at
    ON ledger_entry (occurred_at);
