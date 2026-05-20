# features-cashier Specification

## MODIFIED Requirements

### Requirement: Cashier does not depend on receipt feature

`features.cashier` SHALL NOT call `features.receipt` directly.

#### Scenario: Sell with optional receipt

- **WHEN** cashier sell request asks for receipt rendering
- **THEN** `features.cashier` SHALL call the relevant `core.sales` receipt query/read model
- **AND** render via `common.document`
- **AND** SHALL NOT call `features.receipt`.

#### Scenario: Sell with optional delivery

- **WHEN** cashier sell request asks for external delivery
- **THEN** `features.cashier` SHALL call `common.communication`
- **AND** Spring SHALL call edge-service internally
- **AND** web/mobile SHALL NOT call edge-service directly.
