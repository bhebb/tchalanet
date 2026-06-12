# Tasks: External Auth, Managed Database, and Observability V0

## Status

Proposed

## Goal

Migrate Tchalanet production V0 to:

- Firebase Authentication / Google Identity Platform;
- managed PostgreSQL;
- a provider-neutral identity abstraction;
- OpenTelemetry and Jaeger for development/staging traces.

The migration must not move business authorization outside Tchalanet.

Tchalanet remains the owner of:

- `AppUser`;
- tenant memberships;
- roles and permissions;
- operational context;
- SUPER_ADMIN override;
- RLS context;
- audit.

Firebase remains limited to:

- external authentication;
- ID token issuance;
- external user identity.

## Phase 0: Decision acceptance

### Tasks

- [ ] Confirm Firebase Authentication / Google Identity Platform feature set and billing mode from
      official Google documentation at go-live planning time.
- [ ] Confirm DigitalOcean Managed PostgreSQL region, data residency, latency, cost, connection
      limits, HA, backups, PITR, and maintenance policy.
- [ ] Record the ADR in the canonical central ADR documentation and mark it Accepted.
- [ ] Assign follow-up owners for backend, infra, web, mobile, and POS migration slices.
- [x] Decide and record the V0 `AppUser` bootstrap mode.
- [x] Decide and record the V0 Firebase revocation-check mode.
- [x] Classify sensitive endpoints that require stronger identity and operational-context proof.
- [ ] Record the required proof combination for POS sell, payout, offline grant, and offline sync.
- [ ] Track managed PostgreSQL validation as an independent P0 workstream.
- [ ] Approve identity-verification and revocation-check latency budgets.

### Acceptance criteria

- [ ] ADR has an accepted status.
- [ ] External auth decision is explicit: Firebase in production V0 and local provider for
      development/performance.
- [ ] Managed PostgreSQL provider and fallback path are documented.
- [ ] No implementation phase starts without assigned owners.
- [x] Monetary production defaults to `AppUser` bootstrap mode `deny`.
- [x] V0 defaults to Firebase revocation-check mode `sensitive-only`.
- [x] Sensitive operations and their required proofs are explicitly documented.
- [ ] Managed PostgreSQL validation can proceed independently from the Firebase migration.
- [ ] Identity latency is measured from the start and becomes an acceptance gate before monetary
      production.

## Phase 1: Backend identity abstraction

### Tasks

- [x] Inventory direct Keycloak dependencies in filters, controllers, handlers, configuration,
      modules, and tests.
- [x] Formalize minimal provider-neutral contracts under `platform.identity.api`.
- [x] Introduce `IdentityProviderApi` or an equivalent provider boundary.
- [x] Wrap existing Keycloak verification behind `platform.identity.internal.keycloak`.
- [x] Ensure Firebase, Keycloak, and local provider internals do not leak outside
      `platform.identity.internal`.
- [x] Add architecture tests preventing imports of provider internals outside `platform.identity`.
- [x] Preserve `TchRequestContext` as the only canonical runtime context.
- [x] Preserve the current access-control and permission-evaluation flow.

### Suggested package shape

```text
platform.identity.api
  IdentityProviderApi
  ExternalAuthenticatedUser
  IdentityProviderType
  AppUserBootstrapApi
  AppUserBootstrapResult
platform.identity.internal.keycloak
  KeycloakIdentityProvider
  KeycloakTokenVerifier
platform.identity.internal.firebase
  FirebaseIdentityProvider
  FirebaseTokenVerifier
platform.identity.internal.local
  LocalJwtIdentityProvider
  LocalPerfIdentityProvider
```

### Acceptance criteria

- [ ] Other modules consume only `platform.identity.api`.
- [x] No controller parses Keycloak or Firebase tokens directly.
- [x] No handler depends on provider-specific classes.
- [ ] `TchRequestContext` remains the canonical source for tenant, user, and runtime context.
- [x] Architecture tests fail if provider internals are imported outside the identity module.

## Phase 2: Firebase provider and application mapping

### Tasks

