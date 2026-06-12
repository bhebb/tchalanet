# ADR: External Auth, Managed PostgreSQL, and Observability V0

## Status

Proposed

## Decision date

2026-06-12

## Context

### Context packs

- `tchalanet-server/openspec/context/10-non-negotiables.md`
- `tchalanet-server/openspec/context/78-platform-rules.md`
- `tchalanet-infra/openspec/context/01-infra-decisions.md`

### Canonical references

- `tchalanet-server/openspec/specs/platform.identity/spec.md`
- `tchalanet-server/docs/conventions/persistence/persistence.md`
- `tchalanet-infra/docs/operations/OPERATIONS.md`
- `VERSIONS.md`

Tchalanet's critical V0 concerns are ticket sales, RLS tenant isolation, seller sessions, terminals,
draws/results, payouts, offline synchronization, audit, and traceability. Operating production
identity and database servers does not differentiate the product and consumes capacity needed for
those concerns.

The application already owns identity mapping, access control, operational context, and RLS
binding. An external identity provider can answer who authenticated without becoming the owner of
those application decisions.

## Decision

### V0 runtime

| Concern | Production V0 | Local, E2E, performance |
|---|---|---|
| Authentication | Firebase Authentication | local JWT or explicitly enabled local perf provider |
| Database | managed PostgreSQL | PostgreSQL Docker |
| API | Spring Boot container | Spring Boot local/container |
| Cache/coordination | Redis, initially simple | Redis Docker |
| Reverse proxy | Traefik or equivalent | Traefik where required |
| Telemetry | OpenTelemetry enabled | OpenTelemetry optional/enabled by scenario |
| Trace UI | backend-neutral exporter; Collector recommended | OTEL Collector + Jaeger |

Firebase Authentication is authentication only. Tchalanet remains the authorization and context
authority. PostgreSQL remains the business source of truth and final tenant-isolation enforcement.

At decision time, Google Identity Platform publishes a free Tier 1 allowance up to 50,000 MAU.
Pricing, provider features, quotas, SMS/MFA charges, and whether Identity Platform must be enabled
must be revalidated against official Google documentation before go-live; they are not durable
architecture guarantees.

### Identity boundary

```text
client Firebase ID token
  -> platform.identity provider adapter verifies external identity
  -> external subject maps to AppUser
  -> platform.accesscontrol resolves roles and permissions
  -> trusted operational context is resolved
  -> TchRequestContext is created
  -> DB session variables are bound
  -> PostgreSQL RLS enforces tenant isolation
```

The provider-neutral boundary is:

```text
platform.identity.api
  IdentityProviderApi
  ExternalAuthenticatedUser
  AppUserBootstrapApi
  AppUserBootstrapResult

platform.identity.internal.firebase
  FirebaseIdentityProvider
  FirebaseTokenVerifier
  FirebaseIdentityProperties

platform.identity.internal.local
  LocalJwtIdentityProvider
  LocalPerfIdentityProvider

platform.identity.internal.keycloak
  KeycloakIdentityProvider
```

The names above express responsibilities; implementation may refine names to fit existing APIs.
Other modules consume only `platform.identity.api`. Controllers and handlers never parse
provider-specific tokens. Provider adapters must not resolve permissions, terminal assignment,
seller session, or RLS variables.

### External subject mapping

The stable mapping key is `(provider, issuer/project-or-tenant, externalSubject)`, not email.
Email and phone are mutable attributes and must not be used as durable identity keys.
They may be used only as first-login matching proofs for an admin-preprovisioned active user:
verified email, or an E.164 phone number from a signed Firebase token whose
`firebase.sign_in_provider` is `phone`. After that first link, only
`(provider, issuer, externalSubject)` is used.

Bootstrap is policy-controlled and auditable. A valid external token does not by itself authorize
user creation, tenant membership, or access.

Firebase token verification must validate signature, issuer, audience/project, subject, expiry,
and required provider constraints. Revocation/disabled-user behavior and verifier/JWKS caching must
be explicitly configured and tested before production.

### Token policy

External tokens contain minimal identity facts such as subject, email, optional phone,
email-verification state, and provider metadata.

Optional custom claims may contain stable, low-risk hints such as an application user id, default
tenant id, or global-role hint. The backend treats hints as non-authoritative unless independently
validated.

The following are forbidden in external custom claims:

- full permission lists or all memberships;
- outlet, terminal, or seller-session assignments;
- operational context;
- limits, pricing, payout rights, or other business decisions.

### Local and performance identity

Configuration selects one provider:

```yaml
tch:
  identity:
    provider: firebase # firebase | local-jwt | local-perf | keycloak
```

`local-jwt` and `local-perf` are forbidden when the effective environment is production. Startup
must fail closed if production activates either provider or any equivalent test-auth path.

