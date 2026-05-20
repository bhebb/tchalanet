# Spec Delta — core.sales Dashboard Queries

## ADDED Requirements

### Requirement: Cashier recent tickets query

`core.sales` SHALL expose `ListCashierRecentTicketsQuery` for cashier dashboard widgets.

#### Scenario: Cashier lists recent tickets

- **Given** a tenant and cashier from `TchRequestContext`
- **When** the query is handled with `limit = 5`
- **Then** the handler SHALL return at most five `CashierRecentTicketView` items
- **And** the response SHALL include public ticket code, status, sold time, stake total, potential payout, draw label, and line count
- **And** the response SHALL NOT include internal ticket ID or draw ID.

### Requirement: Cashier overview query

`core.sales` SHALL expose `GetCashierDashboardOverviewQuery` for current business day aggregates.

#### Scenario: Cashier opens dashboard

- **Given** a tenant and cashier
- **When** the query is handled
- **Then** the response SHALL include ticket count, sales total, cancelled count, and pending approval count for the current business day.

### Requirement: Admin KPIs query

`core.sales` SHALL expose `GetAdminDashboardKpisQuery` for tenant admin dashboard aggregates.

#### Scenario: Tenant admin opens dashboard

- **Given** a tenant admin context
- **When** the query is handled
- **Then** the response SHALL include tenant-level sales KPIs for the current business day.

### Requirement: Admin approval queue query

`core.sales` SHALL expose `ListPendingSalesApprovalsQuery` for tenant approval queues.

#### Scenario: Tenant admin sees pending approvals

- **Given** pending ticket sale approvals for a tenant
- **When** the query is handled
- **Then** the handler SHALL return a limited list of pending approval views
- **And** the limit SHALL be clamped to a safe maximum.
