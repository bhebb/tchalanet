-- V2: create ledger_entry table (append-only journal)
CREATE TABLE IF NOT EXISTS ledger_entry (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL,
  ref_type varchar(64) NOT NULL,
  ref_id uuid NOT NULL,
  amount numeric(18,2) NOT NULL,
  direction varchar(8) NOT NULL,
  occurred_at timestamptz NOT NULL DEFAULT now(),
  version bigint DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ledger_entry_tenant ON ledger_entry(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ledger_entry_ref ON ledger_entry(ref_type, ref_id);

