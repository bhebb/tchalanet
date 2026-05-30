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
