-- V22__tchala.sql
-- =========================
-- TCHALA: entries + numbers
-- =========================

-- Table of Tchala entries (dreams -> numbers)
CREATE TABLE IF NOT EXISTS tchala_entry (
  id uuid PRIMARY KEY,

  lang text NOT NULL,             -- 'fr','en','ht'
  dream text NOT NULL,
  dedupe_key text NOT NULL,

  note text NOT NULL DEFAULT '',

  status text NOT NULL,           -- PENDING, APPROVED, REJECTED, MERGED, ARCHIVED
  source text NOT NULL,           -- PUBLIC_SUGGESTION, SUPERADMIN_MANUAL, IMPORT

  conflict_with_entry_id uuid NULL REFERENCES tchala_entry(id),
  canonical_entry_id uuid NULL REFERENCES tchala_entry(id),

  -- Auditing columns (kept consistent with BaseEntity / AuditableEntity)
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid NULL,
  updated_by uuid NULL,
  deleted_at timestamptz NULL,
  version bigint NOT NULL DEFAULT 0
);

-- Ensure there is only one canonical APPROVED entry per (lang, dedupe_key)
CREATE UNIQUE INDEX IF NOT EXISTS ux_tchala_entry_canonical_key
  ON tchala_entry (lang, dedupe_key)
  WHERE status = 'APPROVED' AND canonical_entry_id IS NULL;

CREATE INDEX IF NOT EXISTS ix_tchala_entry_status ON tchala_entry(status);
CREATE INDEX IF NOT EXISTS ix_tchala_entry_lang_status ON tchala_entry(lang, status);
CREATE INDEX IF NOT EXISTS ix_tchala_entry_lang_dedupe ON tchala_entry(lang, dedupe_key);

-- Numbers table (reverse lookup performant)
CREATE TABLE IF NOT EXISTS tchala_entry_number (
  entry_id uuid NOT NULL REFERENCES tchala_entry(id) ON DELETE CASCADE,
  lang text NOT NULL,
  number smallint NOT NULL CHECK (number >= 0 AND number <= 99),
  PRIMARY KEY (entry_id, number)
);

CREATE INDEX IF NOT EXISTS ix_tchala_number_lang_number
  ON tchala_entry_number (lang, number);

-- Optional: trigger to maintain updated_at (if you already have a base entity listener, ignore)
-- Example trigger (uncomment if you prefer trigger-based updated_at):
--
-- CREATE OR REPLACE FUNCTION tchala_entry_set_updated_at()
-- RETURNS trigger AS $$
-- BEGIN
--   NEW.updated_at = now();
--   RETURN NEW;
-- END;
-- $$ LANGUAGE plpgsql;
--
-- DROP TRIGGER IF EXISTS trg_tchala_entry_updated_at ON tchala_entry;
-- CREATE TRIGGER trg_tchala_entry_updated_at
-- BEFORE UPDATE ON tchala_entry
-- FOR EACH ROW EXECUTE FUNCTION tchala_entry_set_updated_at();

-- End of migration V27

