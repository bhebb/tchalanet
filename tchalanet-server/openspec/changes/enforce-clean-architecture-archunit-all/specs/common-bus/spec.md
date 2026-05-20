# common-bus Specification Delta

## ADDED Requirements

### Requirement: CommandBus SHALL expose `execute`

The canonical method for command dispatch SHALL be `CommandBus.execute(command)`.

#### Scenario: Controller executes a command

- **WHEN** a controller receives a write request
- **THEN** it maps the request to a command
- **AND** calls `commandBus.execute(command)`.

### Requirement: QueryBus SHALL expose `ask`

The canonical method for query dispatch SHALL be `QueryBus.ask(query)`.

#### Scenario: Controller asks a query

- **WHEN** a controller receives a read request
- **THEN** it maps inputs to a query
- **AND** calls `queryBus.ask(query)`.

### Requirement: Handler method remains `handle`

Command handlers and query handlers SHALL keep `handle(...)` as the internal handler method.

#### Scenario: Bus routes to handler

- **WHEN** a bus receives a command/query
- **THEN** it locates the unique handler
- **AND** invokes `handler.handle(message)` internally.

### Requirement: Legacy bus methods SHALL be temporary only

Legacy bus methods such as `send(...)` or `handle(...)` on bus interfaces MAY exist only as deprecated migration bridges.

#### Scenario: New code uses legacy bus method

- **WHEN** new code calls `commandBus.send(...)` or `queryBus.handle(...)`
- **THEN** review fails
- **AND** the code must be migrated to `execute(...)` or `ask(...)`.

### Requirement: Bus startup SHALL fail on duplicate handlers

The bus SHALL detect duplicate handlers for the same command/query type at startup and fail fast.

#### Scenario: Two handlers implement the same command

- **WHEN** the application context starts
- **THEN** the bus detects duplicate mapping
- **AND** startup fails with an explicit error.
