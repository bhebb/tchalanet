# Tasks: cross-origin portal auth handoff

## 0. Contract and ownership

- [x] 0.1 Capture the current cross-origin failure mode and why browser storage is insufficient.
- [x] 0.2 Define V1 desired flows for public login and platform support access.
- [x] 0.3 Define initial backend/web contract boundaries and security requirements.
- [ ] 0.4 Confirm backend ownership: handoff orchestration in the existing platform identity/auth
      boundary; support access in `platform.accesscontrol`.
- [ ] 0.5 Confirm target portal URL runtime config keys and allowed entry routes.

## 1. Backend — handoff slice (`platform.auth` contract)

- [ ] 1.1 Handoff record + repository: `handoffId`, `codeHash` (SHA-256), `subjectUserId`,
      `targetPortal`, `entryRoute`, `supportAccessSessionId`, `createdAt`, `expiresAt` (TTL 60 s),
      `consumedAt`; DB-backed.
- [ ] 1.2 `POST /platform/auth/portal-handoffs`: authenticate creator, verify target-portal
      entitlement/authorization, validate `entryRoute` against relative-path allow-list, generate
      >=128-bit secret, return `{ handoffId, code, targetPortal, targetUrl, entryRoute, expiresAt }`
      with `targetUrl` from server runtime config.
- [ ] 1.3 `POST /platform/auth/portal-handoffs/{handoffId}/consume`: anonymous, code in body,
      atomic compare-and-set on `consumedAt`, constant-time hash compare, target-audience check,
      `410 Gone` on replay/expiry; mint Firebase custom token for `subjectUserId`.
- [ ] 1.4 Rate limiting on consume, per IP and per `handoffId`.
- [ ] 1.5 Audit events:
      `PORTAL_HANDOFF_{CREATED,CONSUMED,EXPIRED,REPLAY_DETECTED,TARGET_MISMATCH}`.

## 2. Backend — support access session (`platform.accesscontrol`)

- [ ] 2.1 Server-backed support access session record: super admin uid, tenant, mode, reason,
      `grantedAt`, `expiresAt` (30–60 min), cleared/stopped state, audit metadata.
- [ ] 2.2 Start endpoint creates the session, wired from existing tenant admin access flow near
      `TenantAdminController`.
- [ ] 2.3 `GET /platform/tenants/admin-access/current` for post-handoff restoration; keep existing
      `DELETE .../current` as stop.
- [ ] 2.4 `EffectiveTenantResolver` resolves tenant from the active server-side session for super
      admins; expired/cleared sessions never authorize support mode.
- [ ] 2.5 Audit events: `SUPPORT_ACCESS_{STARTED,RESTORED,STOPPED,EXPIRED}`.

## 3. Web — public-portal

- [ ] 3.1 `AuthRedirectService`: compare target portal base URL from runtime config with current
      origin; same-origin -> direct navigation; cross-origin -> create handoff and redirect to
      `${targetUrl}/login/handoff#code=<handoffId>.<code>`.
- [ ] 3.2 `login.page.ts`: wire post-sign-in flow through the redirect service; never place ID
      tokens in URLs.

## 4. Web — admin-portal and platform-portal

- [ ] 4.1 `/login/handoff` route in both apps, registered ahead of auth guards.
- [ ] 4.2 Handoff page: read fragment, strip from history (`replaceUrl: true`), consume, sign in
      with custom token, load `/runtime/private`, navigate to entry route.
- [ ] 4.3 Failure path: local login page with non-sensitive error code; never bounce back with the
      code in the URL.
- [ ] 4.4 Admin-portal: hydrate `SupportAccessStore` from `GET .../admin-access/current` on app
      bootstrap, after handoff consumption, and on window focus/visibility regain; store acts as
      cache only; reset store on `410`/absent-session responses.
- [ ] 4.5 Platform-portal `start-tenant-admin-access-dialog`: create server session, then reuse the
      same handoff mechanism toward admin-portal.
- [ ] 4.6 Verify route/guard ordering: existing landing guard (valid-local-session check) and
      shared login page unchanged; `/login/handoff` reachable without a session.
- [ ] 4.7 Support-mode UI (sidebar, banner, tenant indicator, stop button, back-to-platform link)
      derives exclusively from `SupportAccessStore`; clears on stop/expiry.

## 5. Tests

- [ ] 5.1 Backend contract/security: atomic single-use under concurrent consume, TTL expiry,
      target mismatch, creation-time entitlement rejection, entry-route allow-list rejection,
      replay audit emission, secret absent from logs/paths.
- [ ] 5.2 Backend: `EffectiveTenantResolver` resolves restored support sessions; expired sessions
      rejected.
- [ ] 5.3 Frontend: handoff route happy path, fragment stripped from history, failure -> local
      login, same-origin path skips handoff, support store rehydration.
- [ ] 5.4 E2E localtest.me multi-origin: public -> admin, public -> platform,
      platform -> admin support flow, replayed code shows recoverable error.
- [ ] 5.5 E2E direct entry: admin link with valid local session enters without login or handoff;
      without session, shared login page signs in locally without handoff.
- [ ] 5.6 E2E support round trips: platform -> admin -> platform -> admin reuses sessions with no
      new handoff; support UI restored on return; tenant switch reflected on next hydration;
      support UI cleared after stop/expiry.

## 6. Documentation

- [ ] 6.1 Update `tchalanet-web/docs/conventions/auth.md` with the handoff flow and same-origin
      fast path.
- [ ] 6.2 Update `tchalanet-server/openspec/context/90-security-flows-guide.md` with the handoff
      security model and audit events.
