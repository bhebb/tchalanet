-- V10: core audit (audit_event)
CREATE TABLE IF NOT EXISTS audit_event (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id uuid,

    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    deleted_at timestamptz,

    occurred_at timestamptz NOT NULL DEFAULT now(),

    actor_type varchar(32) NOT NULL,
    actor_id uuid,

    entity_type varchar(128) NOT NULL,
    entity_id uuid,

    action varchar(64) NOT NULL,
    details jsonb NOT NULL DEFAULT '{}'::jsonb,

    ip inet,
    user_agent text
    );

CREATE INDEX IF NOT EXISTS ix_audit_event_tenant ON audit_event(tenant_id);
CREATE INDEX IF NOT EXISTS ix_audit_event_entity ON audit_event(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS ix_audit_event_occurred_at ON audit_event(occurred_at);
CREATE INDEX IF NOT EXISTS ix_audit_event_created_at ON audit_event(created_at);
