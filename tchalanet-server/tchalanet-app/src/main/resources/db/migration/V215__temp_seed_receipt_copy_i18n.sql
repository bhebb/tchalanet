-- Temporary pre-go-live seed: receipt copy markers used by printed tickets.
WITH receipt_copy_i18n(locale, i18n_key, i18n_value) AS (
  VALUES
    ('fr', 'receipt.copy.original', 'Original'),
    ('fr', 'receipt.copy.duplicate', 'Copie'),
    ('fr', 'receipt.copy.reprint', 'Réimpression'),
    ('en', 'receipt.copy.original', 'Original'),
    ('en', 'receipt.copy.duplicate', 'Copy'),
    ('en', 'receipt.copy.reprint', 'Reprint'),
    ('ht', 'receipt.copy.original', 'Orijinal'),
    ('ht', 'receipt.copy.duplicate', 'Kopi'),
    ('ht', 'receipt.copy.reprint', 'Reenpresyon')
)
INSERT INTO i18n_override (
  level,
  tenant_id,
  surface,
  locale,
  i18n_key,
  i18n_value,
  active
)
SELECT 'GLOBAL',
       NULL,
       'CASHIER',
       locale,
       i18n_key,
       i18n_value,
       TRUE
FROM receipt_copy_i18n seed
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

WITH receipt_copy_i18n(locale, i18n_key, i18n_value) AS (
  VALUES
    ('fr', 'receipt.copy.original', 'Original'),
    ('fr', 'receipt.copy.duplicate', 'Copie'),
    ('fr', 'receipt.copy.reprint', 'Réimpression'),
    ('en', 'receipt.copy.original', 'Original'),
    ('en', 'receipt.copy.duplicate', 'Copy'),
    ('en', 'receipt.copy.reprint', 'Reprint'),
    ('ht', 'receipt.copy.original', 'Orijinal'),
    ('ht', 'receipt.copy.duplicate', 'Kopi'),
    ('ht', 'receipt.copy.reprint', 'Reenpresyon')
)
UPDATE i18n_override target
SET i18n_value = seed.i18n_value,
    active = TRUE,
    updated_at = NOW()
FROM receipt_copy_i18n seed
WHERE target.level = 'GLOBAL'
  AND target.tenant_id IS NULL
  AND target.surface = 'CASHIER'
  AND target.locale = seed.locale
  AND target.i18n_key = seed.i18n_key
  AND target.deleted_at IS NULL;
