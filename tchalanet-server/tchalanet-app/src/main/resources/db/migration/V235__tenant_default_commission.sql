-- ─── Tenant default commission rate ────────────────────────────────────────
-- Stores the tenant-proposed default commission rate for seller_terminals.
-- NULL means no tenant default is set (seller terminals keep their own rate).
-- Per-seller overrides remain on seller_terminal.commission_rate.

ALTER TABLE tenant
    ADD COLUMN IF NOT EXISTS default_commission_rate numeric(5, 2);

COMMENT ON COLUMN tenant.default_commission_rate
    IS 'Default commission rate (%) proposed by the tenant for all seller_terminals. NULL = no default set.';
