#!/usr/bin/env python3
"""
Génère une migration Flyway idempotente depuis tchala_lotto_ht_init.csv.

Usage (depuis tchalanet-server/tchalanet-app/):
  python3 ../scripts/generate_tchala_migration.py

Le script lit le CSV et écrit le SQL dans db/migration/.
La migration est idempotente : elle ne ré-insère pas si (lang, dedupe_key, APPROVED, canonical IS NULL) existe.
"""
import csv
from pathlib import Path

CSV  = Path('src/main/resources/tchala/tchala_lotto_ht_init.csv')
OUT  = Path('src/main/resources/db/migration/V222__import_tchala_full.sql')

def q(s):
    return (s or '').replace("'", "''")

rows = []
with CSV.open(encoding='utf-8') as f:
    for r in csv.DictReader(f):
        rows.append(r)

print(f"CSV : {len(rows)} entrées lues")

lines = [
    "-- V222__import_tchala_full.sql",
    "-- Généré par scripts/generate_tchala_migration.py",
    "-- Importe toutes les entrées APPROVED depuis tchala_lotto_ht_init.csv",
    "-- Idempotent : ne ré-insère pas si (lang, dedupe_key, APPROVED, canonical_entry_id IS NULL) existe déjà",
    "",
    "DO $$",
    "DECLARE",
    "  v_entry_id uuid;",
    "BEGIN",
]

skipped = 0
for r in rows:
    lang   = r.get('tchala_lang', '').strip()
    dream  = q(r.get('dream_text', '').strip())
    numbers_raw = r.get('numbers', '').strip()
    status = r.get('status', 'APPROVED').strip()
    dedupe = q(r.get('dedupe_key', '').strip())
    note   = q(r.get('notes', '').strip())

    if not lang or not dream or not dedupe:
        skipped += 1
        continue

    # normalise séparateurs → liste d'entiers 0..99
    nums_str = numbers_raw.replace(';', ',').replace('.', ',').replace('|', ',')
    nums = []
    for part in nums_str.split(','):
        part = part.strip().lstrip('0') or '0'
        try:
            v = int(part)
        except ValueError:
            continue
        if 0 <= v <= 99:
            nums.append(v)

    if not nums:
        skipped += 1
        continue

    lines.append(
        f"  IF NOT EXISTS (SELECT 1 FROM tchala_entry e"
        f" WHERE e.lang = '{lang}' AND e.dedupe_key = '{dedupe}'"
        f" AND e.status = 'APPROVED' AND e.canonical_entry_id IS NULL) THEN"
    )
    lines.append("    v_entry_id := gen_random_uuid();")
    lines.append(
        f"    INSERT INTO tchala_entry (id, lang, dream, dedupe_key, note, status, source, created_at, updated_at)"
        f" VALUES (v_entry_id, '{lang}', '{dream}', '{dedupe}', '{note}', 'APPROVED', 'IMPORT', now(), now());"
    )
    for n in nums:
        lines.append(
            f"    INSERT INTO tchala_entry_number (entry_id, lang, number)"
            f" VALUES (v_entry_id, '{lang}', {n}) ON CONFLICT DO NOTHING;"
        )
    lines.append("  END IF;")

lines += ["END$$;", ""]

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text('\n'.join(lines), encoding='utf-8')
print(f"SQL  : {OUT}  ({len(rows) - skipped} entrées, {skipped} ignorées)")
