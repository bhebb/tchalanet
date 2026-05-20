# Tasks

## 1. Verify current schema

- [x] Confirm `draw_channel` already has `result_slot_id`.
- [x] Confirm `draw_channel` already has `draw_time`.
- [x] Confirm `draw_channel` already has `timezone`.
- [x] Confirm `draw_channel` already has `cutoff_sec`.
- [x] Confirm `draw_channel` already has `days_of_week` or equivalent.
- [x] Add missing `draw_channel.sales_open_time` to the existing create-table migration.
- [x] Add missing `draw_channel_aud.sales_open_time` to the existing audit create-table migration.
- [x] No new migration was created.

## 2. Update draw channel provisioning / seeds

- [x] Update the query/code that creates tenant `draw_channel` rows.
- [x] Copy default `draw_time` and `timezone` from `result_slot`.
- [x] Set `cutoff_sec` from template/default.
- [x] Set `sales_open_time` from template/default.
- [x] Set `days_of_week` from template/default.
- [x] Keep `active` configurable by template/default.
- [x] Ensure idempotency with `ON CONFLICT DO NOTHING` or equivalent.

## 3. Update catalog calendar row

- [x] Update `DrawChannelCalendarRow` to expose `drawTime`, `timezone`, `salesOpenTime`, `cutoffSec`, `daysOfWeek`, `resultSlotId`.
- [x] Update `DrawChannelCatalog.listCalendarRows(...)` implementation to return these fields.
- [x] Ensure rows are tenant-scoped and active-filtered when requested.

## 4. Update draw generation

- [x] Generate `scheduledAt` from `draw_channel.drawTime + draw_channel.timezone`.
- [x] Generate `cutoffAt = scheduledAt - cutoffSec`.
- [x] Validate `cutoffSec >= 0`.
- [x] Generate status `SCHEDULED`.
- [x] Preserve existing idempotency by `(tenant_id, draw_channel_id, draw_date)`.

## 4b. Update draw opening

- [x] Add `draw_channel.sales_open_time` as tenant/channel commercial opening configuration.
- [x] Default seed value is `05:30`.
- [x] Open scheduler runs every 5 minutes between 04:00 and 10:00 UTC.
- [x] Automatic open uses channel local date/time and falls back to configured `defaultSalesOpenTime`.
- [x] Keep writing `draw.opened_at` as the actual transition timestamp.

## 5. Simplify close due logic

- [x] Find due draws by `status = OPEN` and `cutoff_at <= now`.
- [x] Do not recalculate cutoff from tenant settings or result slot during close.
- [x] Keep locked draws skipped.
- [x] Keep dryRun support if already present.
- [x] Keep Ops force path for manual close.

## 6. Verify sell cutoff guard

- [x] Ensure ticket sale checks `draw.status = OPEN`.
- [x] Ensure ticket sale checks `now < draw.cutoffAt`.
- [x] Do not rely only on draw status because close scheduler can run late.

## 7. Update docs

- [x] Update scheduler/tick docs.
- [x] Document `result_slot` vs `draw_channel` vs `draw` boundary.
- [x] Document that `draw_channel.cutoff_sec` is tenant/channel business configuration.
- [x] Document that `draw.cutoff_at` is a generated snapshot.

## 8. Tests

- [ ] Test two tenants with same `result_slot` but different `cutoff_sec`.
- [ ] Confirm generated `draw.cutoff_at` differs by tenant.
- [ ] Confirm close picks up each draw based on its snapshot cutoff.
- [x] Confirm sell is rejected after cutoff even if draw is still `OPEN`.
