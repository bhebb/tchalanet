# PROCESSUS DE TESTS — Sécurité transactionnelle Web/Mobile/POS

## 1. Objectif

Valider que Tchalanet permet la vente par POS et téléphone sans faille majeure de sécurité transactionnelle.

Les tests doivent prouver :

- aucune vente sans terminal trusted ;
- aucune vente sans session ;
- aucune vente sans permission ;
- aucune vente avec terminal volé/révoqué ;
- aucune double vente sur retry ;
- aucune fuite cross-tenant ;
- audit complet des actions sensibles.

## 2. Jeux de données de test

Créer :

```text
Tenant A
  Admin A
  Seller A1
  Seller A2
  Outlet A1
  POS A1 assigned to Seller A1
  VirtualPhone A1 assigned to Seller A1

Tenant B
  Admin B
  Seller B1
  Outlet B1
  POS B1 assigned to Seller B1
```

Plan/entitlements :

```text
Tenant A: PHONE_SALES_ENABLED = true
Tenant B: PHONE_SALES_ENABLED = false
```

## 3. Tests login web

### WEB-001 Tenant admin login OK

1. Login Angular via Keycloak.
2. Call `GET /tenant/me`.
3. Expect 200.
4. Verify `TchRequestContext` tenant = tenant A.

### WEB-002 Tenant admin cannot access platform ops

1. Login tenant admin.
2. Call platform-only endpoint.
3. Expect 403.

### WEB-003 Super admin override audité

1. Login super admin.
2. Use tenant override.
3. Call tenant scoped read.
4. Expect 200.
5. Verify audit override.

## 4. Tests POS physique

### POS-001 Pairing initial OK

1. Admin creates terminal `PHYSICAL + POS`.
2. Admin assigns terminal to Seller A1 and Outlet A1.
3. Seller logs in on Flutter POS.
4. Pairing challenge is verified.
5. Device binding is created.
6. Terminal status becomes ACTIVE.

### POS-002 Binding missing blocks sale

1. Seller logs in.
2. Call `POST /tenant/tickets` with TerminalId but no binding.
3. Expect 403 or 409 operational_context.untrusted.

### POS-003 Wrong user blocks sale

1. Seller A2 uses POS A1 binding.
2. Attempt sale.
3. Expect deny: terminal not assigned to user.

### POS-004 Revoked terminal blocks sale

1. Admin revokes POS A1.
2. Seller A1 attempts sale.
3. Expect deny: terminal revoked.

### POS-005 Cross-tenant terminal blocked

1. Seller B1 attempts to use POS A1 terminal id.
2. Expect deny / not found under RLS.

## 5. Tests vente téléphone

### PHONE-001 Activation requires entitlement

1. Tenant B has PHONE_SALES_ENABLED=false.
2. Admin B attempts to create/activate `VIRTUAL + MOBILE`.
3. Expect 403/409 plan.capability_missing.

### PHONE-002 Activation OK with entitlement

1. Tenant A has PHONE_SALES_ENABLED=true.
2. Admin A creates `VIRTUAL + MOBILE` for Seller A1.
3. Seller verifies activation code.
4. Virtual binding is created.

### PHONE-003 Phone sale permission required

1. Seller A1 has terminal virtual active but lacks `ticket.sell.phone`.
2. Attempt phone sale.
3. Expect 403.

### PHONE-004 Virtual binding required

1. Seller sends terminalId without virtual binding.
2. Expect operational_context.untrusted.

## 6. Tests session

### SESSION-001 Sale without session denied

1. Seller has active terminal.
2. No open session.
3. Attempt sale.
4. Expect session.required or session.closed.

### SESSION-002 Session terminal mismatch denied

1. Open session for POS A1.
2. Attempt sale with POS A2 or virtual phone terminal.
3. Expect session.terminal_mismatch.

### SESSION-003 Session outlet mismatch denied

1. Open session for Outlet A1.
2. Attempt sale with Outlet A2.
3. Expect session.outlet_mismatch.

## 7. Tests idempotence

### IDEM-001 Missing Idempotency-Key denied

1. Call `POST /tenant/tickets` without key.
2. Expect 400 idempotency.missing.

### IDEM-002 Same key same payload returns same ticket

1. Call sale with key K and payload P.
2. Timeout simulated or retry.
3. Call same key K and payload P again.
4. Expect same ticket id.

### IDEM-003 Same key different payload rejected

1. Call sale with key K and payload P1.
2. Call sale with key K and payload P2.
3. Expect 409 idempotency.payload_mismatch.

## 8. Tests concurrent / multi-vendeur

### CONC-001 Double click same device

1. Send two identical sale requests concurrently with same Idempotency-Key.
2. Expect only one ticket created.

### CONC-002 Two sellers same terminal forbidden

1. Seller A1 and A2 attempt to use the same active terminal.
2. A1 succeeds.
3. A2 denied unless assignment changed by admin and audited.

### CONC-003 Terminal assignment race

1. Sale in progress.
2. Admin revokes terminal concurrently.
3. Handler must re-check terminal status transactionally or with version guard.
4. Expect deterministic result and audit.

## 9. Tests audit

Verify audit records for :

- terminal created ;
- terminal assigned ;
- terminal activated ;
- terminal locked/revoked ;
- phone sales activated ;
- operational context selected by admin ;
- ticket sold ;
- denied sensitive operation where applicable ;
- super admin override.

## 10. Tests RLS

### RLS-001 Tenant isolation

1. Tenant A user attempts to read B terminal id.
2. Expect 404/403, never data leak.

### RLS-002 No tenant from client body

1. Tenant A user sends body containing tenantId=B.
2. Backend ignores/rejects client tenant.
3. Operation remains tenant A or fails.

## 11. Tests Flutter local auth

### FLUTTER-001 Refresh token present

1. App opened.
2. Face ID OK.
3. Refresh token read.
4. Keycloak refresh succeeds.
5. API call succeeds.

### FLUTTER-002 Refresh token missing

1. App opened.
2. Face ID OK.
3. No refresh token.
4. Redirect to Keycloak login.

### FLUTTER-003 Refresh token expired

1. Face ID OK.
2. Refresh token rejected by Keycloak.
3. Local tokens cleared.
4. Redirect to login.

## 12. Definition of Done sécurité

- [ ] All write endpoints have security annotation.
- [ ] All sensitive writes have audit.
- [ ] Sell ticket requires Idempotency-Key.
- [ ] Sell/payout/offline require trusted operational context.
- [ ] POS uses signed binding.
- [ ] Phone sales uses virtual terminal and entitlement.
- [ ] Terminal assignment enforced.
- [ ] Session terminal/outlet/user match enforced.
- [ ] RLS verified with multi-tenant tests.
- [ ] Concurrency tests pass.
- [ ] OpenAPI docs updated.
- [ ] Flutter handles refresh-token-missing flow.
- [ ] Angular admin cannot perform POS sale unless explicit admin POS mode.
