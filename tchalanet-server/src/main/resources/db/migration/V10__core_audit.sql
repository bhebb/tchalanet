-- V10: core audit (audit_event)
CREATE TABLE IF NOT EXISTS audit_event (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid,
  entity_type varchar(128) NOT NULL,
  entity_id uuid,
  action varchar(64) NOT NULL,
  performed_by uuid,
  performed_at timestamptz NOT NULL DEFAULT now(),
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS ix_audit_event_tenant ON audit_event(tenant_id);
CREATE INDEX IF NOT EXISTS ix_audit_event_entity ON audit_event(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS ix_audit_event_performed_at ON audit_event(performed_at);

