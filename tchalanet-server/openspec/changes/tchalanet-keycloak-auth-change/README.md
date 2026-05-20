# OpenSpec Change — align-keycloak-infra-auth-identity-web-mobile

## Goal

Stabilize the end-to-end identity and authentication foundation for Tchalanet:

```text
Docker infra -> Keycloak prod-ready -> token contract -> realm base/overlays
-> platform.identity admin/current-user -> web auth flow -> Flutter mobile auth flow
-> POS offline branchlet prepared.
```

## Non-goals

This change does **not** implement full offline sales, offline sync, settlement, or final offline ticket acceptance rules. It only prepares the mobile/POS auth branchlet for offline grant visibility and future grant flows.

## Specs included

```text
specs/
  infra-docker-runtime/
  local-domain-routing/
  local-dev-manjaro-setup/
  keycloak-prod-ready-config/
  keycloak-token-contract/
  keycloak-local-users/
  platform-identity-admin-users/
  platform-identity-current-profile/
  web-auth-flow/
  mobile-auth-flow/
  mobile-offline-branchlet/
```

## Architecture decision summary

- Tchalanet remains the business-facing user management UI.
- Keycloak is transparent to tenant admins and business users.
- Tenant admins create users in Tchalanet; Tchalanet provisions/synchronizes Keycloak.
- `platform.identity` owns current-user profile, app user identity, Keycloak sync, and identity administration surfaces.
- `platform.accesscontrol` owns fine-grained permissions.
- Keycloak owns authentication, OIDC token issuance, password/MFA/session flows.
- POS operational context is never encoded as a token truth. Terminal/outlet/session/offline grant remain Tchalanet-side validated contexts.

## Recommended change id

```text
align-keycloak-infra-auth-identity-web-mobile
```
