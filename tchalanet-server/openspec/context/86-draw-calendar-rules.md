# OpenSpec — Draw Calendar & Locale Rules (86)

> Status: NORMATIVE

## 1. Draw date timezone

`slot_local_date` is always the date in the **slot timezone** (`result_slot.timezone`), not UTC and not tenant timezone.

Never store draw dates as UTC-derived dates. Always derive the local date from the slot's own zone.

## 2. Cancel reason codes

`cancel_reason_code` is a `varchar(96)` restricted to the canonical set:

```
PROVIDER_CLOSED
PROVIDER_UNAVAILABLE
OPS_MANUAL_CANCEL
TENANT_OPERATION_CLOSED
DRAW_CONFIGURATION_ERROR
```

Free-form operator label lives in `cancel_reason_label`. No `SKIPPED` status in V1 — cancelled draws use `CANCELED` + reason code. `SKIPPED` is parked for V2.

## 3. Global slot calendar override

`result_slot_calendar_override` has no `tenant_id` — it is a global table (provider closures, public holidays). RLS is enforced via `current_role` / `allow_platform_cross_tenant_select`. Writes are SUPER_ADMIN only, enforced at the controller layer.

## 4. Tenant locale source of truth

`tenant.default_language` and `tenant.default_locale` columns are the **canonical source** for hot paths (bootstrap, language resolver). `tenant.config` JSON may hold extended options (`supportedLanguages`, fallback chain) but is not read on hot paths.

## 5. Draw generator policy

The draw generator skips provider-closed dates via `ResultSlotCalendarReaderPort`. A `SCHEDULED` draw whose slot became unavailable after it was created is cancelled via `OPS_MANUAL_CANCEL` or `PROVIDER_CLOSED` — not silently skipped.
