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
- [ ] Keep existing `widgetId` values stable.
- [ ] Create widget registry by `schema_version`.
- [ ] Promote `PageModelType` to `core.pagemodel.api.model`.
- [ ] Promote published read contract to `core.pagemodel.api.query.FindPublishedPageModelQuery`.
- [ ] Expose `PageModelView` through `core.pagemodel.api`.
- [ ] Remove `core.pagemodel.internal.*` imports from `features`.
- [ ] Remove `core.pagemodel.internal.*` imports from `PageModelOnboardingService`.
- [ ] Add ArchUnit guard: no `core.pagemodel.internal..` imports outside `core.pagemodel`.

## 1. PageModel runtime

- [ ] Add `PageModelResolutionContext` with failure-aware memoization.
- [ ] Move/define `DynamicWidgetProvider` as a public API contract.
- [ ] Extend `DynamicWidgetProvider.load(...)` to receive `PageModelResolutionContext`.
- [ ] Add `DynamicWidgetProviderRegistry`.
- [ ] Registry injects `List<DynamicWidgetProvider>` without importing feature packages.
- [ ] Provider error -> `dynamic.errors`.
- [ ] Remove silent `catch -> empty` from providers.
- [ ] Unknown `widgetId` -> explicit `dynamic.error`.
- [ ] Runtime keeps original HTTP context; providers do not recreate/repair context.
- [ ] Validate template `widgetId` against registry during seed/merge where practical.

## 2. Static fragments

- [ ] Add/update `public_header_links.json`.
- [ ] Add/update `public_footer_links.json`.
- [ ] Add/update `private_header_profile_menu.json`.
- [ ] Add/update `private_footer_links.json`.
- [ ] Add/update `private_sidebar_tenant_admin.json`.
- [ ] Add/update `private_sidebar_platform_admin.json`.
- [ ] Add/update `private_sidebar_cashier.json`.
- [ ] Add/update `private_cashier_quick_actions.json`.
- [ ] Add `file_key` allowlist in `json_file` provider.
- [ ] Static paths are frontend routes, not backend `/api/v1/**` endpoints.

## 3. Tenant readiness

- [ ] Add `GetTenantReadinessQuery` for tenant/admin context.
- [ ] Add `TenantReadinessSummary`.
- [ ] Add `TenantReadinessView`.
- [ ] Implement one readiness handler/assembler.
- [ ] Ensure dashboard consumes only `TenantReadinessSummary`.
- [ ] Ensure overview/provisioning can consume `TenantReadinessView`.
- [ ] Add short TTL via `CacheSpecProvider` if needed.
- [ ] Test: `TenantReadinessSummary` has no section fields.
- [ ] Test: readiness excludes KPI fields such as `salesToday`.

## 4. Public runtime

- [ ] Finalize `public.home` PageModel.
- [ ] `public_home` for hero/features/plans/trust.
- [ ] `public_draw_results` for latest/next results.
- [ ] `home.check_ticket` is static/route-only for V1 unless a public provider is needed.
- [ ] Defer `news`/`testimonials` if no real content exists.
- [ ] Finalize `public.draw_results`.
- [ ] Implement `PublicHomeProvider`.
- [ ] Implement `PublicHomePayloadAssembler`.
- [ ] Implement `PublicDrawResultsProvider`.
- [ ] Public pages do not load private data.
- [ ] Public pages work without authentication.

## 5. Tenant admin runtime

- [ ] Align tenant admin sidenav with V1 navigation.
- [ ] Widget sources -> `tenant_admin_dashboard`.
- [ ] Implement `TenantAdminDashboardProvider`.
- [ ] Implement `TenantAdminDashboardPayloadAssembler`.
- [ ] Dashboard includes KPI, alerts, notifications, readiness summary, summaries, quick actions.
- [ ] Dashboard excludes settings/i18n/list details and management lists.
- [ ] Add `GET /admin/overview`.
- [ ] Implement `TenantAdminOverviewController`.
- [ ] Implement `TenantAdminOverviewService`.
- [ ] Overview sections return status, summary, issues and route.
- [ ] Overview does not repeat any dashboard KPI.

## 6. Cashier runtime

- [ ] Keep `GET /tenant/cashier/home` compact for POS/mobile.
- [ ] Mark `/tenant/cashier/web-home` as transitional or remove once PageModel cashier web is ready.
- [ ] Cashier web widget sources -> `cashier_dashboard`.
- [ ] Implement `CashierWebDashboardProvider`.
- [ ] Implement `CashierDashboardPayloadAssembler`.
- [ ] No cashier overview in V1.

## 7. Platform admin runtime

- [ ] Align platform admin sidenav.
- [ ] Widget sources -> `platform_admin_dashboard`.
- [ ] Implement `PlatformAdminDashboardProvider`.
- [ ] Implement `PlatformAdminDashboardPayloadAssembler`.
- [ ] Add `GET /platform/overview`.
- [ ] Implement `PlatformAdminOverviewController`.
- [ ] Implement `PlatformAdminOverviewService`.
- [ ] Platform overview sections return status, summary, issues and route.
- [ ] Platform overview is not dashboard bis.

## 8. Tenant provisioning

- [ ] Define `TenantProvisioningProfile`: `MINIMAL`, `DEFAULT_HAITI_LOTTERY`, `DEMO`.
- [ ] Define preview view.
- [ ] Define result view.
- [ ] Exclude `CUSTOM_FROM_TENANT` from V1.
- [ ] Provisioning orchestrator calls owning domains only.
- [ ] No direct table copy in feature orchestrator.
- [ ] Result returns readiness / next steps through shared readiness.

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
