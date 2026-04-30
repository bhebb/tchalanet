# draw-results-pipeline Specification Delta

## MODIFIED Requirements

### Requirement: Results pipeline SHALL separate game, draw channel, result slot, draw result and draw

The system SHALL maintain clear separation between sold products, tenant draw calendars, external result slots, global draw results and tenant draws.

#### Scenario: Sold game is referenced

- **WHEN** code references a sold betting product
- **THEN** it SHALL use `game_code`
- **AND** examples include Haitian products such as `HT_BOLET`, `HT_MARYAJ`, `HT_LOTO3`, `HT_LOTO4`, `HT_LOTO5`
- **AND** sold game codes SHALL NOT be used as provider fetch identities

#### Scenario: Tenant draw calendar is referenced

- **WHEN** code references a tenant draw calendar
- **THEN** it SHALL use `draw_channel`
- **AND** `draw_channel` SHALL remain tenant-scoped
- **AND** `draw_channel.code` SHALL NOT be used as provider fetch identity

#### Scenario: External result expectation is referenced

- **WHEN** code references an expected external result slot
- **THEN** it SHALL use `result_slot`
- **AND** result slots SHALL be global
- **AND** result slots SHALL drive provider fetch configuration

#### Scenario: Global result is referenced

- **WHEN** code references provider/Haiti-projected result data
- **THEN** it SHALL use `draw_result`
- **AND** draw results SHALL be global
- **AND** draw results SHALL be uniquely identified by result slot and occurrence time

#### Scenario: Tenant draw is referenced

- **WHEN** code references a tenant lifecycle draw
- **THEN** it SHALL use `draw`
- **AND** draw SHALL remain tenant-scoped
- **AND** draw MAY reference a global `draw_result_id` after apply

---

### Requirement: External result fetching SHALL be result-slot-first

External result fetching SHALL be driven by `result_slot_key` and `result_slot.source_cfg`.

#### Scenario: Fetch command is received

- **WHEN** external result fetch is requested
- **THEN** the command SHALL identify the target by `result_slot_key` or a slot window
- **AND** the handler SHALL resolve active global result slots
- **AND** it SHALL NOT resolve provider fetch identity from `draw_channel.code`
- **AND** it SHALL NOT resolve provider fetch identity from sold `game_code`

#### Scenario: Provider query is built

- **GIVEN** a result slot contains provider source configuration
- **WHEN** provider query is built
- **THEN** provider external game codes SHALL be read from `result_slot.source_cfg`
- **AND** the provider client SHALL receive provider/external terminology only

#### Scenario: Slot is inactive

- **GIVEN** a result slot is inactive
- **WHEN** fetch runs
- **THEN** the slot SHALL be skipped
- **AND** a counter/log SHOULD record `slotInactive`

#### Scenario: Slot is missing

- **GIVEN** a requested result slot does not exist
- **WHEN** fetch runs
- **THEN** the slot SHALL be skipped or rejected according to command mode
- **AND** a counter/log SHOULD record `slotNotFound`

---

### Requirement: External fetch SHALL be global

External provider fetch SHALL write global draw results and SHALL NOT run once per tenant.

#### Scenario: Fetch job is registered

- **WHEN** `RESULTS_EXTERNAL_FETCH` is registered
- **THEN** it SHALL be registered as `GLOBAL`
- **AND** it SHALL NOT require `tenant_id`

#### Scenario: Fetch handler writes result

- **WHEN** a provider result is fetched
- **THEN** the handler SHALL write or update a global `draw_result`
- **AND** no tenant context SHALL be required for the write unless explicitly needed by infrastructure
- **AND** the write SHALL not be repeated per tenant

#### Scenario: Fetch is manually triggered

- **WHEN** Ops manually triggers fetch
- **THEN** request parameters MAY include:
  - slot key;
  - from/to;
  - dry run;
  - force;
  - reason;
  - max slots
- **AND** tenant id SHALL not be required

---

### Requirement: Draw result upsert SHALL be atomic and idempotent

Draw result persistence SHALL prevent duplicate rows and race conditions.

