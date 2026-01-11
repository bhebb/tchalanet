-- V28__import_tchala_fr.sql
-- Idempotent import of sample Tchala entries (fr)
-- This migration inserts canonical APPROVED entries if not exists by (lang,dedupe_key)

DO $$
DECLARE
  v_entry_id uuid;
BEGIN
  -- Row 1: 0;La mer;Rêve de grande eau
  IF NOT EXISTS (
    SELECT 1 FROM tchala_entry e
    WHERE e.lang = 'fr' AND e.dedupe_key = lower(trim('La mer')) AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL
  ) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at)
    VALUES (v_entry_id, 'fr', 'La mer', lower(trim('La mer')), '','APPROVED','IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'fr', 0) ON CONFLICT DO NOTHING;
  END IF;

  -- Row 2: 1;La montagne;Rêve d''altitude
  IF NOT EXISTS (
    SELECT 1 FROM tchala_entry e
    WHERE e.lang = 'fr' AND e.dedupe_key = lower(trim('La montagne')) AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL
  ) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at)
    VALUES (v_entry_id, 'fr', 'La montagne', lower(trim('La montagne')), '','APPROVED','IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'fr', 1) ON CONFLICT DO NOTHING;
  END IF;

  -- Row 3: 2;La pluie;Chute d''eau
  IF NOT EXISTS (
    SELECT 1 FROM tchala_entry e
    WHERE e.lang = 'fr' AND e.dedupe_key = lower(trim('La pluie')) AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL
  ) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at)
    VALUES (v_entry_id, 'fr', 'La pluie', lower(trim('La pluie')), '','APPROVED','IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'fr', 2) ON CONFLICT DO NOTHING;
  END IF;

  -- Row 4: 3;Le feu;Rêve de feu
  IF NOT EXISTS (
    SELECT 1 FROM tchala_entry e
    WHERE e.lang = 'fr' AND e.dedupe_key = lower(trim('Le feu')) AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL
  ) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at)
    VALUES (v_entry_id, 'fr', 'Le feu', lower(trim('Le feu')), '','APPROVED','IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'fr', 3) ON CONFLICT DO NOTHING;
  END IF;

  -- Row 5: 4;L'oiseau;Oiseau qui chante
  IF NOT EXISTS (
    SELECT 1 FROM tchala_entry e
    WHERE e.lang = 'fr' AND e.dedupe_key = lower(trim('L''oiseau')) AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL
  ) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at)
    VALUES (v_entry_id, 'fr', 'L''oiseau', lower(trim('L''oiseau')), '','APPROVED','IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'fr', 4) ON CONFLICT DO NOTHING;
  END IF;

END$$;

-- Note: gen_random_uuid() requires the pgcrypto extension; if not available, replace with uuid_generate_v4().

