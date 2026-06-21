-- Remove the Firebase-only provider constraint so the table is provider-neutral.
-- Application-level validation via IdentityProviderType enum replaces the DB check.
-- Verify constraint name first if needed:
--   SELECT conname FROM pg_constraint WHERE conrelid = 'seller_terminal_external_identity'::regclass;

ALTER TABLE seller_terminal_external_identity
    DROP CONSTRAINT IF EXISTS chk_seller_terminal_external_identity__provider;
