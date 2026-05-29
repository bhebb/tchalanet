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
   - support Ed25519 first if available in target runtime.

4. Add nonce storage:
   - table `terminal_binding_nonce` or equivalent;
   - unique `(tenant_id, binding_id, nonce)`;
   - TTL/purge scheduled job later.

5. Hook validation into critical handlers:
   - sell ticket;
   - payout confirm;
   - heartbeat/sync-state if configured;
   - offline grant request;
   - offline sync.

## Acceptance

- A signature for one purpose cannot be used for another purpose.
- A signature for one body cannot be used after body mutation.
- A nonce cannot be reused.
- Expired signedAt window fails.
- Revoked/locked binding fails.
- Other domains do not read terminal binding tables directly.
