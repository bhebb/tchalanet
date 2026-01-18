-- V26: core billing (plan, subscription) - adjusted to match entity column names used by the application
CREATE TABLE IF NOT EXISTS plan (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  billing_frequency varchar(16) NOT NULL DEFAULT 'MONTHLY', -- MONTHLY|YEARLY
  code varchar(64) NOT NULL UNIQUE,
  name varchar(128) NOT NULL,
  description text,
  features jsonb NOT NULL DEFAULT '{}'::jsonb,
  price_amount numeric(12,2) NOT NULL DEFAULT 0,
  currency varchar(3) NOT NULL DEFAULT 'USD',
  public_plan boolean NOT NULL DEFAULT false,
  version bigint NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz
);

CREATE TABLE IF NOT EXISTS subscription (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  plan_id uuid NOT NULL REFERENCES plan(id),
  status varchar(32) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE|SUSPENDED|CANCELED|EXPIRED
  current_period_start timestamptz NOT NULL DEFAULT now(),
  current_period_end timestamptz,
  cancel_at_period_end boolean NOT NULL DEFAULT false,
  billing_provider varchar(16),
  billing_external_id varchar(128),
  meta jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_plan_updated_at') THEN
    CREATE TRIGGER trg_plan_updated_at BEFORE UPDATE ON plan FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_subscription_updated_at') THEN
    CREATE TRIGGER trg_subscription_updated_at BEFORE UPDATE ON subscription FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;
