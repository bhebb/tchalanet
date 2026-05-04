# sales-event-publishing Specification

## MODIFIED Requirements

### Requirement: Ticket events after commit

Sales ticket events SHALL be published after the transaction commits.

#### Scenario: Ticket sold

- **GIVEN** a successful ticket sale
- **WHEN** the ticket is persisted
- **THEN** `TicketPlacedEvent` SHALL be scheduled via `AfterCommit.run`
- **AND** the event SHALL NOT be published before commit

### Requirement: Event id generation

Sales event ids SHALL be generated through the project id generator.

#### Scenario: Create TicketPlacedEvent

- **GIVEN** a sold ticket
- **WHEN** `TicketPlacedEvent` is created
- **THEN** its `EventId` SHALL be generated through `IdGenerator`
- **AND** the handler SHALL NOT call `UUID.randomUUID()` directly

### Requirement: No TicketEventPublisherPort

Sales SHALL use the shared `DomainEventPublisher` for domain event publication.

#### Scenario: Remove unused event port

- **GIVEN** the sales module
- **WHEN** event publication is implemented
- **THEN** `TicketEventPublisherPort` SHALL NOT be used
- **AND** event publication SHALL use `DomainEventPublisher`
