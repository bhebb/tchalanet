-- V220: French receipt label corrections + copy-marker keys.
--
-- V210 seeded some receipt labels with missing accents / abbreviations that the
-- receipt now surfaces verbatim. Align them to proper French, reword the currency
-- note to "Devise : <code>", and update the scan hint. Also seed the ORIGINAL /
-- DUPLICATA copy-marker keys used by the print copy context.
--
-- GLOBAL / tenant NULL / surface CASHIER / locale fr — same scope as V210.

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', 'platform', true);
SELECT set_config('app.is_super_admin', 'true', true);

-- 1. Update existing fr values in place (accents + wording corrections).
UPDATE i18n_override SET i18n_value = 'Vérification'
 WHERE level = 'GLOBAL' AND tenant_id IS NULL AND surface = 'CASHIER'
   AND locale = 'fr' AND i18n_key = 'receipt.verification' AND deleted_at IS NULL;

UPDATE i18n_override SET i18n_value = 'Gain maximal'
 WHERE level = 'GLOBAL' AND tenant_id IS NULL AND surface = 'CASHIER'
   AND locale = 'fr' AND i18n_key = 'receipt.total.max_payout' AND deleted_at IS NULL;

UPDATE i18n_override SET i18n_value = 'Scannez le code QR'
 WHERE level = 'GLOBAL' AND tenant_id IS NULL AND surface = 'CASHIER'
   AND locale = 'fr' AND i18n_key = 'receipt.scan_to_verify' AND deleted_at IS NULL;

UPDATE i18n_override SET i18n_value = 'Devise : {code}'
 WHERE level = 'GLOBAL' AND tenant_id IS NULL AND surface = 'CASHIER'
   AND locale = 'fr' AND i18n_key = 'receipt.currency_note' AND deleted_at IS NULL;

UPDATE i18n_override SET i18n_value = 'Vérifier sur {url}'
 WHERE level = 'GLOBAL' AND tenant_id IS NULL AND surface = 'CASHIER'
   AND locale = 'fr' AND i18n_key = 'receipt.message.backup.verify' AND deleted_at IS NULL;

-- 2. Seed copy-marker keys (ORIGINAL / DUPLICATA) if absent.
INSERT INTO i18n_override (level, tenant_id, surface, locale, i18n_key, i18n_value, active)
SELECT 'GLOBAL', NULL, 'CASHIER', seed.locale, seed.i18n_key, seed.i18n_value, true
FROM (VALUES
    ('fr', 'receipt.copy.original', 'ORIGINAL'),
    ('fr', 'receipt.copy.duplicate', 'DUPLICATA')
) AS seed(locale, i18n_key, i18n_value)
WHERE NOT EXISTS (
  SELECT 1 FROM i18n_override existing
  WHERE existing.level = 'GLOBAL' AND existing.tenant_id IS NULL
    AND existing.surface = 'CASHIER' AND existing.locale = seed.locale
    AND existing.i18n_key = seed.i18n_key AND existing.deleted_at IS NULL
);

SELECT set_config('app.current_tenant', '', true);
SELECT set_config('app.deleted_visibility', 'active', true);
SELECT set_config('app.api_scope', '', true);
SELECT set_config('app.is_super_admin', 'false', true);
