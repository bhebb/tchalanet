# Tasks — Init Web Platform Foundation

## 0. Backup and clean base

- [ ] Create a backup branch/tag or archive of current Web state.
- [ ] Remove obsolete or experimental frontend leftovers only after backup.
- [ ] Add a short `frontend/README.md` with local startup commands.
- [ ] Document which tasks are intentionally manual in this phase.

Manual-friendly tasks:

- workspace cleanup;
- visual folder naming tweaks;
- first README wording;
- choice of exact route labels.

## 1. Transverse contracts/types

Create typed contracts before store/effects/services depend on `any`.

- [x] `ApiResponse<T>`.
- [x] `ApiStatus`.
- [x] `ApiNotice`.
- [x] `NoticeSeverity`.
- [x] `ServiceStatus`.
- [x] `ServiceHealth`.
- [x] `ProblemDetail`.
- [x] `TchPage<T>`.
- [x] `ActionItem`.
- [x] `NavigationDestination`.
- [x] `UserSession`.
- [x] `UserRole`.
- [x] `RuntimeSettings`.
- [x] `FeatureFlag` / `FeatureToggle`.
- [x] `I18nBundle` / `I18nOverrides`.
- [x] `ThemePreset` / `RuntimeTheme`.
- [x] `PageModel`, `PageWidget`, `PageSection` minimal.
- [x] `OperationalContextView` for later cashier/POS usage.

Acceptance:

- [x] No `any` in these contracts.
- [x] Contract names match backend concepts where possible.
- [x] IDs remain strings at the frontend boundary unless a frontend typed-id wrapper is deliberately introduced later.

## 2. Setup Angular technical baseline

- [x] Configure Angular Material.
- [x] Configure NgRx Store.
- [x] Configure NgRx Effects.
- [x] Configure NgRx Devtools in dev only.
- [x] Configure HTTP client providers.
- [x] Add HTTP auth interceptor.
- [x] Add HTTP error interceptor or centralized error mapper.
- [x] Add correlation/request id handling if already available.
- [x] Add `ApiResponse` unwrap helper or typed API client pattern.
- [ ] Add environment/runtime config loader only if needed for Keycloak/backend URLs.

Acceptance:

- [x] App starts with Material loaded.
- [x] Store Devtools are available only in dev.
- [x] HTTP errors can be mapped to `ProblemDetail`.
- [ ] No business feature state is added to store yet.

## 3. Dependency governance

- [x] Add `docs/frontend/dependencies.md` or equivalent.
- [x] For every new dependency, document purpose/category/owner/alternative/removal trigger.
- [x] Keep a short dependency table for web.
- [x] Reject dependencies that duplicate Angular/Material/NgRx built-ins without strong reason.

Required initial dependency rationale entries:

- Angular Material: UI primitives and theme integration.
- NgRx Store: app-level state.
- NgRx Effects: side-effect orchestration for auth/bootstrap/runtime loads.
- NgRx Devtools: dev-only debugging.
- Keycloak JS or Angular wrapper: OIDC auth integration.
- Translation package if used: runtime translation loading and merging.

## 4. Lint / format / pre-commit policy

- [x] Configure lint command.
- [x] Configure format command.
- [x] Configure test command or placeholder if tests are not ready.
- [ ] Decide whether pre-commit hooks are enabled immediately or after baseline cleanup.
- [ ] If hooks are enabled, keep them fast: format + lint only.
- [ ] Do not put e2e/performance tests in pre-commit.

Recommended:

- [ ] Enable lint/format in scripts now.
- [ ] Enable pre-commit only after first clean baseline.

## 5. Keycloak/Auth proof

- [x] Configure Keycloak bootstrap.
- [x] Add login button.
- [x] Add logout button.
- [x] Add token interceptor.
- [x] Extract roles from token.
- [x] Create `UserSession` from Keycloak claims.
- [x] Create `AuthGuard`.
- [x] Create `RoleGuard`.
- [x] Add `/forbidden` page.
- [x] Add `/public` route.
- [x] Add `/app/cashier` route.
- [x] Add `/app/admin` route.
- [x] Add `/app/platform` route.

Acceptance:

- [x] Public route opens without login.
- [x] Protected route redirects or blocks anonymous user.
- [x] A user without required role is blocked.
- [x] A user with required role can access the correct dashboard.
- [x] Empty dashboards display detected role, user, and tenant if available.

## 6. Runtime settings

- [x] Create `SettingsApi`.
- [x] Create `RuntimeSettingsStore` or NgRx feature slice.
- [x] Load public settings for public surface.
- [x] Load private settings after auth for admin/cashier/platform surfaces.
- [x] Add feature toggle helper.
- [x] Document V1 settings as temporary feature-toggle/config mechanism before future Unleash.
- [x] Centralize private runtime bootstrap in `AppRuntimeStore.initPrivateRuntime()`.

