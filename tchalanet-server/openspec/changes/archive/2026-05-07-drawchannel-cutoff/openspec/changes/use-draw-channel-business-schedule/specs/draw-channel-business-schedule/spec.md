# Spec: Draw channel business schedule

## ADDED Requirements

### Requirement: Draw channel owns tenant/channel sales cutoff

`draw_channel` MUST represent the tenant-scoped commercial configuration for selling a global `result_slot`.

It MUST support a tenant/channel cutoff duration before draw time, stored as `cutoff_sec` or equivalent.

It MUST support a tenant/channel sales opening time, stored as `sales_open_time` or equivalent.

#### Scenario: Same result slot with different tenant cutoffs

Given `result_slot` `NY_MID` has draw time `14:30`
And Tenant A has a `draw_channel` for `NY_MID` with `cutoff_sec = 120`
And Tenant B has a `draw_channel` for `NY_MID` with `cutoff_sec = 600`
When draws are generated for the same draw date
Then Tenant A's draw cutoff must be `14:28` local slot/channel time
And Tenant B's draw cutoff must be `14:20` local slot/channel time

### Requirement: Draw channel controls tenant/channel sales opening

Automatic draw opening MUST use `draw_channel.sales_open_time` when present.

If `draw_channel.sales_open_time` is null, automatic draw opening MUST use the configured
default sales opening time.

#### Scenario: Open draw using channel sales opening time

Given a tenant draw channel has `timezone = America/New_York`
And `sales_open_time = 05:00`
And a generated draw for the channel is `SCHEDULED`
When automatic open processing runs at `05:00 America/New_York`
Then the draw should transition to `OPEN`
And `draw.opened_at` should record the actual transition timestamp

#### Scenario: Open draw using fallback sales opening time

Given a tenant draw channel has no `sales_open_time`
And the configured default sales opening time is `05:30`
And a generated draw for the channel is `SCHEDULED`
When automatic open processing runs at `05:30` in the channel timezone
Then the draw should transition to `OPEN`

### Requirement: Draw generation snapshots scheduled and cutoff timestamps

Draw generation MUST calculate and store concrete `scheduled_at` and `cutoff_at` values on `draw`.

`cutoff_at` MUST be calculated from the tenant `draw_channel` cutoff configuration.

#### Scenario: Generate draw with channel cutoff

Given a tenant draw channel has `draw_time = 14:30`, `timezone = America/New_York`, and `cutoff_sec = 120`
When the system generates a draw for `2026-05-06`
Then `scheduled_at` must represent `2026-05-06 14:30 America/New_York`
And `cutoff_at` must represent `2026-05-06 14:28 America/New_York`

### Requirement: Selling uses draw cutoff snapshot

Ticket sale validation MUST use `draw.cutoff_at` and not recalculate from live channel settings.

#### Scenario: Scheduler close is late but sale is blocked

Given a draw is still `OPEN`
And `draw.cutoff_at` is before the current time
When a user attempts to sell a ticket for that draw
Then the sale MUST be rejected as cutoff passed

### Requirement: Close scheduler uses draw cutoff snapshot

The automatic close scheduler/handler MUST close eligible draws based on `draw.cutoff_at <= now`.

It MUST NOT use global scheduler cutoff settings to decide tenant/channel sale cutoff.

#### Scenario: Close due draws

Given an `OPEN` draw has `cutoff_at <= now`
When close due processing runs
Then the draw should transition to `CLOSED`

### Requirement: Result slot remains global result source metadata

`result_slot` MUST remain global and provider/result oriented.

It MUST NOT own tenant-specific sales cutoff rules.

#### Scenario: Fetch uses result slot

Given a global `result_slot` has provider metadata
When external result fetch runs
Then it should use `result_slot` information
And it should not depend on tenant `draw_channel.cutoff_sec`
