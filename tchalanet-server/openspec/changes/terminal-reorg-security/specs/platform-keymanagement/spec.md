# Spec — platform.keymanagement / platform.crypto

## Context

`platform.keymanagement` does not exist today. V1 is created as a minimal, config-backed module. It is the only owner of backend signing key material. No other module (`core.terminal`, `core.offlinesync`, etc.) may sign grants or access private key material directly.

## ADDED Requirements

### Requirement: Backend signing keys are platform-owned

Backend private signing keys SHALL be owned by `platform.keymanagement`, not by `core.terminal` or `core.offlinesync`.

#### Scenario: Grant signing
- WHEN `core.offlinesync` needs to sign a grant
- THEN it SHALL call `platform.keymanagement.api.ServerSigningApi.sign(ServerSigningPurpose, byte[])`
- AND it SHALL NOT inject `Ed25519ServerSigningService` or any implementation class directly.

#### Scenario: No private key exposure
- GIVEN any API endpoint or log
- THEN the backend private signing key SHALL never appear in any response, log, or exception message.

### Requirement: Module structure (V1)

`platform.keymanagement` SHALL expose the following public API package:

```text
platform.keymanagement.api
  ServerSigningApi
    ServerSignatureResult sign(ServerSigningPurpose purpose, byte[] canonicalPayload)

  BackendPublicKeyApi
    BackendPublicKeySetView listActivePublicKeys()

  model/
    ServerSigningPurpose    (enum: OFFLINE_GRANT, …)
    ServerSignatureResult   (String signature, String algorithm, String keyId)
    BackendPublicKeyView    (keyId, algorithm, publicKeyFormat, publicKey, validFrom, validUntil, status)
    BackendPublicKeySetView (activeKeyId, List<BackendPublicKeyView> keys)
```

Internal packages SHALL NOT be imported by other modules.

### Requirement: Config-backed key provider

V1 SHALL load signing keys from environment/secret-injected configuration, not from the database.

Required config (YAML):

```yaml
tch:
  keymanagement:
    server-signing:
      active-key-id: server-signing-key-2026-01
      algorithm: ED25519
      private-key-pkcs8-base64: ${TCH_SERVER_SIGNING_ED25519_PRIVATE_KEY_PKCS8_BASE64}
      public-key-spki-base64: ${TCH_SERVER_SIGNING_ED25519_PUBLIC_KEY_SPKI_BASE64}
```

#### Scenario: Startup validation
- WHEN the application starts
- AND the signing key env vars are absent or blank
- THEN `KeyManagementProperties` SHALL fail fast with a clear configuration error
- AND the application SHALL NOT start in a signing-incapable state.

### Requirement: Backend public keys are discoverable by POS

The platform SHALL expose backend public signing keys through a public endpoint.

Route: `GET /public/security/backend-signing-keys`

- No authentication required (public keys are not secret).
- HTTPS is required.
- POS caches keys and refreshes when it encounters an unknown `keyId` in a grant.

Response shape:

```json
{
  "activeKeyId": "server-signing-key-2026-01",
  "keys": [
    {
      "keyId": "server-signing-key-2026-01",
      "algorithm": "ED25519",
      "publicKeyFormat": "SPKI_BASE64",
      "publicKey": "...",
      "validFrom": "2026-05-30T00:00:00Z",
      "validUntil": null,
      "status": "ACTIVE"
    }
  ]
}
```

#### Scenario: POS key bootstrap
- WHEN POS bootstraps
- THEN it SHALL call `GET /public/security/backend-signing-keys`
- AND receive at least one key entry with `keyId`, `algorithm`, and `publicKey`.

#### Scenario: Key rotation with keyId
- GIVEN two active backend public keys on POS
- WHEN a grant arrives with `keyId` `server-signing-key-2026-02`
- THEN POS SHALL verify with the matching key
- AND SHALL NOT fail because a different `keyId` is active.

### Requirement: Key rotation uses keyId

Signed grants SHALL include `keyId` and `signatureAlgorithm` so the POS can select the correct backend public key.

### Requirement: V1 algorithm

V1 SHALL use Ed25519 (Java 25 native `java.security`). No third-party cryptographic library is needed.

Key encoding:
- private key: PKCS8 base64
- public key: SPKI base64

## Key storage evolution path

```text
V1: env/secret-injected key (this change)
V2: Vault / cloud secrets manager / KMS-backed signer
V3: HSM/KMS signing without private key material in JVM
```
