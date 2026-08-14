# tasks — admin-limits-active-control-v1

## 0. Discovery

- [x] Confirm current limits overview does not expose a full active assignment list with target labels.
- [x] Confirm common quick action should default number blocking to draw channel + today.
- [x] Confirm limits are accessed from dashboard, Limit nav, tenant config, draw channel config/detail, and seller terminal config/detail.
- [x] Inventory existing mutation APIs for disable/delete/edit and note any missing action support.

## 1. Backend Read Model

- [x] Extend tenant admin policies overview with an active-limits collection.
- [x] Include assignment id, rule key, business group, target type, target id, target label, enabled state, params, duration fields, and supported actions.
- [x] Resolve draw channel labels through stable draw channel APIs/query buses.
- [x] Resolve seller terminal labels through stable seller terminal APIs/query buses.
- [x] Keep feature aggregation read-only and free of repository/direct SQL access to other modules.
- [ ] Add focused unit tests for tenant/global, draw channel, and seller terminal active limit rows.

## 2. Web Data Access

- [x] Update limits overview TypeScript models for the new active-limits payload.
- [x] Add business group helpers for supported rule keys.
- [x] Ensure delete/disable/edit actions use existing feature API methods where possible.
- [x] Add error handling using existing web conventions.

## 3. Limits Overview UX

- [x] Rework `/app/admin/limits` into a control page with quick actions plus grouped active limits.
- [x] Group active limits by business type: blocked numbers, number caps, ticket limits, seller limits, advanced.
- [x] Add mobile-first cards and desktop row layout without horizontal scrolling.
- [x] Show clear target labels: tenant/global, draw channel, seller terminal.
- [x] Add empty state for no active limits.
- [x] Remove technical rule/scope language from the default view.

## 4. Quick Actions

- [x] Make `Bloke nimewo` default to draw channel scope and today's duration.
- [x] Prioritize active/open sellable draw channels in the selector.
- [x] Add or adapt quick action for number stake cap.
- [x] Ensure contextual draw/draw-channel launches still lock the channel.
- [x] Add confirmation and success/error messages for disable/delete.

## 5. Navigation and Context

- [x] Keep Limit as a single nav link.
- [x] Keep contextual limit entry points from tenant settings, draw channel detail/config, and seller terminal detail/config.
- [x] Ensure dashboard quick action opens the block-number flow directly.

## 6. i18n

- [x] Add all new keys in `ht`, `fr`, and `en`.
- [x] Audit for hardcoded user-visible strings.
- [x] Keep Haitian Creole as the primary UX wording, with French/English matching the same business meaning.

## 7. Tests and Validation

- [ ] Backend focused unit tests for active-limit read model grouping and label resolution.
- [ ] Web unit tests for grouped active limits, empty state, and quick action defaults.
- [ ] Web e2e smoke for admin login and `/app/admin/limits` mobile/desktop layout.
- [x] `pnpm nx lint admin-portal`.
- [x] `pnpm nx test admin-portal --watch=false`.
- [x] Backend compile for `tchalanet-core,tchalanet-features`.
- [x] Backend spotless/checkstyle/PMD for touched modules.
- [ ] Backend focused unit tests for touched modules.
- [ ] Backend SpotBugs for `tchalanet-features` completed locally.