Local providers still map to a normal `AppUser`, resolve access control, create a normal
`TchRequestContext`, and exercise RLS. Any infrastructure-only bypass must be separately named,
explicitly enabled, unavailable in production artifacts/configuration, and excluded from normal
performance acceptance results.

### RLS

The database does not know Firebase or Keycloak. It receives validated Tchalanet context through
transaction-local settings such as:

```sql
select set_config('app.tenant_id', '<tenant_uuid>', true);
select set_config('app.user_id', '<app_user_uuid>', true);
select set_config('app.scope', 'TENANT', true);
select set_config('app.is_super_admin', 'false', true);
```

RLS remains mandatory. Tenant and scope values never come directly from request bodies, client
headers, or unvalidated external claims. Connection-pool reuse must not leak session context;
transaction-local binding and focused leakage tests are required.

### Managed PostgreSQL

DigitalOcean Managed PostgreSQL is the default production V0 choice. Neon and Supabase remain
acceptable for non-production experiments. Cloud SQL or Azure Database for PostgreSQL may be
reconsidered if the hosting strategy standardizes on those clouds.

The final go-live selection requires a recorded check of:

- supported PostgreSQL version and required extensions;
- deployment region, latency, and data-residency constraints;
- TLS and network-access controls;
- connection limits and pooling strategy;
- automatic backups, PITR window, HA/failover expectations, and maintenance policy;
- monitoring/export capability and monthly cost;
- successful Flyway migration, RLS, backup, and restore rehearsal.

Provider-specific database APIs are forbidden in application code. Standard PostgreSQL JDBC and
Flyway remain the portability boundary.

### Redis

Redis may provide L2 cache, short-lived coordination, and later distributed rate limiting. It is
never the source of truth for money, tickets, payouts, RLS, permissions, or audit.

### Observability

OpenTelemetry is introduced early through configuration and instrumentation that does not couple
business modules to a tracing vendor.

Minimum trace coverage:

- HTTP requests;
- external token verification and AppUser bootstrap;
- permission evaluation and RLS context binding;
- database queries;
- ticket sale, promotion evaluation, and receipt generation;
- draw-result application, settlement, and payout;
- offline synchronization.

Telemetry must not record raw tokens, secrets, sensitive personal data, full receipts, or unsafe
high-cardinality tenant/user attributes. Logs may include `traceId`, `requestId`, and safe context
identifiers under a documented redaction/cardinality policy.

Development and staging use OTEL Collector and Jaeger first. Production enables OpenTelemetry with
explicit sampling; Collector deployment and backend destination are operational choices.

## Consequences

### Positive

- Removes production Keycloak operations from the V0 critical path.
- Reduces database operational risk while retaining PostgreSQL and RLS.
- Keeps authorization and business context under Tchalanet control.
- Allows realistic local/performance testing without Firebase availability or latency.
- Establishes traceability before critical flows become difficult to diagnose.

### Negative and trade-offs

- Adds Firebase vendor dependency and a provider-adapter lifecycle.
- Requires careful external-subject mapping and bootstrap policy.
- Requires strong production guards around local identity modes.
- Managed services add recurring cost, regional constraints, and provider due diligence.
- Existing Keycloak-specific backend, web, mobile, and infra assumptions require migration.

## Evaluated alternative: Clerk

Clerk is attractive for B2B onboarding, organizations, invitations, and hosted user-interface
flows. It is not selected for production V0 because Firebase keeps the first mobile/POS and Java
backend migration narrower.

Clerk remains future-compatible through `IdentityProviderApi`. A future adapter would verify Clerk
JWT/session tokens through JWKS and map `(provider, issuer, subject)` to the provider-neutral
external identity mapping. This change does not add a Clerk SDK, adapter, runtime configuration, or
Clerk-specific concept outside the provider boundary.

## Migration

1. Introduce the provider-neutral identity boundary and wrap existing Keycloak behavior.
2. Implement Firebase verification and durable external-subject mapping.
3. Implement guarded local JWT/performance providers through the same application path.
4. Provision and validate managed PostgreSQL; add OTEL Collector and Jaeger to dev/staging.
5. Migrate production clients to Firebase tokens.
6. Decide whether to retain Keycloak for local use or remove it after migration stabilizes.

## Decision validation checkpoints

This ADR cannot move to Accepted until:

- Firebase Authentication versus Identity Platform feature/billing mode is explicitly recorded;
- DigitalOcean region, cost, HA, PITR, connection limits, and restore rehearsal are accepted;
- local-auth production guards and RLS pool-leakage tests are demonstrated;
- telemetry redaction and sampling rules are documented;
- client migration ownership is assigned.
