# Spec Delta — features.pagemodel Dynamic Providers

## ADDED Requirements

### Requirement: Dynamic providers shall dispatch queries only

PageModel dynamic providers SHALL only resolve widget props and dispatch queries/services from the owning domain.

#### Scenario: Provider loads cashier recent tickets

- **Given** a PageModel widget binding with `source = "cashier_recent_tickets"`
- **When** `PageModelDynamicResolver` resolves dynamic widgets
- **Then** `CashierRecentTicketsProvider` SHALL call `QueryBus.ask(new ListCashierRecentTicketsQuery(...))`
- **And** it SHALL NOT access JDBC, JPA, or repositories directly.

### Requirement: Dynamic source names shall be normalized

Dynamic provider source names SHALL use `snake_case`.

#### Scenario: Legacy cashier source names are replaced

- **Given** a widget with source `recent_tickets`
- **When** the PageModel template is updated
- **Then** the source SHALL become `cashier_recent_tickets`.

### Requirement: JSON fragment provider shall be configuration-only

The generic `json_file` provider SHALL load whitelisted JSON fragments only.

#### Scenario: Header links are loaded from JSON fragment

- **Given** a widget or shell fragment with `source = "json_file"`
- **And** prop `file_key = "private_cashier_sidebar_links"`
- **When** the provider loads the fragment
- **Then** it SHALL resolve the key through a whitelist registry
- **And** it SHALL load the classpath JSON using non-deprecated `JsonUtils` methods from `common`.

### Requirement: Provider errors shall be returned as widget dynamic errors

Provider exceptions SHALL be captured by the existing resolver and returned as `WidgetDynamicError` entries.

#### Scenario: Query handler fails

- **Given** a provider throws an exception while loading a widget
- **When** the resolver catches the exception
- **Then** it SHALL add a `WidgetDynamicError` with code `PROVIDER_ERROR`.
