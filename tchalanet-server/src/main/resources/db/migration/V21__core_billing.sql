-- V21: Billing split - catalog/plan + core/subscription
-- catalog/plan: global billing plans (reference data, cacheable, no lifecycle)
-- core/subscription: tenant-scoped subscription lifecycle (RLS, events, versioning)

-- =============================================================================
-- Part 1: catalog/plan (billing_plan) - GLOBAL REFERENCE DATA
-- =============================================================================

CREATE TABLE IF NOT EXISTS billing_plan (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(128) NOT NULL UNIQUE,  -- functional key (ex: 'free', 'pro-v1', 'enterprise')
  name varchar(255) NOT NULL,
  description text,

  -- Pricing (simple fields for MVP; can evolve to price_json later)
  price_amount numeric(19,2) NOT NULL DEFAULT 0,
  currency varchar(3) NOT NULL DEFAULT 'USD',
  billing_period varchar(50) NOT NULL DEFAULT 'MONTHLY', -- MONTHLY|YEARLY|QUARTERLY

  -- Limits & features (declarative JSONB)
  limits_json jsonb DEFAULT '{}'::jsonb,
  features_json jsonb DEFAULT '{}'::jsonb,

  -- Flags
  active boolean NOT NULL DEFAULT true,
  is_default boolean NOT NULL DEFAULT false,  -- platform default plan

  -- Audit columns (BaseEntity pattern)
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0
);

-- Indexes for catalog/plan
CREATE INDEX IF NOT EXISTS idx_billing_plan_code ON billing_plan(code) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_billing_plan_active ON billing_plan(active) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_billing_plan_default ON billing_plan(is_default) WHERE deleted_at IS NULL AND active = true;

-- =============================================================================
-- Part 2: core/subscription (tenant_subscription) - TENANT-SCOPED LIFECYCLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS tenant_subscription (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

  -- Tenant scoped (RLS key)
  tenant_id uuid NOT NULL REFERENCES tenant(id),

  -- Plan reference (functional code, no FK by design for decoupling)
  plan_code varchar(128) NOT NULL,  -- references billing_plan.code (soft)

  -- Lifecycle status
  status varchar(50) NOT NULL DEFAULT 'ACTIVE', -- TRIAL|ACTIVE|SUSPENDED|CANCELED|EXPIRED

  -- Dates
  started_at timestamptz,
  ends_at timestamptz,
  trial_ends_at timestamptz,
  canceled_at timestamptz,

  -- Metadata (JSONB for extensibility)
  metadata_json jsonb DEFAULT '{}'::jsonb,

  -- Audit columns (AuditableEntity pattern)
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_by uuid,
  deleted_at timestamptz,
  version bigint NOT NULL DEFAULT 0
);

-- Indexes for core/subscription
CREATE UNIQUE INDEX IF NOT EXISTS idx_tenant_subscription_tenant
  ON tenant_subscription(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tenant_subscription_status
  ON tenant_subscription(status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tenant_subscription_plan_code
  ON tenant_subscription(plan_code) WHERE deleted_at IS NULL;

-- =============================================================================
-- Part 3: Triggers (updated_at)
-- =============================================================================

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_billing_plan_updated_at') THEN
    CREATE TRIGGER trg_billing_plan_updated_at
      BEFORE UPDATE ON billing_plan
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_tenant_subscription_updated_at') THEN
    CREATE TRIGGER trg_tenant_subscription_updated_at
      BEFORE UPDATE ON tenant_subscription
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

-- =============================================================================
-- Part 4: RLS (Row-Level Security) for tenant_subscription
-- =============================================================================
-- Note: RLS policies will be created in a separate migration once tenant context
-- management is fully implemented (current_setting('app.current_tenant'))

-- =============================================================================
-- Notes:
-- - billing_plan.code is UNIQUE (functional key for external reference)
-- - tenant_subscription.plan_code is a soft reference (no FK for decoupling)
-- - This allows plans to be soft-deleted without breaking existing subscriptions
-- - Subscription lifecycle validates plan existence via catalog/plan API at runtime
-- =============================================================================


