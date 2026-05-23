# Tchalanet — Transaction Security Test Plan

> Status: NORMATIVE  
> Scope: login, mobile/POS, transaction security, persistence, batch/scheduler  
> Owner: QA / Backend / Security

## 1. Objectif

Ce plan prouve que Tchalanet est viable comme plateforme de vente mobile/POS.

La plateforme doit démontrer :

```text
- un user connecté ne suffit pas pour vendre
- un terminal trusted est obligatoire
- le tenant est isolé par RLS
- les permissions sont centralisées
- les transactions critiques sont idempotentes
- les retries ne doublent rien
- les jobs batch/scheduler sont sûrs
```

## 2. Environnements

Minimum :

```text
local integration with Testcontainers PostgreSQL
Keycloak test realm or mocked JWT for resource server tests
Spring Boot integration tests
Flutter unit/widget tests for local unlock/token flow
Angular route/auth guard tests
```

## 3. Test data minimale

```text
Tenant A
Tenant B
Super admin
Tenant admin A
Seller A1
Seller A2
Seller B1
Outlet A1 active
Outlet A2 suspended
Terminal POS A1 assigned to Seller A1
Terminal POS A2 locked
Virtual Phone Terminal A1 assigned to Seller A1
Sales session open for Seller A1 / Terminal A1 / Outlet A1
Sales session closed for another case
```

## 4. Login Web tests

### WEB-001 Admin valid login

```text
Given tenant admin has valid Keycloak token
When GET /admin/dashboard
Then TchRequestContext is built
And tenant is tenant A
And permissions are evaluated
And response is ApiResponse success
```

### WEB-002 Missing permission

```text
Given user has valid token but lacks permission
When POST /admin/terminals
Then 403 ProblemDetail
And no entity is created
```

### WEB-003 Tenant body injection ignored

```text
Given user from tenant A
When request body contains tenant_id = tenant B
Then backend uses tenant A context
And cannot write/read tenant B
```

## 5. Mobile/POS login tests

### POS-001 First activation success

```text
Given admin created PHYSICAL_POS terminal assigned to Seller A1
When app submits valid pairing challenge
Then device binding is created
And terminal source can become SIGNED_DEVICE_BINDING
And audit entry is written
```

### POS-002 Wrong user tries terminal

```text
Given terminal assigned to Seller A1
When Seller A2 sends valid token with terminal A1 header
Then operational context is refused
And sell is blocked
```

### POS-003 Revoked binding

```text
Given terminal binding is revoked
When app sends binding
Then operational context source is not trusted
And sell returns forbidden
```

### POS-004 Local biometric without refresh token

```text
Given local auth succeeds
And refresh token missing
When app opens
Then app redirects to Keycloak login
```

## 6. Vente téléphone tests

### PHONE-001 Tenant entitlement required

```text
Given tenant does not have PHONE_SALES_ENABLED
When seller activates virtual phone terminal
Then request is refused
```

### PHONE-002 Virtual terminal activation

```text
Given tenant has PHONE_SALES_ENABLED
And terminal assigned to seller
When activation code is valid
Then virtual binding is created
And terminal becomes ACTIVE
```

### PHONE-003 Permission dedicated

```text
Given seller has ticket.sell but not ticket.sell.phone
When seller uses VIRTUAL_PHONE terminal
Then sell is forbidden
```

## 7. Transaction sell tests

### SELL-001 Happy path POS

```text
Given Seller A1 logged in
And terminal binding valid
And outlet active
And session open
And permission ticket.sell
And idempotency key present
When POST /tenant/tickets
Then ticket is created once
And audit is written
And response is ApiResponse success
```

### SELL-002 Missing idempotency key

```text
When POST /tenant/tickets without Idempotency-Key
Then 400 idempotency.missing
And no ticket created
```

### SELL-003 Same key same payload replay

```text
Given first sell succeeded
When same request with same Idempotency-Key is retried
Then same ticket/result is returned
And no duplicate ticket created
```

### SELL-004 Same key different payload conflict

```text
Given first sell used key K with payload A
When request uses key K with payload B
Then 409 idempotency.payload_mismatch
```

### SELL-005 Client claim not trusted

```text
Given user sends X-Terminal-Id but no valid binding
When POST /tenant/tickets
Then trustedOperationalContextRequired fails
```

### SELL-006 Session mismatch

```text
Given session belongs to terminal A
When request uses terminal B
Then sell is refused
```

## 8. RLS tests

### RLS-001 Cross tenant read blocked

```text
Given context tenant A
When query attempts to read tenant B row
Then zero rows visible
```

### RLS-002 Cross tenant write blocked

```text
Given context tenant A
When insert/update with tenant B id is attempted
Then DB rejects by RLS
```

### RLS-003 Super admin override audited

```text
Given super admin override tenant A
When platform action runs as tenant A
Then audit includes override metadata
```

## 9. Permission evaluator tests

```text
- evaluator returns false on deny
- evaluator calls CheckUserPermissions handler/API
- evaluator does not query repositories directly
- controllers protected by annotations
- no manual role checks in controllers
```

## 10. Persistence tests

```text
- Flyway migration applies
- constraints reject duplicate terminal code per tenant
- active terminal assignment uniqueness enforced
- open sales session uniqueness enforced
- optimistic lock conflict handled
- created_at/updated_at are Instant/timestamptz
- soft delete filters default reads
```

## 11. Batch/scheduler tests

```text
- scheduler disabled => no-op
- gate closed => no-op
- generate repeated => no duplicate draws
- open today respects timezone/cutoff
- fetch result does not mutate tenant draw
- apply does not overwrite existing draw_result_id
- settle repeated => no double settlement
- forced operation requires reason
- forced operation audited
```

## 12. Concurrency tests

### CONC-001 Same idempotency key parallel

```text
Launch 10 concurrent POST /tenant/tickets with same key/payload.
Expect exactly one ticket.
Others replay or in_progress/retry safely.
```

### CONC-002 Different keys parallel same session

```text
Launch 20 legitimate sells.
Expect all valid tickets or business limit rejections.
No DB corruption.
```

### CONC-003 Session close while selling

```text
One thread closes session.
Another attempts sell.
Expect either sell before close or reject after close.
Never inconsistent state.
```

## 13. Security abuse suite

Every release must run abuse tests :

```text
- stolen terminalId only
- stale binding
- revoked terminal
- locked terminal
- wrong outlet
- wrong user
- closed session
- cross-tenant token/header mismatch
- missing permission
- missing idempotency
- replay changed payload
```

## 14. CI gates

PR cannot merge if :

```text
- security architecture tests fail
- RLS tests fail
- idempotency tests fail
- migrations fail
- batch idempotence tests fail for touched jobs
- controllers in protected scopes lack method security
```

## 15. Release readiness

Before production :

- [ ] Keycloak clients configured.
- [ ] Access token lifetime decided.
- [ ] Refresh token/session policy documented.
- [ ] Terminal activation tested.
- [ ] Revocation tested.
- [ ] Audit dashboard or query available.
- [ ] RLS enabled in prod DB.
- [ ] Backup/restore tested.
- [ ] Forced ops restricted and audited.
