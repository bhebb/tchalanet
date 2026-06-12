# Change: External Auth, Managed PostgreSQL, and Observability V0

## Status

Proposed

## Why

Tchalanet must deliver V0/V1 without spending the team's limited operational capacity on an
identity server, production PostgreSQL maintenance, or a large observability stack.

The current Keycloak-oriented runtime is useful for local development, but operating Keycloak in
production adds upgrades, security maintenance, realm backup, SMTP/password flows, availability,
and monitoring work. Self-managed PostgreSQL creates similar risk for a monetary system whose
tenant isolation and source-of-truth data depend on PostgreSQL RLS.

Tchalanet must externalize authentication and database operations without externalizing
authorization, operational context, tenant isolation, or business data ownership.

## What changes

This cross-project change adopts the following V0 target:

- Firebase Authentication is the production external identity provider.
- `platform.identity.api` becomes the provider-neutral backend boundary.
- Tchalanet remains the owner of `AppUser`, memberships, roles, permissions, operational context,
  audit, and the canonical `TchRequestContext`.
- PostgreSQL remains the source of truth and RLS remains mandatory.
- Production uses managed PostgreSQL, with DigitalOcean Managed PostgreSQL as the default V0
  selection subject to a go-live validation checkpoint.
- Local, E2E, and performance environments use Docker PostgreSQL and a local identity provider.
- Redis remains a non-authoritative cache and coordination service.
- OpenTelemetry is introduced as the observability backbone, with Collector and Jaeger first in
  development and staging.
- Existing Keycloak integration is wrapped behind the provider boundary during migration and may
  later be retained only for local use or removed.

## Impact

### Backend slice

- Formalize provider-neutral identity contracts and provider-specific internal adapters.
- Preserve the existing access-control, `TchRequestContext`, method-security, and RLS paths.
- Add production startup guards against local identity providers.
- Add traces around identity, authorization, RLS binding, and critical business flows.

### Infra slice

- Separate local Docker dependencies from the production managed PostgreSQL topology.
- Add environment-driven Firebase, PostgreSQL, Redis, and OpenTelemetry configuration.
- Add OTEL Collector and Jaeger for development/staging.
- Update deployment, secret, backup, restore-test, and operational documentation.

### Client follow-up

Web, mobile, and POS clients will obtain Firebase ID tokens for production calls. Their migration
is a follow-up slice and does not change the backend authorization model.

## Non-goals

- Moving authorization or business data into Firebase custom claims or Firestore.
- Replacing PostgreSQL RLS with application-only tenant filtering.
- Making Redis authoritative for tickets, money, payouts, or security context.
- Selecting a full long-term metrics/logs/traces platform.
- Implementing every provider or infrastructure change in this ADR/spec change.
- Keeping Keycloak as a required production V0 service.