#### Scenario: First result for slot occurrence is inserted

- **GIVEN** no draw result exists for `(result_slot_id, occurred_at)`
- **WHEN** fetch writes the result
- **THEN** a new row SHALL be inserted

#### Scenario: Existing provisional result changes

- **GIVEN** a provisional draw result exists
- **AND** provider source hash changes
- **WHEN** fetch writes the result
- **THEN** the existing row MAY be updated
- **AND** source payload/hash SHALL be updated

#### Scenario: Existing confirmed result exists

- **GIVEN** a confirmed draw result exists
- **WHEN** fetch runs with `force=false`
- **THEN** the result SHALL NOT be overwritten

#### Scenario: Existing overridden result exists

- **GIVEN** an overridden draw result exists
- **WHEN** fetch runs with `force=false`
- **THEN** the result SHALL NOT be overwritten

#### Scenario: Force update is requested

- **GIVEN** force update is requested
- **WHEN** fetch overwrites a protected result
- **THEN** reason/audit SHALL be required for manual use
- **AND** the update SHALL be observable

#### Scenario: Concurrent fetches run

- **GIVEN** two fetches process the same `(result_slot_id, occurred_at)`
- **WHEN** both write
- **THEN** database uniqueness and upsert SHALL produce one logical row
- **AND** no duplicate draw results SHALL exist

---

### Requirement: Haiti projection SHALL be applied after provider normalization

Provider results SHALL be normalized before Haiti projection.

#### Scenario: Provider bundle is found

- **WHEN** provider returns pick3/pick4 or equivalent source values
- **THEN** source values SHALL be normalized into the internal provider result model
- **AND** Haiti projection SHALL be applied from normalized source data
- **AND** projection rules SHALL come from result slot config or configured fallback

#### Scenario: Provider returns partial data

- **GIVEN** provider returns only part of the expected bundle
- **WHEN** fetch runs
- **THEN** result quality/flags SHALL indicate partial or missing data
- **AND** the handler SHALL not produce misleading confirmed data

#### Scenario: Provider returns no result

- **WHEN** no external result is available
- **THEN** fetch SHALL record a `noExternalResult` counter/log
- **AND** it SHALL not create invalid draw result data

---

### Requirement: External apply SHALL be tenant-scoped

Applying results SHALL mutate tenant draws and therefore SHALL be tenant-scoped.

#### Scenario: Apply job is registered

- **WHEN** `RESULTS_EXTERNAL_APPLY` is registered
- **THEN** it SHALL be registered as `TENANT`
- **AND** it SHALL require `tenant_id`

#### Scenario: Apply command is handled

- **GIVEN** tenant context is bound
- **WHEN** apply runs
- **THEN** it SHALL search eligible tenant draws
- **AND** it SHALL match through `draw_channel.result_slot_id`
- **AND** it SHALL attach global `draw_result_id` to matching draws

#### Scenario: Draw is not closed

- **GIVEN** a draw is not in an eligible status such as `CLOSED`
- **WHEN** apply runs
- **THEN** the draw SHALL NOT be updated
- **AND** no applied event SHALL be emitted for that draw

#### Scenario: Draw already has result

- **GIVEN** a draw already references a draw result
- **WHEN** apply runs without repair/force semantics
- **THEN** the draw SHALL not be overwritten
- **AND** no duplicate applied event SHALL be emitted

#### Scenario: Apply succeeds

- **WHEN** a result is attached to a draw
- **THEN** draw lifecycle SHALL transition according to domain rules
- **AND** `DrawResultAppliedEvent` SHALL be emitted after commit

---

### Requirement: DrawResultAppliedEvent SHALL be the main cross-domain event for tenant result application

Consumers that react to tenant draw results SHALL consume `DrawResultAppliedEvent`.

#### Scenario: Sales settlement reacts

- **WHEN** a tenant draw receives a result
- **THEN** sales/settlement consumers SHALL react to `DrawResultAppliedEvent`
- **AND** they SHALL not infer tenant draw result application from global ingestion

#### Scenario: Payout reacts

