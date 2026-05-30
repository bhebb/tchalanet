# Spec — platform.keymanagement / platform.crypto

## ADDED Requirements

### Requirement: Backend signing keys are platform-owned

Backend private signing keys SHALL be owned by a platform capability such as `platform.keymanagement` or `platform.crypto`, not by `core.terminal` or `core.offlinesync`.

#### Scenario: Grant signing
- WHEN `core.offlinesync` needs to sign a grant
- THEN it SHALL call a platform API such as `ServerSigningApi.sign(payload, purpose)`
- AND it SHALL NOT access private key material directly.

### Requirement: Backend public keys are discoverable by POS

The platform SHALL expose backend public signing keys through a safe bootstrap/public-key mechanism.

#### Scenario: POS key bootstrap
- WHEN POS bootstraps
- THEN it SHALL receive or refresh known backend public keys with `keyId`, `algorithm`, and `publicKey`.

### Requirement: Key rotation uses keyId

Signed grants SHALL include `keyId` so the POS can select the right backend public key.

#### Scenario: Rotated server key
- GIVEN two active backend public keys on POS
- WHEN a grant arrives with keyId `server-signing-key-2026-02`
- THEN POS SHALL verify with the matching key.
