-- V22: recreate outlet table with full schema (drop if exists)

DROP TABLE IF EXISTS outlet CASCADE;

-- =====================================================================
-- OUTLET (Point de vente / PDV)
-- =====================================================================

CREATE TABLE outlet (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version    bigint NOT NULL DEFAULT 0,

  tenant_id  uuid NOT NULL REFERENCES tenant(id),

  name       text   NOT NULL,
  slug       citext NOT NULL,

  -- --- Outlet state (ops)
  day_closed     boolean NOT NULL DEFAULT false,

  -- --- Sales guard
  sales_blocked      boolean NOT NULL DEFAULT false,
  sales_block_reason text,
  sales_blocked_at   timestamptz,

  -- --- Business context
  timezone           varchar(64) NOT NULL DEFAULT 'America/Port-au-Prince',
  business_day_cutoff time,

  -- --- Receipt / POS options
  receipt_printing_enabled boolean NOT NULL DEFAULT true,
  receipt_header_message   text,
  receipt_footer_message   text,
  require_opening_float    boolean NOT NULL DEFAULT true,

  -- --- Standard audit fields (BaseTenantEntity/BaseEntity style)
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz
);

-- Unicité du slug par tenant (soft-delete friendly via partial unique index)
CREATE UNIQUE INDEX IF NOT EXISTS ux_outlet_tenant_slug_active
  ON outlet(tenant_id, slug)
  WHERE deleted_at IS NULL;

-- Index utiles
CREATE INDEX IF NOT EXISTS ix_outlet_tenant_active
  ON outlet(tenant_id)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_outlet_tenant_sales_blocked
  ON outlet(tenant_id, sales_blocked)
  WHERE deleted_at IS NULL;

-- Trigger updated_at (si tu as la fonction utilitaire)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_outlet_set_updated_at') THEN
    CREATE TRIGGER trg_outlet_set_updated_at
    BEFORE UPDATE ON outlet
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

-- (Optionnel) Check de cohérence : si sales_blocked=false alors reason/at peuvent rester null
ALTER TABLE outlet
  ADD CONSTRAINT IF NOT EXISTS chk_outlet_sales_block_consistency
  CHECK (
    (sales_blocked = false)
    OR
    (sales_blocked = true AND sales_blocked_at IS NOT NULL)
  );

