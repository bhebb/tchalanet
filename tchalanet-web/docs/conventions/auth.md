# Auth Convention

> Status: DRAFT v0.1
> Scope: identity providers, application session, guards, auth headers
> Flow détaillé (topologie 3 apps, LoginPage, restauration de session, interceptor, logout) :
> [`../auth-flow.md`](../auth-flow.md) — ce fichier-ci ne porte que les règles.

## Rule

Auth is a platform capability. Components and feature pages consume session state and guards; they
do not read provider tokens directly.

## Placement

```text
libs/core/auth/src/lib/                  neutral session, commands, guards, bearer
libs/core/auth/src/lib/{provider}/       provider SDK adapter
apps/<portal>/src/app/app.config.ts      composition root selecting the provider adapter
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

Apps consume auth through `@tch/core/auth`; they must not import another app's `core/auth` files
directly.

## Session Contract

The frontend session is a view model derived from the private runtime bootstrap
(`/tenant/runtime/bootstrap`). Provider tokens establish authentication and supply bearer
credentials only; they are not authoritative for application identity, tenant context, roles, or
permissions.

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

## Cross-Origin Portal Handoff

The three web apps may run on different origins:

```text
public-portal    http://localhost:4200
platform-portal  http://localhost:4202
admin-portal     http://localhost:4302
```

Browser storage is origin-bound, so a Firebase/local session established on the public portal does
not automatically exist on platform/admin. Cross-origin navigation must use a backend handoff, not
tokens in URLs and not shared browser storage.

### Portal URL Source

`TchRuntimeConfig.portalBaseUrls` is the web-side contract for inter-portal navigation.

Runtime config JSON may provide local defaults, but authenticated private apps should prefer the
server-provided `/runtime/private` value:

```json
{
  "portalConfig": {
    "portalBaseUrls": {
      "admin-portal": "http://localhost:4302",
      "platform-portal": "http://localhost:4202"
    }
  }
}
```

The backend value is sourced from:

```yaml
tch.portal-auth-handoff.portal-base-urls.admin
tch.portal-auth-handoff.portal-base-urls.platform
```

Do not hardcode `/admin` or `/platform` in feature code. Relative portal URLs are only valid when a
deployment intentionally serves all apps behind the same origin/reverse proxy. In local multi-port
development, relative URLs cause broken redirects such as
`http://localhost:4200/platform/app/platform` or `http://localhost:4202/admin/app/admin`.

### Public Login To Private Portal

After public login, `AuthRedirectService` resolves the target app from the backend session:

- `SUPER_ADMIN` or `/app/platform` -> `platform-portal`;
- `TENANT_OWNER` / `TENANT_ADMIN` or `/app/admin` -> `admin-portal`;
- account/profile routes map to their owning private portal.

If the target portal has the same origin, navigate directly:

```text
<targetBaseUrl><entryRoute>
```

If the target portal is cross-origin, create a one-time handoff:

```text
POST /platform/auth/portal-handoffs
{
  "targetPortal": "PLATFORM" | "ADMIN",
  "entryRoute": "/app/platform" | "/app/admin" | ...
}
```

Then redirect to:

```text
<targetUrl>/login/handoff#code=<handoffId>.<code>
```

The receiving app's `/login/handoff` route must be registered before private/auth guards. It reads
the fragment, immediately strips it from history, consumes the handoff anonymously, signs in with
the returned provider custom token, refreshes `/runtime/private`, then navigates to the returned
`entryRoute`.

Never put ID tokens, Firebase tokens, bearer tokens, or long-lived secrets in URLs.

### Platform Support To Admin Portal

The support flow starts in `platform-portal` and lands in `admin-portal`.

1. `StartTenantAdminAccessDialog` calls the backend start endpoint to create a server-side support
   session for the selected tenant.
2. The dialog reads `portalBaseUrls['admin-portal']`.
3. Same-origin deployments may navigate directly to `<adminBaseUrl>/app/admin`.
4. Cross-origin deployments create an `ADMIN` portal handoff with:

```json
{
  "targetPortal": "ADMIN",
  "entryRoute": "/app/admin",
  "supportAccessSessionId": "<support-session-id>"
}
```

5. `admin-portal` consumes the handoff, signs in locally, refreshes `/runtime/private`, calls
   `GET /platform/tenants/admin-access/current`, and hydrates `SupportAccessStore`.

`SupportAccessStore` is a cache of the server-side support session. It must hydrate from the
backend on app bootstrap, after handoff consumption, and on focus/visibility regain. It must clear
on absent, expired, or forbidden current-session responses.

The backend owns tenant authorization for support mode. The web must not send tenant override
headers as a substitute for the support session.

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
