# Tasks

## 1. Runtime resolver

- [ ] Replace `PageModelTypeResolver.forDashboard(TchRole role)` with `forDashboard(TchRequestContext ctx)`.
- [ ] Add PLATFORM + SUPER_ADMIN -> `platform.dashboard.super_admin`.
- [ ] Add TENANT + SUPER_ADMIN + tenant override -> `private.dashboard.tenant_admin`.
- [ ] Add TENANT + TENANT_ADMIN -> `private.dashboard.tenant_admin`.
- [ ] Keep TENANT + CASHIER -> `private.dashboard.cashier`.
- [ ] Add optional/future TENANT + OPERATOR -> `private.dashboard.operator` if operator template is implemented.
- [ ] Add unit tests for all dashboard resolution cases.
- [ ] Ensure unsupported roles/scopes return a forbidden-style error.

## 2. Dashboard service split

- [ ] Replace ambiguous `resolve(logicalId, tenantIdOverride, lang)` usage for runtime paths.
- [ ] Add `resolveTenantDashboard(logicalId, langFromUrl)`.
- [ ] Add `resolvePlatformDashboard(logicalId, langFromUrl)`.
- [ ] Use `contextResolver.currentOrThrow()` for tenant and platform runtime.
- [ ] Tenant runtime must use `ctx.tenantId()` only.
- [ ] Platform runtime must not accidentally resolve tenant dashboard unless scope is TENANT.
- [ ] Preserve language resolution behavior with URL lang override, doc default lang, doc supported langs, and fallback `fr`.
- [ ] Preserve notification summary for authenticated dashboard runtime.

## 3. Controllers

- [ ] Update `TenantPageModelController.tenantDashboard` to call `typeResolver.forDashboard(ctx)`.
- [ ] Update `TenantPageModelController.tenantDashboard` to call `resolveTenantDashboard(...)`.
- [ ] Update `PlatformPageModelController.platformDashboard` to call `typeResolver.forDashboard(ctx)`.
- [ ] Update `PlatformPageModelController.platformDashboard` to call `resolvePlatformDashboard(...)`.
- [ ] Review `/{logicalId}` endpoints and either restrict them or mark them as preview/admin-only.
- [ ] Ensure no runtime tenant endpoint accepts a tenant id override from request payload/query.
- [ ] Confirm `@PreAuthorize` uses the correct authority/role convention for `SUPER_ADMIN`.

## 4. Dynamic provider contract

- [ ] Add deterministic `order()` to `PageModelDynamicProvider` or use Spring `@Order`.
- [ ] Update `PageModelDynamicResolver` to pick the lowest-order matching provider.
- [ ] Add provider-error logging server-side.
- [ ] Sanitize provider error messages returned to frontend.
- [ ] Keep `dynamic.errors` per widget.
- [ ] Add tests for no provider found.
- [ ] Add tests for provider exception.
- [ ] Add tests for provider priority/order.
- [ ] Add tests to ensure static widgets are ignored.

## 5. Public home template

- [ ] Normalize `public.home` widget binding sources:
  - `public.hero`
  - `public.draws.today`
  - `public.features`
  - `public.news.latest`
  - `public.tchala.featured`
  - `public.testimonials`
  - `public.plans`
- [ ] Keep `home.check_ticket` static with route `/verifier`.
- [ ] Preserve final public home order:
  - hero
  - draws/check ticket
  - features
  - news
  - tchala
  - testimonials
  - plans
- [ ] Ensure `meta.scope` uses the canonical value expected by backend.
- [ ] Ensure i18n keys follow project functional namespace conventions.

## 6. Cashier dashboard template

- [ ] Normalize cashier binding sources:
  - `cashier.overview`
  - `cashier.quick_sale`
  - `cashier.open_draws`
  - `cashier.recent_tickets`
  - `cashier.session`
- [ ] Add `open_draws` row between quick sale and recent tickets.
- [ ] Add `dashboard.cashier.open_draws` widget.
- [ ] Ensure template has `required_roles: ["CASHIER"]` if supported by model meta.
- [ ] Preserve cashier sidenav routes or align them with existing Angular routes.

## 7. Tenant admin dashboard template

- [ ] Add system template `private.dashboard.tenant_admin`.
- [ ] Add rows:
  - overview
  - sales
  - draws
  - outlets
  - users_limits
  - alerts
- [ ] Add widgets:
  - `dashboard.tenant_admin.overview`
  - `dashboard.tenant_admin.sales`
  - `dashboard.tenant_admin.draws`
  - `dashboard.tenant_admin.outlets`
  - `dashboard.tenant_admin.users`
  - `dashboard.tenant_admin.limits`
  - `dashboard.tenant_admin.alerts`
