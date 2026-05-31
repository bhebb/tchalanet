# tasks.md — terminal-device-proof-client

Read this file at the start of each session. Resume at the first unchecked task.

## Progress

- [ ] 01 — `TerminalKeyStore` — abstract interface + `SecureStorageTerminalKeyStore` implementation (flutter_secure_storage, Ed25519 via pointycastle or platform)
- [ ] 02 — activation challenge verify — send `bindingPublicKey` + `publicKeyAlgorithm = "ED25519"` in the verify request
- [ ] 03 — `TerminalSignaturePayloadCanonicalizerV1` — canonical newline payload matching backend V1 format
- [ ] 04 — device proof header injection — `TerminalDeviceProofInterceptor` (or Dio interceptor) adds the 5 headers to all required endpoints
- [ ] 05 — `BackendPublicKeyStore` — bootstrap on login, cache by keyId, refresh on unknown keyId
- [ ] 06 — offline grant verification — verify backend signature before storing grant; reject grants with unknown keyId after refresh
- [ ] 07 — tests: unit tests for canonicalizer + key store + interceptor; integration test against local dev server

## Ordering constraints

- 01 must be done before 02 (needs public key to send during activation).
- 03 must be done before 04 (interceptor uses the canonicalizer).
- 04 depends on 01 + 03.
- 05 is independent of 01–04.
- 06 depends on 05.
- 07 requires 01–06.

## Server contract

See `tchalanet-server/openspec/changes/terminal-reorg-security/follow-up-mobile.md` for:
- canonical payload format and field order
- request headers (`X-Terminal-Id`, `X-Terminal-Binding-Id`, `X-Terminal-Nonce`, `X-Terminal-Signed-At`, `X-Terminal-Signature`)
- endpoints requiring device proof
- offline grant verification steps
- backend public key bootstrap endpoint: `GET /public/security/backend-signing-keys`
