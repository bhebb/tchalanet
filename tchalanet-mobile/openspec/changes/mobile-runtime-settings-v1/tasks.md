# Tasks

- [x] Confirm `GET /public/runtime/bootstrap` and `GET /tenant/runtime/bootstrap`
  as the server-composed mobile runtime contracts.
- [x] Confirm `GET /tenant/runtime/state` as the lightweight authenticated refresh
  contract and record the current static-version backend gap.
- [ ] Define typed public and authenticated bootstrap models and precedence.
- [ ] Define typed private runtime-state and version-hint models.
- [ ] Implement public and authenticated bootstrap Services.
- [ ] Implement the authenticated runtime-state Service.
- [ ] Implement the bootstrap Repository with typed mapping and safe fallbacks.
- [ ] Expose app-scoped Riverpod bootstrap state.
- [ ] Implement runtime-state polling, foreground/manual refresh, cooldown, and
  logout cancellation.
- [ ] Use runtime-state notification summary during its polling cycle without
  starting a duplicate notification-summary poll.
- [ ] Compare runtime version hints and trigger one tenant re-bootstrap when a hint
  changes.
- [ ] Handle `BLOCKED`, `FORCE_RELOAD`, and `SESSION_EXPIRED` state transitions.
- [ ] Apply settings, i18n overrides, gates/entitlements, readiness,
  notification summary, and notices from bootstrap state.
- [ ] Ignore bootstrap `pageModelRef` values and keep feature data behind explicit
  typed POS Services and Repositories.
- [ ] Add typed feature-toggle/config and authorization-gate helpers outside Views.
- [ ] Document V1 settings ownership and future Unleash boundary.
- [ ] Test public-to-tenant precedence, missing sections, backend failures, typed
  values, and bundled/safe fallbacks.
- [ ] Test polling cadence, changed/unchanged versions, cooldown, refresh failure,
  logout cancellation, and no re-bootstrap loop.
- [ ] Run Flutter checks and strict OpenSpec validation.
