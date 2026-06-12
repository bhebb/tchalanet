-- V221: Give the seeded cashier a real human name for receipts.
--
-- The receipt seller line resolves from sales_ticket_print_header_v which reads
-- COALESCE(app_user.display_name, app_user.email). The seed cashier had
-- display_name='Cashier' (a role-like placeholder) and NULL first/last names, so
-- receipts printed "Cashier" instead of a person. In real onboarding these come
-- from Keycloak (given_name/family_name) via identity sync; this seeds sensible
-- dev values consistent with CurrentUserProfileService (displayName = first+last).
--
-- Guarded on the seed cashier; idempotent (only overwrites the placeholder).

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'platform', true);
SELECT set_config('app.is_super_admin', 'true', true);

UPDATE app_user
SET first_name   = 'Marie',
    last_name    = 'Joseph',
    display_name = 'Marie Joseph',
    updated_at   = now()
WHERE id = '00000000-0000-0000-0000-000000010003'::uuid
  AND (display_name IS NULL OR display_name = 'Cashier')
  AND deleted_at IS NULL;

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
