## ADDED Requirements

### Requirement: Ticket aggregate exposes a single canonical state machine

The `Ticket` aggregate SHALL expose exactly one transition method per state change:

- `sell(...)` — factory : creates a `Ticket` in `(SOLD, NOT_RESULTED, UNSETTLED)`
- `pendingApproval(...)` / `requestApproval(...)` — factories : create in `(PENDING_APPROVAL, NOT_RESULTED, UNSETTLED)`
- `approve(when)` — `PENDING_APPROVAL → SOLD`
- `reject(when)` — `PENDING_APPROVAL → REJECTED`
- `voidTicket(when)` — `(SOLD|PENDING_APPROVAL) → VOID`
- `markResulted(payout, when)` — applies a draw result on a `SOLD` ticket; transitions `(NOT_RESULTED → WON|LOST)`
- `forceResult(payout, resultStatus, when)` — admin override; transitions to `OVERRIDDEN` (or `WON|LOST` per the explicit `resultStatus`); refuses `SETTLED` tickets (cf. `harden-ticket-settlement-integrity`)
- `settle(when)` — `UNSETTLED → SETTLED` after payout was executed; requires `resultStatus != NOT_RESULTED`

The aggregate SHALL NOT expose `markAsPaid`, `markPayoutPaid`, `markPayoutPending`, or any other settlement alias. The aggregate SHALL NOT expose `updateSettlementStatus(...)` (a public mutator without invariant enforcement).

#### Scenario: settle is the unique path to SETTLED

- **GIVEN** a `Ticket(SOLD, WON, UNSETTLED)`
- **WHEN** `ticket.settle(now)` is called
- **THEN** the ticket transitions to `(SOLD, WON, SETTLED)`

#### Scenario: legacy aliases are removed from the API surface

- **WHEN** the `Ticket` class is reflected at compile time
- **THEN** the methods `markAsPaid`, `markPayoutPaid`, `markPayoutPending`, `updateSettlementStatus` are absent

### Requirement: Ticket codes are retried on DB collision

`JpaTicketRepositoryAdapter.save(Ticket)` SHALL detect collisions on the unique constraints `uq_ticket_tenant_code` (per-tenant `ticketCode`) and `uq_ticket_public_code` (global `publicCode`). On collision, the adapter SHALL regenerate both codes via `TicketNumberGeneratorPort` and `TicketPublicCodeGeneratorPort` and retry the save up to 3 times. After the 3rd failure, the adapter SHALL throw `TicketCodeGenerationException` (mapped to HTTP 503 Service Unavailable).

#### Scenario: First-attempt collision retries and succeeds

- **GIVEN** the first generated `publicCode` collides with an existing row
- **WHEN** `save(ticket)` is called
- **THEN** the adapter regenerates the codes and saves successfully on the 2nd attempt

#### Scenario: Three consecutive collisions raise TicketCodeGenerationException

- **GIVEN** every generated `publicCode` collides (extreme adversarial scenario)
- **WHEN** `save(ticket)` is called
- **THEN** the adapter throws `TicketCodeGenerationException` after 3 attempts
- **AND** the controller returns HTTP 503

### Requirement: Tenant ticket sale request body MUST NOT carry context-derived fields

`SellTicketRequest` SHALL NOT contain `tenantId`, `sessionId`, or `cashierId` fields. These values SHALL be resolved server-side from `TchContext` (RLS context). The body SHALL only carry domain inputs: `terminalId`, `drawId`, `currency`, `lines[]`.

#### Scenario: Body without tenantId/sessionId/cashierId is accepted

- **WHEN** `POST /tenant/tickets` is called with body `{ terminalId, drawId, currency, lines }`
- **THEN** the server resolves tenantId and cashierId from the authenticated context
- **AND** the sale proceeds normally

#### Scenario: Body containing legacy fields is rejected

- **WHEN** `POST /tenant/tickets` is called with body containing `tenantId` or `cashierId`
- **THEN** the server returns HTTP 400 (deserialization or validation error) — clients MUST migrate

### Requirement: All `performedBy` references use typed `UserId`

The fields `performedBy` in `CancelSaleCommand`, `OverrideTicketResultCommand`, `TicketCancelledEvent`, `TicketResultOverriddenEvent` SHALL be of type `UserId` (typed wrapper) instead of raw `UUID`. Web request DTOs MUST accept the value as a UUID and convert to `UserId` in the mapper.

