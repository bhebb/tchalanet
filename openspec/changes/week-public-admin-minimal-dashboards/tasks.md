# Tasks

## 1. Coordination

- [x] Define the weekly scope as backend + web.
- [x] Split work into root coordination, backend BFF, and web UI OpenSpec changes.
- [ ] Confirm with human that the older broad PageModel dashboard change should be paused, replaced, or kept as future backlog.

## 2. Backend slice

- [x] Create backend OpenSpec change folder.
- [x] Define backend proposal, design, tasks, and spec.
- [x] Validate backend change with OpenSpec CLI.
- [x] Analyze existing backend routes and mark backend work as contract confirmation/integration, not greenfield creation.

## 3. Web slice

- [x] Create web OpenSpec change folder.
- [x] Define web proposal, design, tasks, and spec.
- [x] Validate web change with OpenSpec CLI.

## 4. Delivery gate

- [x] Backend and web agree on widget layout contract names: web types the real backend
      `PageModelDoc` (meta/theme/shell/content + separate `dynamic.widgets[id]`/`errors`); no abstract
      vocabulary.
- [x] Backend and web agree on route ownership: public (`/public`), SUPER_ADMIN (`/app/platform`),
      TENANT_ADMIN (`/app/admin`) — role routing already wired with guards.
- [x] Scope accepted; implementation started — PR1 foundations (M3 theme pipeline + isolated
      feature/entitlement/access gating + conventions) delivered.

## 5. Foundations delivered (PR1)

- [x] Material 3 theme engine ported and wired (web `core/theme`, generated preset registry).
- [x] Isolated gating seams (`core/feature`, `core/entitlement`, `core/access`) with `*tchCan`/pipe/guard.
- [x] Convention docs updated (theme, settings, feature-flags, entitlements, access, pagemodel).
- [x] W1 delivered: public PageModel renderer (typed on real `PageModelDoc`) + minimal SUPER_ADMIN
      (tenant provisioning) and TENANT_ADMIN (seller onboarding) surfaces in `tch-portal`.
      `nx build`/`test`/`lint` green (54 unit tests). Backend unchanged (contracts confirmed).
