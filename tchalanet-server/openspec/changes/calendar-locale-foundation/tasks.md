# Tasks — calendar-locale-foundation

## 1. SQL — V100 (extend in place)

- [x] Add `default_language varchar(8) NOT NULL DEFAULT 'fr'` to `tenant`.
- [x] Add `default_locale varchar(16) NOT NULL DEFAULT 'fr-HT'` to `tenant`.
- [x] Replace `draw.cancel_reason text` with `cancel_reason_code varchar(96)` + `cancel_reason_label varchar(255)`.
- [x] Add `result_slot_calendar_override` table — XOR shape `slot_local_date` (specific) vs `recurring_md` (year-less 'MM-dd'), `chk_...__shape` CHECK.

## 2. SQL — V101 audit

- [x] Update `draw_aud`: drop `cancel_reason`, add `cancel_reason_code` + `cancel_reason_label`.

## 3. SQL — V103 indexes

- [x] Two partial unique indexes on `result_slot_calendar_override`: `__specific` (slot, slot_local_date) and `__recurring` (slot, recurring_md).

## 3b. SQL — V213 seed provider no-draw days

- [x] `V213__seed_result_slot_calendar_override.sql`: Christmas as **recurring** rows (`recurring_md='12-25'`) for NY/FL/GA/TX state slots — year-less, never stale. Fixed-date holidays only; movable feasts (Easter) and one-offs are runtime-only. Re-runnable via `ON CONFLICT (result_slot_id, recurring_md) DO NOTHING`.

## 4. SQL — V105 RLS

- [x] Add policies for `business_day_override` (tenant + platform-select).
- [x] Add policies for `result_slot_calendar_override` (global read, restricted write).

## 5. Java — Draw cancel reason

- [x] `DrawJpaEntity`: split `cancelReason` into `cancelReasonCode` + `cancelReasonLabel`.
- [x] `Draw` aggregate: split field; update `cancel(...)` signature; update accessors.
- [x] `DrawMapper`: update read/write paths.
- [x] Update `CorrectAppliedDrawResultCommandHandlerTest` constructor call.

## 6. Java — Tenant locale

- [x] `TenantJpaEntity`: add `defaultLanguage` + `defaultLocale` columns.
- [x] `TenantRegistryJpaEntity`: add same columns.
- [x] `TenantBootstrapView`: add `defaultLanguage` + `defaultLocale`.
- [x] `TenantRegistryMapper`: parse columns with `fr` / `fr-HT` fallback.
- [x] `TenantConfigContextLookupTest`: update `TenantBootstrapView` construction.
- [x] `TenantInternalLocaleConfig` record + `TenantInternalSettings.locale` field.
- [x] `ConfigBackedTenantLocaleApi` replaces stub; reads via `TenantConfigReader`.
- [x] `TenantLocaleApi.resolveSupportedLanguages(TenantId)` added.
- [x] `tenantconfig/locale_config.json` template (auto-merged on tenant creation).

## 7. Java — draw-lifecycle impacts

- [x] `ResultSlotCalendarReaderPort` (core.draw port.out) + `ResultSlotCalendarJdbcAdapter` — `findUnavailableDates(slot, from, to)`, specific-overrides-recurring, recurring `MM-dd` materialized in Java.
- [x] Generation: `GenerateDrawsForRangeCommandHandler` skips provider-closed dates; `GenerateDrawsForRangeResult.skippedProviderClosed`.
- [x] Open-today: `resultSlotId` + `drawDate` added to `OpenableDrawRow`/projection/3 queries; `OpenTodayDrawsCommandHandler` splits open/cancel; `DrawLifecyclePort.bulkCancelScheduled`; `OpenDueDrawsResult.canceledProviderClosed`.

## 8. Catalog — provider calendar CRUD + 24h cache

- [x] `ResultSlotCalendarOverrideId` typed id + `StringTo…Converter` + `TypedIdRegistry` + `CommonIdMapper`.
- [x] `ResultSlotCalendarOverrideJpaEntity` (non-audited) + repository.
- [x] `ResultSlotCalendarOverrideView` + MapStruct mapper.
- [x] `ResultSlotCalendarCacheNames` + `ResultSlotCalendarCacheSpecProvider` (24h L1/L2).
- [x] `ResultSlotCalendarCatalog` + `…Impl` (`@Cacheable` `listBySlot`).
- [x] `ResultSlotCalendarAdminService` (create/update/softDelete + `@CacheEvict`).
- [x] `ResultSlotCalendarAdminController` `/platform/result-slots/{resultSlotId}/calendar` (SUPER_ADMIN).
- [x] Rewire `core.draw` reader → `ResultSlotCalendarReaderAdapter` over the cached catalog (dropped raw JDBC).

## 8b. Tenant business-day write surface (how a tenant says "closed")

- [x] `BusinessDayOverrideId` typed id + converter + `TypedIdRegistry` + `CommonIdMapper`.
- [x] `BusinessDayOverrideJpaEntity` (BaseEntity + explicit `tenant_id`, non-audited) + repository (natural-key finders for upsert + range list).
- [x] `BusinessDayOverrideView` + MapStruct mapper.
- [x] `BusinessDayOverrideAdminService` — idempotent upsert on (tenant, outlet-or-null, date), list, softDelete; tenant from context, RLS-isolated.
- [x] `BusinessDayOverrideController` `/tenant/business-days` (`GET`/`PUT`/`DELETE`), TENANT_ADMIN/SUPER_ADMIN.
- [x] Outlet immediate close (`outlet.day_closed`) already exists (`POST /admin/outlets/{id}/close-day`) — documented, no change.

## 9. Docs

- [x] `docs/CALENDARS.md` — two calendars, storage/priority, lifecycle impacts, CRUD + 24h cache, resolver/port map.
- [x] Link from `tchalanet-server/CLAUDE.md` and `core/draw/CLAUDE.md`.

## 10. Verification

- [x] `./mvnw -o install -pl tchalanet-common,tchalanet-catalog -am -DskipTests` → BUILD SUCCESS.
- [x] `./mvnw -o compile -pl tchalanet-core` → BUILD SUCCESS.
- [ ] Run module tests.
- [ ] DB regenerated and Flyway clean.
- [ ] Manual: `POST /platform/result-slots/{id}/calendar {recurringMd:'12-25',available:false,reasonCode:'PROVIDER_CLOSED'}` → 201; `GET` lists it; second create with both/neither shape → 400.
