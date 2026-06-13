# Change: Firebase cutover and Keycloak cleanup V1

## Status

Proposed

## Context

The web client can obtain a Firebase ID token and send it to the backend. The
backend already contains provider-neutral identity contracts, Firebase token
verification, external identity mappings, and local providers.

Keycloak nevertheless remains embedded in the standard runtime:

- infrastructure compose files and Make targets still start or require Keycloak;
- server configuration still contains Keycloak issuer, JWKS, bootstrap, Swagger,
  provisioning, synchronization, and public-model assumptions;
- the web application still contains Keycloak dependencies, imports, runtime
  paths, tests, and comments;
- the mobile application must adopt Firebase authentication without duplicating
  backend authorization rules;
- published documentation still needs a single provider-neutral onboarding and
  operations reference;
- a local Firebase service-account JSON file is present in the server workspace
  and must never become a committed secret.

Keeping two production authentication paths creates ambiguous ownership,
unnecessary operational cost, and a larger security surface.

This change follows
`external-auth-managed-postgres-observability-v0` and completes its Firebase
production cutover.

## Goal

Make Firebase the only required external authentication provider for the
standard production and staging runtime, while preserving:

- Tchalanet-owned authorization, memberships, roles, permissions, and audit;
- `TchRequestContext` as the canonical runtime context;
- PostgreSQL RLS as the final tenant-isolation layer;
- `local-jwt` for deterministic local, E2E, and performance testing;
- provider-neutral identity contracts that allow a future provider to be added
  without keeping unused Keycloak code.

## Proposed changes

### Infrastructure

- Remove Keycloak from the standard production, staging, and local runtime
  topology.
- Remove the API dependency on Keycloak health and Keycloak environment
  variables.
- Make Firebase configuration environment-driven for production/staging.
- Add Firebase Auth Emulator to the local infrastructure for functional web,
  mobile, onboarding, and backend integration tests.
- Make local IDE mode switch explicitly between Firebase Auth Emulator and the
  real Firebase staging instance.
- Keep `local-jwt` for deterministic backend E2E and performance execution.
- Delete Keycloak compose services, images, scripts, databases, variables, and
  runtime targets.

### Server

- Remove Keycloak Spring Resource Server issuer/JWKS configuration and make
  Firebase boot independently from Keycloak configuration.
- Remove Keycloak-specific types from provider-neutral APIs and domain models.
- Replace Keycloak user provisioning and role synchronization with
  Tchalanet-owned AppUser and membership provisioning.
- Delete Keycloak synchronization endpoints, listeners, services, jobs, and
  configuration.
- Remove Keycloak adapters, dependencies, tests, and provider-specific identity
  mappings from the active implementation.
- Remove the local absolute Firebase credentials default and require secrets to
  be injected safely.
- Support Firebase Auth Emulator tokens only when an explicit non-production
  emulator mode is active.
- Change OpenAPI authentication to bearer-token input rather than a
  Keycloak-specific OAuth flow.

### Web

- Complete Firebase authentication and session handling.
- Allow local development to target either Firebase Auth Emulator or the real
  Firebase staging instance through environment configuration.
- Attach Firebase ID tokens only to Tchalanet API requests.
- Remove Keycloak libraries, configuration helpers, imports, comments, and
  tests.
- Handle token refresh, logout, invalid/revoked tokens, and backend 401/403
  responses consistently.

### Mobile

- Integrate Firebase authentication using the platform-supported Flutter
  Firebase SDK.
- Allow local development builds to target either Firebase Auth Emulator or the
  real Firebase staging instance through environment configuration.
- Attach current Firebase ID tokens only to Tchalanet API requests.
- Preserve Tchalanet-owned roles, tenant context, operational context, and
  offline behavior.
- Handle refresh, logout, revoked/disabled identities, and 401/403 responses.

### Documentation

- Publish provider configuration, onboarding/linking, local development,
- credential management, and troubleshooting guidance.
- Remove Keycloak setup from standard production, staging, web, and mobile
  documentation.
- Update both `tchalanet-server/docs` and the published `tchalanet-docs` portal.

## Non-goals

- Moving authorization or permission lists into Firebase claims.
- Replacing `TchRequestContext` or PostgreSQL RLS.
- Using Firestore as a source of truth.
- Replacing POS device binding or operational context.
- Completing POS Firebase migration in this change.
- Changing managed PostgreSQL or observability architecture.

## Risks

- Removing Keycloak provisioning before the Firebase onboarding/linking policy
  is complete could prevent new users from signing in.
- A broadly scoped web interceptor could leak Firebase tokens to third-party
  URLs.
- A committed service-account credential would require immediate rotation.

## Success criteria

- Standard production and staging runtime boots without Keycloak.
- A Firebase ID token resolves to an AppUser, memberships, permissions,
  `TchRequestContext`, and RLS context.
- Local/E2E authentication follows the same authorization and RLS path.
- No standard server, web, or mobile runtime code depends on Keycloak-specific
  types or configuration.
- No executable Keycloak synchronization path remains.
- Keycloak is absent from executable code, dependencies, configuration, infra,
  tests, and current documentation.
- Provider-neutral identity contracts remain available for future providers.
- No Firebase private credential is committed or embedded in application
  configuration.
- Firebase Auth Emulator mode is impossible in production.
- Published documentation describes the supported Firebase and local-jwt
  configurations without presenting Keycloak as a standard runtime option.

## Context packs

- `openspec/context/00-index.md`
- `openspec/context/05-version-guard.md`
- `tchalanet-server/openspec/context/10-non-negotiables.md`
