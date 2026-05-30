# Calendars — Business vs Provider

Two calendars govern whether a draw is sold and whether it really happens. They
have **different owners, different sources of truth, and different effects**.
Never mix them.

```
Business calendar (tenant/outlet)      ≠      Provider/result-slot calendar
"can this commerce sell right now?"           "is there a real draw for this slot?"
owner: TENANT_ADMIN / outlet manager          owner: SUPER_ADMIN / platform ops
```

## 1. The two calendars

### Business calendar — *can this commerce sell?*
Drives `OpenSalesSession` eligibility and the ability to sell. Owned by the
tenant. Examples: tenant closed on Sundays, outlet closed for maintenance, local
holiday, voluntary closure.

### Provider/result-slot calendar — *is there a real draw?*
Drives draw **generation** and **opening**. Owned by the platform. Examples:
NY Lottery has no draw on Christmas; a provider is exceptionally unavailable.

## 2. Business cases

- **A — tenant/outlet closed, provider runs.** The POS cannot sell; the provider
  may still produce a global result. A draw generated with zero sales settles
  normally with zero payouts — settlement of an empty draw is valid.
- **B — provider closed (no draw).** No real draw exists; we must not generate /
  open / sell the draws bound to that result_slot for that date.
- **C — tenant opts out of a specific draw.** Not modeled yet
  (`draw_channel_calendar_override`, future).

## 3. Storage and priority

### Business day (tenant/outlet)
Evaluated highest-wins:
```
1. outlet.day_closed = true   → CLOSED   (immediate operational flag, absolute gate)
   else ↓
2. outlet business_day_override (outlet_id = X, dated)   → wins over tenant
   else ↓
3. tenant business_day_override (outlet_id IS NULL, dated)
   else ↓
4. tenant.config.rules.businessCalendar.closedWeekdays   (e.g. SUNDAY)
   else ↓
5. defaultOpen                → else OPEN
```
`business_day_override` is tenant-scoped (RLS). `business_date` is a `LocalDate`
in the resolved business timezone (outlet → tenant → UTC).

**Where it's resolved (single source of truth):** the full ladder is assembled
in `JdbcSalesSessionOpeningContextAdapter` (core.session): step 1
(`outlet.day_closed`) short-circuits to closed; otherwise it calls
`TenantBusinessCalendarApi.resolveBusinessDay(...)` (platform.tenantconfig) for
steps 2-5. The split exists because `outlet.day_closed` lives in `core.outlet`
and the platform API must not depend on core — so core.session does the merge. A
force-open override (`open=true`) **cannot** reopen an outlet whose
`day_closed=true` (step 1 wins).

**One table, ownership split (V1):** `business_day_override` has a nullable
`outlet_id`. Each module touches only its slice — never the other's:
| Slice | Writer module | Rows |
|-------|---------------|------|
| Tenant-level | `platform.tenantconfig` | `outlet_id IS NULL` |
| Outlet-level | `core.outlet` (validates outlet exists/belongs/active) | `outlet_id IS NOT NULL` |

The calendar reader (`JdbcTenantBusinessCalendarOverrideReader`) reads both slices
(it partitions on `outlet_id`), so resolution is unaffected by the write split.

### Provider/result-slot calendar
Global table `result_slot_calendar_override` (**no `tenant_id`** — provider truth
is global; one override affects every tenant bound to that slot).

Two mutually-exclusive (XOR) shapes — see `chk_result_slot_calendar_override__shape`:

| Shape | Column | Use |
|-------|--------|-----|
| Specific | `slot_local_date` (date) | movable feasts (Easter), one-off closures |
| Recurring | `recurring_md` ('MM-dd') | fixed annual holidays (Christmas `'12-25'`) |

- `available = false` → no draw that day. `available = true` → force-open exception.
- A **specific** dated row **overrides** a recurring rule for the same day.
- Both dates are in the **slot timezone** (`result_slot.timezone`).
- `recurring_md` is year-less and never goes stale — same convention as
  `HolidayUtils` (`MM-dd`) fed today from `application-uslottery.yaml`
  (`common.holidays`). Seeds are bootstrap only; runtime truth is this table,
  managed by SUPER_ADMIN.

## 4. Draw-lifecycle impacts

| Stage | Behavior |
|-------|----------|
| **Generation** | For each (channel, date), if the channel's `result_slot` is unavailable that day, the draw is **not generated** (counter `skippedProviderClosed`). |
| **Open-today** | A `SCHEDULED` draw whose slot is unavailable on its `draw_date` is **CANCELED** (`cancel_reason_code='PROVIDER_CLOSED'`) instead of opened. Idempotent (`WHERE status='SCHEDULED'`). |
| **Sell** | Sells only on an `OPEN` draw. A draw never generated / cancelled / closed cannot be sold — no provider-calendar check on the hot path. |
| **Apply results** | Already filters `status='CLOSED'`, so `CANCELED` draws are ignored. No change. |
| **OPEN draw + sales, override added late** | Not corrected silently. Audited ops action (cancel / close-new-sales); refund flow (`CancelDrawWithSales`) is future work. |

Status model: V1 uses `CANCELED` + `cancel_reason_code`
(`PROVIDER_CLOSED`, `TENANT_OPERATION_CLOSED`, `OPS_MANUAL_CANCEL`,
`DRAW_CONFIGURATION_ERROR`, …). A dedicated `SKIPPED` status is parked for V2.

## 5. Resolvers & ports

