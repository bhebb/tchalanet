-- Sketch only — adapt to existing outlet table/migration.

-- ALTER TABLE outlet ADD COLUMN kind varchar(48) NOT NULL DEFAULT 'OWNED_SHOP';
-- ALTER TABLE outlet ADD COLUMN partner_ref varchar(96) NULL;
-- ALTER TABLE outlet ADD COLUMN partner_metadata jsonb NULL;

-- Future allowed outlet kinds:
-- OWNED_SHOP, KIOSK, MOBILE_POINT, BANK_BRANCH,
-- PARTNER_INSTITUTION, PARTNER_TENANT, REGIONAL_OFFICE
