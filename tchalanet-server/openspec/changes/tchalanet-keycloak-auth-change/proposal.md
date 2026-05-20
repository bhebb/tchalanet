# Proposal — Align Keycloak, Infra, Auth, Identity, Web, and Mobile

## Status

Draft for implementation.

## Change id

```text
align-keycloak-infra-auth-identity-web-mobile
```

## Problem

Tchalanet is ready to start Web/Mobile authentication and POS mobile foundation, but the identity stack must be stabilized first:

- Docker infra must be reproducible.
- Keycloak must be prod-ready and not only local-dev friendly.
- Token claims must be explicit and stable.
- Realm base/overlays must separate local users from production config.
- Tenant admins need a Tchalanet UI/API to create users, with Keycloak provisioned transparently.
- Current-user profile endpoints must be tenant-scoped and shared by Web/Mobile.
- Flutter/Web auth flows need the same API/profile contract.
- Offline POS branchlet should be prepared without implementing full offline sales yet.

## Decision

Use Tchalanet as the source of business intent for user administration.

```text
Tenant Admin UI
  -> platform.identity admin API
  -> Tchalanet user/profile/membership state
  -> Keycloak Admin API provisioning/sync
  -> Keycloak token/login remains transparent
```

Keycloak remains the OIDC provider, password/session/MFA system, and token issuer.

## Scope

Included:

```text
infra docker image/config updates
local domain routing
Manjaro local setup
Keycloak prod-ready config
Keycloak token contract
Keycloak local users
platform.identity admin users cleanup
platform.identity current profile cleanup
Web auth flow
Flutter mobile auth flow
mobile offline branchlet
```

Excluded:

```text
full offline sales
full offline sync
offline ticket acceptance/rejection implementation
settlement/offline payout rules
```

## Acceptance summary

- Local Docker stack starts with Postgres, Redis, Traefik, Keycloak.
- Keycloak provider is loaded.
- Realm local imports with dev users.
- Prod overlay has no demo users.
- Tokens include stable claims including `sub`, names, locale, roles, and `tch` claim.
- Swagger OAuth works.
- `/tenant/me/profile` works for Web/Mobile.
- `/admin/identity/users` is the canonical tenant-admin user management API.
- Web login/profile flow works.
- Flutter login/profile/POS bootstrap flow works.
- Offline branchlet exists and displays explicit seller confirmation when offline.
