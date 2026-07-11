SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'platform', true);
SELECT set_config('app.is_super_admin', 'true', true);

UPDATE i18n_override
SET deleted_at = now(),
    updated_at = now()
WHERE level = 'GLOBAL'
  AND tenant_id IS NULL
  AND surface = 'CASHIER'
  AND i18n_key = 'receipt.line.header.payout'
  AND deleted_at IS NULL;

WITH receipt_i18n(locale, i18n_key, i18n_value) AS (
  VALUES
    ('fr', 'receipt.promotion.free_game_short', 'GRATIS'),
    ('fr', 'receipt.promotion.maryaj_offered_note', '* Maryaj offert'),
    ('en', 'receipt.promotion.free_game_short', 'FREE'),
    ('en', 'receipt.promotion.maryaj_offered_note', '* Free Maryaj'),
    ('ht', 'receipt.promotion.free_game_short', 'GRATIS'),
    ('ht', 'receipt.promotion.maryaj_offered_note', '* Maryaj gratis')
)
INSERT INTO i18n_override (level, tenant_id, surface, locale, i18n_key, i18n_value, active)
SELECT 'GLOBAL', NULL, 'CASHIER', seed.locale, seed.i18n_key, seed.i18n_value, true
FROM receipt_i18n seed
WHERE NOT EXISTS (
  SELECT 1
  FROM i18n_override existing
  WHERE existing.level = 'GLOBAL'
    AND existing.tenant_id IS NULL
    AND existing.surface = 'CASHIER'
    AND existing.locale = seed.locale
    AND existing.i18n_key = seed.i18n_key
    AND existing.deleted_at IS NULL
);

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