- [x] Implement Firebase token verification.
- [x] Validate issuer.
- [x] Validate audience/project id.
- [x] Validate signature.
- [x] Validate expiry.
- [x] Validate subject.
- [x] Add verifier/JWKS/cache behavior.
- [x] Define revocation and disabled-user policy.
- [x] Implement configurable Firebase revocation-check mode: `off`, `sensitive-only`, or `always`.
- [x] Default Firebase revocation checking to `sensitive-only` for V0.
- [x] Require local active-`AppUser` status for every authenticated request.
- [x] Add tests for expired, malformed, wrong-audience, wrong-issuer, revoked, and disabled-user
      cases.
- [x] Persist durable `(provider, issuer/project-or-tenant, externalSubject)` to `AppUser` mapping.
- [x] Remove provider-specific identity columns from `app_user`; store Keycloak IDs as KEYCLOAK
      external identity subjects.
- [x] Implement policy-controlled `AppUser` bootstrap.
- [x] Implement bootstrap modes: `deny`, `invite-only`, `admin-preprovisioned`, and
      `controlled-auto`.
- [x] Default unknown Firebase users to denied in monetary production.
- [x] Prevent `controlled-auto` in production unless explicitly enabled by an ADR-approved
      allowlist.
- [x] Audit `AppUser` bootstrap and provider-link creation.
- [ ] Prove tenant, role, permission, operational-context, and RLS resolution remain server-owned.
- [ ] Add integration tests for Firebase-authenticated tenant users.
- [ ] Add integration tests for Firebase-authenticated SUPER_ADMIN override.
- [ ] Instrument and measure token verification, revocation check, external-identity mapping,
      bootstrap policy, auth-to-context resolution, and RLS binding.
- [ ] Make JWKS/verifier cache refresh and cold-cache behavior visible in traces.
- [ ] Document Firebase outage behavior and prevent aggressive request-path retries.

### Data model requirement

A durable external identity mapping must exist.

```text
app_user_external_identity
  id
  app_user_id
  provider
  issuer
  external_subject
  email_snapshot
  created_at
  updated_at
  created_by
```

Unique constraint:

```text
(provider, issuer, external_subject)
```

### Acceptance criteria

- [x] Firebase token only proves external identity.
- [x] Tchalanet database maps external identity to `AppUser`.
- [x] Roles and permissions are loaded from Tchalanet, not Firebase claims.
- [ ] Tenant is resolved server-side.
- [ ] RLS receives tenant, user, and scope from Tchalanet context.
- [ ] Disabled/revoked-user policy is tested.
- [x] `AppUser` bootstrap is explicit, controlled, and audited.
- [x] Locally disabled `AppUser` is denied even when the external token is valid.
- [x] Revoked tokens and disabled Firebase users are denied on sensitive endpoints in
      `sensitive-only` mode.
- [ ] Revoked-token behavior is documented for `off` mode.
- [ ] Invalid revocation-check and bootstrap modes fail startup.
- [x] Unknown Firebase user is denied by default.
- [ ] Invitation linking, expired invitation denial, provider-link audit, and bootstrap-denial
      audit are tested.
- [ ] Identity mapping lookup is indexed.

### Firebase risk-tier policy

Standard low-risk reads require:

```text
valid signature + issuer + audience + expiry + subject
active mapped Tchalanet AppUser
normal Tchalanet access-control/context/RLS path
```

Sensitive operations require:

```text
valid Firebase token
active mapped Tchalanet AppUser
Firebase revocation/disabled-user check when mode is sensitive-only or always
normal Tchalanet permission, operational-context, and RLS checks
```

Sensitive operations include:

- [x] sell;
- [x] ticket void/cancel;
- [x] payout request, approve, pay, and confirm;
- [x] offline grant and synchronization;
- [x] SUPER_ADMIN tenant override;
- [x] user, role, and permission changes;
- [x] terminal binding and unlock.

### Bootstrap audit events

- [x] `APP_USER_EXTERNAL_IDENTITY_LINKED`.
- [x] `APP_USER_BOOTSTRAP_DENIED`.
- [x] `APP_USER_BOOTSTRAP_CREATED`.
- [x] `APP_USER_BOOTSTRAP_INVITE_CONSUMED`.

