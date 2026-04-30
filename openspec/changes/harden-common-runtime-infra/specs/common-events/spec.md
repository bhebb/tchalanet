# common-events Specification Delta

## MODIFIED Requirements

### Requirement: Domain event primitives SHALL remain simple and métier-independent

Common event primitives SHALL define technical contracts only.

#### Scenario: Domain event is implemented

- **WHEN** a class implements `DomainEvent`
- **THEN** it SHALL expose:
  - `eventId`;
  - `occurredAt`;
  - `tenantId`
- **AND** it MAY expose `eventType` as a derived simple class name
- **AND** it SHALL not depend on infrastructure or persistence

#### Scenario: Event publisher publishes one event

- **WHEN** `DomainEventPublisher.publish(event)` is called
- **THEN** the implementation SHALL delegate to the framework event publisher
- **AND** it SHALL not contain métier logic

#### Scenario: Event publisher publishes multiple events

- **WHEN** `DomainEventPublisher.publish(events)` is called
- **THEN** each event SHALL be published consistently
- **AND** publisher behavior SHALL not depend on concrete event type

#### Scenario: Logging listener is enabled

- **WHEN** dev/stg logging listener catches an event
- **THEN** it MAY log event type, tenant and event id
- **AND** it SHALL not mutate state
- **AND** it SHALL not run in production unless explicitly enabled

---

### Requirement: Event ownership SHALL follow producer and consumer boundaries

Event classes SHALL live in the bounded context that owns the state change. Listeners SHALL live in the bounded context that consumes the event.

#### Scenario: Producer defines event

- **GIVEN** a bounded context owns a state change
- **WHEN** it exposes a domain event for that change
- **THEN** the event class SHALL live in the producer bounded context
- **AND** the event name SHALL describe the state change that occurred

#### Scenario: Consumer handles event

- **GIVEN** another bounded context reacts to an event
- **WHEN** it defines a listener
- **THEN** the listener SHALL live in the consumer bounded context
- **AND** it SHALL use consumer-owned services, ports or evictors

#### Scenario: Listener imports producer infra

- **GIVEN** a listener in one bounded context imports another bounded context's `infra.*` or internal adapter
- **WHEN** event handlers are audited
- **THEN** this SHALL be considered a boundary violation
- **AND** the listener SHALL be refactored to use consumer-owned abstractions

---

### Requirement: Cross-domain event side effects SHALL run after commit

Side effects triggered by cross-domain events SHALL observe committed state.

#### Scenario: Listener performs side effects

- **WHEN** a listener updates projections, cache, notifications, stats, settlement, payout, audit or other side effects
- **THEN** it SHALL use `@TransactionalEventListener(phase = AFTER_COMMIT)`
- **AND** it SHALL be idempotent

#### Scenario: Listener is synchronous

- **WHEN** a listener uses plain `@EventListener`
- **THEN** it SHALL be limited to safe local behavior such as dev logging
- **OR** it SHALL be explicitly justified as not requiring committed state

#### Scenario: Producer emits cross-domain event

- **WHEN** a command handler persists a state change
- **THEN** cross-domain event publication SHOULD occur after the write is persisted
- **AND** preferably after commit through `AfterCommit.run(...)` or equivalent
- **AND** the event SHALL not describe a state that may roll back

---

### Requirement: Event consumers SHALL be idempotent

Event consumers SHALL be safe under duplicate delivery or duplicate framework invocation.

#### Scenario: Duplicate event is received

- **GIVEN** a listener receives the same event more than once
- **WHEN** the listener handles the event
- **THEN** side effects SHALL run at most once per consumer key and event id

#### Scenario: Multi-instance duplicate delivery occurs

- **GIVEN** two application instances handle the same event
- **WHEN** both attempt to process it
- **THEN** idempotency SHALL be enforced atomically
- **AND** a `markProcessedIfAbsent`-style method or unique constraint SHALL be used where possible

#### Scenario: Legacy check-then-mark exists

- **WHEN** a listener uses `alreadyProcessed(...)` followed by `markProcessed(...)`
- **THEN** it SHALL be refactored
- **OR** documented as temporary MVP debt if not safety-critical

---

### Requirement: Event listeners SHALL be split by concern

Listeners SHALL be focused and named by concern.

#### Scenario: Listener invalidates cache

- **WHEN** a listener exists only to evict cache
- **THEN** it SHALL be named as a cache invalidation listener
- **AND** it SHALL use the evictor owned by the cache owner domain

#### Scenario: Listener schedules or sends notifications

- **WHEN** a listener reacts by notifying users or systems
- **THEN** it SHALL live in notification context or the feature that owns notification orchestration
- **AND** it SHALL dispatch work to notification commands/services

#### Scenario: Listener updates stats or projections

- **WHEN** a listener updates read models, stats or projections
- **THEN** it SHALL live in the projection/stat owner context
- **AND** it SHALL be idempotent

#### Scenario: Listener performs settlement or payout reaction

- **WHEN** a listener triggers settlement/payout-related work
- **THEN** it SHALL live in the consuming sales/payout/settlement context
- **AND** complex business logic SHALL be delegated to command handlers or application services

---

### Requirement: Draw result event semantics SHALL be explicit

Draw result events SHALL distinguish global ingestion from tenant application.

#### Scenario: Global external result is stored

- **WHEN** a provider result is fetched and stored as global `draw_result`
- **THEN** `DrawResultIngestedEvent` MAY be emitted
- **AND** it SHALL mean global drawresult/provider ingestion only
- **AND** it SHALL not imply that tenant draws were updated

#### Scenario: Result is attached to tenant draw

- **WHEN** a global draw result is applied to a tenant draw
- **THEN** `DrawResultAppliedEvent` SHALL be emitted after commit
- **AND** it SHALL be the main cross-domain event for:
  - draw cache invalidation;
  - sales settlement;
  - payout reactions;
  - stats;
  - public draw projections;
  - tenant-facing result updates

#### Scenario: Misused ingestion event is found

- **WHEN** a consumer uses `DrawResultIngestedEvent` as if tenant draws were updated
- **THEN** that consumer SHALL be migrated to `DrawResultAppliedEvent`
- **OR** the event semantics SHALL be renamed/refactored