- [ ] Set sources:
  - `tenant_admin.overview`
  - `tenant_admin.sales_summary`
  - `tenant_admin.draws_today`
  - `tenant_admin.outlets`
  - `tenant_admin.users`
  - `tenant_admin.limits`
  - `tenant_admin.alerts`
- [ ] Ensure SUPER_ADMIN tenant override resolves to this template.
- [ ] Ensure template can support a visible tenant override banner in shell/context if the frontend consumes it.

## 8. Platform super admin dashboard template

- [ ] Add system template `platform.dashboard.super_admin`.
- [ ] Add rows:
  - overview
  - tenants
  - services
  - flags_jobs
  - audit_release
- [ ] Add widgets:
  - `dashboard.platform.overview`
  - `dashboard.platform.tenants`
  - `dashboard.platform.services`
  - `dashboard.platform.flags`
  - `dashboard.platform.jobs`
  - `dashboard.platform.audit`
  - `dashboard.platform.release_notes`
- [ ] Set sources:
  - `platform.overview`
  - `platform.tenants`
  - `platform.services.health`
  - `platform.flags`
  - `platform.jobs`
  - `platform.audit`
  - `platform.release_notes`

## 9. Public providers

- [ ] Rename/adapt `HeroProvider` -> `PublicHeroProvider`.
- [ ] Rename/adapt `DrawsProvider` -> `PublicDrawsProvider`.
- [ ] Rename/adapt `PlansProvider` -> `PublicPlansProvider`.
- [ ] Keep/adapt `PublicNewsProvider`.
- [ ] Add `PublicFeaturesProvider`.
- [ ] Add `PublicTchalaProvider`.
- [ ] Add `PublicTestimonialsProvider`.
- [ ] Ensure providers use QueryBus/catalog APIs only.
- [ ] Ensure provider payloads are stable frontend-friendly records/maps.

## 10. Cashier providers

- [ ] Keep/adapt `CashierOverviewProvider`.
- [ ] Add `CashierQuickSaleProvider`.
- [ ] Add `CashierOpenDrawsProvider`.
- [ ] Add `CashierRecentTicketsProvider`.
- [ ] Add `CashierSessionProvider`.
- [ ] Providers must use QueryBus/catalog APIs only.
- [ ] Providers must not compute money-critical rules.
- [ ] Providers must tolerate missing optional data with empty payloads or widget errors.

## 11. Tenant admin providers

- [ ] Add `TenantAdminOverviewProvider`.
- [ ] Add `TenantAdminSalesProvider`.
- [ ] Add `TenantAdminDrawsProvider`.
- [ ] Add `TenantAdminOutletsProvider`.
- [ ] Add `TenantAdminUsersProvider`.
- [ ] Add `TenantAdminLimitsProvider`.
- [ ] Add `TenantAdminAlertsProvider`.
- [ ] Providers must use QueryBus/catalog APIs only.
- [ ] Providers must not compute money/limits/payout rules directly.
- [ ] Providers should return summaries/projections, not domain aggregates.

## 12. Platform providers

- [ ] Add `PlatformOverviewProvider`.
- [ ] Add `PlatformTenantsProvider`.
- [ ] Add `PlatformServicesHealthProvider`.
- [ ] Add `PlatformFlagsProvider`.
- [ ] Add `PlatformJobsProvider`.
- [ ] Add `PlatformAuditProvider`.
- [ ] Add `PlatformReleaseNotesProvider`.
- [ ] Providers must use QueryBus/catalog APIs only.
- [ ] Platform providers must not depend on tenant RLS state unless explicitly resolving tenant override mode.

## 13. BFF response tests

- [ ] Public home resolves and returns dynamic payload for public widgets.
- [ ] Cashier dashboard resolves for CASHIER.
- [ ] Tenant admin dashboard resolves for TENANT_ADMIN.
- [ ] Tenant admin dashboard resolves for SUPER_ADMIN in TENANT override mode.
- [ ] Platform dashboard resolves for SUPER_ADMIN in PLATFORM mode.
- [ ] Provider failure returns sanitized `dynamic.errors`.
- [ ] Static widgets do not require providers.
- [ ] No provider found produces `NO_PROVIDER` widget error.
- [ ] Unsupported role/scope returns forbidden-style error.

## 14. Documentation / notes

- [ ] Document provider source names in the feature README or nearby feature doc.
- [ ] Document SUPER_ADMIN two-mode dashboard resolution.
- [ ] Document that PageModel/template admin remains out of scope.
- [ ] Add a short note explaining that providers are runtime BFF assemblers, not business-rule owners.
