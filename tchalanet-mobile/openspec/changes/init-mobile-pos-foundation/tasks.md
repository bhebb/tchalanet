# Tasks — Init Mobile POS Foundation

## 0. Backup and clean base

- [ ] Create a backup branch/tag or archive of current mobile state.
- [ ] Confirm Flutter app path and run command.
- [ ] Add or update `tchalanet-mobile/README.md` with local startup notes.
- [ ] Remove obsolete mobile artifacts only after backup.

Manual-friendly tasks:

- Android emulator/device setup notes;
- app display name/icon cleanup;
- initial folder naming tweaks.

## 1. Dart contracts/types

Create contracts before services/state depend on `dynamic` maps.

- [x] `ApiResponse<T>`.
- [x] `ApiStatus`.
- [x] `ApiNotice`.
- [x] `NoticeSeverity`.
- [x] `ServiceStatus`.
- [x] `ProblemDetail`.
- [x] `TchPage<T>` if mobile consumes paged APIs early.
- [x] `ActionItem` if reused by mobile actions. — skipped, no use case yet
- [x] `NavigationDestination` or mobile route descriptor if needed. — skipped, GoRouter covers it
- [x] `UserSession`.
- [x] `UserRole`.
- [x] `RuntimeSettings`.
- [x] `FeatureFlag` / `FeatureToggle`. — merged into RuntimeSettings
- [x] `I18nBundle` / `I18nOverrides`.
- [x] `RuntimeTheme` / `ThemePreset`.
- [x] `OperationalContextView`.
- [x] `TerminalBindingView`.
- [x] `SalesSessionView` minimal.

Acceptance:

- [x] Runtime models do not use untyped `Map<String, dynamic>` outside serialization boundaries.
- [x] Serialization/deserialization is explicit.
- [x] Sensitive auth/session fields are not logged.

## 2. Setup Flutter technical baseline

- [ ] Configure project structure for features/core/shared.
- [ ] Configure HTTP client.
- [ ] Configure auth token interceptor.
- [ ] Configure secure storage.
- [ ] Configure state management baseline.
- [ ] Configure routing.
- [ ] Configure theme baseline.
- [ ] Configure localization baseline.

Recommended existing dependencies from version source:

- `flutter_riverpod` for app state.
- `go_router` for routing/guards/redirects.
- `dio` for HTTP/interceptors.
- `flutter_secure_storage` for tokens.

Do not add additional dependencies unless documented in `docs/mobile/dependencies.md`.

## 3. Dependency governance

- [x] Add `docs/mobile/dependencies.md` or equivalent.
- [x] For every dependency, document purpose/category/owner/alternative/removal trigger.
- [x] Keep package additions minimal.
- [x] Prefer Flutter SDK primitives when enough.

Required initial dependency rationale entries:

- Riverpod: app state/session/settings/theme/i18n state.
- GoRouter: route protection/redirects.
- Dio: API calls and interceptors.
- Flutter secure storage: secure token storage.

## 4. Lint / format / pre-commit policy

- [x] Configure `dart analyze`.
- [x] Configure `dart format` or `flutter format` workflow.
- [ ] Add `flutter test` placeholder or initial tests.
- [x] Decide whether pre-commit hooks are enabled immediately or after clean baseline.
- [x] If hooks are enabled, keep them fast: format + analyze only.
- [x] Do not put emulator integration tests in pre-commit.

Recommended:

- [x] Enable analyze/format commands now.
- [x] Add pre-commit only after baseline is clean.

## 5. Auth/session proof

- [ ] Configure mobile Keycloak/OIDC login flow. — endpoint still TODO (confirmed with backend)
- [x] Add login test screen/button.
- [x] Store tokens securely.
- [x] Implement logout and token cleanup.
- [x] Extract roles from token/session. — JWT payload decoded; Keycloak claim paths marked TODO
- [x] Create `UserSession` provider/state.
- [x] Add route guard/redirect for protected POS dashboard.
- [x] Add forbidden/unauthorized screen.

Acceptance:

- [x] Anonymous user cannot open protected POS dashboard.
- [x] User role is displayed after login.
- [x] Logout clears session and protected pages are blocked again.

## 6. Runtime settings

- [ ] Create settings API client.
- [ ] Load mobile/POS settings after auth or for public pre-auth screens if needed.
- [ ] Add safe defaults.
- [ ] Add feature toggle helper.
- [ ] Document settings as V1 feature-toggle/config mechanism before future Unleash.

Acceptance:

- [ ] POS can check a setting/flag without direct component API calls.
- [ ] Missing settings fail safely.

## 7. i18n runtime

- [ ] Add local mobile translations.
- [ ] Add backend i18n override client.
- [ ] Merge local translations with backend overrides.
- [ ] Backend wins when the same key exists on both sides.
- [ ] Support switching active language if needed for V1.

Acceptance:

- [ ] Local translation works offline or when backend override is unavailable.
- [ ] Backend override changes a visible label after merge.

## 8. Theme runtime

- [ ] Add Tchalanet default mobile theme.
- [ ] Create runtime theme provider/state.
- [ ] Support applying backend/tenant theme or preset when available.
- [ ] Keep custom theme builder out of V1.
- [ ] Align naming with Web theme concepts where practical.

Acceptance:

- [ ] App starts with Tchalanet default theme.
- [ ] Theme can be updated from runtime state.

## 9. Operational context foundation

- [ ] Create `OperationalContextView` model.
- [ ] Create `TerminalBindingView` model.
- [ ] Create `OperationalContextApi` placeholder/client.
- [ ] Create provider/state for current operational context.
- [ ] Show terminal/outlet/session state on POS dashboard.
- [ ] Do not implement sensitive sell/payout validation in the client; backend remains source of truth.

Acceptance:

- [ ] POS dashboard clearly displays missing/ready operational context.
- [ ] No sale flow can be started from this change unless operational context rules are explicitly implemented later.

## 10. POS dashboard skeleton

- [ ] Create protected POS dashboard route.
- [ ] Display user/session role.
- [ ] Display tenant if available.
- [ ] Display terminal binding placeholder/actual state.
- [ ] Display outlet/session placeholder/actual state.
- [ ] Display settings/i18n/theme readiness.

## 11. Minimal mobile/POS UI core

- [ ] Notice/banner component.
- [ ] Error panel/screen.
- [ ] Loading overlay/spinner.
- [ ] Empty state.
- [ ] Status badge.
- [ ] Operational context bar/card.
- [ ] Session status card.
- [ ] Sync/offline indicator placeholder.

Deferred until real POS flows:

- [ ] Ticket summary.
- [ ] Ticket line list.
- [ ] Money breakdown.
- [ ] Receipt preview.
- [ ] Payout status card.
- [ ] Pending sync list.

## 12. Prepare next V1 POS legs

- [ ] Document next steps for terminal binding.
- [ ] Document next steps for session open/close or selection.
- [ ] Document next steps for sell flow.
- [ ] Document next steps for print/send/verify flows.
- [ ] Keep cashier Web separate unless chosen for V1.

