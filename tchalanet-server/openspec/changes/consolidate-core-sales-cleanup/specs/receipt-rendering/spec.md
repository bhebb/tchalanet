# receipt-rendering Specification

## ADDED Requirements

### Requirement: Receipt feature is global

`features.receipt` MUST be the global feature for receipt/document/print artifact endpoints exposed through Spring APIs.

#### Scenario: Ticket receipt PDF is generated

- **WHEN** a tenant requests a ticket PDF
- **THEN** `features.receipt` orchestrates receipt generation
- **AND** `core.sales` provides the canonical ticket receipt/read model and ticket-specific formatter
- **AND** low-level rendering primitives come from `common.document`.

#### Scenario: Future receipt type is added

- **WHEN** a future payout, settlement, or report receipt endpoint is needed
- **THEN** it belongs under `features.receipt` when it is an API/artifact orchestration concern
- **AND** its domain-specific read model stays in the owning core domain.

### Requirement: Receipt feature follows existing feature structure

`features.receipt` MUST follow the existing feature style and avoid unnecessary hexagonal structure.

#### Scenario: Receipt feature is implemented

- **THEN** it uses packages such as root controller, `app/`, and `model/` according to the local rule-of-3
- **AND** it does not introduce `api/`, `port/out`, or `infra/persistence` packages unless a separate documented need appears.

### Requirement: common.document remains technical

`common.document` MUST remain free of ticket/sales business concepts.

#### Scenario: common document renderer is used

- **WHEN** receipt rendering calls `common.document`
- **THEN** `common.document` receives generic rendering inputs/primitives
- **AND** it does not import `core.sales`, `features.receipt`, `features.ticketreceipt`, or ticket domain models.

### Requirement: Interim ticketreceipt is not target architecture

An existing interim `features.ticketreceipt` package MAY exist only during migration and MUST NOT be the target architecture.

#### Scenario: Receipt feature cleanup continues

- **WHEN** the package structure is normalized
- **THEN** `features.ticketreceipt` is renamed or migrated to `features.receipt`
- **AND** no new code depends on `features.ticketreceipt`.

### Requirement: Print reader not-found is explicit

Ticket receipt data readers MUST not throw low-level persistence not-found exceptions directly from JDBC adapters.

#### Scenario: Ticket print view does not exist

- **WHEN** receipt data is requested for an unknown ticket
- **THEN** the reader returns `Optional.empty()` or an application-level not-found result
- **AND** the feature/service maps it to the HTTP error.
