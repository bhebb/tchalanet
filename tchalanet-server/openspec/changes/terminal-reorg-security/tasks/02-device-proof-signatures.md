# Task 02 — Device proof signatures POS/mobile -> backend

## Goal

Allow sell, payout, heartbeat, sync, offline grant request, and offline sync to require a signed device proof.

## Recommended headers

```http
X-Terminal-Id: <terminalId>
X-Terminal-Binding-Id: <bindingId>
X-Terminal-Purpose: SELL_TICKET | PAYOUT_CONFIRM | OFFLINE_GRANT_REQUEST | OFFLINE_SYNC | HEARTBEAT | SYNC_STATE
X-Terminal-Nonce: <random unique nonce>
X-Terminal-Signed-At: <Instant ISO-8601>
X-Terminal-Signature: <base64url signature>
```

## Canonical signed payload

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

Use deterministic canonicalization. Do not sign ad-hoc JSON string formats.

## Steps

1. Add public API in `core.terminal.api.query`:
   - `VerifyTerminalDeviceProofQuery`
   - `VerifyTerminalDeviceProofResult`
   - `TerminalProofPurpose`

2. Add application service:
   - `TerminalDeviceProofVerifier`
   - `TerminalSignaturePayloadCanonicalizer`
   - `TerminalNonceReplayGuard`

3. Add technical crypto primitive:
   - `SignatureVerifier` in common/platform crypto;
   - use Java 25 native Ed25519 (`java.security` — no third-party library needed).

4. Add nonce storage — table `terminal_device_nonce`:
   - columns: `tenant_id`, `binding_id`, `nonce`, `purpose`, `signed_at`, `expires_at`;
   - `expires_at = signed_at + 65 minutes`;
   - unique constraint: `(tenant_id, binding_id, purpose, nonce)`;
   - index on `expires_at` for future purge job;
   - purge job is a follow-up task; do not block V1 on it.

5. Hook validation into critical handlers (V1 required):
   - `POST /tenant/tickets` — sell ticket;
   - `POST /tenant/payouts/{id}/confirm` — payout confirm;
   - `POST /tenant/offline-grants` — offline grant request;
   - `POST /tenant/offline-sync` — offline sync submission.

   Not required in V1:
   - `POST /tenant/terminals/{id}/heartbeat`.

   Deferred (conditional):
   - `POST /tenant/terminals/{id}/sync-state` — add device proof only if this endpoint mutates grant or offline state.

## Acceptance

- A signature for one purpose cannot be used for another purpose.
- A signature for one body cannot be used after body mutation.
- A nonce cannot be reused.
- Expired signedAt window fails.
- Revoked/locked binding fails.
- Other domains do not read terminal binding tables directly.
