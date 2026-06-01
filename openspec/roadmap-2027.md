# Decision — Terminal Security MVP + Roadmap

## MVP decision

For the MVP, implement the existing OpenSpec change:

```text
tchalanet-server/openspec/changes/terminal-reorg-security/
```

No new server OpenSpec is required.

The MVP goal is to make POS/mobile device trust explicit and defensible without building a full enterprise key-management system.

## MVP scope

Implement:

```text
1. terminal activation challenge
2. durable terminal binding
3. POS/mobile public key stored on terminal_binding
4. device proof verification with Ed25519
5. nonce + timestamp anti-replay
6. device proof required before sensitive POS actions
7. backend-signed offline grants
8. minimal platform.signing API
9. root tasks.md for OpenSpec pipeline
```

## Naming decision

Use:

```text
platform.signing
```

rather than `platform.keymanagement` for MVP.

Reason: the MVP is not a full KMS. It only provides a stable backend signing API so core modules do not manipulate backend private keys directly.

Minimal shape:

```text
platform.signing
  api/
    ServerSigningApi
    ServerSigningPurpose
    ServerSignatureResult

  internal/
    ConfigBackedEd25519ServerSigningService
    ServerSigningProperties
```

`core.offlinesync` consumes only `ServerSigningApi`.

## Backend signing key storage for MVP

Use Doppler / environment-injected secrets.

MVP configuration:

```yaml
tch:
  signing:
    server:
      active-key-id: server-signing-key-2026-01
      algorithm: ED25519
      private-key-pkcs8-base64: ${TCH_SERVER_SIGNING_ED25519_PRIVATE_KEY_PKCS8_BASE64}
      public-key-spki-base64: ${TCH_SERVER_SIGNING_ED25519_PUBLIC_KEY_SPKI_BASE64}
```

Rules:

```text
- private key is never committed to Git
- private key is never stored in DB
- private key is never logged
- private key is never returned by API
- key is loaded from secret/config at backend startup
```

Doppler is acceptable for MVP as the secret source.

Vault/KMS is not in MVP scope.

## Public key distribution for MVP

Do not implement a public backend-signing-keys endpoint in the MVP unless already required by mobile.

MVP public key distribution can be done by POS/mobile config/provisioning.

However, all signed offline grants must include:

```json
{
  "keyId": "server-signing-key-2026-01",
  "signatureAlgorithm": "Ed25519",
  "signature": "..."
}
```

The POS/mobile `BackendPublicKeyStore` must be able to resolve a public key by `keyId`, even if MVP has only one key.

## Device proof required in MVP

Required:

```text
POST /tenant/tickets
POST /tenant/payouts/{id}/confirm
POST /tenant/offline-grants
POST /tenant/offline-sync
```

Not required in MVP:

```text
POST /tenant/terminals/{id}/heartbeat
```

Conditional/follow-up:

```text
POST /tenant/terminals/{id}/sync-state
```

If `sync-state` only reports liveness, no proof required in MVP.
If it mutates offline counters, grant state, or trust state, require proof.

## Nonce policy MVP

Use:

```text
clockSkewWindow = 5 minutes
nonceRetentionTtl = 65 minutes
```

Verification rules:

```text
- signedAt must be within clockSkewWindow
- nonce must be unique for tenant + binding + purpose
- binding must be active
- terminal must be active/unlocked
- signature must be valid
- purpose must match endpoint
```

Purge expired nonce rows in a follow-up scheduler task if needed.

## Virtual phone activation

Out of scope for MVP.

Do not implement:

```text
VirtualPhoneTerminalActivationController
phone-specific terminal activation UX
phone-specific terminal lifecycle
```

Keep it as roadmap/follow-up.

## Mobile scope

Server MVP must define the mobile contract but should not implement `tchalanet-mobile` code in this server OpenSpec.

Add a follow-up task or linked change:

```text
tchalanet-mobile/openspec/changes/terminal-device-proof-client/
```

Expected mobile work:

```text
TerminalKeyStore
Ed25519 keypair generation/loading
sign canonical payload
BackendPublicKeyStore
verify backend signed offline grant
```

## Root tasks.md required

Add a root `tasks.md` under:

```text
tchalanet-server/openspec/changes/terminal-reorg-security/tasks.md
```

Suggested tasks:

```text
- [ ] 01 terminal binding schema + activation challenge
- [ ] 02 terminal device proof verification
- [ ] 03 platform.signing minimal MVP
- [ ] 04 backend signed offline grant
- [ ] 05 integrate proof into sell / payout / offline grant / offline sync
- [ ] 06 tests + security/ArchUnit checks
- [ ] 07 docs + mobile contract + roadmap
```

## Roadmap after MVP

### V1.1 — Operational hardening

```text
- nonce purge scheduler
- better audit events for activation, binding, proof rejection, grant issue
- admin view for active bindings
- revoke binding action
- failed proof metrics
- clearer ProblemDetail error codes
```

### V1.2 — Public key distribution

```text
- GET /public/security/backend-signing-keys
- POS dynamic refresh of backend public keys
- cache headers
- multiple public keys returned
- unknown keyId recovery flow
```

### V1.3 — Key rotation

```text
- support active + previous backend signing keys
- manual rotation runbook
- key status: ACTIVE, VERIFY_ONLY, RETIRED
- POS accepts multiple backend public keys by keyId
- retire old key after all grants expire
```

### V2 — Better secret/security backend

```text
- replace config-backed signer with Vault Transit or cloud KMS signer
- backend no longer sees private key material
- signing key policy managed outside JVM
- operational key rotation
```

### V2.1 — Device key lifecycle

```text
- terminal_binding_key table
- POS public key rotation
- multiple device keys per binding
- binding recovery flow
- lost/stolen device revocation flow
```

### V2.2 — Device attestation / stronger mobile security

```text
- Android Keystore-backed keys
- iOS Keychain/Secure Enclave where applicable
- device attestation if needed
- stronger anti-tampering checks
```

## Final MVP principle

Do not overbuild.

MVP must guarantee:

```text
POS proves requests with its private key.
Backend verifies POS proof through core.terminal.
Backend signs offline grants through platform.signing.
POS verifies backend grants using backend public key.
Sensitive actions still validate terminal, outlet, seller, session, and business rules.
```

Device proof is not business authorization. It is only device trust.