Audit details include provider, issuer/project, safe external-subject reference, safe email snapshot,
actor when available, tenant when applicable, bootstrap mode, decision, and reason code. Raw tokens
must never be audited.

### Identity latency targets

Initial V0 targets, measured during development and enforced before monetary production:

```text
standard token verification p95 <= 25 ms, excluding cold start and JWKS refresh
sensitive revocation check p95 <= 250 ms
identity mapping DB lookup p95 <= 25 ms
full auth-to-context resolution p95 <= 100 ms for standard requests
```

### Required latency checks

- [ ] JWKS/verifier cache is warmed or explicitly tested.
- [ ] Cold JWKS refresh is visible in traces.
- [ ] Revocation-check latency is measured separately.
- [ ] Failure paths do not retry aggressively inside the request path.
- [ ] Firebase outage behavior is documented.

## Phase 2B: Sensitive POS operational proof

### Tasks

- [ ] Require two independent proofs for sensitive POS operations: authenticated actor proof and
      trusted operational-context proof.
- [ ] Require `SIGNED_DEVICE_BINDING`, `SERVER_BOOTSTRAP`, or explicitly allowed
      `ADMIN_SELECTION` as the trusted operational-context source.
- [ ] Ensure sensitive handlers call or rely on `ctx.trustedOperationalContextRequired()` before
      action-specific validation.
- [ ] Preserve fail-fast validation order: trusted operational context, terminal, assignment,
      outlet, session, then action-specific gates.

### Required acceptance tests

- [ ] Valid Firebase token with no operational context causes sell to fail.
- [ ] Valid Firebase token with `CLIENT_CLAIM` operational context causes sell to fail.
- [ ] Valid Firebase token with an unbound terminal causes sell to fail.
- [ ] Valid Firebase token with signed device binding and invalid seller assignment causes sell to
      fail.
- [ ] Valid Firebase token with signed device binding and closed session causes sell to fail.
- [ ] Valid Firebase token with signed device binding and valid outlet, terminal, and session can
      proceed to domain validation.
- [ ] Valid Firebase token without signed binding causes payout confirmation to fail.
- [ ] Valid Firebase token without signed binding causes offline grant to fail.
- [ ] Valid Firebase token without signed binding causes offline sync to fail.

### Acceptance criteria

- [ ] Firebase token alone cannot authorize sell, payout confirmation, offline grant, or offline
      synchronization.
- [ ] Device binding remains mandatory and independent from operator authentication.
- [ ] Operational context proves where the operator acts and never replaces authenticated actor
      proof.

## Phase 3: Local and performance providers

### Tasks

- [x] Implement `local-jwt` through the normal provider-neutral identity and `AppUser` resolution
      path.
- [x] Implement `local-perf` only if needed.
- [x] Document the exact threat boundary of `local-perf`.
- [x] Fail startup when any local/test identity mode is active in production.
- [x] Add tests proving production guards fail closed.
- [x] Replace external-token authorization hints with database-owned roles and effective
      permissions before handlers execute.
- [x] Reject tenant override when a token's `SUPER_ADMIN` hint is not confirmed by the database.
- [x] Add performance fixtures that exercise normal permissions and RLS.
- [ ] Ensure performance mode does not accidentally bypass tenant isolation.

### Configuration

Allowed values:

```yaml
tch:
  identity:
    provider: firebase # firebase | keycloak | local-jwt | local-perf
```

Production guard:

```text
If the effective profile contains prod:
  provider must not be local-jwt
  provider must not be local-perf
```

### Acceptance criteria

- [ ] Local auth works for development, E2E, and performance tests.
- [ ] Local auth creates a normal `TchRequestContext`.
- [ ] Local auth exercises permission checks.
- [ ] Local auth exercises RLS.
- [x] Production startup fails closed if a local/test provider is configured.

## Phase 4: P0 managed PostgreSQL and RLS validation

