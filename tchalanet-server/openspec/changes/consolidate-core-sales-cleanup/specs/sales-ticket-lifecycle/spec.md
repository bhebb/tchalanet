## ADDED Requirements

### Requirement: Sell result carries response notices

`SellTicketCommandHandler` MUST return approval/warning information through `SellTicketResult` and MUST NOT mutate `ApiResponseContext`.

#### Scenario: Sale requires approval

- **WHEN** a sale requires approval
- **THEN** the handler returns `SellTicketResult` with the approval information
- **AND** the controller maps that result to API notices
- **AND** the handler does not call `ApiResponseContext.get().addNotice(...)`

### Requirement: Approval request id is typed

`Ticket.approvalRequestId` MUST use a typed wrapper outside persistence.

#### Scenario: Pending approval ticket is created

- **WHEN** a sale creates a `PENDING_APPROVAL` ticket
- **THEN** the ticket stores an `ApprovalRequestId`
- **AND** raw `UUID` is limited to JPA entities, repositories, JDBC, and migrations

### Requirement: Currency representation is consistent

Ticket currency MUST use one consistent representation outside persistence, and persistence MUST store ISO 4217 currency codes with length 3.

#### Scenario: Ticket entity maps currency

- **WHEN** `TicketEntity.currency` is mapped
- **THEN** the column length is `3`
- **AND** audit table definitions are kept in sync

### Requirement: Redundant Ticket getter aliases are removed

The `Ticket` aggregate MUST use Lombok/getter-style accessors consistently and MUST NOT keep redundant alias methods such as `id()`, `tenantId()`, `terminalId()`, or `drawId()`.

#### Scenario: Application code reads ticket id

- **WHEN** application code needs the ticket id
- **THEN** it calls `ticket.getId()`
- **AND** no redundant `ticket.id()` method remains

### Requirement: Ticket sale policy uses query bus for autonomy

Sales policy orchestration MUST resolve autonomy through `QueryBus` instead of injecting `ResolveAutonomyPolicyService` directly.

#### Scenario: Sell policy evaluates autonomy

- **WHEN** `TicketSalePolicyService` evaluates a sale
- **THEN** it sends the appropriate autonomy query through `QueryBus`
- **AND** it does not directly inject `ResolveAutonomyPolicyService`

### Requirement: Session validation reports conflict semantics

Selling with no open session or blocked outlet MUST produce conflict semantics with stable error keys.

#### Scenario: No open session

- **GIVEN** no open sales session for the terminal
- **WHEN** a sale is attempted
- **THEN** the operation fails with key `session.not_open`

#### Scenario: Outlet sales blocked

- **GIVEN** the outlet blocks sales
- **WHEN** a sale is attempted
- **THEN** the operation fails with key `outlet.sales_blocked`

### Requirement: Fixed-payout odds semantics are verified

`TicketLine.oddsSnapshot` and `potentialPayout` MUST be coherent for games with fixed payout semantics.

#### Scenario: Fixed-payout game line is prepared

- **GIVEN** a fixed-payout game such as Cash-3, Cash-4, or Mariage
- **WHEN** ticket lines are prepared
- **THEN** the odds/payout snapshot stored on `TicketLine` represents the configured payout rule consistently
- **AND** tests document the expected behavior