#### Scenario: Cancel command exposes UserId

- **WHEN** the `CancelSaleCommand` constructor is invoked with a UserId
- **THEN** compilation succeeds and `cmd.performedBy()` returns `UserId`

#### Scenario: Raw UUID performedBy is no longer supported in command constructors

- **WHEN** code attempts `new CancelSaleCommand(..., UUID.randomUUID(), ...)` for `performedBy`
- **THEN** compilation fails (type mismatch)

### Requirement: Persistence adapters MUST NOT access tables outside their bounded context

The ArchUnit rule `SalesIsolationArchTest` SHALL verify that no class under `com.tchalanet.server.core.sales.infra.persistence.adapter.*` references tables outside the sales bounded context (allowed tables: `ticket`, `ticket_line`, `ticket_settlement`, `pricing_odds`). String-literal SQL referencing forbidden tables SHALL fail the build.

#### Scenario: SQL JOIN against draw_result fails build

- **WHEN** a class in `core.sales.infra.persistence.adapter.*` contains a SQL string `"... FROM draw_result JOIN result_slot ..."`
- **THEN** the ArchUnit test fails

#### Scenario: SQL on allowed sales tables passes

- **WHEN** a class in `core.sales.infra.persistence.adapter.*` contains `"... FROM ticket WHERE id = ?"`
- **THEN** the ArchUnit test passes

### Requirement: Cross-domain reads happen via published ports

Sales SHALL consume read-only data from other bounded contexts only through ports exposed in their `*.api` package:

- `core.drawresult.api.DrawResultProjectionCatalog` for draw result projections
- `core.draw.application.port.out.DrawLookupPort` for draws (existing)
- `core.outlet.application.port.out.OutletReaderPort` for outlets (existing)
- `core.pos.application.port.out.TerminalReaderPort` for terminals (existing)
- `core.address.application.port.AddressReaderPort` for addresses (existing)
- `catalog.pricing.api.PricingCatalog` for odds (existing)
- `catalog.settings.api.SettingsCatalog` for tenant settings (existing)

Sales SHALL NOT execute SQL referencing tables owned by other domains (enforced by `SalesIsolationArchTest`).

#### Scenario: DrawResultProjectionCatalog is consumed instead of raw SQL

- **WHEN** the settlement handler needs draw result data
- **THEN** it calls `drawResultProjectionCatalog.findById(drawResultId)`
- **AND** does NOT execute `SELECT ... FROM draw_result JOIN result_slot ...`

### Requirement: Persistence adapters carry only persistence concerns

Adapters in `core.sales.infra.persistence.adapter.*` SHALL inject only:

- The Spring Data JPA repositories of sales (`SpringTicketJpaRepository`, `TicketSettlementJpaRepository`, etc.)
- `TicketMapper` (entity ↔ domain translation)
- `Clock` and other infrastructure utilities

They SHALL NOT inject any port from another bounded context (`DrawLookupPort`, `OutletReaderPort`, `PosSessionReaderPort`, etc.). Cross-domain orchestration (e.g., `TicketPrintView` assembly) lives in `core.sales.application.service.*`.

#### Scenario: JpaTicketRepositoryAdapter has no cross-domain dependency

- **WHEN** the constructor of `JpaTicketRepositoryAdapter` is reflected
- **THEN** no parameter type belongs to a `core.X` package other than `core.sales`

#### Scenario: TicketPrintViewAssembler orchestrates lookups

- **WHEN** `TicketPrintViewAssembler.assemble(ticket, locale)` is called
- **THEN** it calls `DrawLookupPort`, `OutletReaderPort`, `PosSessionReaderPort` and composes a `TicketPrintView`

### Requirement: TicketWriterPort naming is correct

The port `TicketWritterPort` (typo: double 't') SHALL be renamed to `TicketWriterPort`. All consumers and the implementing adapter SHALL be updated.

#### Scenario: TicketWriterPort exists and TicketWritterPort does not

- **WHEN** the project compiles
- **THEN** `TicketWriterPort` exists in `core.sales.application.port.out.*`
- **AND** `TicketWritterPort` does not exist
