# Task 03 — Backend-signed offline grants

## Goal

POS can verify offline that a grant was issued by Tchalanet and was not modified.

## Direction of trust

```text
POS -> Backend:
  POS signs with POS private key.
  Backend verifies with POS public key in terminal_binding.

Backend -> POS:
  Backend signs with backend private key.
  POS verifies with backend public key.
```

The backend does not sign with the POS public key. Public keys verify signatures; private keys produce signatures.

## Prerequisite

Task 03 (`platform.keymanagement` V1) must be complete before implementing grant signing. `core.offlinesync` must not sign grants itself.

## Steps

1. Confirm `platform.keymanagement` V1 is done (see `tasks.md` task 03):
   - `platform.keymanagement.api.ServerSigningApi` exists;
   - `Ed25519ServerSigningService` is wired with config-backed key;
   - `GET /public/security/backend-signing-keys` endpoint is live;
   - startup fails fast if signing key env vars are absent.

2. Call `ServerSigningApi.sign(ServerSigningPurpose.OFFLINE_GRANT, canonicalPayload)` from `core.offlinesync` handler — do NOT inject `Ed25519ServerSigningService` directly.

3. Offline grant payload must include:
   - type/version;
   - grantId;
   - tenantId;
   - terminalId;
   - bindingId;
   - outletId;
   - sellerId;
   - sessionId;
   - businessDate;
   - validFrom/validUntil;
   - allowedPurposes;
   - limits;
   - issuedAt;
   - issuer;
   - keyId.

4. POS stores (mobile contract — see `follow-up-mobile.md`):
   - signed grant;
   - backend public key(s) by keyId;
   - local usage counters.

5. During offline sell, POS verifies:
   - backend signature;
   - grant not expired;
   - terminal/binding/outlet/session/seller match;
   - limits not exceeded.

6. During sync, backend revalidates:
   - grant signature;
   - grant scope;
   - submitted tickets within grant limits;
   - no duplicate ticket/client ids;
   - draw/cutoff/business rules.

## Acceptance

- POS rejects grant with invalid signature.
- POS rejects grant for a different terminal/binding.
- Backend rejects offline submissions outside grant limits.
- Grant contains `keyId` and `signatureAlgorithm`.
