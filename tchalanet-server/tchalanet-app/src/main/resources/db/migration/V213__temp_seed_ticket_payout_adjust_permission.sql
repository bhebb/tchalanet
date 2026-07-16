-- Temporary additive seed for ticket paid payout adjustment.
-- Keeps already-applied V202 stable while allowing local/E2E Flyway databases
-- to pick up the new permission without repair.

INSERT INTO permission (code, name, category, system, active)
VALUES ('ticket.payout.adjust', 'Adjust ticket paid payout amount', 'ticket', true, true)
ON CONFLICT (code) DO UPDATE SET
  name = EXCLUDED.name,
  category = EXCLUDED.category,
  system = EXCLUDED.system,
  active = EXCLUDED.active,
  updated_at = now();

INSERT INTO role_permission (role_id, permission_code)
SELECT role_id, 'ticket.payout.adjust'
FROM (VALUES
  ('00000000-0000-0000-0000-000000000301'::uuid),
  ('00000000-0000-0000-0000-000000000305'::uuid),
  ('00000000-0000-0000-0000-000000000302'::uuid)
) AS roles(role_id)
ON CONFLICT DO NOTHING;
