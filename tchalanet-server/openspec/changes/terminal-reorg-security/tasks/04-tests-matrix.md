# Task 04 — Tests matrix

## Unit tests

### Challenge create

- creates POS_PAIRING challenge for active terminal;
- creates MOBILE_OTP challenge for phone terminal;
- defaults POS delivery mode correctly;
- defaults phone delivery mode correctly;
- rejects locked terminal;
- rejects retired terminal;
- stores only code hash;
- sets expiresAt.

### Challenge verify

- valid code creates binding;
- wrong code increments failed attempts;
- too many attempts blocks challenge;
- expired challenge fails;
- consumed challenge fails;
- terminal mismatch fails;
- binding type mismatch fails;
- credential hash computed in handler;
- publicKeyHash computed if publicKey present.

### Device proof

- valid Ed25519 signature returns trusted;
- invalid signature rejected;
- changed body hash rejected;
- wrong purpose rejected;
- reused nonce rejected;
- old signedAt rejected;
- revoked binding rejected;
- locked terminal rejected.

### Offline grant

- valid request issues signed grant;
- invalid device proof rejects grant;
- grant canonical payload stable;
- server signature verifies with public key;
- wrong keyId fails on POS side.

## Controller integration tests

- write endpoints have security and audit;
- invalid request body returns ProblemDetail;
- success returns ApiResponse;
- typed IDs bind correctly;
- CASHIER cannot call admin endpoints;
- TENANT_ADMIN can call admin endpoints with permissions.

## RLS / tenant tests

- tenant A cannot read tenant B terminal;
- tenant A cannot verify tenant B challenge;
- tenant A cannot use tenant B binding;
- superadmin override is explicit and auditable.

## E2E cashier onboarding

1. create tenant;
2. create outlet;
3. create cashier;
4. create terminal;
5. assign outlet;
6. assign user;
7. login cashier;
8. create challenge;
9. verify challenge;
10. heartbeat signed;
11. open session;
12. sell signed;
13. payout signed.

## E2E offline grant

1. POS signs grant request;
2. backend verifies POS signature;
3. backend returns signed grant;
4. POS verifies grant locally;
5. POS sells offline within limit;
6. POS syncs with signed batch;
7. backend validates grant + tickets + no duplicates.
