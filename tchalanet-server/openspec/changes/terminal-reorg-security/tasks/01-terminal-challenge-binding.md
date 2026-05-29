# Task 01 — Terminal challenge and binding

## Goal

Make POS/mobile activation safe and explicit.

## Steps

1. Ensure challenge entity/table has:
   - id
   - tenant_id
   - terminal_id
   - challenge_type
   - delivery_mode
   - channel/delivery_ref if needed
   - code_hash, not clear code
   - expires_at
   - consumed_at
   - failed_attempts
   - max_attempts
   - created_by
   - audit columns

2. Create/confirm secure code generation:
   - `SecureRandom`, 6 digits for V1;
   - short expiry, preferably 5 to 10 minutes;
   - max attempts, preferably 5;
   - hash = sha256/HMAC with tenantId + challengeId + clearCode + server secret.

3. Rework verify command:
   - include `terminalId` from URL;
   - include raw `bindingCredential`, not pre-hashed by controller;
   - include `bindingPublicKey` and `publicKeyAlgorithm` if present;
   - handler computes credentialHash and publicKeyHash.

4. Verify handler must check:
   - challenge exists;
   - tenant matches;
   - terminalId matches URL;
   - type matches expected flow;
   - not expired;
   - not consumed;
   - attempts under max;
   - terminal exists and active/unlocked;
   - code hash matches;
   - then create binding and mark consumed.

5. Terminal binding stores:
   - binding type;
   - state;
   - credential hash;
   - device fingerprint hash;
   - optional public key, algorithm, public key hash.

## Acceptance

- Wrong terminalId cannot verify a challenge.
- Consumed challenge cannot be reused.
- Expired challenge fails.
- Wrong code increments attempts.
- Code is never stored in clear text.
- Controller no longer hashes credential.
