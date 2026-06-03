# Auth Convention

> Status: DRAFT v0.1  
> Scope: Keycloak/OIDC, session, guards, auth headers

## Rule

Auth is a platform capability. Components and feature pages consume session state and guards; they do not read Keycloak tokens directly.

## Placement

```text
apps/tch-portal/src/app/core/auth/
```

Use this area for:

- Keycloak bootstrap and login/logout commands;
- token refresh and auth interceptor integration;
- `UserSession` mapping from token claims;
- auth and role guards;
- session-facing helpers such as `hasRole`.

Do not put tenant business rules, seller operational validation, or PageModel permissions here.

## Session Contract

The frontend session is derived from claims and remains a view model:

```text
authenticated
userId
username
displayName
tenantId / tenantCode
roles
tokenExpiresAt
```

Roles are normalized at the frontend boundary. Components should not parse raw claim paths.

## Guards

Use two layers:

- `AuthGuard`: blocks anonymous access.
- `RoleGuard`: blocks authenticated users without the required surface role.

Route data declares the required role/surface. The guard reads session state; it does not call backend APIs during navigation.

## HTTP Auth Header

The auth interceptor attaches `Authorization: Bearer <token>` only for application API calls.

It must not attach tokens to:

- local assets;
- external public URLs;
- Keycloak token/login endpoints;
- absolute URLs outside approved API hosts.

## Seller / Cashier Context

Seller operational context is not auth. It belongs to cashier/POS runtime state and is sent through dedicated operational headers only for cashier flows.

For real sale operations, validation happens at operation time. Ordinary navigation or read calls should not force database validation on every request.

## Anti-Patterns

Do not:

- inject Keycloak into components;
- parse JWTs in pages;
- call login/logout from arbitrary feature components without a core auth command;
- use roles as business permissions when the backend has a stronger authorization rule;
- send sensitive override headers from normal tenant/cashier clients.
