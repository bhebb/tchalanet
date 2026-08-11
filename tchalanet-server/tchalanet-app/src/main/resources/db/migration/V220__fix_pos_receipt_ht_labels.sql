SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'platform', true);
SELECT set_config('app.is_super_admin', 'true', true);

WITH receipt_i18n(locale, i18n_key, i18n_value) AS (
  VALUES
    ('ht', 'receipt.terminal', 'Tèminal'),
    ('ht', 'receipt.game.HT_BOLET', 'Bòlèt'),
    ('ht', 'receipt.game.HT_LOTO3', 'Loto 3 chif'),
    ('ht', 'receipt.game.HT_LOTO4', 'Loto 4 chif'),
    ('ht', 'receipt.game.HT_LOTO5', 'Loto 5 chif'),
    ('fr', 'receipt.game.HT_LOTO3', 'Loto 3 chiffres'),
    ('fr', 'receipt.game.HT_LOTO4', 'Loto 4 chiffres'),
    ('fr', 'receipt.game.HT_LOTO5', 'Loto 5 chiffres'),
    ('en', 'receipt.game.HT_LOTO3', 'Loto 3'),
    ('en', 'receipt.game.HT_LOTO4', 'Loto 4'),
    ('en', 'receipt.game.HT_LOTO5', 'Loto 5')
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

WITH receipt_i18n(locale, i18n_key, i18n_value) AS (
  VALUES
    ('ht', 'receipt.terminal', 'Tèminal'),
    ('ht', 'receipt.game.HT_BOLET', 'Bòlèt'),
    ('ht', 'receipt.game.HT_LOTO3', 'Loto 3 chif'),
    ('ht', 'receipt.game.HT_LOTO4', 'Loto 4 chif'),
    ('ht', 'receipt.game.HT_LOTO5', 'Loto 5 chif'),
    ('fr', 'receipt.game.HT_LOTO3', 'Loto 3 chiffres'),
    ('fr', 'receipt.game.HT_LOTO4', 'Loto 4 chiffres'),
    ('fr', 'receipt.game.HT_LOTO5', 'Loto 5 chiffres'),
    ('en', 'receipt.game.HT_LOTO3', 'Loto 3'),
    ('en', 'receipt.game.HT_LOTO4', 'Loto 4'),
    ('en', 'receipt.game.HT_LOTO5', 'Loto 5')
)
UPDATE i18n_override existing
SET i18n_value = seed.i18n_value
FROM receipt_i18n seed
WHERE existing.level = 'GLOBAL'
  AND existing.tenant_id IS NULL
  AND existing.surface = 'CASHIER'
  AND existing.locale = seed.locale
  AND existing.i18n_key = seed.i18n_key
  AND existing.deleted_at IS NULL
  AND existing.i18n_value IS DISTINCT FROM seed.i18n_value;

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
