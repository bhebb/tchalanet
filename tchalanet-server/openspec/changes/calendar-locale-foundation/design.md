# Design: Calendar + Locale Foundation

## 1. Vocabulary

| Term | Meaning |
|------|---------|
| Provider calendar | Days when the provider (NY Lottery, etc.) does not draw |
| Business calendar | Days when a tenant or outlet is open |
| Slot local date | A `LocalDate` in the `result_slot.timezone` |
| Business date | A `LocalDate` in `outlet.timezone` → `tenant.timezone` → UTC |

## 2. Tables

### 2.1 `result_slot_calendar_override` (NEW, global)

```sql
CREATE TABLE result_slot_calendar_override (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    result_slot_id  uuid        NOT NULL REFERENCES result_slot(id),
    slot_local_date date        NULL,           -- specific occurrence (Easter, one-offs)
    recurring_md    varchar(5)  NULL,           -- year-less 'MM-dd' annual rule (e.g. '12-25')
    available       boolean     NOT NULL,
    reason_code     varchar(96) NOT NULL,
    reason_label    varchar(255) NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    created_by      uuid        NULL,
    updated_at      timestamptz NOT NULL DEFAULT now(),
    updated_by      uuid        NULL,
    deleted_at      timestamptz NULL,
    deleted_by      uuid        NULL,
    version         bigint      NOT NULL DEFAULT 0,
    CONSTRAINT chk_result_slot_calendar_override__shape CHECK (
        (slot_local_date IS NOT NULL AND recurring_md IS NULL)
        OR
        (slot_local_date IS NULL
         AND recurring_md ~ '^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$')
    )
);
-- two partial unique indexes (V103), one per shape.
```

- **Global**: no `tenant_id`. Reflects provider truth.
- **Two XOR shapes** (`chk_...__shape`): exactly one populated, never both, never
  neither.
  - `slot_local_date` — a **specific** dated occurrence (movable feasts like
    Easter, one-off provider closures).
  - `recurring_md` — a **year-less** `'MM-dd'` annual rule (fixed holidays like
    Christmas `'12-25'`). Never goes stale; the same convention as the existing
    `HolidayUtils` / `application-uslottery.yaml` (`common.holidays`).
- Both are interpreted *in the slot timezone* (`result_slot.timezone`). Callers
  convert `now` → slot zone before comparing.
- **`available = false`** is the common case (provider closure). `true` is a
  "force-open" exception; a specific `available=true` row **overrides** a
  recurring closure for the same day.

#### Read semantics

**(a) Point-in-time** — is slot `S` available on date `D` (slot tz)?
```sql
WHERE result_slot_id = :slot AND deleted_at IS NULL
  AND (slot_local_date = :d OR recurring_md = to_char(:d,'MM-DD'))
ORDER BY slot_local_date NULLS LAST   -- specific date wins over recurring
LIMIT 1;
```

**(b) Upcoming provider-off list** — only dates strictly `> now`, materializing
recurring rules to their **next future occurrence** (`:today` = today in slot tz):
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

The Java read port `ResultSlotCalendarReaderPort.findUnavailableDates(slot, from, to)`
implements (a) over a range: it materializes recurring `MM-dd` per year in
`[from,to]` and subtracts specific `available=true` rows.

### 2.2 `business_day_override` (EXISTS — RLS to add)

Already created with `created_by/updated_by/deleted_by/version`. Only the RLS
policy is missing — added in V105.

### 2.3 `draw.cancel_reason` → `cancel_reason_code` + `cancel_reason_label`

- `cancel_reason_code  varchar(96)` — canonical, indexable.
- `cancel_reason_label varchar(255)` — free-form context (e.g. `"US Independence Day"`).
- Old `cancel_reason text` is dropped (project policy: DB regenerated, no
  backfill migration).

### 2.4 `tenant.default_language` + `tenant.default_locale`

```sql
default_language varchar(8)  NOT NULL DEFAULT 'fr',
default_locale   varchar(16) NOT NULL DEFAULT 'fr-HT',
```

Audit mirror added to `tenant_aud`-equivalent if one exists (today: none —
`tenant` is platform registry, not audited). Nothing to mirror.

## 3. RLS

- `business_day_override`: standard tenant policies (FOR ALL with
  `current_tenant` guard + FOR SELECT with
  `allow_platform_cross_tenant_select`).
- `result_slot_calendar_override`: global; no tenant filter. Read open to all
  authenticated; writes require the platform actor (enforced at controller
  later).

## 4. Java surface

### 4.1 Draw aggregate

