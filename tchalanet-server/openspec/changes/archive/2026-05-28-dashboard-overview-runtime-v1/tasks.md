# Tasks — dashboard-overview-runtime-v1

Tasks are ordered by dependency. Do not start downstream runtime implementation before prerequisites are green.

## 0. Prerequisites — blocking

- [ ] Re-seed `PageModelTemplate` documents to collapse widget sources:
      - `public_home`
      - `public_draw_results`
      - `tenant_admin_dashboard`
      - `cashier_dashboard`
      - `platform_admin_dashboard`
      - `json_file`
- [x] Keep existing `widgetId` values stable.
- [ ] Create widget registry by `schema_version`.
- [x] Promote `PageModelType` to `core.pagemodel.api.model`.
- [ ] Promote published read contract to `core.pagemodel.api.query.FindPublishedPageModelQuery`.
- [ ] Expose `PageModelView` through `core.pagemodel.api`.
- [ ] Remove `core.pagemodel.internal.*` imports from `features`.
- [ ] Remove `core.pagemodel.internal.*` imports from `PageModelOnboardingService`.
- [ ] Add ArchUnit guard: no `core.pagemodel.internal..` imports outside `core.pagemodel`.

## 1. PageModel runtime

- [x] Add `PageModelResolutionContext` with failure-aware memoization.
- [x] Move/define `DynamicWidgetProvider` as a public API contract.
- [x] Extend `DynamicWidgetProvider.load(...)` to receive `PageModelResolutionContext`.
- [ ] Add `DynamicWidgetProviderRegistry`.
- [ ] Registry injects `List<DynamicWidgetProvider>` without importing feature packages.
- [x] Provider error -> `dynamic.errors`.
- [ ] Remove silent `catch -> empty` from providers.
- [x] Unknown `widgetId` -> explicit `dynamic.error`.
- [x] Runtime keeps original HTTP context; providers do not recreate/repair context.
- [ ] Validate template `widgetId` against registry during seed/merge where practical.

## 2. Static fragments

- [x] Add/update `public_header_links.json`.
- [x] Add/update `public_footer_links.json`.
- [ ] Add/update `private_header_profile_menu.json`.
- [ ] Add/update `private_footer_links.json`.
- [x] Add/update `private_sidebar_tenant_admin.json` (`private_shell_tenantadmin.json`).
- [x] Add/update `private_sidebar_platform_admin.json` (`private_shell_superadmin.json`).
- [x] Add/update `private_sidebar_cashier.json` (`private_shell_cashier.json`).
- [x] Add/update `private_cashier_quick_actions.json`.
- [ ] Add `file_key` allowlist in `json_file` provider.
- [x] Static paths are frontend routes, not backend `/api/v1/**` endpoints.

## 3. Tenant readiness

- [x] Add `GetTenantReadinessQuery` for tenant/admin context.
- [x] Add `TenantReadinessSummary`.
- [x] Add `TenantReadinessView`.
- [x] Implement one readiness handler/assembler.
- [x] Wire identity, outlets, terminals, sellers section checks in assembler.
- [x] Ensure dashboard consumes only `TenantReadinessSummary`.
- [x] Ensure overview/provisioning can consume `TenantReadinessView`.
- [ ] Add short TTL via `CacheSpecProvider` if needed.
- [ ] Test: `TenantReadinessSummary` has no section fields.
- [ ] Test: readiness excludes KPI fields such as `salesToday`.

## 4. Public runtime

- [x] Finalize `public.home` PageModel.
- [x] `public_home` for hero/features/plans/trust.
- [x] `public_draw_results` for latest/next results.
- [x] `home.check_ticket` is static/route-only for V1 unless a public provider is needed.
- [x] Defer `news`/`testimonials` if no real content exists.
- [x] Finalize `public.draw_results`.
- [x] Implement `PublicHomeProvider`.
- [x] Implement `PublicHomePayloadAssembler`.
- [x] Implement `PublicDrawResultsProvider`.
- [x] Public pages do not load private data.
- [x] Public pages work without authentication.

## 5. Tenant admin runtime

- [x] Align tenant admin sidenav with V1 navigation.
- [x] Widget sources -> `tenant_admin_dashboard`.
- [x] Implement `TenantAdminDashboardProvider`.
- [x] Implement `TenantAdminDashboardPayloadAssembler`.
- [x] Dashboard includes KPI, alerts, notifications, readiness summary, summaries, quick actions.
- [x] Dashboard excludes settings/i18n/list details and management lists.
- [x] Add `GET /admin/overview`.
- [x] Implement `TenantAdminOverviewController`.
- [x] Implement `TenantAdminOverviewService`.
- [x] Overview sections return status, summary, issues and route.
- [x] Overview does not repeat any dashboard KPI.

## 6. Cashier runtime

- [x] Keep `GET /tenant/cashier/home` compact for POS/mobile.
- [x] Mark `/tenant/cashier/web-home` as transitional or remove once PageModel cashier web is ready.
- [x] Cashier web widget sources -> `cashier_dashboard`.
- [x] Implement `CashierWebDashboardProvider`.
- [x] Implement `CashierDashboardPayloadAssembler`.
- [x] No cashier overview in V1.

## 7. Platform admin runtime

- [x] Align platform admin sidenav.
- [x] Widget sources -> `platform_admin_dashboard`.
- [x] Implement `PlatformAdminDashboardProvider`.
- [x] Implement `PlatformAdminDashboardPayloadAssembler`.
- [x] Add `GET /platform/overview`.
- [x] Implement `PlatformAdminOverviewController`.
- [x] Implement `PlatformAdminOverviewService` (`PlatformAdminOverviewOrchestrator`).
- [x] Platform overview sections return status, summary, issues and route.
- [x] Platform overview is not dashboard bis.

## 8. Tenant provisioning

- [x] Define `TenantProvisioningProfile`: `MINIMAL`, `DEFAULT_HAITI_LOTTERY`, `DEMO`.
- [x] Define preview view.
- [x] Define result view.
- [x] Exclude `CUSTOM_FROM_TENANT` from V1.
- [x] Provisioning orchestrator calls owning domains only.
- [x] No direct table copy in feature orchestrator.
- [x] Result returns readiness / next steps through shared readiness.

## 9. Tests

- [ ] E2E public home loads without auth.
- [ ] E2E public draw results loads without auth.
- [ ] E2E tenant admin dashboard resolves `tenant_admin_dashboard`.
- [ ] E2E tenant overview returns structural sections and no KPI fields.
- [ ] E2E cashier POS home works independently of PageModel.
- [ ] E2E cashier web dashboard resolves `cashier_dashboard`.
- [ ] E2E platform dashboard resolves `platform_admin_dashboard`.
- [ ] E2E platform overview returns sections and no tenant KPI detail list.
- [ ] Test grouped reads per surface respect design targets.
- [ ] Test unknown `widgetId` -> explicit `dynamic.error`.
- [ ] Test provider failure is memoized and not retried once per widget.
