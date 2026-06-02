-- V217: Repair the seeded POS terminal_binding credential_hash so the known dev
-- credential 'e2e-cred-dev' actually verifies against the binding.
--
-- Why: V205 inserted terminal_binding 00000000-0000-0000-0000-000000003121 with a
-- hardcoded placeholder credential_hash ('4d0e4221...') that does NOT correspond to
-- 'e2e-cred-dev'. V211 later re-inserted the same id with the correctly computed hash
-- but used ON CONFLICT DO NOTHING, so the bad placeholder was never overwritten. The
-- result: ResolveOperationalContextQueryHandler never finds a matching binding, the POS
-- operational context stays WEAK, and every sell is rejected OPERATIONAL_CONTEXT_UNTRUSTED.
--
-- The hash must equal Hashing.sha256Hex(tenantId + "|" + terminalId + "|" + credential)
-- (see TerminalBindingCredentialHasher) — reproduced here with pgcrypto digest().

DO $$
DECLARE
  t_id uuid;
BEGIN
  SELECT id INTO t_id FROM tenant WHERE code = 'tchalanet' LIMIT 1;
  IF t_id IS NULL THEN
    RAISE NOTICE 'V217: tenant tchalanet not found, skipping terminal_binding repair';
    RETURN;
  END IF;

  -- terminal_binding is tenant-scoped under RLS; set the context so the UPDATE applies.
  PERFORM set_config('app.current_tenant', t_id::text, true);
  PERFORM set_config('app.deleted_visibility', 'active', true);

  UPDATE terminal_binding
  SET credential_hash = encode(
        digest(t_id::text || '|' || '00000000-0000-0000-0000-000000003101' || '|' || 'e2e-cred-dev', 'sha256'),
        'hex'),
      updated_at = now()
  WHERE id = '00000000-0000-0000-0000-000000003121'::uuid;

  PERFORM set_config('app.current_tenant', '', true);

  RAISE NOTICE 'V217: terminal_binding credential_hash repaired for e2e-cred-dev';
END $$;
