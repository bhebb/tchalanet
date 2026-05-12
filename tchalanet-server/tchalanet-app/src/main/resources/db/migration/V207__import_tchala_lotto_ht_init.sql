-- V37__import_tchala_lotto_ht_init.sql
-- Import initial (HT) du Tchala depuis: src/main/resources/tchala/tchala_lotto_ht_init.csv
-- Objectif: insérer des entrées canoniques APPROVED si aucune entrée canonique n'existe déjà pour (lang, dedupe_key)
-- Règles:
-- - Idempotent: relançable sans doublons
-- - N'insère que si (lang, dedupe_key, status=APPROVED, canonical_entry_id IS NULL) n'existe pas
-- - Numbers: split par ".", ";" ou "," puis validation 0..99
-- - Numbers: insert ON CONFLICT DO NOTHING

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
v_entry_id uuid;
  v_row text;
  v_parts text[];
  v_numbers_s text;
  v_numbers text[];
  v_num text;

  v_lang text;
  v_dream text;
  v_dedupe text;
  v_note text;
BEGIN
  -- Encodage des lignes: lang|dream|numbers|dedupe|note
  -- Note FR standardisée: "Importé depuis la page publique (lotto.ht)"
FOR v_row IN
SELECT unnest(ARRAY[
                  'ht|Achte|55.07.76.36|achte|Importé depuis la page publique (lotto.ht)',
              'ht|Adiltè|29.58.69.28|adilte|Importé depuis la page publique (lotto.ht)',
              'ht|Akouchman|32.56.11.33|akouchman|Importé depuis la page publique (lotto.ht)',
              'ht|Aksidan|96.06.05.49|aksidan|Importé depuis la page publique (lotto.ht)',
              'ht|Alimèt|11.08.42|alimet|Importé depuis la page publique (lotto.ht)',
              'ht|Altagras|12.21|altagras|Importé depuis la page publique (lotto.ht)',
              'ht|Anana|73.19.11|anana|Importé depuis la page publique (lotto.ht)',
              'ht|Anbilans|30.37.22.42|anbilans|Importé depuis la page publique (lotto.ht)',
              'ht|Ansent|20|ansent|Importé depuis la page publique (lotto.ht)',
              'ht|Antèman|09.91.10.66|anteman|Importé depuis la page publique (lotto.ht)',
              'ht|Arete|36.13.24.25|arete|Importé depuis la page publique (lotto.ht)',
              'ht|Atelye|41.47.45.73|atelye|Importé depuis la page publique (lotto.ht)',
              'ht|Avyon|03.29.47.85.04|avyon|Importé depuis la page publique (lotto.ht)',
              'ht|Bag Maryaj|25.59|bag_maryaj|Importé depuis la page publique (lotto.ht)',
              'ht|Bale|55.03.14|bale|Importé depuis la page publique (lotto.ht)',
              'ht|Balèn|65.04.59|balen|Importé depuis la page publique (lotto.ht)',
              'ht|Bank|61.15.84.14|bank|Importé depuis la page publique (lotto.ht)',
              'ht|Bannann|87.48|bannann|Importé depuis la page publique (lotto.ht)',
              'ht|Batay|78.11.19.32|batay|Importé depuis la page publique (lotto.ht)',
              'ht|Bato|18.68|bato|Importé depuis la page publique (lotto.ht)',
              'ht|Baton|89.40.41.88|baton|Importé depuis la page publique (lotto.ht)',
              'ht|Baton|43.61|baton|Importé depuis la page publique (lotto.ht)',
              'ht|Batèm|88.69.58|batem|Importé depuis la page publique (lotto.ht)',
              'ht|Bay Tete|19|bay_tete|Importé depuis la page publique (lotto.ht)',
              'ht|Baza|39.62|baza|Importé depuis la page publique (lotto.ht)',
              'ht|Bekàn|52.41.78|bekan|Importé depuis la page publique (lotto.ht)',
              'ht|Benyen|32.25.59|benyen|Importé depuis la page publique (lotto.ht)',
              'ht|Bib|32.20|bib|Importé depuis la page publique (lotto.ht)',
              'ht|Bibwon|82.94|bibwon|Importé depuis la page publique (lotto.ht)',
              'ht|Bijou|53.41.52|bijou|Importé depuis la page publique (lotto.ht)',
              'ht|Ble|01.79.09.83|ble|Importé depuis la page publique (lotto.ht)',
              'ht|Blese|09.03.42.62|blese|Importé depuis la page publique (lotto.ht)',
              'ht|Bo|23.14.22.05|bo|Importé depuis la page publique (lotto.ht)',
              'ht|Bokit|49|bokit|Importé depuis la page publique (lotto.ht)',
              'ht|Boul|14.82.46.26|boul|Importé depuis la page publique (lotto.ht)',
              'ht|Boulanje|03.21.32.60|boulanje|Importé depuis la page publique (lotto.ht)',
              'ht|Bourik|53.35.91.14|bourik|Importé depuis la page publique (lotto.ht)',
              'ht|Bourèt|41|bouret|Importé depuis la page publique (lotto.ht)',
              'ht|Bous|02.60.10.69|bous|Importé depuis la page publique (lotto.ht)',
              'ht|Bouzen|66.21|bouzen|Importé depuis la page publique (lotto.ht)',
              'ht|Bwat|81.93|bwat|Importé depuis la page publique (lotto.ht)',
              'ht|Bèf|16.76.96|bef|Importé depuis la page publique (lotto.ht)',
              'ht|Bèso|01.36.62.72|beso|Importé depuis la page publique (lotto.ht)',
              'ht|Chabon|85.59.07.30|chabon|Importé depuis la page publique (lotto.ht)',
              'ht|Chantye|32.85|chantye|Importé depuis la page publique (lotto.ht)',
              'ht|Chaplè|04.32|chaple|Importé depuis la page publique (lotto.ht)',
              'ht|Chapo|20.28.71.11|chapo|Importé depuis la page publique (lotto.ht)',
              'ht|Chasè|09.59.99|chase|Importé depuis la page publique (lotto.ht)',
              'ht|Chat|74.04.14.84|chat|Importé depuis la page publique (lotto.ht)',
              'ht|Chef|22|chef|Importé depuis la page publique (lotto.ht)',
              'ht|Chen|15.75.95|chen|Importé depuis la page publique (lotto.ht)',
              'ht|Chèn|42.73|chen|Importé depuis la page publique (lotto.ht)',
              'ht|Chèz|63.16.73|chez|Importé depuis la page publique (lotto.ht)',
              'ht|Dan|31.58.15|dan|Importé depuis la page publique (lotto.ht)',
           -- ... (garde le reste de ta liste inchangée, même format)
              'ht|Ènmi|60.04.73.32|enmi|Importé depuis la page publique (lotto.ht)'
                  ])
           LOOP
    v_parts := string_to_array(v_row, '|');

