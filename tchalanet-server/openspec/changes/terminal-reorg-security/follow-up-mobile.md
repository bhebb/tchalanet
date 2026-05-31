# Follow-up — Mobile Contract (Flutter)

Mobile implementation is **out of scope** for this server OpenSpec change. This file defines the contract the server side establishes so mobile can implement it consistently.

When mobile work starts, open a linked change at:

```text
tchalanet-mobile/openspec/changes/terminal-device-proof-client/
```

---

## Expected Flutter abstractions

### TerminalKeyStore

```dart
abstract class TerminalKeyStore {
  /// Creates a new Ed25519 keypair for this device installation, or loads the existing one.
  Future<TerminalKeyPairInfo> createOrLoadBindingKey();

  /// Signs the canonical payload string with the device private key.
  /// Returns base64url-encoded signature.
  Future<String> signPayload(String canonicalPayload);

  /// Returns the SPKI base64-encoded public key to send during challenge verification.
  Future<String> getPublicKey();

  /// Returns the algorithm identifier (e.g. "ED25519").
  Future<String> getPublicKeyAlgorithm();
}
```

V1 can use a simple in-memory or secure-storage implementation. Keep the abstraction ready for Android Keystore / iOS Secure Enclave migration.

### BackendPublicKeyStore

```dart
abstract class BackendPublicKeyStore {
  /// Returns the cached backend public key for a given keyId, or null if not found.
  Future<BackendPublicKey?> findByKeyId(String keyId);

  /// Refreshes keys from GET /public/security/backend-signing-keys.
  Future<void> refreshKeys();
}
```

POS strategy:
1. On bootstrap, call `refreshKeys()`.
2. On grant receipt, call `findByKeyId(grant.keyId)`.
3. If null, call `refreshKeys()` and retry once.
4. If still null, reject the grant and surface an error.

---

## Canonical signed payload (POS → backend)

POS signs this exact newline-separated payload using `TerminalKeyStore.signPayload`:

```text
purpose\n
method\n
path\n
bodyHash\n
terminalId\n
bindingId\n
outletId\n
sessionId\n
nonce\n
signedAt
```

Class to implement: `TerminalSignaturePayloadCanonicalizerV1` (Flutter equivalent).

Field order is frozen for V1. Do not reorder fields across POS releases without a matching backend V2 canonicalizer.

Request headers:

```http
X-Terminal-Id: <terminalId>
X-Terminal-Binding-Id: <bindingId>
X-Terminal-Purpose: SELL_TICKET | PAYOUT_CONFIRM | OFFLINE_GRANT_REQUEST | OFFLINE_SYNC
X-Terminal-Nonce: <random unique nonce — UUID or 32-byte hex>
X-Terminal-Signed-At: <Instant ISO-8601>
X-Terminal-Signature: <base64url signature>
```

---

## Offline grant verification (backend → POS)

POS verifies the backend-signed grant before using it offline:

1. Extract `keyId` and `signatureAlgorithm` from the grant response.
2. Call `BackendPublicKeyStore.findByKeyId(keyId)`.
3. Reconstruct the canonical grant payload deterministically.
4. Verify the signature using the backend Ed25519 public key.
5. Check grant not expired (`validUntil > now`).
6. Check `terminalId`, `bindingId`, `outletId`, `sessionId`, `sellerId` match local context.
7. Track local usage against `limits` (maxTickets, maxTotalAmount, maxStakePerTicket).

---

## Endpoints requiring device proof headers (V1)

All paths below are relative to the API base (`/api/v1` added by server config).

| Operation | Endpoint |
|---|---|
| Sell ticket | `POST /tenant/tickets` |
| Payout confirm | `POST /tenant/payouts/{id}/execute` |
| Offline grant request | `POST /tenant/offline/grants` |
| Offline sync | `POST /tenant/offline/sync` |

Heartbeat (`POST /tenant/terminals/{id}/heartbeat`) does NOT require device proof in V1.

## V1 body hash note

V1 canonical payload uses an empty string for `bodyHash`. Sign with `""` as the 4th field. Body hash verification will be added in a follow-up when `ContentCachingFilter` is wired.
