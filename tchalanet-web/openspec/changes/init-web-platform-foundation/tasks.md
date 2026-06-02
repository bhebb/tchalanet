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

- [ ] `ApiResponse<T>`.
- [ ] `ApiStatus`.
- [ ] `ApiNotice`.
- [ ] `NoticeSeverity`.
- [ ] `ServiceStatus`.
- [ ] `ServiceHealth`.
- [ ] `ProblemDetail`.
- [ ] `TchPage<T>`.
- [ ] `ActionItem`.
- [ ] `NavigationDestination`.
- [ ] `UserSession`.
- [ ] `UserRole`.
- [ ] `RuntimeSettings`.
- [ ] `FeatureFlag` / `FeatureToggle`.
- [ ] `I18nBundle` / `I18nOverrides`.
- [ ] `ThemePreset` / `RuntimeTheme`.
- [ ] `PageModel`, `PageWidget`, `PageSection` minimal.
- [ ] `OperationalContextView` for later cashier/POS usage.

Acceptance:

- [ ] No `any` in these contracts.
- [ ] Contract names match backend concepts where possible.
- [ ] IDs remain strings at the frontend boundary unless a frontend typed-id wrapper is deliberately introduced later.

## 2. Setup Angular technical baseline

- [ ] Configure Angular Material.
- [ ] Configure NgRx Store.
- [ ] Configure NgRx Effects.
- [ ] Configure NgRx Devtools in dev only.
- [ ] Configure HTTP client providers.
- [ ] Add HTTP auth interceptor.
- [ ] Add HTTP error interceptor or centralized error mapper.
- [ ] Add correlation/request id handling if already available.
- [ ] Add `ApiResponse` unwrap helper or typed API client pattern.
- [ ] Add environment/runtime config loader only if needed for Keycloak/backend URLs.

Acceptance:

- [ ] App starts with Material loaded.
- [ ] Store Devtools are available only in dev.
- [ ] HTTP errors can be mapped to `ProblemDetail`.
- [ ] No business feature state is added to store yet.

## 3. Dependency governance

- [ ] Add `docs/frontend/dependencies.md` or equivalent.
- [ ] For every new dependency, document purpose/category/owner/alternative/removal trigger.
- [ ] Keep a short dependency table for web.
- [ ] Reject dependencies that duplicate Angular/Material/NgRx built-ins without strong reason.

Required initial dependency rationale entries:

- Angular Material: UI primitives and theme integration.
- NgRx Store: app-level state.
- NgRx Effects: side-effect orchestration for auth/bootstrap/runtime loads.
- NgRx Devtools: dev-only debugging.
- Keycloak JS or Angular wrapper: OIDC auth integration.
- Translation package if used: runtime translation loading and merging.

## 4. Lint / format / pre-commit policy

- [ ] Configure lint command.
- [ ] Configure format command.
- [ ] Configure test command or placeholder if tests are not ready.
- [ ] Decide whether pre-commit hooks are enabled immediately or after baseline cleanup.
- [ ] If hooks are enabled, keep them fast: format + lint only.
- [ ] Do not put e2e/performance tests in pre-commit.

Recommended:

- [ ] Enable lint/format in scripts now.
- [ ] Enable pre-commit only after first clean baseline.

## 5. Keycloak/Auth proof

- [ ] Configure Keycloak bootstrap.
- [ ] Add login button.
- [ ] Add logout button.
- [ ] Add token interceptor.
- [ ] Extract roles from token.
- [ ] Create `UserSession` from Keycloak claims.
- [ ] Create `AuthGuard`.
- [ ] Create `RoleGuard`.
- [ ] Add `/forbidden` page.
- [ ] Add `/public` route.
- [ ] Add `/app/cashier` route.
- [ ] Add `/app/admin` route.
- [ ] Add `/app/platform` route.

Acceptance:

- [ ] Public route opens without login.
- [ ] Protected route redirects or blocks anonymous user.
- [ ] A user without required role is blocked.
- [ ] A user with required role can access the correct dashboard.
- [ ] Empty dashboards display detected role, user, and tenant if available.

## 6. Runtime settings

- [ ] Create `SettingsApi`.
- [ ] Create `RuntimeSettingsStore` or NgRx feature slice.
- [ ] Load public settings for public surface.
- [ ] Load private settings after auth for admin/cashier/platform surfaces.
- [ ] Add feature toggle helper.
- [ ] Document V1 settings as temporary feature-toggle/config mechanism before future Unleash.

Acceptance:

- [ ] UI can check a feature flag without calling API directly from components.
- [ ] Missing settings fail safely with defaults.

## 7. i18n runtime

- [ ] Add local frontend translation files.
- [ ] Add backend i18n override API client.
- [ ] Merge local translations with backend overrides.
- [ ] Backend wins when a key exists in both.
- [ ] Add `LanguageSwitcher` later or in layout stage.
- [ ] Support at least default language and one additional language if available.

Acceptance:

- [ ] Local translation works without backend override.
- [ ] Backend override changes a visible label at runtime.
- [ ] Missing backend response does not break app if local fallback exists.

## 8. Theme runtime

- [ ] Create Tchalanet default theme.
- [ ] Create theme preset contract.
- [ ] Add Material-equivalent preset list placeholder.
- [ ] Add theme runtime service/store.
- [ ] Apply active theme to CSS variables / Material theme integration.
- [ ] Add light/dark mode support if low cost.
- [ ] Keep custom theme builder out of V1.

Acceptance:

- [ ] App starts with Tchalanet default theme.
- [ ] Theme can be switched to a preset without rebuild.
- [ ] Components use tokens, not hardcoded colors.

## 9. PageModel runtime

- [ ] Create PageModel API client.
- [ ] Create PageModel renderer minimal.
- [ ] Support text/title keys, actions, and simple widget dispatch.
- [ ] Ensure PageModel uses i18n keys, not embedded translations.
- [ ] Ensure PageModel uses active theme, not embedded theme data.
- [ ] Ensure PageModel uses settings only through runtime state, not embedded flags.

Acceptance:

- [ ] Public home can render a minimal PageModel payload.
- [ ] If PageModel fails, show `TchErrorPanel` or fallback.

## 10. Runtime bootstrap orchestration

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

- [ ] `TchNotice`.
- [ ] `TchErrorPanel`.
- [ ] `TchLoading`.
- [ ] `TchEmptyState`.
- [ ] `TchActionButton`.
- [ ] `TchActionList`.
- [ ] `TchStatusBadge`.
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

- [ ] `PublicShell`.
- [ ] `PrivateShell`.
- [ ] `TopAppBar`.
- [ ] `NavigationDrawer` / `SideNav`.
- [ ] `UserMenu`.
- [ ] `LanguageSwitcher`.
- [ ] `ThemeToggle`.

Acceptance:

- [ ] Private top app bar remains utility-focused.
- [ ] Main navigation lives in drawer/sidenav, not duplicated in top bar.
- [ ] Shell consumes resolved navigation/session data; it does not decide business permissions manually.

## 13. First public home

- [ ] Create `PublicHomePage`.
- [ ] Render through public bootstrap state.
- [ ] Show loading/error/fallback states.
- [ ] Render at least one PageModel widget and one action.

## 14. Prepare next V1 legs

Tenant admin Web:

- [ ] Create placeholder route and dashboard.
- [ ] Prepare overview/users/outlets/terminals/sessions/settings route structure.

Cashier Web:

- [ ] Keep as optional for V1 after Flutter POS direction is confirmed.

Superadmin:

- [ ] Create minimal protected platform dashboard.
- [ ] Prioritize onboarding tenant later before full superadmin console.