- `Draw.cancelReason` → `cancelReasonCode` + `cancelReasonLabel`.
- `Draw.cancel(reasonCode, reasonLabel, now)`.
- `cancelReasonCode` is required + non-blank; `cancelReasonLabel` is optional.

### 4.2 Tenant locale

Two layers:

1. **Fast-path columns** (`tenant.default_language`, `tenant.default_locale`)
   for hot reads like bootstrap context. Surfaced through
   `TenantBootstrapView.defaultLanguage` / `.defaultLocale`. `TenantRegistryMapper`
   reads with `fr` / `fr-HT` fallback.

2. **Typed config slice** (`tenant.config.locale`) for evolving options —
   supported languages, fallback chain. Modelled as:

   ```java
   public record TenantInternalLocaleConfig(
       String defaultLanguage,
       String defaultLocale,
       List<String> supportedLanguages,
       String fallbackLanguage) {}
   ```

   Added to `TenantInternalSettings.locale`. Bootstrapped from
   `tenantconfig/locale_config.json` (auto-merged by
   `TenantConfigService`).

`TenantLocaleApi` exposes:

```java
Locale resolveDefaultLocale(TenantId tenantId);
String resolveDefaultLanguage(TenantId tenantId);
List<String> resolveSupportedLanguages(TenantId tenantId);
```

Implementation (`ConfigBackedTenantLocaleApi`) reads via `TenantConfigReader`
(JSON-backed). The columns are reserved for downstream fast-path callers that
need locale on the bootstrap query without hitting the JSON.

`TenantConfig` aggregate is **not** modified in this slice (defer until a
command needs to mutate locale).

## 5. State machine

No change. SKIPPED is deferred. Cancel from any non-terminal state via
`Draw.cancel(code, label, now)`. Caller (handler) is responsible for the code
being a canonical value.

## 5b. Draw-lifecycle impacts (implemented)

| Stage | Behavior | Code |
|-------|----------|------|
| Generation (§7) | Skip generating a draw when its `result_slot` is unavailable on that date. Counter `skippedProviderClosed`. | `GenerateDrawsForRangeCommandHandler` + `ResultSlotCalendarReaderPort` |
| Open-today (§8) | A `SCHEDULED` draw whose slot is unavailable on its `draw_date` is `CANCELED` (`cancel_reason_code='PROVIDER_CLOSED'`) instead of opened. Idempotent (`WHERE status='SCHEDULED'`). | `OpenTodayDrawsCommandHandler` + `DrawLifecyclePort.bulkCancelScheduled` |
| Apply results (§9) | **No change** — apply already filters `status='CLOSED'`, so CANCELED is excluded. | — |
| OPEN draw + sales (§10) | Manual audited ops action; the sell hot-path does not consult the provider calendar. | docs only |
| SKIPPED status (§11) | Not added — `CANCELED` + `cancel_reason_code` covers V1. | — |

Full narrative + the resolver/port map live in `docs/CALENDARS.md`.

## 5c. Provider calendar catalog (CRUD + cache)

`result_slot_calendar_override` is global read-mostly referential data → it lives
in **catalog** (`catalog.resultslot`), not core:

- `ResultSlotCalendarCatalog.listBySlot(ResultSlotId)` — `@Cacheable` **24h**
  (`ResultSlotCalendarCacheSpecProvider`, cache `catalog:resultslot:calendar:v1:by_slot`).
- `ResultSlotCalendarAdminService` — create / update / delete, each `@CacheEvict`
  (allEntries). SUPER_ADMIN only.
- `ResultSlotCalendarAdminController` — `/platform/result-slots/{resultSlotId}/calendar`
  (`GET`, `POST`, `PUT/{overrideId}`, `DELETE/{overrideId}`). Create takes a
  specific `slotLocalDate` XOR `recurringMd` ('MM-dd'); the service enforces the XOR.
- `ResultSlotCalendarOverrideId` typed id + converter + `TypedIdRegistry`/`CommonIdMapper`.
- `core.draw`'s `ResultSlotCalendarReaderPort` is implemented by
  `ResultSlotCalendarReaderAdapter`, which reads the **cached** catalog (no raw
  JDBC) and materializes recurring 'MM-dd' over the range.

## 6. Out of scope (tracked elsewhere)

- Impact-preview endpoint (`…/calendar/{date}/impact`) — follow-up.
- Migrating `HolidayUtils` / `application-uslottery.yaml` holidays into the table
  (the `MM-dd` shape makes this trivial later).
- `CancelDrawWithSales` refund/credit flow; sell-path provider-calendar check.
- Event emission for open-today provider cancellations.
- `tenant.config.locale.supportedLanguages` extended use; `EffectiveLanguageResolver`
  integration.
