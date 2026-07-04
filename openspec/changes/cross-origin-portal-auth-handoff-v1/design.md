# Design: cross-origin portal auth handoff

## Problem statement

Cross-origin navigation cannot rely on browser storage. For example:

```text
public.localtest.me   -> platform.localtest.me
platform.localtest.me -> admin.localtest.me
localhost:4200        -> localhost:4202
localhost:4202        -> localhost:4302
```

Each origin has its own Firebase persistence, `localStorage`, and `sessionStorage`. The target app
must therefore receive a server-verifiable handoff, not a client-only context.

## Desired flows

### Public login to private portal

1. User submits credentials on public-portal.
2. Public signs in with the identity provider and obtains an ID token.
3. Public calls backend runtime/bootstrap or a dedicated handoff creation endpoint with the bearer
   token.
4. Backend resolves the authenticated user, roles, entry route, and target portal.
5. Backend creates a DB-backed one-time handoff with a short TTL and returns:
   - `handoffId`
   - `code`
   - `targetPortal`
   - `targetUrl`
   - `entryRoute`
   - `expiresAt`
6. Public redirects to:
   - `${targetUrl}/login/handoff#code=<handoffId>.<code>`
7. Target app reads the URL fragment, immediately strips it from browser history, consumes the
   handoff by posting the code in the request body, establishes/restores local auth state, calls
   `/runtime/private`, then navigates to the final entry route.

### Platform support to tenant admin

1. Super admin starts tenant support access from platform-portal.
2. Backend creates a support access session bound to the super admin identity, tenant, mode, reason,
   expiry, and audit metadata.
3. Backend creates a one-time handoff targeted to admin-portal and returns target URL.
4. Platform redirects to admin-portal handoff route.
5. Admin consumes handoff, restores support access context from backend, then enters `/app/admin`.
6. Admin API calls use the server-backed support context resolved by backend request context.

## Security requirements

- Handoff codes MUST be single-use.
- Handoff codes MUST expire quickly, default target <= 60 seconds.
- Handoff codes MUST be opaque and non-guessable.
- Handoff secrets MUST have at least 128 bits of entropy.
- Handoff secrets MUST NOT be stored raw; only a SHA-256 hash is persisted.
- URLs MUST NOT contain Firebase ID tokens, backend bearer tokens, or long-lived support secrets.
- The handoff secret SHOULD be carried in the URL fragment, not query string, so it is not sent in
  normal HTTP request paths or logs.
- Handoff creation MUST validate target portal authorization and final entry route against a
  relative-path allow-list.
- Handoff consumption MUST validate:
  - handoff id and secret;
  - expected target portal;
  - expiry;
  - unused state;
  - final redirect allow-list;
  - target audience/runtime configuration.
- Handoff consumption MUST use atomic compare-and-set semantics on `consumedAt`.
- Handoff secret comparison MUST use constant-time comparison after hashing.
- Replayed or expired handoffs SHOULD return `410 Gone`.
- Handoff consumption MUST be rate-limited per IP and per handoff id.
- Support access MUST be audited with super admin, tenant, reason, mode, timestamps, and final target.
- Support access MUST be revocable/clearable.
- Audit events MUST include portal handoff created, consumed, expired, replay detected, and target
  mismatch.

## Backend shape

The exact Java package placement should follow existing backend ownership. The domain contract is
an auth handoff boundary; implementation should prefer the current `platform.identity` /
`platform.accesscontrol` ownership instead of introducing a new package name only for symmetry.

Handoff record:

```text
handoffId
codeHash
subjectUserId
targetPortal
entryRoute
supportAccessSessionId?
createdAt
expiresAt
consumedAt?
```

Creation:

```http
POST /platform/auth/portal-handoffs
Authorization: Bearer <identity-provider-token>
Content-Type: application/json

{
  "targetPortal": "ADMIN" | "PLATFORM",
  "entryRoute": "/app/admin" | "/app/platform" | "/app/account/activation",
  "supportAccessSessionId": "optional"
}
```

The server derives `targetUrl` from trusted runtime configuration. The client never submits a
target URL.

Creation response:

```json
{
  "handoffId": "...",
  "code": "...",
  "targetPortal": "ADMIN",
  "targetUrl": "https://admin.localtest.me",
  "entryRoute": "/app/admin",
  "expiresAt": "2026-07-04T12:00:00Z"
}
```

Consumption:

```http
POST /platform/auth/portal-handoffs/{handoffId}/consume
Content-Type: application/json

{
  "code": "...",
  "targetPortal": "ADMIN" | "PLATFORM"
}
```

The consume endpoint is anonymous at the HTTP-auth layer because the target origin may have no local
provider session yet. It authorizes only through the one-time secret, target binding, TTL, and rate
limits.

If Firebase remains the provider, V1 should mint a Firebase custom token for `subjectUserId` and the
target app should call `signInWithCustomToken`. The response also includes `entryRoute`,
`targetPortal`, and whether a `supportAccessSessionId` is bound.

Support access current-session lookup:

```http
GET /platform/tenants/admin-access/current
Authorization: Bearer <super-admin-token>
```

The current `DELETE /platform/tenants/admin-access/current` can remain the stop endpoint.

Support access belongs to `platform.accesscontrol`. The server-backed support access session record
contains super admin uid, tenant id, mode, reason, granted/expires timestamps, and audit metadata.
`EffectiveTenantResolver` should resolve the effective tenant from the active server-side support
session for super admins. Expired or cleared sessions never authorize support mode.

Audit events:

- `PORTAL_HANDOFF_CREATED`
- `PORTAL_HANDOFF_CONSUMED`
- `PORTAL_HANDOFF_EXPIRED`
- `PORTAL_HANDOFF_REPLAY_DETECTED`
- `PORTAL_HANDOFF_TARGET_MISMATCH`
- `SUPPORT_ACCESS_STARTED`
- `SUPPORT_ACCESS_RESTORED`
- `SUPPORT_ACCESS_STOPPED`
- `SUPPORT_ACCESS_EXPIRED`

## Web shape

- Public `LoginPage` MUST NOT assume target apps share browser storage.
- `AuthRedirectService` should choose between:
  - same-origin fast navigation, when configured and safe;
  - server handoff redirect, when target portal base URL is cross-origin.
- Admin/platform apps add `/login/handoff` route.
- Handoff page consumes the code, signs into/restores the local provider session, refreshes runtime,
  and navigates to entry route.
- Handoff page failure navigates to the local login page with a non-sensitive error code. It must
  never bounce back with the handoff code still in the URL.
- `/login/handoff` must be reachable before private auth guards. Existing landing/session guard
  behavior for normal private routes remains unchanged.
- `SupportAccessStore` may cache state locally for UX, but must restore from backend on app
  bootstrap, after handoff consumption, and when the window regains focus/visibility.
- Support-mode UI, including sidebar state, banner, tenant indicator, stop button, and
  back-to-platform link, derives from `SupportAccessStore` and clears on stop, expiry, or absent
  current support session response.

## Open questions

- Should handoff creation live in `platform.identity`, `platform.accesscontrol`, or a small auth
  orchestration slice?
- Confirm whether Firebase custom tokens are acceptable for V1 target-app session bootstrap, or if
  backend wants an application session token/cookie instead.
