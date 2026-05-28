# Tasks — harden-pagemodel-security-v2

## 1. Security

- [x] Add `PageModelAccessPolicy`.
- [ ] Add `PageModelAccessRule`.
- [ ] Add `PageModelSecurity` metadata to private PageModel documents.
- [x] Ensure private PageModel access is checked before dynamic providers load.
- [x] Ensure dynamic providers revalidate sensitive access.
- [x] Return 403 for unauthorized private PageModel access.
- [x] Remove any endpoint that lets a client freely request a private admin/superadmin page by arbitrary id/context/file_key.

## 2. Public boundary

- [ ] Mark public providers as public-safe.
- [x] Ensure public PageModels never expose private routes or admin/superadmin metadata.
- [ ] Move public/internal news consumption to `platform.publiccontent.api` when ready.
- [ ] Add tests proving anonymous public home works and exposes only public-safe widgets.

## 3. Cashier concept

- [x] Document cashier as a private operational surface.
- [x] Ensure cashier PageModel uses only cashier providers and cashier routes.
- [x] Ensure cashier can show operational readiness without granting admin powers.
- [x] Ensure cashier cannot receive tenant admin or superadmin navigation fragments.

## 4. Predictable contract

- [x] Add typed records for `ImageRef`, `BrandBlock`, `NavigationEntry`, `NavigationSection`, `Badge`.
- [x] Add typed records for `PublicHeader`, `TopAppBar`, `NavigationDrawer`, `ShellFragment`.
- [x] Add typed records for `HeroPayload`, `KpiGridPayload`, `QuickActionsPayload`, `AlertsPayload`, etc.
- [x] Add typed records for `ActionItem`, `AlertItem`.
- [x] Replace main rendering `Map<String, Object>` payloads with typed DTO/records.
- [x] Allow maps only under `meta` or `ext`.
- [x] Normalize `path` vs `route`: use `path` in rendering contracts.
- [x] Normalize `label_key` vs `label`: static text uses `label_key`, dynamic backend text may use `label`.
- [x] Normalize all image fields to `ImageRef` (record defined; wiring deferred to V2 where image fields appear).

## 5. Shell fragments

- [x] Add/regenerate public shell fragments.
- [x] Add/regenerate cashier private shell fragments.
- [x] Add/regenerate tenant admin private shell fragments.
- [x] Add/regenerate superadmin private shell fragments.
- [ ] Ensure private top app bar contains no main destinations.
- [x] Ensure private navigation drawer contains brand + destinations.
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
