# Tasks — private-bootstrap-v1

## 0. Pre-flight

- [ ] Confirm `platform.identity.api` exposes current-user query (userId, username,
      displayName, roles, preferredLocale, preferredTimezone).
- [ ] Confirm `platform.accesscontrol.api` exposes entitlements query + space assertion.
- [ ] Confirm `platform.tenantconfig.api` exposes locale, timezone, currency per tenant/space.
- [ ] Confirm `catalog.settings.api` exposes effective BOOLEAN settings query by scope.
- [ ] Confirm `platform.tenanttheme.api` exposes scope/mode/colors/logos.
- [ ] Confirm or plan `catalog.i18n` `privateBundle(locale, surface)` API — add if absent.
- [ ] Confirm cashier page-model endpoint: `/tenant/cashier/home` is a BFF (not PageModel).
      If a `GET /tenant/cashier/dashboard` page-model endpoint is needed, log as gap in
      `features.pagemodel` before implementing `PageModelRefResolver` for CASHIER space.
- [ ] Confirm `/summary` sub-endpoint existence on `TenantNotificationController` and
      equivalent admin/platform controllers. Note paths for frontend polling.
- [ ] Confirm multi-role priority rule (PLATFORM > ADMIN > CASHIER) with domain team.

## 1. Module scaffold — `features/runtime`

- [ ] Create `features/runtime/model/` with all records:
      `RuntimeBootstrapResponse`, `PrivateBootstrapSpace`, `AuthenticatedUserView`,
      `TenantContextView`, `RuntimeSettingsView`, `RuntimeThemeView`, `RuntimeI18nBundle`,
      `EntitlementsView`, `RuntimeReadinessView`, `RuntimeReadinessCheck`,
      `NotificationSummaryView`, `NotificationPreview`, `PageModelRef`,
      `RuntimeBootstrapNotice`.
- [ ] Create `features/runtime/mapper/RuntimeBootstrapMapper.java` (stub).
- [ ] Create `features/runtime/app/PageModelRefResolver.java` — pure space → endpoint mapping.
- [ ] Create `features/runtime/app/ReadinessFacade.java` — PLATFORM/ADMIN → READY;
      CASHIER → PARTIAL with terminal/session/seller MISSING checks (V1).
- [ ] Create `features/runtime/app/RuntimeBootstrapService.java` — context dispatch flow.
- [ ] Create `features/runtime/web/RuntimeBootstrapController.java` — single `GET /runtime/bootstrap`.

## 2. Facades wiring

- [ ] Wire `identityFacade` → `platform.identity.api`.
- [ ] Wire `accessFacade` → `platform.accesscontrol.api`; implement `assertSpaceAccessible()`.
- [ ] Wire `tenantConfigFacade` → `platform.tenantconfig.api` (locale, timezone, currency).
- [ ] Wire `settingsFacade` → `catalog.settings.api` (effective BOOLEAN settings).
- [ ] Wire `themeFacade` → `platform.tenanttheme.api`.
- [ ] Wire `i18nFacade` → `catalog.i18n` (after §0 pre-flight confirms API).
- [ ] Wire `notificationFacade` → `platform.notification.api`.

## 3. Context dispatch logic

- [ ] Implement `resolveSpace(TchRequestContext)`: SUPER_ADMIN → PLATFORM,
      TENANT_ADMIN → ADMIN, CASHIER/OPERATOR → CASHIER, else → 403.
- [ ] Implement `resolveTenantContext()`: null for PLATFORM, required for ADMIN/CASHIER.
- [ ] Implement `assembleSettings()`: tenantconfig + catalog.settings combined.
- [ ] Implement soft-failure handling: theme/i18n/notification failures → `notices` + partial,
      identity/entitlements failures → hard 403.

## 4. Notification summary endpoint (`platform.notification`)

- [ ] Check each existing notification controller for a `/summary` endpoint.
- [ ] If absent, add `GET /…/notifications/summary` to `TenantNotificationController` and
      equivalent admin/platform controllers — delegates to `NotificationService` only.

## 5. PageModelRef — cashier gap (if needed)

- [ ] If `/tenant/cashier/home` is BFF-only and no cashier page-model endpoint exists,
      document the gap and decide: reuse `home` endpoint or add a proper PageModel endpoint
      to `features.pagemodel`. Update `PageModelRefResolver` accordingly.

## 6. Security

- [ ] Enforce role-to-space resolution; unresolvable → 403 ProblemDetail.
- [ ] Tenant context validation: ADMIN/CASHIER require tenant in context.
- [ ] Verify tenant resolved from `TchRequestContext` only (never client body).

## 7. Tests

- [ ] `RuntimeBootstrapControllerTest`:
  - [ ] SUPER_ADMIN → space PLATFORM, null tenantContext, readiness READY.
  - [ ] TENANT_ADMIN → space ADMIN, tenantContext present, readiness READY.
  - [ ] CASHIER → space CASHIER, tenantContext present, readiness PARTIAL.
  - [ ] Unknown role → 403 ProblemDetail.
- [ ] `RuntimeBootstrapServiceTest`:
  - [ ] Settings: locale/timezone/currency from tenantconfig; features from catalog.settings.
  - [ ] Navigation absent from response.
  - [ ] Soft failure: theme missing → notices WARNING, response still returned.
  - [ ] Soft failure: i18n missing → notices WARNING, no bundle, response returned.
  - [ ] Soft failure: notifications fail → notices WARNING, unreadCount=0, response returned.
  - [ ] Hard failure: entitlements missing → 403.
- [ ] `RuntimeBootstrapSecurityTest`:
  - [ ] Unauthenticated → 401.
  - [ ] Wrong role → 403.
  - [ ] Tenant context absent for ADMIN → error.
  - [ ] pageModelRef endpoint matches space.
- [ ] Notification summary endpoints tested in `platform.notification` (if new).
