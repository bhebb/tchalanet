# Change: calendar-locale-foundation

## Status

Draft V1 — derives from the "Business Calendar vs Provider Calendar" challenges document.

## Why

The session opening eligibility work added `business_day_override` but left
several adjacent gaps that block the next slice:

- No way to model **provider** calendar exceptions (US Independence Day,
  no-draw holidays). The draw generator must skip slots that are unavailable
  on a given date.
- `draw.cancel_reason` is a free-form `text` column. We cannot index, group, or
  reason about cancellations (e.g. count draws cancelled for
  `US_INDEPENDENCE_DAY`).
- Tenant locale/language is stored only inside `tenant.config` JSON. The hot
  paths (bootstrap, language resolver) need a fast, typed source of truth.
- `business_day_override` exists but is not yet covered by RLS, so it leaks
  cross-tenant under the platform-cross-tenant context.

Everything below stays inside this change so we don't re-open V100.

## Goals

- Add `result_slot_calendar_override` (global slot calendar — provider closures), seeded with the well-known US-state holidays (Christmas, Easter) so a fresh DB skips them out of the box.
- Replace `draw.cancel_reason` with `cancel_reason_code` + `cancel_reason_label`.
- Add `tenant.default_language` and `tenant.default_locale` columns + audit mirror.
- Wire `TenantLocaleApi` to the tenant catalog so it reads the new columns.
- Add RLS policies for `business_day_override` (and a SUPER_ADMIN-only policy
  for `result_slot_calendar_override`, which is global).

## Non-goals

- A `SKIPPED` draw status. We keep V1 = `CANCELED` + reason code; SKIPPED is
  parked for V2.
- An admin endpoint to manage `result_slot_calendar_override` at runtime.
  V1 ships the table + RLS + a read port consumed by the draw lifecycle; the
  write endpoint and preview-impact API are a follow-up change.
- Sell-handler check against `result_slot_calendar_override`. We accept the
  documented manual ops process for an OPEN draw that already has sales.

Now **in scope** (was deferred, pulled in on user request): the draw generator
skips provider-closed dates and open-today cancels `SCHEDULED` draws whose slot
became unavailable — via the new `ResultSlotCalendarReaderPort`.

## Key decisions

- **`slot_local_date`** (not `local_date`) — date in the *slot* timezone
  (`result_slot.timezone`), not UTC, not tenant.
- **`cancel_reason_code` is a `varchar(96)`** — canonical codes only:
  `PROVIDER_CLOSED`, `PROVIDER_UNAVAILABLE`, `OPS_MANUAL_CANCEL`,
  `TENANT_OPERATION_CLOSED`, `DRAW_CONFIGURATION_ERROR`. Free-form label
  lives in `cancel_reason_label`.
- **No new Flyway migration** — we extend V100/V101/V103/V105 in place
  (project policy: regenerate DB).
- **Tenant locale columns are the source of truth** for fast access
  (bootstrap, language resolver). `tenant.config` JSON keeps evolving
  options (`supportedLanguages`, fallback chain).
- **`result_slot_calendar_override` is global** (no `tenant_id`). RLS is
  policed by `current_role` / `allow_platform_cross_tenant_select`; writes
  are SUPER_ADMIN only (enforced at the controller layer when added).
