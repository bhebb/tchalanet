-- Allow pre-auth/global security flows to persist functional audit events.
--
-- The baseline audit_event RLS permits tenant-scoped writes or platform/super-admin global writes.
-- Portal handoff consumption is intentionally anonymous and has no tenant context yet, but still
-- needs to log replay/consume/expiry security events. Keep this narrowly scoped to global SYSTEM
-- events emitted through the server audit API.

CREATE POLICY audit_event_global_system_insert
  ON audit_event
  FOR INSERT
  WITH CHECK (
    tenant_id IS NULL
    AND actor_type = 'SYSTEM'
    AND actor_id IS NULL
    AND created_by IS NULL
    AND entity_type = 'SYSTEM'
    AND action = 'OTHER'
  );