v_lang := v_parts[1];
    v_dream := v_parts[2];
    v_numbers_s := v_parts[3];
    v_dedupe := v_parts[4];
    v_note := v_parts[5];

    -- Vérifie si une entrée canonique APPROVED existe déjà pour (lang, dedupe_key)
    IF NOT EXISTS (
      SELECT 1
      FROM tchala_entry e
      WHERE e.lang = v_lang
        AND e.dedupe_key = v_dedupe
        AND e.status = 'APPROVED'
        AND e.canonical_entry_id IS NULL
    ) THEN
      v_entry_id := gen_random_uuid();

INSERT INTO tchala_entry (
    id, lang, dream, dedupe_key, note, status, source, created_at, updated_at
)
VALUES (
           v_entry_id, v_lang, v_dream, v_dedupe, v_note, 'APPROVED', 'IMPORT', now(), now()
       );

-- Normalise les séparateurs vers virgule puis split
v_numbers_s := regexp_replace(v_numbers_s, '\.|;', ',', 'g');
      v_numbers := string_to_array(v_numbers_s, ',');

      FOREACH v_num IN ARRAY v_numbers LOOP
        v_num := btrim(v_num);

        -- retirer les zéros de tête, ex: "09" -> "9", mais garder "0"
        v_num := ltrim(v_num, '0');
        IF v_num = '' THEN v_num := '0'; END IF;

        IF v_num ~ '^[0-9]+$' THEN
          IF (v_num::int >= 0 AND v_num::int <= 99) THEN
            INSERT INTO tchala_entry_number (entry_id, lang, number)
            VALUES (v_entry_id, v_lang, v_num::int)
            ON CONFLICT DO NOTHING;
END IF;
END IF;
END LOOP;
END IF;
END LOOP;
END$$;

