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

- [x] Add local mobile translations.
- [x] Add backend i18n override client.
- [x] Merge local translations with backend overrides.
- [x] Backend wins when the same key exists on both sides.
- [x] Support switching active language if needed for V1.

Acceptance:

- [x] Local translation works offline or when backend override is unavailable.
- [x] Backend override changes a visible label after merge.

## 8. Theme runtime

- [x] Add Tchalanet default mobile theme.
- [x] Create runtime theme provider/state.
- [x] Support applying backend/tenant theme or preset when available.
- [x] Keep custom theme builder out of V1.
- [x] Align naming with Web theme concepts where practical.

Acceptance:

- [x] App starts with Tchalanet default theme.
- [x] Theme can be updated from runtime state.

## 9-A. Cashier home data layer

All endpoints under `/tenant/cashier/*` require `Authorization: Bearer` and `X-Client-Surface: MOBILE_POS`.
`GET /home` returns three distinct states driven by `requiredStep`:
`SELECT_OPERATIONAL_CONTEXT` → setup screen, `OPEN_SESSION` → closed-session screen, `null` → operational home.

- [ ] `data/models/cashier_home_models.dart` — Dart records: `CashierHomeResponse`, `CashierHomeHeader`,
  `CashierHomeRequiredStep`, `CashierHomeOpCtx`, `CashierHomeSession`, `CashierHomeDrawSummary`,
  `HomeAction`, `HomeWidget`, `HomeNavigationItem`, `CashierReadinessResponse`.
- [x] `data/services/cashier_home_service.dart` — `GET /tenant/cashier/home` (header `X-Tch-Surface: MOBILE_POS`).
- [x] `data/services/cashier_readiness_service.dart` — `GET /tenant/cashier/readiness`.
- [x] `presentation/view_models/cashier_home_providers.dart` — `cashierHomeProvider` + `cashierReadinessProvider`.
- [x] Header `X-Tch-Surface: MOBILE_POS` added per-call (matches `ClientSurfaceResolver.HEADER_NAME`).

Acceptance:

- [x] `cashierHomeProvider` returns the correct state for each of the three server responses.
- [x] Missing header does not silently fail — `mapDioException` wraps 403 as typed `ApiException`.
- [x] `requiredStep` null-safety handled exhaustively via `needsOpContext / needsSession / isOperational`.

## 9-B. Operational context setup screen

Decision (2026-06-03): picker must use controlled backend lists — no free-form UUID.
Agreed endpoint: `GET /tenant/me/operational-context/options` →
`{outlets: OutletOption[], terminals: TerminalOption[], defaults?: {outletId?, terminalId?}}`.
Do NOT use `/tenant/outlets` (admin CRUD). Backend must add this endpoint before T9-B is unblocked.

- [ ] `data/services/cashier_op_context_service.dart` — `GET /operational-context/options`,
  `GET /operational-context/current`, `POST /operational-context/select`, `DELETE /operational-context`.
- [ ] Setup screen triggered when `home.requiredStep.type == SELECT_OPERATIONAL_CONTEXT`.
- [ ] Outlet picker → terminal picker (filtered by selected outlet) → `POST /select`.
- [ ] On success, `ref.invalidate(cashierHomeProvider)` and pop back to home.

Acceptance:

- [ ] Seller without operational context sees the setup screen (not the operational home).
- [ ] After selecting outlet + terminal, home screen renders operational state.

## 10. POS home screen

Driven entirely by `GET /tenant/cashier/home`. Three screen states must be covered:
- `requiredStep.type == SELECT_OPERATIONAL_CONTEXT` → `_SetupRequiredView` (btn "Configurer le poste" → T9-B flow).
- `requiredStep.type == OPEN_SESSION` → `_SessionClosedView` (btn "Ouvrir session" → `POST /session/open`).
- `requiredStep == null` → operational layout (mockup).

Operational layout components:
- [ ] **TopAppBar** — menu icon + "Tchalanet" + terminal ID (`#` + shortId from op context) + `OnlineBadge` + user avatar.
- [ ] **Primary action button** — `home.primaryAction` ("Vendre Ticket", h-128, disabled if `enabled == false`).
- [ ] **Quick actions grid** — 2-col: "Vérifier Ticket" + "Payer Gagnant" from `home.quickActions`.
- [x] **Sync button** — label "Actualiser", calls `ref.invalidate(cashierHomeProvider)` (V1 refresh-only).
  `POST /offline/sync` reserved for future offline — do NOT wire it until offline is activated.
- [x] **Stats section** — `home.session.salesTotal` (display-numeric) + `home.session.ticketCount`.
  Payout "Gagnants" widget shown only if `home.widgets` contains `POS_PAYOUT_STATUS`; hidden otherwise.
  Backend must add `payout_summary` widget to `CashierHomeResponse` to enable this stat.
- [ ] **Quick log** — deferred; last transaction needs `GET /tenant/cashier/tickets?size=1&sort=createdAt,desc`.
- [x] **Bottom navigation** — 4 tabs via `NavigationBar` + GoRouter.
  History = stub for now (can show ticket list once tickets service is wired). Reports = stub V1.

Refresh strategy (decided 2026-06-03): pull-to-refresh + app foreground return + after sell/pay/select.
No 30s polling. Local countdown timer for `primaryDraw.cutoffLabel` display; server revalidates at sell time.

Acceptance:

- [x] All three home states render without error.
- [x] Primary action button is disabled when `home.primaryAction.enabled == false`.
- [x] Stats section reflects live session data from `home.session`.
- [x] Tapping bottom nav "Sales" preserves active state; other tabs navigate to stub screens.

## 11. Design system components

- [x] `StatCard` / `StatCardLarge` — large display-numeric value, label, optional unit and accent border.
- [x] `PosActionButton` — large (h-128) and medium (h-112); icon + uppercase label; enabled/disabled.
- [x] `OnlineBadge` — animated pulse dot + label; animation scoped to `SingleTickerProviderStateMixin`.
- [x] `_PosBottomNavBar` — Material 3 `NavigationBar`, 4 destinations, active index via GoRouter.

Acceptance:

- [x] `PosActionButton` disabled uses `scheme.onSurface.withValues(alpha: 0.12/0.38)` — no custom hacks.
- [x] `OnlineBadge` pulse isolated in `_OnlineBadgeState` — no parent rebuilds.

## 12. Sell / verify / session flows — contract docs only

Do not implement these flows in this cycle. Document the contracts so the next cycle can start immediately.

- [ ] Document sell flow: `POST /tickets/preview` (validate) → confirm → `POST /tickets/sell` (idempotent) → `POST /tickets/{id}/print` or `/send`.
- [ ] Document verify flow: scan public code → `POST /tickets/verify` → display `CashierTicketVerificationResponse`.
- [ ] Document session lifecycle: `GET /session/current` on home load → `POST /session/open` (OPEN_SESSION required step) → `POST /session/close`.
- [ ] Document payout stat: backend must add `payout_summary` widget (type `POS_PAYOUT_STATUS`) to `CashierHomeResponse.widgets`
  with `{pendingCount, payableCount, paidTodayAmount, pendingAmount}`. No separate endpoint. Mobile shows widget if present.
- [ ] Keep cashier Web separate unless chosen for V1.

