# Proposal — Terminal Device Proof Client (Flutter)

## Why

The backend now requires POS/mobile apps to sign sensitive requests with a device-side Ed25519 private key. Without the device proof headers, the backend rejects calls to sell, payout confirm, offline grant request, and offline sync.

This change implements the Flutter-side contract defined in:
`tchalanet-server/openspec/changes/terminal-reorg-security/follow-up-mobile.md`

## What

1. Generate and persist an Ed25519 keypair per device installation.
2. Send the public key to the backend during activation challenge verification.
3. Sign each sensitive request with the canonical payload format.
4. Bootstrap and cache backend public signing keys.
5. Verify backend-signed offline grants before using them offline.

## Design principles

- `TerminalKeyStore` abstracts key generation and signing — V1 uses `flutter_secure_storage`; the abstraction is ready for Android Keystore / iOS Secure Enclave.
- `BackendPublicKeyStore` caches backend keys by `keyId`; refreshes on unknown keyId.
- Canonical payload is built by a dedicated `TerminalSignaturePayloadCanonicalizerV1` — field order is frozen and must match the backend exactly.
- `bodyHash` = `""` in V1 (backend accepts empty string; body hash verification deferred).

## Non-goals

- Android Keystore / iOS Secure Enclave hardware binding (V2).
- Key rotation UI.
- Multi-device concurrent binding.
- Offline grant limit engine beyond simple counter checks.
