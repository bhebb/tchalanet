-- V222__import_tchala_full.sql
-- Généré par scripts/generate_tchala_migration.py
-- Importe toutes les entrées APPROVED depuis tchala_lotto_ht_init.csv
-- Idempotent : ne ré-insère pas si (lang, dedupe_key, APPROVED, canonical_entry_id IS NULL) existe déjà

DO $$
DECLARE
  v_entry_id uuid;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'achte' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Achte', 'achte', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 55) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 76) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 36) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'adilte' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Adiltè', 'adilte', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 29) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 58) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 69) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 28) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'akouchman' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Akouchman', 'akouchman', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 56) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 33) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'aksidan' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Aksidan', 'aksidan', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 96) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 6) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 5) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 49) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'alimet' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Alimèt', 'alimet', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 8) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 42) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'altagras' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Altagras', 'altagras', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 12) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'anana' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Anana', 'anana', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 73) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 19) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'anbilans' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Anbilans', 'anbilans', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 30) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 37) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 42) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'ansent' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Ansent', 'ansent', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 20) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'anteman' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Antèman', 'anteman', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 9) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 91) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 10) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 66) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'arete' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Arete', 'arete', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 36) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 13) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 24) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 25) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'atelye' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Atelye', 'atelye', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 47) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 45) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 73) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'avyon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Avyon', 'avyon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 3) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 29) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 47) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 85) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bag_maryaj' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bag Maryaj', 'bag_maryaj', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 25) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 59) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bale' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bale', 'bale', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 55) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 3) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 14) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'balen' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Balèn', 'balen', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 65) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 59) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bank' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bank', 'bank', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 61) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 15) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 84) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 14) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bannann' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bannann', 'bannann', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 87) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 48) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'batay' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Batay', 'batay', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 78) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 19) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bato' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bato', 'bato', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 18) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 68) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'baton' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Baton', 'baton', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 89) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 40) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 88) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'baton' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Baton', 'baton', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 43) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 61) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'batem' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Batèm', 'batem', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 88) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 69) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 58) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bay_tete' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bay Tete', 'bay_tete', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 19) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'baza' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Baza', 'baza', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 39) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 62) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bekan' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bekàn', 'bekan', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 52) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 78) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'benyen' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Benyen', 'benyen', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 25) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 59) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bib' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bib', 'bib', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 20) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bibwon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bibwon', 'bibwon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 82) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 94) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bijou' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bijou', 'bijou', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 53) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 52) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'ble' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Ble', 'ble', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 1) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 79) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 9) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 83) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'blese' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Blese', 'blese', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 9) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 3) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 42) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 62) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bo' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bo', 'bo', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 23) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 14) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 5) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bokit' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bokit', 'bokit', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 49) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'boul' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Boul', 'boul', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 14) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 82) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 46) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 26) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'boulanje' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Boulanje', 'boulanje', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 3) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 60) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bourik' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bourik', 'bourik', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 53) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 35) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 91) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 14) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bouret' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bourèt', 'bouret', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bous' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bous', 'bous', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 2) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 60) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 10) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 69) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bouzen' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bouzen', 'bouzen', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 66) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bwat' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bwat', 'bwat', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 81) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 93) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'bef' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bèf', 'bef', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 16) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 76) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 96) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'beso' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Bèso', 'beso', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 1) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 36) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 62) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 72) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'chabon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Chabon', 'chabon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 85) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 59) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 30) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'chantye' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Chantye', 'chantye', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 85) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'chaple' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Chaplè', 'chaple', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'chapo' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Chapo', 'chapo', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 20) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 28) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 71) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'chase' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Chasè', 'chase', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 9) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 59) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 99) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'chat' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Chat', 'chat', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 74) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 14) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 84) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'chef' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Chef', 'chef', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'chen' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Chen', 'chen', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 15) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 75) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 95) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'chen' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Chèn', 'chen', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 42) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 73) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'chez' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Chèz', 'chez', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 63) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 16) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 73) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'dan' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Dan', 'dan', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 31) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 58) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 15) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'danse' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Danse', 'danse', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 79) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 8) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 86) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 39) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'dlo' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Dlo', 'dlo', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 89) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 5) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 51) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'drapo' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Drapo', 'drapo', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 77) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 73) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 14) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 24) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'echel' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Echèl', 'echel', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 65) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 3) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 25) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 62) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'egliz' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Egliz', 'egliz', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 95) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 75) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 18) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'ekri' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Ekri', 'ekri', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'eleksyon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Eleksyon', 'eleksyon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 68) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'enjistis' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Enjistis', 'enjistis', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 82) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 61) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'enprimri' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Enprimri', 'enprimri', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 73) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 30) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'ensidan' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Ensidan', 'ensidan', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 27) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 61) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 83) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'envazyon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Envazyon', 'envazyon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 1) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 51) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 20) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'epeng' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Epeng', 'epeng', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 73) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 6) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'eskalye' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Eskalye', 'eskalye', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 6) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 36) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'estati' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Estati', 'estati', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 19) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 55) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 66) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'etidye' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Etidye', 'etidye', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 57) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 73) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'etwal' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Etwal', 'etwal', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 82) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 12) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'fanm' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Fanm', 'fanm', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 12) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 5) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 50) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 42) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'fatra' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Fatra', 'fatra', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 85) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 86) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'fe_pou_pase_rad' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Fè Pou Pase Rad', 'fe_pou_pase_rad', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 44) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 16) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'gason' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Gason', 'gason', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 19) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 2) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 91) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'gato' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Gato', 'gato', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 52) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 48) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 50) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'genyen' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Genyen', 'genyen', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 82) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 54) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 26) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'gita' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Gita', 'gita', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 83) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 31) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 75) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 89) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'glas' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Glas', 'glas', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 29) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 84) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'glas' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Glas', 'glas', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 92) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'ge' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Gè', 'ge', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 95) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 36) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'imaj' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Imaj', 'imaj', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 66) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 79) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 57) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'imakile' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Imakile', 'imakile', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 50) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 12) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 8) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'inondasyon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Inondasyon', 'inondasyon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 85) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'izin' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Izin', 'izin', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 19) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 27) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'ja' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Ja', 'ja', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 31) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 86) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'jaden' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Jaden', 'jaden', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 1) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'jalouzi' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Jalouzi', 'jalouzi', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 5) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 80) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'jandam' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Jandam', 'jandam', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 25) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 50) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 3) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 49) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'jape' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Jape', 'jape', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 31) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'jij' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Jij', 'jij', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 42) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 89) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 51) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'jipon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Jipon', 'jipon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 80) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 95) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'jounal' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Jounal', 'jounal', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 96) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 62) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'jwet' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Jwèt', 'jwet', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 14) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 52) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 62) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kabann' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kabann', 'kabann', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 57) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 59) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 46) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kabrit' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kabrit', 'kabrit', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 28) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 82) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kadna' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kadna', 'kadna', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 97) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 61) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 5) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kado' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kado', 'kado', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 30) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 5) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kafe' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kafe', 'kafe', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 68) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 27) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 6) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kaka' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kaka', 'kaka', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kana' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kana', 'kana', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 42) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kanaval' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kanaval', 'kanaval', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 66) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 67) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kap' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kap', 'kap', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 3) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'katedral' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Katedral', 'katedral', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 64) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 16) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 8) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kaye' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kaye', 'kaye', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kayiman' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kayiman', 'kayiman', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 29) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 90) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 60) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kazen' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kazèn', 'kazen', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 0) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 56) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 31) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kivet' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kivèt', 'kivet', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 30) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 14) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 64) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kizin' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kizin', 'kizin', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 24) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kle' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kle', 'kle', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 5) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 64) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kleren' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kleren', 'kleren', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 49) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 14) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 40) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 36) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'klou' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Klou', 'klou', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 67) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 27) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kloch' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Klòch', 'kloch', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 3) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 27) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 48) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 25) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kochon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kochon', 'kochon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 58) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kodak' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kodak', 'kodak', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 86) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kola' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kola', 'kola', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 16) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 85) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kondui' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kondui', 'kondui', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 3) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 62) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'konstriksyon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Konstriksyon', 'konstriksyon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 19) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kostim' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kostim', 'kostim', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 44) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 23) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 84) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kou' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kou', 'kou', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 8) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 60) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'koulin' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Koulin', 'koulin', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 62) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 14) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 96) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'koulev' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Koulèv', 'koulev', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 39) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 72) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kouto' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kouto', 'kouto', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 58) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 54) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 37) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 9) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'krab' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Krab', 'krab', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 55) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 30) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'krapo' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Krapo', 'krapo', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 9) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 75) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'krapo' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Krapo', 'krapo', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 28) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 73) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kreyon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kreyon', 'kreyon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 1) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kribich' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kribich', 'kribich', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 30) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 31) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'krich' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Krich', 'krich', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 6) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 42) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 81) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 28) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kre' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Krè', 'kre', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 28) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 19) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 46) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 62) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kwa' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kwa', 'kwa', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 10) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 40) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 33) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kobya' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kòbya', 'kobya', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 61) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kok' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kòk', 'kok', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 71) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 1) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'kok_batay' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kòk Batay', 'kok_batay', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 15) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 64) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'komes' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Kòmès', 'komes', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 74) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 77) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'labatwa' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Labatwa', 'labatwa', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 16) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 69) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 31) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 43) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'labou' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Labou', 'labou', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 6) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 60) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 62) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 82) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'laboure' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Laboure', 'laboure', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 1) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 29) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 91) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'lajan_papye' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lajan Papye', 'lajan_papye', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 18) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 74) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'lalin' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lalin', 'lalin', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 15) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 17) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 6) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'lames' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lamès', 'lames', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 92) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 93) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 5) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'lanmou' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lanmou', 'lanmou', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 12) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'lanme' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lanmè', 'lanme', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 18) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'lapli' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lapli', 'lapli', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 99) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'lapolis' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lapolis', 'lapolis', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 44) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'latrin' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Latrin', 'latrin', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 5) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 81) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'lave' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lave', 'lave', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 30) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 66) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 28) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'legim' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Legim', 'legim', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 47) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 29) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'lenj' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lenj', 'lenj', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 27) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 43) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 65) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 75) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'lenn' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lenn', 'lenn', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 45) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 78) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'lesiv' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lesiv', 'lesiv', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 3) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 30) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'li' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Li', 'li', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 6) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 42) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'limye' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Limyè', 'limye', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 28) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 80) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 42) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'linet' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Linèt', 'linet', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 88) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 85) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 52) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'liv' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Liv', 'liv', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 43) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'lopital' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lopital', 'lopital', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 60) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 42) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'loray' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Loray', 'loray', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 44) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 99) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 47) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'lougawou' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lougawou', 'lougawou', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 37) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 47) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'lyon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lyon', 'lyon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 42) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'let' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lèt', 'let', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 75) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 72) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 13) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'let_ekri' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Lèt (Ekri)', 'let_ekri', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 66) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 12) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'machann' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Machann', 'machann', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'mache' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Mache', 'mache', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 27) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 25) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'machin' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Machin', 'machin', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 15) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 77) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 0) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 33) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'madigra' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Madigra', 'madigra', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 37) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 77) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'magazen' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Magazen', 'magazen', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 37) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'majistra' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Majistra', 'majistra', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 0) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 50) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'malad' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Malad', 'malad', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 66) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 67) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 58) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 75) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'manje' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Manje', 'manje', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 57) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 40) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'matla' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Matla', 'matla', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 3) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'mato' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Mato', 'mato', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 51) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 62) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'mayi_moulen' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Mayi Moulen', 'mayi_moulen', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 27) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'mayi_an_gren' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Mayi an gren', 'mayi_an_gren', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 20) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 18) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 97) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'mekanik' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Mekanik', 'mekanik', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 63) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'melon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Melon', 'melon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 82) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 29) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 62) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'metres' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Metrès', 'metres', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 13) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 0) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'mi' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Mi', 'mi', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 44) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 67) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'milet' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Milèt', 'milet', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 34) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 43) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'miskad' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Miskad', 'miskad', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 10) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 45) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 28) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 65) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'mizisyen' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Mizisyen', 'mizisyen', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 39) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'mo_sal' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Mo sal', 'mo_sal', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 19) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 5) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 47) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'monsegne' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Monsegnè', 'monsegne', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 78) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 79) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'mont' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Mont', 'mont', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 25) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 90) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'motosiklet' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Motosiklèt', 'motosiklet', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 53) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'mouch' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Mouch', 'mouch', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 45) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 25) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 9) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'mouchwa' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Mouchwa', 'mouchwa', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 84) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'moun_mouri' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Moun mouri', 'moun_mouri', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 8) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 33) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 74) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'moustik' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Moustik', 'moustik', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 35) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 96) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'myel' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Myèl', 'myel', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 96) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 6) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 8) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 49) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'meb' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Mèb', 'meb', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 20) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'mon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Mòn', 'mon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 12) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 50) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'otel' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Otèl', 'otel', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 42) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 44) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 69) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'ougan' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Ougan', 'ougan', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 37) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pafen' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pafen', 'pafen', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 91) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 23) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pale' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Palè', 'pale', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 53) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 9) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pantalon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pantalon', 'pantalon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 88) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 20) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 77) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'panye' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Panye', 'panye', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 19) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 69) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 89) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'papiyon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Papiyon', 'papiyon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 2) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 94) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 12) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 35) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'papye' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Papye', 'papye', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 79) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'parapli' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Parapli', 'parapli', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 27) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 57) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'parapli' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Parapli', 'parapli', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 80) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 57) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'paspo' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Paspò', 'paspo', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 79) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 15) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'patat' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Patat', 'patat', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 3) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 30) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pate' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pate', 'pate', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 89) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 2) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pay' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pay', 'pay', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 27) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 48) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 41) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pen' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pen', 'pen', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 60) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 33) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 58) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 50) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pentad' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pentad', 'pentad', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 33) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 13) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 73) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pijon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pijon', 'pijon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 24) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 15) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'piman' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Piman', 'piman', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 71) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pinez' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pinèz', 'pinez', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 97) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 99) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 30) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pip' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pip', 'pip', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 56) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 12) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 17) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'planch' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Planch', 'planch', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 81) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 88) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'plante' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Plante', 'plante', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 97) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'plenn' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Plenn', 'plenn', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 72) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 26) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 43) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'plim' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Plim', 'plim', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 33) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 18) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pon', 'pon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 12) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 73) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'poul' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Poul', 'poul', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 23) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 70) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 37) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'prezidan' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Prezidan', 'prezidan', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 26) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 45) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 79) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pwa' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pwa', 'pwa', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 87) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 19) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 90) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pwason' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pwason', 'pwason', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 27) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 18) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 78) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pyano' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pyano', 'pyano', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 98) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 99) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 29) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pe' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pè', 'pe', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 45) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 16) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pedi' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pèdi', 'pedi', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 51) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 54) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 74) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'pom' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Pòm', 'pom', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 28) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 59) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'rab' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Rab', 'rab', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 37) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'rach' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Rach', 'rach', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 36) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 10) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'radyo' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Radyo', 'radyo', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 43) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 24) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'ranyon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Ranyon', 'ranyon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 57) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 53) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'rara' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Rara', 'rara', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 79) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 97) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'rat' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Rat', 'rat', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 29) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 90) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 26) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'ravin' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Ravin', 'ravin', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 1) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'ravet' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Ravèt', 'ravet', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 48) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 33) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 30) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'recho' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Recho', 'recho', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 13) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'reken' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Reken', 'reken', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 45) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'restoran' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Restoran', 'restoran', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 68) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 28) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 78) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'rezen' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Rezen', 'rezen', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 19) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 90) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 42) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'ri' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Ri', 'ri', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 64) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 13) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'riches' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Richès', 'riches', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 50) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 51) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'rido' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Rido', 'rido', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 51) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 53) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 45) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'rigol' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Rigòl', 'rigol', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 56) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 85) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'rivye_kle' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Rivyè klè', 'rivye_kle', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 19) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 33) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'rivye_sal' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Rivyè sal', 'rivye_sal', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 9) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 23) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'ren' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Rèn', 'ren', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 56) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 31) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 82) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'sab' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Sab', 'sab', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 89) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 97) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'sak' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Sak', 'sak', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 33) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 78) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'san' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'San', 'san', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 45) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 61) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 5) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'sandal' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Sandal', 'sandal', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 9) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 52) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'savon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Savon', 'savon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 76) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 34) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 15) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 92) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'seremoni' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Seremoni', 'seremoni', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 2) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'sik' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Sik', 'sik', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 6) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'simitye' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Simityè', 'simitye', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 3) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 13) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'sinema' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Sinema', 'sinema', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 75) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'siret' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Sirèt', 'siret', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 38) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 51) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 56) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'siwo' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Siwo', 'siwo', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 82) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 90) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'soley' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Solèy', 'soley', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 61) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 25) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 33) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'sosis' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Sosis', 'sosis', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 8) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 97) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'sou' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Sou', 'sou', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 10) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 51) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'soulye' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Soulye', 'soulye', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 88) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 28) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'soup' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Soup', 'soup', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 38) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 45) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 58) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'sourit' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Sourit', 'sourit', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 15) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 29) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 5) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 89) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'soutyen' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Soutyen', 'soutyen', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 61) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 6) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 43) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'swaf' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Swaf', 'swaf', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 25) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 75) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 51) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'syel' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Syèl', 'syel', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 66) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 89) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'sekey' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Sèkèy', 'sekey', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 31) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'sel' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Sèl', 'sel', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 9) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 16) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 18) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'tabak' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Tabak', 'tabak', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 43) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 51) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'tablo' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Tablo', 'tablo', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 17) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 60) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'tanbou' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Tanbou', 'tanbou', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 30) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 28) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'tapi' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Tapi', 'tapi', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 17) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'telefon' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Telefòn', 'telefon', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 70) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'teren' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Teren', 'teren', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 65) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'teyat' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Teyat', 'teyat', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 80) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 2) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'ti_monnen_lajan' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Ti Monnen Lajan', 'ti_monnen_lajan', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 78) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'timoun' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Timoun', 'timoun', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 13) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 25) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'tiyo' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Tiyo', 'tiyo', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 22) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 5) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 34) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'tomat' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Tomat', 'tomat', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 74) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 45) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'tonb' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Tonb', 'tonb', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 67) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 8) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'travay' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Travay', 'travay', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 54) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 25) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 78) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'tren' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Tren', 'tren', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 73) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 12) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 70) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'tribinal' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Tribinal', 'tribinal', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 35) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 37) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'twonpet' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Twonpèt', 'twonpet', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'twou' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Twou', 'twou', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 9) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'tol' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Tòl', 'tol', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 63) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'van' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Van', 'van', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 75) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 86) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'vwayaj' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Vwayaj', 'vwayaj', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 44) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 61) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 92) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 16) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 8) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'vole' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Vòlè', 'vole', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 59) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 47) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'wa' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Wa', 'wa', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 27) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 56) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 78) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'woz' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Woz', 'woz', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 35) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'wob' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Wòb', 'wob', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 2) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 51) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 75) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'woch' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Wòch', 'woch', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 25) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 96) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 21) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'zam' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Zam', 'zam', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 44) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 45) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 30) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'ze' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Ze', 'ze', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 60) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 0) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 13) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 16) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 75) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 87) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'zegwi' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Zegwi', 'zegwi', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 38) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 11) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 7) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 17) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'zoranj' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Zoranj', 'zoranj', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 44) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 2) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 40) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 13) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'zouti' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Zouti', 'zouti', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 29) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'zwazo' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Zwazo', 'zwazo', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 47) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 60) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 27) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 2) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'zeb' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Zèb', 'zeb', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 89) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 12) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 43) ON CONFLICT DO NOTHING;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM tchala_entry e WHERE e.lang = 'ht' AND e.dedupe_key = 'enmi' AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN
    v_entry_id := gen_random_uuid();
    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at) VALUES (v_entry_id, 'ht', 'Ènmi', 'enmi', 'imported_from_public_web_page', 'APPROVED', 'IMPORT', now(), now());
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 60) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 4) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 73) ON CONFLICT DO NOTHING;
    INSERT INTO tchala_entry_number (entry_id, lang, number) VALUES (v_entry_id, 'ht', 32) ON CONFLICT DO NOTHING;
  END IF;
END$$;
