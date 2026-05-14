# Spec: core.sales

## ADDED Requirements

### Requirement: API contains only externally consumed Java contracts

`core.sales.api` SHALL expose only commands, queries, events, and models consumed by another Java module.

#### Scenario: command used only by sales controller

- **GIVEN** `SellTicketCommand` is only dispatched by a controller under `core.sales.internal.infra.web`
- **THEN** the command SHALL live under `core.sales.internal.application.command.model`
- **AND** it SHALL NOT live under `core.sales.api.command`

#### Scenario: command consumed by offlinesync

- **GIVEN** `core.offlinesync` dispatches `ProcessOfflineSubmissionForSalesCommand`
- **THEN** `ProcessOfflineSubmissionForSalesCommand` SHALL live under `core.sales.api.command`
- **AND** its result model SHALL be exposed under `core.sales.api.model` or adjacent API package

### Requirement: cashier-facing ticket queries are public Java API when consumed by features.cashier

`core.sales` SHALL expose cashier ticket queries when they are consumed by `features.cashier`.

#### Scenario: cashier feature prints a ticket

- **GIVEN** `features.cashier` needs a ticket print snapshot
- **WHEN** it asks `GetTicketPrintViewQuery`
- **THEN** the query SHALL be available under `core.sales.api.query`
- **AND** it SHALL return a business print snapshot such as `TicketPrintView`
- **AND** it SHALL NOT return JPA entities or web DTOs

#### Scenario: cashier feature shows ticket details

- **GIVEN** `features.cashier` needs all ticket details for a cashier page
- **WHEN** it asks `GetTicketDetailsQuery` or `GetCashierTicketDetailsQuery`
- **THEN** the query SHALL be available under `core.sales.api.query`
- **AND** it SHALL return a stable API view

#### Scenario: cashier feature lists tickets

- **GIVEN** `features.cashier` needs a ticket page/list
- **WHEN** it asks `ListCashierTicketsQuery` or `ListTicketsQuery`
- **THEN** the query SHALL be available under `core.sales.api.query`
- **AND** it SHALL return `TchPage` of stable ticket rows

### Requirement: sales controllers are thin and non-duplicated

`core.sales` SHALL expose no duplicate route/method pairs for the same operation.

#### Scenario: sell endpoint is defined once

- **GIVEN** both `TicketLifecycleController` and `TicketSalesController` contain `POST /tenant/tickets`
- **THEN** one of the sell endpoints SHALL be removed or merged
- **AND** the remaining endpoint SHALL dispatch exactly one sell command through `CommandBus.execute(...)`

#### Scenario: ticket lifecycle controller handles lifecycle writes

- **GIVEN** the user sells, approves, rejects, cancels, or voids a ticket
- **THEN** `TicketLifecycleController` SHALL map the request to a command
- **AND** it SHALL not contain business rules

#### Scenario: ticket query controller handles read operations

- **GIVEN** the user lists tickets or opens a ticket detail
- **THEN** `TicketQueryController` SHALL ask a query through `QueryBus.ask(...)`
- **AND** it SHALL not access persistence adapters directly

#### Scenario: ticket print controller handles print-related operations

- **GIVEN** the user requests print data or reprint
- **THEN** `TicketPrintController` SHALL dispatch a query/command
- **AND** sales SHALL decide print/reprint eligibility
- **AND** platform.document MAY render bytes when requested by a feature or controller

### Requirement: sales owns official tickets and offline sales decisions

`core.sales` SHALL be the only domain that creates official `Ticket` records.

#### Scenario: online POS sale

- **WHEN** a POS sale succeeds
- **THEN** sales SHALL create the official ticket
- **AND** publish `TicketPlacedEvent` after commit

#### Scenario: offline submission sales processing

- **GIVEN** `core.offlinesync` dispatches `ProcessOfflineSubmissionForSalesCommand`
- **WHEN** sales accepts the submission
- **THEN** sales SHALL create the official ticket
- **AND** publish `OfflineSubmissionAcceptedAsTicketEvent` and `TicketPlacedEvent` after commit

#### Scenario: offline submission rejected by sales

- **WHEN** sales rejects the submission for business reasons
- **THEN** sales SHALL NOT create a ticket
- **AND** it SHALL publish `OfflineSubmissionRejectedBySalesEvent` after commit

## Suggested Public API

```text
core.sales.api.command
  ProcessOfflineSubmissionForSalesCommand

core.sales.api.query
  GetTicketPrintViewQuery
  GetTicketDetailsQuery or GetCashierTicketDetailsQuery
  ListTicketsQuery or ListCashierTicketsQuery
  GetTicketForPayoutQuery
  GetTicketForDrawSettlementQuery

core.sales.api.event
  TicketPlacedEvent
  TicketCancelledEvent
  TicketVoidedEvent
  TicketPrintedEvent
  OfflineSubmissionAcceptedAsTicketEvent
  OfflineSubmissionRejectedBySalesEvent

core.sales.api.model
  TicketPrintView
  TicketDetailsView
  TicketRow
  ProcessOfflineSubmissionForSalesResult
```
