# draw-channels Specification

## Purpose
Define draw channel scheduling, provider calendars, tenant sale calendars, and platform-owned result-slot configs.
## Requirements
### Requirement: Draw generation is gated by the provider calendar; sale opening by the business calendar

Draw **generation** SHALL derive from active channels, the result-slot day-of-week/active flags, and
the provider/result-slot calendar (`result_slot_calendar_override`, global, platform-owned), never
from the tenant business calendar, and SHALL be idempotent via
`UNIQUE(tenant_id, draw_channel_id, draw_date)`. Sale **opening/availability** SHALL require a
generated draw, an active channel, being within the cutoff/open window, and the tenant business
calendar being open. The two calendars SHALL NOT be mixed: a date the tenant marks closed SHALL still
generate the provider draw if the provider runs, and the tenant simply cannot sell into it.

#### Scenario: Tenant closed but provider runs

- **WHEN** a date is closed on the tenant business calendar but the provider slot runs
- **THEN** the draw is still generated for that date
- **AND** the tenant POS cannot sell into it
- **AND** a generated draw with zero sales settles normally with zero payouts

#### Scenario: Provider closed

- **WHEN** the provider/result-slot calendar marks a date closed
- **THEN** no draw is generated for the bound result slot on that date

### Requirement: Calendar changes are non-retroactive and use explicit cancel reasons

Calendar changes SHALL NOT retroactively alter draws already opened, resulted, or settled. A
`SCHEDULED` draw whose slot becomes provider-unavailable SHALL be cancelled with `CANCELED` plus a
canonical `cancel_reason_code` (`PROVIDER_CLOSED` / `OPS_MANUAL_CANCEL` / `TENANT_OPERATION_CLOSED`),
never silently skipped. There is no `SKIPPED` status in V1.

#### Scenario: Slot becomes unavailable after scheduling

- **WHEN** a slot becomes provider-unavailable after a `SCHEDULED` draw was created
- **THEN** the draw is cancelled with a canonical reason code
- **AND** opened/resulted/settled draws are untouched

### Requirement: Business calendar resolution order (sale availability)

Tenant sale availability SHALL resolve highest-wins: dated `business_day_override`, then recurring
`businessCalendar.holidays`, then `businessCalendar.closedWeekdays`, then `defaultOpen`. Resolution
SHALL use `TenantBusinessCalendarApi.resolveBusinessDay(...)`. Per-date, per-channel tenant opt-out
(`draw_channel_calendar_override`) is a future capability, not V0.

#### Scenario: Dated override wins over recurring rule

- **WHEN** a dated `business_day_override` marks a date open on a normally-closed weekday
- **THEN** the resolved business day is open for sale

### Requirement: Channels and offer matrix are distinct but chained

The channels surface SHALL configure channels/schedules; the matrix SHALL configure which games are
sold on which channels. They SHALL be chained. On the matrix, only the game-on-channel offer toggle
SHALL be editable; stake and limits SHALL be read-only with provenance.

#### Scenario: Matrix offer toggle is the only edit

- **WHEN** a tenant opens the offer matrix
- **THEN** the game offer toggle is editable
- **AND** stake and limits are read-only with provenance badges and links to their editing surfaces

### Requirement: Result-slot JSON configs are platform-owned expert settings

`result_slot.source_cfg` SHALL describe how the platform retrieves an external provider result, and
`result_slot.projection_cfg` SHALL describe how that external result is projected into the Haiti draw
result. These configs SHALL be treated as platform/provider settings, not tenant settings. Tenant
admin surfaces MAY display them as read-only provenance, but SHALL NOT allow tenant admins to edit
them. Platform/super-admin surfaces SHALL display both raw JSON and a readable summary, and MAY offer
an expert update operation only when the payload passes server-side validation.

Changing `source_cfg` is runtime-impacting for result ingestion (`core.drawresult` and provider
fetchers such as US lottery integrations). Changing `projection_cfg` is runtime-impacting for Haiti
projection (`core.haiti`), produced `draw_result` values, and downstream settlement. Updates SHALL
evict result-slot caches and SHALL affect future fetch/projection operations only; persisted
`draw_result` rows SHALL NOT be silently reinterpreted.

#### Scenario: Tenant admin views result-slot config as provenance

- **WHEN** a tenant admin opens a draw channel bound to a result slot
- **THEN** the UI may show provider slot, source game mappings, and projection rules as read-only
- **AND** no tenant-admin action can update `source_cfg` or `projection_cfg`

#### Scenario: Super admin updates source config with validation

- **WHEN** a super admin submits a `source_cfg` expert update for an active result slot
- **THEN** the server requires a JSON object
- **AND** `provider_slot_code` is required
- **AND** each present source game (`pick3`, `pick4`) has a non-empty `game_code` and boolean
  `active`
- **AND** at least one source game is active
- **AND** result-slot caches are evicted after persistence

#### Scenario: Super admin updates projection config with validation

- **WHEN** a super admin submits a `projection_cfg` expert update
- **THEN** the server requires a JSON object with projection `rules`
- **AND** every configured projection token is known by the Haiti projection engine
- **AND** an invalid projection update is rejected instead of falling back silently to defaults