Acceptance:

- [x] UI can check a feature flag without calling API directly from components.
- [x] Missing settings fail safely with defaults.

## 7. i18n runtime

- [x] Add local frontend translation files.
- [x] Add backend i18n override API client.
- [x] Merge local translations with backend overrides.
- [x] Backend wins when a key exists in both.
- [x] Add `LanguageSwitcher` later or in layout stage.
- [x] Support at least default language and one additional language if available.
- [x] Make i18n initialization idempotent so repeated bootstraps do not reset selected language.
- [x] Document backend surface override ordering and V1 response contract.

Acceptance:

- [x] Local translation works without backend override.
- [x] Backend override changes a visible label at runtime.
- [x] Missing backend response does not break app if local fallback exists.

## 8. Theme runtime

- [x] Create Tchalanet default theme.
- [x] Create theme preset contract.
- [x] Add Material-equivalent preset list placeholder.
- [x] Add theme runtime service/store.
- [x] Apply active theme to CSS variables / Material theme integration.
- [x] Add light/dark mode support if low cost.
- [x] Keep custom theme builder out of V1.

Acceptance:

- [x] App starts with Tchalanet default theme.
- [x] Theme can be switched to a preset without rebuild.
- [x] Components use tokens, not hardcoded colors.

## 9. PageModel runtime

- [x] Create PageModel API client.
- [x] Create PageModel renderer minimal.
- [x] Support text/title keys, actions, and simple widget dispatch.
- [x] Ensure PageModel uses i18n keys, not embedded translations.
- [x] Ensure PageModel uses active theme, not embedded theme data.
- [x] Ensure PageModel uses settings only through runtime state, not embedded flags.

Acceptance:

- [x] Public home can render a minimal PageModel payload.
- [x] If PageModel fails, show `TchErrorPanel` or fallback.

## 10. Runtime bootstrap orchestration

- [x] Create pre-PageModel `AppRuntimeStore` for public auth/i18n/theme/settings readiness.
- [x] Expose global runtime signals: current language, connected user/session, current theme, settings.
- [x] Keep PageModel out of the initial runtime readiness check.
- [x] Split public and private runtime bootstrap paths.
- [ ] Create `PublicBootstrapService` or NgRx effect.
- [ ] Load settings, i18n overrides, theme, and PageModel as separate calls.
- [ ] Use parallel loading where safe.
- [ ] Merge i18n before final render.
- [ ] Apply theme before or during shell render.
- [ ] Expose bootstrap loading/error state.

Acceptance:

- [ ] Public route performs four distinct runtime loads.
- [ ] Failure modes are visible and not silent.
- [ ] PageModel remains independent from settings/i18n/theme payloads.

## 11. Minimal UI core

Create only components needed now.

- [x] `TchNotice`.
- [x] `TchErrorPanel`.
- [x] `TchLoading`.
- [x] `TchEmptyState`.
- [x] `TchActionButton`.
- [ ] `TchActionList`.
- [x] `TchStatusBadge`.
- [ ] `TchPageHeader`.
- [ ] `TchConfirmDialog`.

Deferred until real admin/POS pages:

- [ ] `TchDataTable`.
- [ ] `TchPagedList`.
- [ ] `TicketSummaryCard`.
- [ ] `TicketLineList`.
- [ ] `MoneyBreakdown`.
- [ ] `ReceiptPreview`.
- [ ] `PayoutStatusBadge`.

## 12. Layout shells

- [x] `PublicShell`.
- [x] `PrivateShell`.
- [ ] `TopAppBar`.
- [ ] `NavigationDrawer` / `SideNav`.
- [ ] `UserMenu`.
- [x] `LanguageSwitcher`.
- [ ] `ThemeToggle`.

Acceptance:

- [ ] Private top app bar remains utility-focused.
- [x] Main navigation lives in drawer/sidenav, not duplicated in top bar.
- [ ] Shell consumes resolved navigation/session data; it does not decide business permissions manually.

## 13. First public home

- [x] Create `PublicHomePage`.
- [x] Render through public bootstrap state.
- [x] Show loading/error/fallback states.
- [x] Render at least one PageModel widget and one action.

## 14. Prepare next V1 legs

Tenant admin Web:

- [x] Create placeholder route and dashboard.
- [ ] Prepare overview/users/outlets/terminals/sessions/settings route structure.

Cashier Web:

- [ ] Keep as optional for V1 after Flutter POS direction is confirmed.

Superadmin:

- [x] Create minimal protected platform dashboard.
- [x] Prioritize onboarding tenant later before full superadmin console.