| Class | Path | Role |
|-------|------|------|
| `SalesSessionBusinessDateResolver` / `Default…` | `core/session/internal/application/service/time/` | Business date for session open: outlet tz → tenant tz → UTC |
| `OutletOperationalSettingsReaderPort` | `core/session/internal/application/port/out/` | Reads `outlet.timezone` |
| `TenantZoneApi` / `DefaultTenantZoneApi` | `platform/tenantconfig/…` | Tenant timezone |
| `TenantBusinessCalendarApi` / `Default…` | `platform/tenantconfig/…` | Resolves business-day open/closed via the priority chain |
| `TenantBusinessCalendarOverrideReader` / `Jdbc…` | `platform/tenantconfig/internal/…` | Reads `business_day_override` |
| `TenantLocaleApi` / `ConfigBackedTenantLocaleApi` | `platform/tenantconfig/…` | Tenant locale / language |
| `ResultSlotCalendarCatalog` / `ResultSlotCalendarCatalogImpl` | `catalog/resultslot/…` | Cached (24h) read of provider overrides per slot |
| `ResultSlotCalendarReaderPort` / `ResultSlotCalendarReaderAdapter` | `core/draw/internal/…` | Materializes no-draw dates (specific + recurring) over the cached catalog |

## 6. Read-semantics queries

**Point-in-time** (is slot available on date D, slot tz):
```sql
WHERE result_slot_id = :slot AND deleted_at IS NULL
  AND (slot_local_date = :d OR recurring_md = to_char(:d,'MM-DD'))
ORDER BY slot_local_date NULLS LAST   -- specific overrides recurring
LIMIT 1;
```

**Upcoming provider-off list** (strictly `> :today` in slot tz; recurring rules
materialized to their next future occurrence) — used by the future
impact-preview endpoint:
```sql
SELECT slot_local_date AS off_date, reason_code, reason_label
FROM result_slot_calendar_override
WHERE result_slot_id = :slot AND deleted_at IS NULL AND available = false
  AND slot_local_date > :today
UNION ALL
SELECT CASE
         WHEN to_date(extract(year from :today)::int || '-' || recurring_md, 'YYYY-MM-DD') > :today
           THEN to_date(extract(year from :today)::int      || '-' || recurring_md, 'YYYY-MM-DD')
           ELSE to_date((extract(year from :today)::int + 1) || '-' || recurring_md, 'YYYY-MM-DD')
       END AS off_date, reason_code, reason_label
FROM result_slot_calendar_override
WHERE result_slot_id = :slot AND deleted_at IS NULL AND available = false
  AND recurring_md IS NOT NULL
ORDER BY off_date;
```

## 7. Admin surfaces

### How a tenant says "we are closed"
Four mechanisms, by need and owner (each maps to a precise endpoint):

| Need | Mechanism | Endpoint (owner) |
|------|-----------|------------------|
| Seller/responsable: "close **my current outlet** today" (POS, sick manager, emergency) | `outlet.day_closed` | `POST /tenant/outlet/current/close-day` — outlet resolved from **trusted operational context**, perm `outlet.day.close` (`CurrentOutletDayController`, core.outlet) |
| Admin: "this POS is closed **now / today**" | `outlet.day_closed` | `POST /admin/outlets/{outletId}/close-day` (`OutletAdminController`, core.outlet); reopen via the matching endpoint |
| Planned, dated, **one outlet** ("outlet A closed June 10") | `business_day_override` (`outlet_id` set) | `PUT /admin/outlets/{outletId}/business-days` + `GET …?from&to` + `DELETE …/{id}` (`OutletBusinessDayController`, core.outlet) |
| Planned, dated, **whole commerce** ("closed Jan 1") | `business_day_override` (`outlet_id IS NULL`) | `PUT /admin/business-days` + `GET …?from&to` + `DELETE …/{id}` (`BusinessDayOverrideController`, platform.tenantconfig) |
| Recurring ("closed every **Sunday**") | `tenant.config.rules.businessCalendar.closedWeekdays` | tenant config |

Notes:
- Outlet-level writes live in **core.outlet** because they validate the outlet
  (exists / belongs to tenant / ACTIVE); tenant-level writes live in
  **platform.tenantconfig** (no outlet knowledge). Same table, disjoint slices.
- The dated upserts are idempotent on (tenant, outlet-or-null, date) — calling
  twice just updates. Tenant from request context; RLS enforces isolation.
- The seller endpoint resolves the outlet from the **trusted** POS frame only —
  a seller can never close another outlet; it reuses the same
  `CloseOutletDayCommand` (validates open sessions, publishes `OutletDayClosedEvent`).

These all feed `TenantBusinessCalendarApi.resolveBusinessDay(...)` /
`outlet.day_closed`, which session-opening eligibility consults (ladder in §3).

- **Tenant admin**: `outlet.day_closed`, `business_day_override`,
  `tenant.config.rules.businessCalendar`. Cannot touch the provider calendar.
- **Super admin / platform**: `result_slot_calendar_override` (provider no-draw
  days, global), via SUPER_ADMIN CRUD under
  `/platform/result-slots/{resultSlotId}/calendar` (`ResultSlotCalendarAdminController`).
  Create accepts a specific `slotLocalDate` **or** a recurring `recurringMd`
  ('MM-dd', e.g. `12-25`) — exactly one. Reads are served by
  `ResultSlotCalendarCatalog`, cached **24h** (Caffeine/Redis), evicted on every
  write. Seeds (V213) bootstrap the known fixed holidays; the impact-preview
  endpoint is still a follow-up.

### Caching
Provider closures change rarely and are admin-managed with explicit eviction, so
they are cached for **24h** (`ResultSlotCalendarCacheSpecProvider`,
cache `catalog:resultslot:calendar:v1:by_slot`). Reads from generation /
open-today therefore hit the DB at most once per slot per 24h.
