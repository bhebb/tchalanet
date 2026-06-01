-- V216: Two repairs run per-tenant (RLS requires current_tenant to be set):
--
-- A) terminal_binding — set public_key_algorithm = 'ED25519' where binding_public_key
--    is a non-null placeholder but public_key_algorithm was omitted (V205/V211 seeds).
--    Fixes: IllegalArgumentException "publicKeyAlgorithm is required when bindingPublicKey is provided".
--
-- B) draw — clear opened_at / closed_at on SCHEDULED draws with stale timestamps.
--    Caused by GenerateDrawsForRangeCommandHandler always writing now() into
--    opened_at/closed_at regardless of status. Fixes: open-today finding 0 openable draws.

DO $$
DECLARE
  t_id uuid;
  total_bindings  int := 0;
  total_draws     int := 0;
  rows_affected   int;
BEGIN
  FOR t_id IN SELECT id FROM tenant WHERE deleted_at IS NULL LOOP

    PERFORM set_config('app.current_tenant',       t_id::text, true);
    PERFORM set_config('app.deleted_visibility',  'active',    true);
    PERFORM set_config('app.api_scope',            'tenant',   true);
    PERFORM set_config('app.is_super_admin',       'false',    true);

    -- A) terminal_binding
    UPDATE terminal_binding
    SET    public_key_algorithm = 'ED25519',
           updated_at           = now()
    WHERE  tenant_id       = t_id
      AND  binding_public_key IS NOT NULL
      AND  public_key_algorithm IS NULL;
    GET DIAGNOSTICS rows_affected = ROW_COUNT;
    total_bindings := total_bindings + rows_affected;

    -- B) draw
    UPDATE draw
    SET    opened_at  = NULL,
           closed_at  = NULL,
           updated_at = now()
    WHERE  tenant_id = t_id
      AND  status    = 'SCHEDULED'
      AND  (opened_at IS NOT NULL OR closed_at IS NOT NULL);
    GET DIAGNOSTICS rows_affected = ROW_COUNT;
    total_draws := total_draws + rows_affected;

  END LOOP;

  RAISE NOTICE 'V216: repaired terminal_bindings=% draws=%', total_bindings, total_draws;
END $$;

SELECT set_config('app.current_tenant',      '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope',           '', true);
SELECT set_config('app.is_super_admin',      'false', true);