- **WHEN** payout logic needs result application
- **THEN** it SHALL consume `DrawResultAppliedEvent` or downstream settlement events

#### Scenario: Stats reacts

- **WHEN** stats/projections need tenant result application
- **THEN** they SHALL consume `DrawResultAppliedEvent`

#### Scenario: Public draw projection reacts

- **WHEN** public draw result pages or projections need updates
- **THEN** they SHALL consume `DrawResultAppliedEvent`

#### Scenario: Draw cache reacts

- **WHEN** draw cache must be invalidated after result application
- **THEN** `core.draw` SHALL consume `DrawResultAppliedEvent`
- **AND** use `DrawCacheEvictor`

---

### Requirement: DrawResultIngestedEvent SHALL represent global ingestion only

Global provider ingestion SHALL not be confused with tenant draw result application.

#### Scenario: Provider result is stored globally

- **WHEN** fetch creates or updates global draw result
- **THEN** `DrawResultIngestedEvent` MAY be emitted
- **AND** it SHALL describe global ingestion only

#### Scenario: Consumer needs tenant draw id

- **WHEN** a consumer needs tenant draw id or tenant draw lifecycle state
- **THEN** it SHALL NOT consume `DrawResultIngestedEvent`
- **AND** it SHALL consume `DrawResultAppliedEvent`

#### Scenario: Ingested event contains tenant/draw fields

- **WHEN** `DrawResultIngestedEvent` contains tenant or draw fields that do not belong to global ingestion
- **THEN** the event SHALL be refactored
- **OR** renamed if it actually represents tenant application

---

### Requirement: US Lottery provider clients SHALL use provider terminology

Provider code SHALL not use tenant draw terminology.

#### Scenario: Provider query contains external codes

- **WHEN** a provider query is created
- **THEN** the requested provider codes SHALL be named `externalGameCodes`, `providerGameCodes`, or equivalent
- **AND** they SHALL not be called `channelCodes` if that can be confused with `draw_channel.code`

#### Scenario: Provider result is normalized

- **WHEN** provider data is normalized to internal `LatestDraw`
- **THEN** the provider code field SHALL be named `externalGameCode` or `providerGameCode`
- **AND** it SHALL not be named `channelCode`

#### Scenario: Provider client parses payload

- **WHEN** a provider client parses NY/FL/GA/TX/TN payload
- **THEN** it SHALL only know provider payload shape and provider codes
- **AND** it SHALL not know tenant, draw, draw_channel, draw_result or Haitian sold game concepts

---

### Requirement: US Lottery adapter SHALL map result_slot source config to provider queries

The adapter between drawresult fetch and US Lottery providers SHALL translate result slot config into provider query terms.

#### Scenario: Source config contains provider codes

- **GIVEN** `result_slot.source_cfg` defines provider and wanted external game codes
- **WHEN** fetch requests external results
- **THEN** `UsLotteryExternalResultsFetchPortAdapter` SHALL build provider query using that config
- **AND** it SHALL call the provider client once per slot/date where possible

#### Scenario: Provider client returns normalized draws

- **WHEN** provider client returns `List<LatestDraw>`
- **THEN** the adapter SHALL select matching external game codes
- **AND** recompose pick3/pick4 or other expected bundle values
- **AND** return a normalized external bundle to drawresult application logic

---

### Requirement: US Lottery raw provider cache SHALL belong to core.uslottery

Raw provider payload cache SHALL be declared and managed by US Lottery infrastructure.

#### Scenario: Cache spec is declared

- **WHEN** provider raw cache is declared
- **THEN** it SHALL live under `core.uslottery.infra.cache`
- **AND** its cache name SHALL be `infra.uslottery.provider_raw`
- **AND** `core.draw` SHALL not declare it

#### Scenario: Provider payload is fetched

- **WHEN** provider client fetches a raw payload
- **THEN** it MAY use `UsLotteryProviderRawCache`
- **AND** the cache key SHALL include provider, draw date and query hash
- **AND** null payloads SHALL not be cached by default

#### Scenario: Cache is unavailable

