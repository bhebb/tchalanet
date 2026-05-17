-- V40: seed tenant (tchalanet) - moved from monolithic seed
DO $$ BEGIN
  RAISE NOTICE 'V200__seed_tenant: seeding tenant tchalanet';
END $$;

INSERT INTO tenant (id, code, name, timezone, currency, status, type)
SELECT
    '00000000-0000-0000-0000-000000000003'::uuid,
    'tchalanet',
    'Tchalanet',
    'America/Toronto',
    'USD',
    'ACTIVE',
    'BORLETTE'
WHERE NOT EXISTS (SELECT 1 FROM tenant WHERE code = 'tchalanet');

-- Sanity check
DO $$
DECLARE tcount int;
BEGIN
  SELECT count(*) INTO tcount FROM tenant WHERE code = 'tchalanet';
  IF tcount = 0 THEN
    RAISE EXCEPTION 'V40__seed_tenant sanity check failed: tenant tchalanet not present';
  ELSE
    RAISE NOTICE 'V40__seed_tenant sanity check OK: tenant tchalanet present';
  END IF;
END $$;

