# Spec: features.reporting and core.analytics integration

## CHANGED Requirements

### Requirement: Reporting feature uses analytics queries

The reporting feature SHALL use `core.analytics` queries for KPI/report data.

#### Scenario: Tenant KPI endpoint is called

- **WHEN** a user requests tenant KPIs
- **THEN** `features.reporting` SHALL map the request to `GetTenantKpisQuery`
- **AND** dispatch through `QueryBus`
- **AND** return a reporting response DTO.

#### Scenario: Sales report endpoint is called

- **WHEN** a user requests a sales report by period/game
- **THEN** `features.reporting` SHALL dispatch `GetSalesReportQuery`
- **AND** it SHALL NOT query `sales_ticket` or `ticket` tables directly.

### Requirement: Reporting feature owns exports, not metrics truth

The reporting feature SHALL own report presentation/export orchestration only.

#### Scenario: Outlet report CSV is generated

- **WHEN** a user exports an outlet report
- **THEN** `features.reporting` SHALL obtain data from `core.analytics`
- **AND** it MAY generate CSV/PDF output
- **AND** it SHALL not compute core KPI definitions itself.

### Requirement: Analytics readers handle performance-sensitive SQL

Performance-sensitive report SQL SHALL live in `core.analytics.internal.infra.persistence`.

#### Scenario: A new KPI reader is added

- **WHEN** the reader computes metrics from source tables or analytics projections
- **THEN** it SHALL be implemented under `core.analytics.internal.infra.persistence`
- **AND** exposed through a query handler.
