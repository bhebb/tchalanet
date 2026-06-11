# Tasks

- [x] Confirm `GET /public/runtime/bootstrap` and `GET /tenant/runtime/bootstrap`
  as the server-composed mobile runtime contracts.
- [x] Confirm `GET /tenant/runtime/state` as the lightweight authenticated refresh
  contract and record the current static-version backend gap.
- [x] Define typed public and authenticated bootstrap models and precedence.
- [x] Define typed private runtime-state and version-hint models.
- [x] Implement public and authenticated bootstrap Services.
- [x] Implement the authenticated runtime-state Service.
- [x] Implement the bootstrap Repository with typed mapping and safe fallbacks.
- [x] Expose app-scoped Riverpod bootstrap state.
- [x] Implement runtime-state polling, foreground/manual refresh, cooldown, and
  logout cancellation.
- [x] Use runtime-state notification summary during its polling cycle without
  starting a duplicate notification-summary poll.
- [x] Compare runtime version hints and trigger one tenant re-bootstrap when a hint
  changes.
- [x] Handle `BLOCKED`, `FORCE_RELOAD`, and `SESSION_EXPIRED` state transitions.
- [x] Apply settings, i18n overrides, gates/entitlements, readiness,
  notification summary, and notices from bootstrap state.
- [x] Ignore bootstrap `pageModelRef` values and keep feature data behind explicit
  typed POS Services and Repositories.
- [x] Add typed feature-toggle/config and authorization-gate helpers outside Views.
- [ ] Document V1 settings ownership and future Unleash boundary.
- [ ] Test public-to-tenant precedence, missing sections, backend failures, typed
  values, and bundled/safe fallbacks.
- [ ] Test polling cadence, changed/unchanged versions, cooldown, refresh failure,
  logout cancellation, and no re-bootstrap loop.
- [x] Run Flutter checks and strict OpenSpec validation.
