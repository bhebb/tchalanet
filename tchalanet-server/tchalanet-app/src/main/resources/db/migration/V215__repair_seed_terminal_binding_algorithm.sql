-- V215: Set public_key_algorithm = 'ED25519' for seeded dev bindings where
-- binding_public_key is a non-null placeholder but public_key_algorithm was omitted.
-- TerminalDeviceBinding domain model requires publicKeyAlgorithm when bindingPublicKey != null.

UPDATE terminal_binding
SET    public_key_algorithm = 'ED25519',
       updated_at = now()
WHERE  binding_public_key IS NOT NULL
  AND  public_key_algorithm IS NULL;
