-- autonomy_policy_rule_rule: policy unique par target (TENANT/OUTLET/TERMINAL/AGENT)
CREATE TABLE IF NOT EXISTS autonomy_policy_rule_rule (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version bigint NOT NULL DEFAULT 0,

  tenant_id uuid NOT NULL,

  target_type varchar(20) NOT NULL, -- TENANT | OUTLET | TERMINAL | AGENT
  target_id uuid NOT NULL,

  level varchar(10) NOT NULL,       -- NONE | PARTIAL | FULL
  require_approval_on_block boolean NOT NULL DEFAULT true,
  approval_role varchar(10),        -- OPERATOR | ADMIN (nullable)

  enabled boolean NOT NULL DEFAULT true,
  starts_at timestamptz,
  ends_at timestamptz,

  created_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid,
  deleted_at timestamptz
);

-- 1 policy active par target
CREATE UNIQUE INDEX IF NOT EXISTS ux_autonomy_policy_rule_rule_target
  ON autonomy_policy_rule_rule (tenant_id, target_type, target_id)
  WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_autonomy_policy_rule_rule_lookup
  ON autonomy_policy_rule_rule (tenant_id, target_type, target_id, enabled);

-- trigger updated_at si tu as déjà set_updated_at()
-- sinon tu peux ignorer ce bloc
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
    DROP TRIGGER IF EXISTS trg_autonomy_policy_rule_rule_updated_at ON autonomy_policy_rule_rule;
    CREATE TRIGGER trg_autonomy_policy_rule_rule_updated_at
      BEFORE UPDATE ON autonomy_policy_rule_rule
      FOR EACH ROW
      EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;
