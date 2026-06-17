# Tasks: simplify-tenant-admin-pagemodel-nav

## Analysis

- [x] Confirmed web is driven by backend `PageRuntimeResponse`; frontend override is not the right fix.
- [x] Identified `private.dashboard.tenant_admin.template.json` as the tenant admin template.
- [x] Identified `private_shell_tenantadmin.json` as the active shell fragment resolved by registry key
      `private_shell_tenant_admin`.
- [x] Identified `tenant_admin_sidebar.json` as a duplicate/adjacent fragment not currently referenced by
      the active template/registry.

## Implementation

- [x] Update `private_shell_tenantadmin.json` navigation to the compact seven-entry structure including
      `Tirages`.
- [x] Keep `tenant_admin_sidebar.json` aligned with the same compact structure, or document/remove it if
      confirmed unused.
- [x] Check i18n keys for all new labels in `fr`, `en`, and `ht`; add only missing fallback keys.
- [x] Add `Tirages` children (Tous les tirages, Tirages à venir, Tirages passés) to shell + sidebar.
- [x] Switch KPI/alerts/quickActions/readiness widgets to `dynamic` in template; update KPI items to
      seller_terminal model (salesToday, activeSellerTerminals, winningsToday, notificationCount).
- [x] Wire `TenantAdminDashboardPayloadAssembler`: add `GetTenantKpisQuery` (activeSellerTerminals proxy),
      `NotificationApi.getNotificationSummary` (notificationCount), update `buildQuickActions` to V0 routes.
- [ ] Check whether admin route placeholders are needed separately; do not solve that inside PageModel
      unless the backend already owns the route.

## Verification

- [x] Parse changed JSON resources.
- [ ] Run focused PageModel runtime tests only if requested/Java is available.
- [ ] Run `openspec validate simplify-tenant-admin-pagemodel-nav --strict` from `tchalanet-server`.
- [ ] Confirm working tree contains no accidental frontend changes.
