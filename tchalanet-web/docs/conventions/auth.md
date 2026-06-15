# Auth Convention

> Status: DRAFT v0.1  
> Scope: identity providers, application session, guards, auth headers

## Rule

Auth is a platform capability. Components and feature pages consume session state and guards; they
do not read provider tokens directly.

## Placement

```text
apps/tch-portal/src/app/core/auth/                  neutral session, commands, guards, bearer
apps/tch-portal/src/app/core/auth/{provider}/       provider SDK adapter
```

Use this area for:

- provider-neutral login/logout commands;
- token refresh and auth interceptor integration;
- `UserSession` mapping from the private backend runtime;
- auth and role guards;
- session-facing helpers such as `hasRole`.

Do not put tenant business rules, seller operational validation, or PageModel permissions here.

The composition root selects the identity-provider adapter. Core auth orchestration depends only on
`AuthClient`; provider SDK imports stay inside the provider adapter and composition root.

## Session Contract

The frontend session is a view model derived from `/runtime/private`. Provider tokens establish
authentication and supply bearer credentials only; they are not authoritative for application
identity, tenant context, roles, or permissions.

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

The auth interceptor attaches `Authorization: Bearer <token>` only for application API calls
matched by the shared application API pattern.

It must not attach tokens to:

- local assets;
- external public URLs;
- identity-provider token/login endpoints;
- absolute URLs outside approved API hosts.

## Seller / Cashier Context

Seller operational context is not auth. It belongs to cashier/POS runtime state and is sent through dedicated operational headers only for cashier flows.

For real sale operations, validation happens at operation time. Ordinary navigation or read calls should not force database validation on every request.

## Anti-Patterns

Do not:

- inject an identity-provider SDK into components or core session orchestration;
- parse JWTs in pages;
- call login/logout from arbitrary feature components without a core auth command;
- use roles as business permissions when the backend has a stronger authorization rule;
- send sensitive override headers from normal tenant/cashier clients.
