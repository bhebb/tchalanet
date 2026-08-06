-- Fix the spelling of the Bòlèt game.
--
-- The correct Haitian Creole spelling carries a grave accent on both vowels:
-- Bòlèt. Three different spellings had drifted apart across the product:
--
--   POS screen      "Bolet"   hardcoded in PosGamesService (fixed in code)
--   game.name       "Bolèt"   seeded by V204
--   printed receipt "Bolèt"   ht, and "Bolet" en, seeded by V208
--
-- A seller therefore read one spelling on the terminal and handed the customer
-- a ticket printed with another. This aligns the stored values; the POS label
-- was corrected in the Java switch that bypasses this i18n entirely.
--
-- French keeps "Borlette", which is the established French Haitian form and not
-- a misspelling.

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'platform', true);
SELECT set_config('app.is_super_admin', 'true', true);

-- Canonical game name.
UPDATE game
SET name = 'Bòlèt',
    updated_at = now()
WHERE code = 'HT_BOLET'
  AND name <> 'Bòlèt';

-- Receipt label. Only the Creole and English rows: `fr` stays "Borlette".
UPDATE i18n_override
SET i18n_value = 'Bòlèt',
    updated_at = now()
WHERE level = 'GLOBAL'
  AND tenant_id IS NULL
  AND surface = 'CASHIER'
  AND locale IN ('ht', 'en')
  AND i18n_key = 'receipt.game.HT_BOLET'
  AND deleted_at IS NULL
  AND i18n_value <> 'Bòlèt';
