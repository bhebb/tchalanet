# Proposal — Reorganize Terminal + Strengthen POS/Mobile Device Proof

## Why

`core.terminal` currently mixes several concerns in controllers and lacks a complete security story for POS/mobile binding, request signatures, and offline grants. The system needs a clear lifecycle:

- admin creates/configures a terminal;
- POS/mobile activates through a challenge;
- activation creates a durable binding;
- POS/mobile signs sensitive requests;
- backend verifies device proof before `sell`, `payout`, `offline grant`, and `offline sync`;
- backend signs offline grants so the POS can verify them offline.

## Design principles

- `core.terminal` owns terminals, terminal bindings, public keys of POS/mobile devices, nonces, and device proof validation.
- `core.sales`, `core.payout`, and `core.offlinesync` do not read terminal tables directly. They ask `core.terminal` through stable query/command APIs.
- The POS signs requests with its private key; backend verifies with the POS public key stored on `terminal_binding`.
- The backend signs grants with a backend private key; POS verifies with the backend public key.
- Do not encrypt application payloads in V1 unless there is a specific requirement. HTTPS + signatures + nonce + idempotency is enough for V1.
- Controllers remain thin: validation, context, bus dispatch, response mapping, security/audit annotations only.

## Non-goals

- Full HSM/KMS implementation.
- Full device attestation.
- Public key rotation UI.
- Complex offline risk engine.
- Multi-key grant revocation protocol beyond short TTL and keyId.
- `VirtualPhoneTerminalActivationController` or phone-specific activation UX.
- Mobile (Flutter) implementation — see `follow-up-mobile.md` for the expected client contract.

## Deepened — 2026-05-30

### Decisions

**Q1 — `platform.keymanagement` existence**
Does not exist today. V1 is created as a minimal config-backed capability. Owns `ServerSigningApi`, `BackendPublicKeyApi`, and `BackendPublicKeysController`. See `design.md §7`.

**Q2 — Backend private key storage**
V1: env/secret-injected (PKCS8 base64). Never stored in DB, never committed to the repo.
Config key: `TCH_SERVER_SIGNING_ED25519_PRIVATE_KEY_PKCS8_BASE64`.
Evolution path: V2 → Vault/cloud secrets manager; V3 → HSM/KMS signing without key material in JVM.

**Q3 — Nonce TTL policy**
Clock skew window = 5 minutes. Nonce retention TTL = 65 minutes.
Table: `terminal_device_nonce`. Unique: `(tenant_id, binding_id, purpose, nonce)`.
Purge condition: `expires_at < now`. Purge job is a follow-up task.

**Q4 — `VirtualPhoneTerminalActivationController`**
Out of scope for V1. Added to non-goals. Open a separate OpenSpec when phone activation is needed.

**Q5 — Java version**
Runtime target is Java 25. Ed25519 native support confirmed. No third-party crypto library needed for signatures.

**Q6 — Backend public keys endpoint**
`GET /public/security/backend-signing-keys` — owned by `platform.keymanagement`. No auth required.
POS caches keys; refreshes when grant verification encounters an unknown `keyId`.

**Q7 — Mobile scope**
Option B: `follow-up-mobile.md` defines the expected Flutter contract. Mobile implementation is out of scope for this server change.

**Q8 — Device proof endpoint scope**
Required in V1: `POST /tenant/tickets`, `POST /tenant/payouts/{id}/confirm`, `POST /tenant/offline-grants`, `POST /tenant/offline-sync`.
Not required in V1: `POST /tenant/terminals/{id}/heartbeat`.
Deferred: `POST /tenant/terminals/{id}/sync-state` — required only if it mutates grant or offline state.

### Assumptions promoted to decisions

- Canonical payload is versioned: `TerminalSignaturePayloadCanonicalizerV1`. Field order is frozen in V1.
- `terminal_binding` keeps public-key columns directly (no child table in V1).
- Project is pre-go-live; migration policy follows: rewrite only if never applied outside local dev; otherwise create a new Flyway migration.
