-- V44: seed global game catalog
DO $$ BEGIN
  RAISE NOTICE 'V44__seed_game_catalog: seeding global game catalog';
END $$;

WITH games(id, code, name, category, min_digits, max_digits, combination, description, active, sort_order) AS (
    VALUES
        ('00000000-0000-0000-0000-000000000101'::uuid,'US_NY_PICK3','NY Pick 3','PICK',3,3,'STRAIGHT','New York Pick 3 (midday/evening)',true,10),
        ('00000000-0000-0000-0000-000000000102'::uuid,'US_NY_PICK4','NY Pick 4','PICK',4,4,'STRAIGHT','New York Pick 4 (midday/evening)',true,20),
        ('00000000-0000-0000-0000-000000000103'::uuid,'US_NY_TAKE5','NY Take 5','LOTTO',5,5,'STRAIGHT','New York Take 5 (evening)',true,30),

        ('00000000-0000-0000-0000-000000000111'::uuid,'US_FL_PICK3','FL Pick 3','PICK',3,3,'STRAIGHT','Florida Pick 3 (midday/evening)',true,110),
        ('00000000-0000-0000-0000-000000000112'::uuid,'US_FL_PICK4','FL Pick 4','PICK',4,4,'STRAIGHT','Florida Pick 4 (midday/evening)',true,120),
        ('00000000-0000-0000-0000-000000000113'::uuid,'US_FL_LOTTO','Florida Lotto','LOTTO',1,6,'STRAIGHT','Florida Lotto (evening)',true,130),

        ('00000000-0000-0000-0000-000000000201'::uuid,'HT_BORLETTE','Borlette (Haïti)','BORLETTE',2,3,'MATCH','Famille Borlette (2/3 chiffres)',true,210),
        ('00000000-0000-0000-0000-000000000202'::uuid,'HT_MARIAGE','Mariage (Haïti)','MARIAGE',4,4,'MARRIAGE','Mode Mariage',true,220),
        ('00000000-0000-0000-0000-000000000203'::uuid,'HT_LOTO_3','Loto 3 (Haïti)','LOTO',3,3,'STRAIGHT','Mode Loto 3',true,230),
        ('00000000-0000-0000-0000-000000000204'::uuid,'HT_LOTO_4','Loto 4 (Haïti)','LOTO',4,4,'STRAIGHT','Mode Loto 4',true,240),
        ('00000000-0000-0000-0000-000000000205'::uuid,'HT_LOTO_5','Loto 5 (Haïti)','LOTO',5,5,'STRAIGHT','Mode Loto 5',true,250)
)
INSERT INTO game (id, code, name, category, min_digits, max_digits, combination, description, active, sort_order)
SELECT g.id, g.code, g.name, g.category, g.min_digits, g.max_digits, g.combination, g.description, g.active, g.sort_order
FROM games g
ON CONFLICT (code) DO UPDATE
  SET name = EXCLUDED.name,
      category = EXCLUDED.category,
      min_digits = EXCLUDED.min_digits,
      max_digits = EXCLUDED.max_digits,
      combination = EXCLUDED.combination,
      description = EXCLUDED.description,
      active = EXCLUDED.active,
      sort_order = EXCLUDED.sort_order;

-- Sanity check
DO $$
DECLARE cnt int;
BEGIN
  SELECT count(*) INTO cnt FROM game WHERE code IN ('US_NY_PICK3','US_NY_PICK4','US_NY_TAKE5');
  IF cnt < 3 THEN
    RAISE EXCEPTION 'V44__seed_game_catalog sanity check failed: expected base games missing (found %)', cnt;
  ELSE
    RAISE NOTICE 'V44__seed_game_catalog sanity check OK: base games present';
  END IF;
END $$;

