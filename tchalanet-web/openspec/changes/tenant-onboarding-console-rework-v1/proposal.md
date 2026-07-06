# tenant-onboarding-console-rework-v1

## Why

Two console screens own the tenant lifecycle handoff between `SUPER_ADMIN` and `TENANT_ADMIN`,
and both are out of sync with the current conventions and the current backend entitlement model:

- **`apps/admin-portal/.../features/setup/pages/complete-config/`** (`admin-complete-tenant-config.page`)
  is a single 179-line `.ts` / 258-line `.html` file with **zero `components/` extraction**. The
  template repeats the same "section card" markup 9 times (identity/address/games/draws/channel
  matrix/generated draws/theme/promotions/seller-terminal), only label/icon/status/route/description
  vary. This violates `structure.md` §5 (extract a component once a block repeats / the template
  passes ~150 lines). It also drives loading/error with hand-rolled `pageState`/`pageError` signals
  and a manual `.subscribe(...)`, which `feature-playbook.md` §1.3 explicitly forbids for new/touched
  pages (`resource` + `tch-async-view` is the standard since `web-async-state-resource-v1`).
- **`apps/platform-portal/.../features/tenants/pages/onboarding/`** (`platform-tenant-provisioning.page`)
  has already been partially decomposed (`components/tenant-provisioning-aside`,
  `-profile-summary`, `-success`, `tenant-type-picker`) but its domain/status/step label maps
  (`DOMAIN_LABEL_KEYS`, `STATUS_LABEL_KEYS`, `STEP_LABEL_KEYS`) were written before the backend
  change `align-plan-entitlements-seller-terminal-v1` (tchalanet-server) renamed quotas around
  `admin_users` / `seller_terminals` / `draw_channels` / `promotion_rules` and dropped
  `outlets`/`cashier`/`mobile`/`exports`. That backend change shipped with "no frontend
  implementation in this slice" — these two screens are the frontend catch-up.

## What Changes

### Admin setup (`admin-portal/features/setup/pages/complete-config`)

- Extract the repeated section markup into a stateless `components/setup-checklist-card/`
  (`input()` only: id, labelKey, icon, status, badge kind, descKey/description, ctaKey, route,
  blocking flag, section error view-model; no API calls, no router navigation logic beyond the
  `routerLink` it renders).
- Extract `components/setup-progress-header/` (completed/total/pct + icon) and
  `components/setup-seller-terminal-card/` (locked/unlocked variant + blocking-steps list) as
  their own stateless components — both currently inline in the template.
- The page becomes the orchestrator: loads the overview, builds the card view-model array from
  `setup()`/`sectionMap()`, keeps ownership of `sectionErrors` (per `error-management.md`, targets
  `admin.setup.*` stay page-owned), and composes the extracted components.
- The setup checklist stays focused on domains that actually block getting the tenant operational.
  Seeded/default domains such as Maryaj gratis/promotions and subscription are not presented as
  setup work; they remain accessible from their own navigation surfaces.
- **Deferred, not part of this change:** migrating `pageState`/`pageError`/`.subscribe(...)` to
  `TchBackendClient.getResource` + `tch-async-view`. Discovery (`tasks.md` 0.4) found that
  `getResource` unwraps `ApiResponse<T>` and drops `notices`, which this page needs for its
  section-error mapping — no page in the codebase combines the resource pattern with
  notices-derived section errors today, including the page `error-management.md` itself cites as
  "migrated" for this exact pattern. Forcing it here would either silently break section errors or
  require extending `TchBackendClient` (a shared-lib change, out of scope). The manual-signal
  loading stays as-is; only its wiring to the new stateless components changes.

### Platform provisioning (`platform-portal/features/tenants/pages/onboarding`)

- Audit `DOMAIN_LABEL_KEYS` / `STATUS_LABEL_KEYS` / `STEP_LABEL_KEYS` in
  `platform-tenant-provisioning.page.ts` against the current backend `domainStatuses` /
  `nextSteps` vocabulary (`TenantProvisioningOrchestrator`, tchalanet-server) — remove stale
  entries, add missing ones, keep the "never surface a raw backend code" rule already in place.
- Confirm the already-extracted components (`tenant-provisioning-aside`,
  `-profile-summary`, `-success`, `tenant-type-picker`) stay presentational — no direct
  `data-access`/API import inside a component; fix if any slipped in during the earlier
  decomposition pass.

### Both screens

- Components stay `input()`/`output()` only, `OnPush`, no direct HTTP/API calls — the page/service
  keeps orchestration, per `feature-playbook.md` §1 invariants and `structure.md` §5.
- No route, selector, or i18n-key renaming beyond what a newly extracted component naturally needs
  (new component selectors only).

## Non-Goals

- No new backend endpoint. The remaining backend gap (auto-apply a plan at provisioning, an
  aggregated `GET /platform/tenants/{id}/entitlements` view) is tracked in
  `tchalanet-server/tenant-provisioning-config-completion-todo-claude.md` and in the still-open
  task of `align-plan-entitlements-seller-terminal-v1` — this change only makes the two existing
  screens correct and consistent with what the backend already returns.
- No rework of the `platform-tenants` **detail** page tabs (Abonnement / Droits d'usage /
  Seller-terminals / Audit are still `tch-admin-empty-state` placeholders) — separate scope, not
  requested here.
- No visual/UX redesign beyond applying the existing `feature-playbook.md` archetypes and
  `libs/ui/console` bricks already in use elsewhere.
- No promotion of the new components to `libs/web/console` — each is a single-feature consumer
  today (`structure.md` §6/§7: stays in `<feature>/components/` until a second consumer appears).

## Impact

- `apps/admin-portal/src/app/features/setup/pages/complete-config/` gains a `components/`
  subfolder and keeps manual loading signals until the backend client has a notices-aware resource
  variant.
- `apps/platform-portal/src/app/features/tenants/pages/onboarding/` label maps updated; no
  structural change beyond what already landed.
- Behavior change for end users: the checklist is less noisy and no longer asks them to configure
  seeded/default domains such as Maryaj gratis/promotions or subscription. Existing section-error
  targets remain visible through the merged cards.
