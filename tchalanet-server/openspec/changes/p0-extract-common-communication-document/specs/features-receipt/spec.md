# features-receipt Specification

## ADDED Requirements

### Requirement: Receipt feature owns print/download/preview orchestration

`features.receipt` SHALL expose receipt rendering endpoints and orchestrate read model retrieval plus technical rendering.

#### Scenario: Ticket receipt PDF

- **WHEN** a tenant admin or authorized user requests a ticket receipt PDF
- **THEN** `features.receipt` SHALL query `core.sales` for the ticket receipt/read model
- **AND** SHALL render through `common.document`
- **AND** SHALL return file/bytes with appropriate HTTP headers.

#### Scenario: No ticket lifecycle mutation

- **WHEN** `features.receipt` renders a receipt
- **THEN** it SHALL NOT mutate ticket lifecycle state.