This is an independent P0 workstream named `P0-managed-postgres-rls-validation`. It proceeds in
parallel with the Firebase migration. Firebase migration cannot be production-ready until this
workstream passes, but neither workstream must block early development of the other.

### Tasks

- [ ] Provision a non-production managed PostgreSQL validation instance.
- [ ] Verify supported PostgreSQL version.
- [ ] Verify required extensions.
- [ ] Verify TLS connectivity.
- [ ] Verify JDBC compatibility.
- [ ] Verify Flyway migration execution.
- [ ] Verify connection-pooling behavior.
- [ ] Verify TLS, JDBC, and Hikari settings together.
- [x] Add focused unit tests proving the datasource bridge applies server context and resets every
      RLS session variable on connection close.
- [ ] Run focused RLS tests.
- [ ] Run transaction-local context tests.
- [ ] Run pool-reuse leakage tests.
- [ ] Configure production backups/PITR.
- [ ] Complete a restore rehearsal before monetary production.
- [ ] Document provider-neutral migration and rollback procedures.

### RLS-specific checks

- [ ] Tenant A cannot read Tenant B rows.
- [ ] Tenant A cannot write Tenant B rows.
- [ ] SUPER_ADMIN override is explicit and audited.
- [ ] Transaction-local `set_config(..., true)` does not leak across pooled connections.
- [ ] Failed transactions do not leave stale RLS context.
- [ ] Batch and scheduler contexts bind tenant/platform context explicitly.

### Acceptance criteria

- [ ] Flyway runs cleanly against managed PostgreSQL.
- [ ] RLS acceptance tests pass.
- [ ] Pool reuse does not leak tenant, user, or scope variables.
- [ ] Backup and restore procedure is rehearsed.
- [ ] Rollback path is documented.
- [ ] The Firebase migration is not marked production-ready until managed PostgreSQL RLS validation
      passes.

## Phase 5: Infra and observability

### Tasks

- [ ] Remove Keycloak from the required production V0 service set.
- [ ] Preserve the Keycloak migration path until backend compatibility is complete.
- [x] Add environment-driven Firebase configuration.
- [ ] Add environment-driven managed PostgreSQL configuration.
- [ ] Add environment-driven Redis configuration.
- [ ] Add environment-driven OpenTelemetry configuration.
- [ ] Add OTEL Collector to development/staging topology.
- [ ] Add Jaeger to development/staging topology.
- [ ] Instrument and verify critical-flow trace coverage.
- [ ] Add identity verification result, revocation-check state, and sanitized failure-code
      attributes without logging secrets.
- [ ] Add identity verification, revocation-check, and external-identity mapping duration/failure
      metrics.
- [ ] Document sampling policy.
- [ ] Document redaction policy.
- [ ] Document cardinality limits.
- [ ] Document secret handling.
- [ ] Document safe context logging.
- [ ] Validate compose/configuration.
- [ ] Update operations documentation.
- [x] Document provider configuration, Firebase Phone onboarding/testing, and local/performance
      setup for new contributors.
- [x] Document Clerk as an evaluated future provider without adding an implementation.

### Minimum trace coverage

- [ ] HTTP request entry.
- [ ] Identity token verification.
- [ ] `AppUser` mapping/bootstrap.
- [ ] Permission evaluation.
- [ ] `TchRequestContext` creation.
- [ ] RLS context binding.
- [ ] Sell-ticket flow.
- [ ] Promotion evaluation.
- [ ] Receipt generation.
- [ ] Draw-result application.
- [ ] Settlement.
- [ ] Payout.
- [ ] Offline grant/synchronization.
- [ ] `identity.verify_token`.
- [ ] `identity.revocation_check`.
- [ ] `identity.map_external_identity`.
- [ ] `identity.bootstrap_policy`.
- [ ] `accesscontrol.resolve_permissions`.
- [ ] `context.bind_tch_request_context`.
- [ ] `rls.bind_session_context`.

### Identity trace attributes

Allowed attributes:

```text
identity.provider=firebase
identity.verify.result=success|failure
identity.revocation_check=true|false
identity.failure_code=expired|issuer|audience|signature|revoked|disabled|mapping_missing|app_user_disabled
```

