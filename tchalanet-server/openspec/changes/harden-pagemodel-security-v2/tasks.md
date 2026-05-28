# Tasks — harden-pagemodel-security-v2

## 1. Security

- [ ] Add `PageModelAccessPolicy`.
- [ ] Add `PageModelAccessRule`.
- [ ] Add `PageModelSecurity` metadata to private PageModel documents.
- [ ] Ensure private PageModel access is checked before dynamic providers load.
- [ ] Ensure dynamic providers revalidate sensitive access.
- [ ] Return 403 for unauthorized private PageModel access.
- [ ] Remove any endpoint that lets a client freely request a private admin/superadmin page by arbitrary id/context/file_key.

## 2. Public boundary

- [ ] Mark public providers as public-safe.
- [ ] Ensure public PageModels never expose private routes or admin/superadmin metadata.
- [ ] Move public/internal news consumption to `platform.publiccontent.api` when ready.
- [ ] Add tests proving anonymous public home works and exposes only public-safe widgets.

## 3. Cashier concept

- [ ] Document cashier as a private operational surface.
- [ ] Ensure cashier PageModel uses only cashier providers and cashier routes.
- [ ] Ensure cashier can show operational readiness without granting admin powers.
- [ ] Ensure cashier cannot receive tenant admin or superadmin navigation fragments.

## 4. Predictable contract

- [ ] Add typed records for `ImageRef`, `BrandBlock`, `NavigationDestination`, `NavigationSection`, `ActionItem`, `Badge`, `AlertItem`.
- [ ] Add typed records for `PublicHeader`, `PrivateTopAppBar`, `NavigationDrawer`, `ShellFooter`.
- [ ] Add typed records for `HeroPayload`, `KpiGridPayload`, `QuickActionsPayload`, `AlertsPayload`, etc.
- [ ] Replace main rendering `Map<String, Object>` payloads with typed DTO/records.
- [ ] Allow maps only under `meta` or `ext`.
- [ ] Normalize `path` vs `route`: use `path` in rendering contracts.
- [ ] Normalize `label_key` vs `label`: static text uses `label_key`, dynamic backend text may use `label`.
- [ ] Normalize all image fields to `ImageRef`.

## 5. Shell fragments

- [ ] Add/regenerate public shell fragments.
- [ ] Add/regenerate cashier private shell fragments.
- [ ] Add/regenerate tenant admin private shell fragments.
- [ ] Add/regenerate superadmin private shell fragments.
- [ ] Ensure private top app bar contains no main destinations.
- [ ] Ensure private navigation drawer contains brand + destinations.
- [ ] Ensure all empty collections are returned as `[]`, not absent/null.

## 6. Tests

- [ ] CASHIER cannot access tenant admin PageModel.
- [ ] CASHIER cannot access superadmin PageModel.
- [ ] TENANT_ADMIN can access tenant admin PageModel.
- [ ] TENANT_ADMIN cannot access superadmin PageModel.
- [ ] SUPER_ADMIN can access superadmin PageModel.
- [ ] Anonymous cannot access private PageModels.
- [ ] Public home remains public.
- [ ] Provider-level security rejects wrong role.
- [ ] Snapshot/schema tests for public, cashier, tenant admin, superadmin shell.
- [ ] Contract tests verify no unexpected main fields in shell payloads.
