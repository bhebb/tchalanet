## ADDED Requirements

### Requirement: Sales events publish after commit

Sales domain events MUST be published after the originating transaction commits.

#### Scenario: Sale command commits

- **WHEN** a sell command persists a ticket successfully
- **THEN** `TicketPlacedEvent` is published through `AfterCommit.run(...)`

#### Scenario: Sale command rolls back

- **WHEN** a sell command fails before commit
- **THEN** no sales domain event is published

### Requirement: Ledger listener dispatches before marking processed

`SalesLedgerListener` MUST NOT mark an event processed before the ledger command dispatch succeeds.

#### Scenario: Ledger command dispatch fails

- **GIVEN** a `TicketPlacedEvent`
- **AND** `commandBus.send(RecordTicketSaleLedgerCommand)` throws
- **WHEN** `SalesLedgerListener` handles the event
- **THEN** the event is not marked processed
- **AND** a later replay can retry ledger recording

#### Scenario: Ledger command dispatch succeeds

- **GIVEN** a `TicketPlacedEvent` that was not already processed
- **WHEN** `SalesLedgerListener` handles the event
- **THEN** it sends `RecordTicketSaleLedgerCommand`
- **AND** only then calls `markProcessedIfAbsent`

#### Scenario: Ledger event was already processed

- **GIVEN** `alreadyProcessed(CONSUMER, eventId)` returns `true`
- **WHEN** `SalesLedgerListener` handles the event
- **THEN** it does not send a ledger command

### Requirement: Cross-domain sales listeners use command bus

Cross-domain side effects from sales events MUST be dispatched through `CommandBus` or an after-commit event listener, not by directly mutating another domain repository.

#### Scenario: Ticket sale is recorded in ledger

- **WHEN** `TicketPlacedEvent` is consumed for ledger recording
- **THEN** the listener sends `RecordTicketSaleLedgerCommand`
- **AND** it does not call ledger persistence directly
