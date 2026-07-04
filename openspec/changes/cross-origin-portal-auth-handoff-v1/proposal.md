# Change: cross-origin-portal-auth-handoff-v1

## Why

Tchalanet has separate web apps for public, tenant admin, and platform admin. In local and deployed
setups these apps may run on different hosts, ports, or subdomains.

The current browser-only handoff assumptions are not valid across origins:

- Firebase/local browser persistence is origin-scoped and cannot be relied on after redirecting from
  public to admin/platform.
- `localStorage` and `sessionStorage` are origin-scoped and cannot carry platform support context
  into admin-portal when hosts differ.
- Redirecting directly to `/admin/app/admin` or `/platform/app/platform` can land in the target app
  without a local provider session, causing `/runtime/private` to fail with 401.
- Platform support access is currently V0 client-session scoped and therefore cannot survive a
  platform-host to admin-host redirect.

## What changes

- Introduce a server-backed, short-lived, one-time portal handoff for cross-origin navigation after
  login.
- Public login creates a handoff targeted to `admin-portal` or `platform-portal`, then redirects the
  browser to the target app handoff route.
- The target app consumes the handoff, establishes/restores its local auth state, then loads
  `/runtime/private` and navigates to the final private entry route.
- Platform support tenant access becomes server-backed for the current super admin and can be
  restored by admin-portal after cross-origin redirect.
- Browser storage remains allowed only as an optimization for same-origin/local cases, not as the
  source of truth.

## Impact

- Backend: platform identity/auth handoff endpoints, server-backed support access session in
  platform access control, audit events, security validation.
- Web: public login redirect flow, admin/platform handoff routes, support access restoration,
  runtime config usage for target portal URLs.
- Tests: backend contract/security tests and frontend route/service tests.

## Non-goals

- No general SSO redesign.
- No long-lived bearer token, Firebase ID token, or support session secret in URL.
- No dependency on cross-origin `localStorage`, `sessionStorage`, or third-party cookies.
- No automatic merge of public/admin/platform into one Angular app.

## Context packs

- `tchalanet-server/openspec/context/10-non-negotiables.md`
- `openspec/context/05-version-guard.md`
- `tchalanet-server/openspec/context/90-security-flows-guide.md`
- `tchalanet-web/docs/conventions/auth.md`

## Near-code references

- `tchalanet-web/libs/core/auth/src/lib/firebase/login.page.ts`
- `tchalanet-web/libs/core/auth/src/lib/auth-redirect.service.ts`
- `tchalanet-web/libs/core/auth/src/lib/support/support-access.store.ts`
- `tchalanet-web/apps/public-portal/src/app/app.config.ts`
- `tchalanet-web/apps/admin-portal/src/app/app.routes.ts`
- `tchalanet-web/apps/platform-portal/src/app/features/shared/start-tenant-admin-access-dialog.ts`
- `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/tenant/internal/web/TenantAdminController.java`
- `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/accesscontrol/internal/web/EffectiveTenantResolver.java`