- **WHEN** provider raw cache is missing or fails
- **THEN** the provider fetch path SHALL continue
- **AND** the error SHALL be logged without failing the fetch solely because of cache

#### Scenario: Concurrent requests occur in same instance

- **WHEN** multiple threads request the same provider/date/query hash
- **THEN** an in-process anti-stampede lock MAY prevent duplicate fetches
- **AND** this SHALL be considered MVP-level protection
- **AND** distributed locking SHALL not be required for this change

---

### Requirement: Draw cache SHALL belong to core.draw

Draw lifecycle/read-model caches SHALL be declared under core draw.

#### Scenario: Draw summary cache is declared

- **WHEN** core draw declares draw summary cache
- **THEN** the cache name SHALL start with `core.draw.`
- **AND** it SHALL not use `catalog.draw.*`

#### Scenario: Draw cache keys are built

- **WHEN** draw cache keys include tenant id
- **THEN** they SHALL use `tenantId.value()`
- **AND** key builders SHALL avoid duplication where possible

#### Scenario: Draw cache is invalidated after apply

- **WHEN** a draw receives a result
- **THEN** draw cache invalidation SHALL be performed by `core.draw`
- **AND** it SHALL use `DrawCacheEvictor`
- **AND** it SHALL not use `DrawResultCacheEvictor`

---

### Requirement: Draw and drawresult listeners SHALL not cross-use infra evictors

Event listeners SHALL use the infrastructure of their own bounded context.

#### Scenario: Draw consumes DrawResultAppliedEvent

- **WHEN** `core.draw` consumes `DrawResultAppliedEvent`
- **THEN** the listener SHALL live in `core.draw`
- **AND** it SHALL use `DrawCacheEvictor`
- **AND** it SHALL not inject `core.drawresult.infra.cache.DrawResultCacheEvictor`

#### Scenario: DrawResult consumes DrawResultIngestedEvent

- **WHEN** `core.drawresult` consumes its own ingestion event
- **THEN** the listener SHALL live in `core.drawresult`
- **AND** it SHALL use `DrawResultCacheEvictor`

#### Scenario: Listener needs data from another bounded context

- **WHEN** a listener needs cross-domain data
- **THEN** it SHALL use a public API/port/catalog/query as allowed
- **AND** it SHALL not import another bounded context's internal persistence or infra adapter

---

### Requirement: Apply repositories SHALL use result slot relationships

Apply queries SHALL rely on the result slot relationship instead of legacy external/channel naming.

#### Scenario: Apply selects candidate draws

- **WHEN** apply selects candidate tenant draws
- **THEN** it SHALL join draw to draw_channel
- **AND** use `draw_channel.result_slot_id`
- **AND** match against `draw_result.result_slot_id`

#### Scenario: Legacy external fields are found

- **WHEN** apply query uses `external_game_key`, `external_channel_code`, or provider channel naming
- **THEN** it SHALL be refactored to result slot semantics unless explicitly justified

#### Scenario: Invalid draw status literal is found

- **WHEN** query uses a status literal not present in the domain enum
- **THEN** the query SHALL be corrected
- **AND** tests SHALL cover the valid lifecycle status

---

### Requirement: Scheduler SHALL trigger, not implement business logic

Schedulers SHALL decide when to try work and delegate business logic to commands/jobs.

#### Scenario: Results scheduler ticks

- **WHEN** results scheduler runs
- **THEN** it SHALL compute due slots/windows
- **AND** it SHALL dispatch commands or jobs
- **AND** it SHALL not embed fetch/apply business rules

#### Scenario: Local profile disables scheduler

- **GIVEN** local/debug configuration disables scheduler
- **WHEN** the application starts
- **THEN** scheduled jobs SHALL not run automatically
- **AND** Ops/manual endpoints SHALL be able to drive fetch/apply/refresh

#### Scenario: Manual operation uses force

- **WHEN** an operator triggers a forced fetch/apply/refresh
- **THEN** reason SHALL be required where the operation can overwrite protected state
- **AND** the operation SHALL be audited
