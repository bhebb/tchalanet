## ADDED Requirements

### Requirement: Tenant admin financials page

The web application SHALL expose a tenant-admin financials page under the admin reports area.

#### Scenario: Tenant admin opens financials

- **WHEN** a tenant admin navigates to `/app/admin/reports/financials`
- **THEN** the page loads tenant financial breakdown data from the backend
- **AND** the page shows summary financial metrics, draw rows, and seller-terminal-by-draw rows.

#### Scenario: New tenant has no projected data

- **WHEN** the backend returns zero totals and no financial rows
- **THEN** the page shows an explicit empty state
- **AND** the page does not render misleading fake KPI/chart data.

#### Scenario: Tenant context is backend-owned

- **WHEN** the page requests the financial breakdown
- **THEN** the frontend uses the tenant-scoped backend endpoint
- **AND** the frontend MUST NOT send a `tenantId` query parameter.
