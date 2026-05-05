# Spec delta: core.draw

## ADDED Requirements

### Requirement: Draw lifecycle uses tenant timezone

Generate/open/close calculations SHALL use a tenant timezone resolver and fallback to UTC only when tenant timezone is unavailable.

#### Scenario: Daily generation

- GIVEN an active tenant with timezone `America/Port-au-Prince`
- WHEN daily generation runs
- THEN `from` is the tenant-local date
- AND draws are generated from `from` to `from + configuredDays`

### Requirement: Draw lifecycle commands are transactional

Generate, open, and close command handlers that mutate draw state SHALL run in a transaction.

#### Scenario: Open due draws

- WHEN open due draws command updates draw status
- THEN the handler is annotated with `@TchTx` or equivalent transactional boundary.

### Requirement: Draw aggregate is independent from catalog DTOs

The `Draw` aggregate SHALL NOT depend on `DrawChannelView` or catalog internal entities.

#### Scenario: Draw domain construction

- WHEN a Draw aggregate is constructed
- THEN it uses typed IDs and/or a minimal snapshot, not catalog DTOs.

### Requirement: Draw uses Instant for event moments

Draw domain event timestamps SHALL use `Instant`. Timezones are used only for calendar calculations and presentation.

### Requirement: Apply result uses slot-first matching

Applying a result SHALL match tenant draws by `draw_channel.result_slot_id`, `draw.draw_date`, and `draw.status = CLOSED`.

#### Scenario: Applying a draw result

- GIVEN a global draw_result for result_slot X and date D
- WHEN apply runs for tenant T
- THEN CLOSED draws whose channel references result_slot X and draw_date D are updated to RESULTED.

## MODIFIED Requirements

### Requirement: Draw projections are not domain models

`DrawSummary` and `DrawChannelSummary` SHALL be query projections, not domain model records.

## REMOVED Requirements

### Requirement: Draw batch query uses provider fields on draw_channel

Legacy queries using `draw_result.channel_code`, `draw_result.draw_date`, `draw_channel.external_game_key`, or `draw_channel.external_channel_code` SHALL be removed or migrated.
