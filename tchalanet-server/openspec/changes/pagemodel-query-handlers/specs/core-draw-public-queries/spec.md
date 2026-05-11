# Spec Delta — core.draw / core.drawresult Public and Dashboard Queries

## ADDED Requirements

### Requirement: Public draw results by slot query

`core.drawresult` SHALL expose `ListPublicDrawResultsBySlotQuery` for public PageModel widgets.

#### Scenario: Public home loads draw result widget

- **Given** a public PageModel widget with `source = "public_draw_results"`
- **And** props `include_history = false`
- **When** the query is handled
- **Then** the response SHALL include visible slots with latest result and optional next draw
- **And** it SHALL NOT expose internal result IDs, draw IDs, tenant IDs, or provider payloads.

#### Scenario: Public result page loads history

- **Given** a public PageModel widget with `include_history = true`
- **And** `history_limit = 10`
- **When** the query is handled
- **Then** each slot MAY include up to ten historical result items.

### Requirement: Cashier next draws query

`core.draw` SHALL expose or reuse a query for cashier next vendable draws.

#### Scenario: Cashier dashboard loads next draws

- **Given** a cashier private context
- **When** the query is handled
- **Then** it SHALL return upcoming or open vendable draws for the tenant ordered by cutoff/scheduled time.

### Requirement: Admin draw operations summary query

`core.draw` SHALL expose `GetAdminDrawOperationsSummaryQuery` for tenant admin dashboards.

#### Scenario: Tenant admin views draw operations

- **Given** a tenant admin context
- **When** the query is handled
- **Then** it SHALL summarize open draws, closed draws awaiting result, resulted draws awaiting settlement, and next due draw.