Required metrics:

```text
identity.verify_token.duration
identity.revocation_check.duration
identity.map_external_identity.duration
identity.verify_token.failure.count
identity.revocation_check.failure.count
```

### Logging and redaction rules

Do not log:

- raw ID tokens;
- refresh tokens;
- Firebase credentials;
- database passwords;
- customer private data;
- full sensitive ticket payloads.

Allowed with care:

- `traceId`;
- `requestId`;
- `tenantId`;
- `appUserId`;
- role;
- scope;
- operation name;
- status;
- duration;
- sanitized error code.

### Acceptance criteria

- [ ] Development/staging traces appear in Jaeger.
- [ ] Sensitive secrets are absent from traces and logs.
- [ ] Sampling is documented.
- [ ] OTEL configuration is environment-driven.
- [ ] Production does not require Keycloak to boot.

## Phase 6: Client migration

### Tasks

- [ ] Define production Firebase token acquisition for web.
- [ ] Define production Firebase token acquisition for mobile.
- [ ] Define production Firebase token acquisition for POS.
- [ ] Define token-refresh behavior.
- [ ] Define logout behavior.
- [ ] Remove direct production Keycloak assumptions after backend compatibility is available.
- [ ] Verify disabled-user behavior.
- [ ] Verify revoked-token behavior.
- [ ] Verify token-refresh behavior.
- [ ] Verify frontend error handling for authentication failure.

### Web acceptance criteria

- [ ] Web obtains a Firebase ID token.
- [ ] Web attaches the token to API calls.
- [ ] Web refreshes the token safely.
- [ ] Web logout clears local authentication state.
- [ ] Web handles HTTP 401/403 consistently.

### Mobile/POS acceptance criteria

- [ ] Mobile/POS obtains a Firebase ID token.
- [ ] Mobile/POS refreshes the token before expiry.
- [ ] POS does not store long-lived secrets insecurely.
- [ ] POS operational context still comes from Tchalanet.
- [ ] POS device binding/signing is not replaced by Firebase token alone.

## Phase 7: Delivery validation

### Tasks

- [x] Run focused backend identity tests.
- [ ] Run access-control tests.
- [ ] Run context tests.
- [ ] Run RLS tests.
- [ ] Run architecture tests.
- [ ] Run managed PostgreSQL Flyway acceptance.
- [ ] Run backup acceptance.
- [ ] Run restore acceptance.
- [ ] Run RLS acceptance on managed PostgreSQL.
- [ ] Prove Jaeger receives sanitized traces for representative critical flows.
- [x] Run OpenSpec strict validation.
- [x] Run `git diff --check`.

### Suggested commands

```shell
./mvnw -pl tchalanet-app -am verify
openspec validate external-auth-managed-postgres-observability-v0 --strict
git diff --check
```

### Acceptance criteria

- [ ] Backend tests pass.
- [ ] Architecture tests pass.
- [ ] Managed PostgreSQL validation passes.
- [ ] RLS validation passes.
- [ ] Restore rehearsal is completed.
- [ ] Jaeger shows sanitized development/staging traces.
- [x] OpenSpec strict validation passes.
- [x] `git diff --check` passes.

## Non-goals

This change does not:

- move Tchalanet authorization into Firebase;
- move tenant permissions into Firebase custom claims;
- replace `TchRequestContext`;
- replace PostgreSQL RLS;
- replace POS device binding/signing;
- introduce Firestore as a source of truth;
- require a full production observability stack such as Grafana, Loki, or Tempo in V0;
- require Keycloak in production V0.

## Final delivery rule

The migration is accepted only while all of the following remain true:

```text
Firebase authenticates.
Tchalanet authorizes.
Tchalanet owns tenant and operational context.
PostgreSQL RLS enforces tenant isolation.
OpenTelemetry observes without leaking secrets.
```

Additionally:

```text
A Firebase token alone is never enough to perform a sensitive POS operation.
A known AppUser alone never bypasses tenant, permission, operational-context, terminal, outlet,
session, or RLS checks.
A trusted operational context alone is never enough without an authenticated and authorized actor.
```
