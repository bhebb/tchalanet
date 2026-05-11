# common-bus Specification Delta

## ADDED Requirements

### Requirement: In-process synchronous bus semantics

`CommandBus` and `QueryBus` SHALL be in-process synchronous dispatchers.

They SHALL NOT publish commands or queries to an external broker by default.

#### Scenario: command dispatch is synchronous

- **GIVEN** a controller sends a `SellTicketCommand`
- **WHEN** `CommandBus.send(command)` is called
- **THEN** the matching `CommandHandler` is invoked in the same JVM call path
- **AND** the controller receives the handler result or exception synchronously

#### Scenario: bus does not own external async delivery

- **GIVEN** a command is sent through `CommandBus`
- **WHEN** the command is dispatched
- **THEN** no Kafka/RabbitMQ/Azure Service Bus/SQS publication is performed by the bus

---

### Requirement: One message has exactly one handler

Each concrete command or query message class SHALL have exactly one matching handler when used.

The bus registry SHALL fail startup if two handlers map to the same message class.

#### Scenario: duplicate command handlers fail startup

- **GIVEN** two `CommandHandler` beans resolve to `CancelTicketCommand`
- **WHEN** the application context initializes the command bus
- **THEN** startup fails with a duplicate handler error
- **AND** the error message includes both handler implementation classes

#### Scenario: duplicate command and void-command handlers fail startup

- **GIVEN** one `CommandHandler` and one `VoidCommandHandler` resolve to the same command class
- **WHEN** the application context initializes the command bus
- **THEN** startup fails with a duplicate handler error

#### Scenario: duplicate query handlers fail startup

- **GIVEN** two `QueryHandler` beans resolve to `ListTicketsQuery`
- **WHEN** the application context initializes the query bus
- **THEN** startup fails with a duplicate handler error

---

### Requirement: Handler generic type resolution is fail-fast

Every `CommandHandler`, `VoidCommandHandler`, and `QueryHandler` bean SHALL expose a resolvable concrete message type.

If a handler message type cannot be resolved, the application SHALL fail startup.

#### Scenario: unresolved command handler fails startup

- **GIVEN** a `CommandHandler` bean whose command generic type cannot be resolved
- **WHEN** the command bus initializes
- **THEN** startup fails with `BusRegistrationException` or equivalent
- **AND** the error identifies the handler class

#### Scenario: unresolved query handler fails startup

- **GIVEN** a `QueryHandler` bean whose query generic type cannot be resolved
- **WHEN** the query bus initializes
- **THEN** startup fails with `BusRegistrationException` or equivalent
- **AND** the error identifies the handler class

---

### Requirement: Handler registry is immutable after startup

The command and query handler registries SHALL be immutable after initialization.

#### Scenario: registry cannot be mutated after initialization

- **GIVEN** the bus has completed startup
- **WHEN** runtime dispatch occurs
- **THEN** no code path mutates the handler registry
- **AND** the registry is backed by an immutable map or equivalent

---

### Requirement: Exact class dispatch

The bus SHALL dispatch by exact runtime message class.

It SHALL NOT perform polymorphic nearest-handler lookup.

#### Scenario: subclass command does not match parent handler

- **GIVEN** a handler registered for `BaseCommand`
- **AND** a message instance of `SpecificCommand extends BaseCommand`
- **WHEN** `CommandBus.send(specificCommand)` is called
- **THEN** the bus searches for `SpecificCommand.class`
- **AND** it does not dispatch to the `BaseCommand` handler unless a handler is explicitly registered for `SpecificCommand`

---

### Requirement: Missing handlers fail clearly

If no handler exists for a command or query, the bus SHALL throw a clear runtime exception.

#### Scenario: missing command handler

- **GIVEN** no handler is registered for `ArchiveTicketCommand`
- **WHEN** `CommandBus.send(command)` is called
- **THEN** `NoHandlerException` or equivalent is thrown
- **AND** the message includes `ArchiveTicketCommand`

#### Scenario: missing query handler

- **GIVEN** no handler is registered for `GetTicketReceiptQuery`
- **WHEN** `QueryBus.send(query)` is called
- **THEN** `NoHandlerException` or equivalent is thrown
- **AND** the message includes `GetTicketReceiptQuery`

---

### Requirement: Null messages are rejected

The buses SHALL reject null commands and null queries.

#### Scenario: null command

- **WHEN** `CommandBus.send(null)` is called
- **THEN** a null input exception is thrown before registry lookup

#### Scenario: null query

- **WHEN** `QueryBus.send(null)` is called
- **THEN** a null input exception is thrown before registry lookup

---

### Requirement: Bus remains technical common infrastructure

The bus SHALL remain in `common.bus` and SHALL NOT contain business rules.

#### Scenario: bus has no domain decision

- **GIVEN** a command is dispatched
- **WHEN** the bus locates its handler
- **THEN** the bus does not inspect tenant policy, permissions, money rules, draw rules, payout rules, or sales rules
- **AND** those decisions remain in the appropriate controller/aspect/domain/application layer

---

### Requirement: Startup observability

The buses SHALL log a startup summary.

#### Scenario: command bus startup summary

- **WHEN** `SimpleCommandBus` initializes successfully
- **THEN** it logs the number of command handlers, void command handlers, total registered commands, and initialization duration

#### Scenario: query bus startup summary

- **WHEN** `SimpleQueryBus` initializes successfully
- **THEN** it logs the number of query handlers, total registered queries, and initialization duration

---

## ADDED Architecture Requirements

### Requirement: Clean architecture rules are executable tests

The project SHALL include architecture tests that enforce the main dependency boundaries.

#### Scenario: common does not depend on business layers

- **GIVEN** production classes under `com.tchalanet.server.common..`
- **WHEN** architecture tests run
- **THEN** they do not depend on `com.tchalanet.server.core..`, `com.tchalanet.server.catalog..`, or `com.tchalanet.server.features..`

#### Scenario: core does not depend on features

- **GIVEN** production classes under `com.tchalanet.server.core..`
- **WHEN** architecture tests run
- **THEN** they do not depend on `com.tchalanet.server.features..`

#### Scenario: feature slices do not access persistence directly

- **GIVEN** production classes under `com.tchalanet.server.features..`
- **WHEN** architecture tests run
- **THEN** they do not depend on JPA repositories, JPA entities, or `..infra.persistence..` packages

#### Scenario: domain packages are framework-free

- **GIVEN** production classes in `..domain..`
- **WHEN** architecture tests run
- **THEN** they do not depend on Spring Framework, JPA, web, or infra packages

#### Scenario: catalog API does not depend on internal implementation

- **GIVEN** production classes under `com.tchalanet.server.catalog..api..`
- **WHEN** architecture tests run
- **THEN** they do not depend on `com.tchalanet.server.catalog..internal..`
