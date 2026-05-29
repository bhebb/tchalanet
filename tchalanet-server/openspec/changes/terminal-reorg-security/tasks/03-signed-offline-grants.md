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

## Steps

1. Introduce platform capability:
   - `platform.keymanagement.api.ServerSigningApi`
   - `SignedPayload sign(ServerSigningPurpose purpose, byte[] canonicalPayload)`
   - `List<ServerPublicKeyView> listActivePublicKeys()`

2. Offline grant payload must include:
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

3. `core.offlinesync` creates the grant and asks platform signing API to sign canonical payload.

4. POS stores:
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
