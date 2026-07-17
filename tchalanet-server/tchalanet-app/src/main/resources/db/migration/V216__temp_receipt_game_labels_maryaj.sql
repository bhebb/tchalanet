-- Temporary receipt-label correction for printed-ticket V0 finishing.
-- Keeps the business display label as Maryaj and separates the free promotion title.
SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'platform', true);
SELECT set_config('app.is_super_admin', 'true', true);

UPDATE i18n_override
SET i18n_value = 'Maryaj'
WHERE locale = 'fr'
  AND i18n_key = 'receipt.game.HT_MARYAJ'
  AND level = 'GLOBAL'
  AND tenant_id IS NULL
  AND surface = 'CASHIER'
  AND deleted_at IS NULL;

UPDATE i18n_override
SET i18n_value = 'Maryaj gratis'
WHERE locale = 'fr'
  AND i18n_key = 'receipt.game.HT_MARYAJ_GRATIS'
  AND level = 'GLOBAL'
  AND tenant_id IS NULL
  AND surface = 'CASHIER'
  AND deleted_at IS NULL;

UPDATE i18n_override
SET i18n_value = 'Maryaj gratis'
WHERE locale = 'fr'
  AND i18n_key = 'receipt.promotion.free_game_line'
  AND level = 'GLOBAL'
  AND tenant_id IS NULL
  AND surface = 'CASHIER'
  AND deleted_at IS NULL;

UPDATE i18n_override
SET i18n_value = 'Maryaj'
WHERE locale = 'en'
  AND i18n_key = 'receipt.game.HT_MARYAJ'
  AND level = 'GLOBAL'
  AND tenant_id IS NULL
  AND surface = 'CASHIER'
  AND deleted_at IS NULL;

UPDATE i18n_override
SET i18n_value = 'Maryaj gratis'
WHERE locale = 'en'
  AND i18n_key = 'receipt.game.HT_MARYAJ_GRATIS'
  AND level = 'GLOBAL'
  AND tenant_id IS NULL
  AND surface = 'CASHIER'
  AND deleted_at IS NULL;

UPDATE i18n_override
SET i18n_value = 'Maryaj gratis'
WHERE locale = 'en'
  AND i18n_key = 'receipt.promotion.free_game_line'
  AND level = 'GLOBAL'
  AND tenant_id IS NULL
  AND surface = 'CASHIER'
  AND deleted_at IS NULL;

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
