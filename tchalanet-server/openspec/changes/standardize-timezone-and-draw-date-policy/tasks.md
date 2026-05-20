# Tasks: standardize-timezone-and-draw-date-policy

## 1. Shared time primitives

- [ ] Add `common.time.TchTimeProvider`.
- [ ] Add `common.time.DefaultTchTimeProvider` backed by injected `Clock`.
- [ ] Confirm single `Clock` bean exists and defaults to UTC/app zone only as runtime source, not business zone.
- [ ] Add tests for `today(zoneId)` and `nowAt(zoneId)` using fixed clock.

## 2. Draw scheduling

- [ ] Add `DrawScheduleSnapshot`.
- [ ] Add `DrawScheduleCalculator` or `DrawSchedulePlanner`.
- [ ] Ensure generated draw stores `scheduledAt` and `cutoffAt` as `Instant`/`timestamptz`.
- [ ] Ensure `drawDate` is documented and treated as channel-local date.
- [ ] Update `GenerateDrawsForRangeCommandHandler` to use the calculator.
- [ ] Preserve idempotency using unique key `(tenant_id, draw_channel_id, draw_date)`.

## 3. Open/close scheduler

- [ ] Update open-today logic to compute `channelToday` from `channel.timezone`.
- [ ] Update open-today logic to compute `channelNowTime` from `channel.timezone`.
- [ ] Keep close logic based on persisted `cutoffAt <= now`.
- [ ] Add structured logs containing `now`, `channelZone`, `channelDate`, and counts.

## 4. Result pipeline

- [ ] Add or standardize `ResultOccurredAtResolver`.
- [ ] Ensure fetch window uses result-slot/provider timezone, not tenant timezone.
- [ ] Ensure apply matches results to tenant draws using stable draw/channel/slot mapping, not server-local dates.
- [ ] Add observability for fallback-to-clock occurredAt resolution.

## 5. Sales cutoff

- [ ] Replace any `LocalDateTime` cutoff comparison with `Instant` comparison.
- [ ] Enforce `now < cutoffAt` for sale acceptance.
- [ ] Reject `now == cutoffAt`.
- [ ] Add tests for sale immediately before cutoff, exactly at cutoff, and after cutoff.

## 6. Offline sync

- [ ] Define accepted trusted offline sale timestamp source.
- [ ] Treat device timezone as metadata only.
- [ ] Reject or route to review when sale instant is unverifiable.
- [ ] Ensure offline acceptance uses `trustedSaleInstant < draw.cutoffAt`.
- [ ] Add tests for late sync after cutoff/result with trusted pre-cutoff sale instant.
- [ ] Add tests for late sync after cutoff/result without trusted sale instant.

## 7. Database/schema review

- [ ] Inventory draw/sales/result/offlinesync timestamp columns.
- [ ] Confirm event moments are `timestamptz`.
- [ ] Confirm `draw_date` is `date`.
- [ ] Confirm schedule times are `time`.
- [ ] Confirm zone identifiers are explicit strings or catalog references.
- [ ] Create migration only for unsafe columns; do not reinterpret legacy data silently.

## 8. Static enforcement

- [ ] Add ArchUnit/static test blocking `LocalDateTime` in `core/**/domain/**` and `core/**/application/**` except explicit allowlist.
- [ ] Add ArchUnit/static test blocking `ZoneId.systemDefault()` in business modules.
- [ ] Add static search/test blocking direct `Instant.now()` / `LocalDate.now()` in business modules.
- [ ] Add review checklist item for “semantic owner timezone identified”.

## 9. Documentation

- [ ] Update `docs/conventions/timezone.md` with draw-date policy.
- [ ] Add `docs/testing/timezone-critical-cases.md`.
- [ ] Add comments/Javadoc on `draw.drawDate` and `ResultOccurredAtResolver`.
- [ ] Update scheduler docs to state channel-local open-today behavior.

## 10. Verification

- [ ] Run unit tests for calculators.
- [ ] Run integration tests for draw generation/open/close.
- [ ] Run sales cutoff tests.
- [ ] Run offline sync acceptance tests.
- [ ] Run scheduler tests with fixed clock near UTC/channel/tenant midnight.
- [ ] Run full backend verification before merge.
