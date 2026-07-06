# Calendars — Business vs Provider

Two calendars govern whether a draw is sold and whether it really happens. They
have **different owners, different sources of truth, and different effects**.
Never mix them.

```
Business calendar (tenant)             ≠      Provider/result-slot calendar
"can this commerce sell right now?"           "is there a real draw for this slot?"
owner: TENANT_ADMIN                            owner: SUPER_ADMIN / platform ops
```

## 1. The two calendars

### Business calendar — *can this commerce sell?*
Drives tenant-level sale availability. Owned by the tenant. Examples: tenant
closed on Sundays, local holiday, voluntary closure.

Seller-terminal availability is not a tenant-settings calendar concern. The
active operational actor is `SellerTerminal`; its immediate availability is
represented by `core.sellerterminal` status and validation. If V2 needs planned,
dated seller-terminal closures, the writer must live in `core.sellerterminal`.

### Provider/result-slot calendar — *is there a real draw?*
Drives draw **generation** and **opening**. Owned by the platform. Examples:
NY Lottery has no draw on Christmas; a provider is exceptionally unavailable.

## 2. Business cases

- **A — tenant closed, provider runs.** The POS cannot sell; the provider
  may still produce a global result. A draw generated with zero sales settles
  normally with zero payouts — settlement of an empty draw is valid.
- **B — provider closed (no draw).** No real draw exists; we must not generate /
  open / sell the draws bound to that result_slot for that date.
- **C — tenant opts out of a specific draw.** Not modeled yet
  (`draw_channel_calendar_override`, future).

## 3. Storage and priority

### Business day (tenant)
Evaluated highest-wins:
```
1. tenant business_day_override (outlet_id IS NULL, dated)
   else ↓
2. tenant.config.rules.businessCalendar.holidays         (recurring MM-dd)
   else ↓
3. tenant.config.rules.businessCalendar.closedWeekdays   (e.g. SUNDAY)
   else ↓
4. defaultOpen                → else OPEN
```
`business_day_override` is tenant-scoped (RLS). `business_date` is a `LocalDate`
in the resolved tenant business timezone.

**Where it's resolved:** `TenantBusinessCalendarApi.resolveBusinessDay(...)`
(`platform.tenantconfig`) resolves tenant-level overrides and recurring tenant
rules. Seller-terminal sale eligibility is validated separately by
`core.sellerterminal` / `core.sales`.

**Where it's applied:**

| Surface | Class | Effect |
|---------|-------|--------|
| POS available draws | `PosDrawsService` | Returns no sellable draw when the tenant business day is closed. |
| POS home primary action | `PosHomeService` via `PosDrawsService` | Disables the sell action when no draw is available because the tenant is closed. |
| Final/prepared sale | `SalePreparationOrchestrator` | Rejects sale with `sales.tenant_closed` before ticket persistence. |

Tenant business calendar is **not** used by draw generation/opening. A tenant
can be closed while the provider draw still exists; in that case the draw may
settle with zero sales.

**Table shape:** `business_day_override` still has a nullable legacy `outlet_id`
column. In the current model, tenant-level rows use `outlet_id IS NULL`.
Seller-terminal-specific dated closures are not implemented; if introduced, they
must use seller-terminal concepts rather than reviving outlet ownership.

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
| `TenantZoneApi` / `DefaultTenantZoneApi` | `platform/tenantconfig/…` | Tenant timezone |
| `TenantBusinessCalendarApi` / `Default…` | `platform/tenantconfig/…` | Resolves business-day open/closed via the priority chain |
| `TenantBusinessCalendarOverrideReader` / `Jdbc…` | `platform/tenantconfig/internal/…` | Reads tenant-level `business_day_override` |
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
Two active tenant-level mechanisms:

| Need | Mechanism | Endpoint (owner) |
|------|-----------|------------------|
| Planned, dated, **whole commerce** ("closed Jan 1") | `business_day_override` (`outlet_id IS NULL`) | `PUT /admin/business-days` + `GET …?from&to` + `DELETE …/{id}` (`BusinessDayOverrideController`, platform.tenantconfig) |
| Recurring ("closed every **Sunday**") | `tenant.config.rules.businessCalendar.closedWeekdays` | tenant config |

Notes:
- Tenant-level writes live in **platform.tenantconfig**. Tenant comes from
  request context; RLS enforces isolation.
- Seller-terminal status and sale eligibility live in **core.sellerterminal** /
  **core.sales**. Planned seller-terminal-specific closures are not part of V1.

These tenant mechanisms feed `TenantBusinessCalendarApi.resolveBusinessDay(...)`.

- **Tenant admin**: `business_day_override`,
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
