# Spec: common.context

## MODIFIED Requirements

### Requirement: Operational Package Is Neutral

`common.context.operational` SHALL remain neutral and SHALL carry only parsed operational facts for V0 seller-terminal resolution.

It SHALL NOT validate seller-terminal existence, tenant ownership, status, permissions, limits, outlet, session or offline grants.

#### Scenario: Operational parser reads seller-terminal context

- **GIVEN** an HTTP request includes seller-terminal operational context headers
- **WHEN** the operational parser runs
- **THEN** it parses seller-terminal identity and trust/source metadata
- **AND** it does not call repositories, buses, platform APIs, core APIs, catalog APIs or feature APIs.

#### Scenario: Seller-terminal context is not proof of validity

- **GIVEN** a parsed operational context contains a seller terminal id
- **WHEN** it is attached to `TchRequestContext`
- **THEN** the id is treated as an input fact
- **AND** ownership/status validation happens outside `common.context`.

### Requirement: Operational context no longer requires session or outlet for V0 sales

V0 operational context SHALL NOT require sales-session id or outlet id to sell a ticket.

#### Scenario: Sale context has seller terminal only

- **GIVEN** a request has tenant and actor context
- **AND** its operational context contains a seller terminal id
- **WHEN** sales resolves V0 operational identity
- **THEN** sales can proceed to seller-terminal validation without session or outlet ids.

### Requirement: Request context exposes explicit seller-terminal helpers

`TchRequestContext` SHALL expose explicit seller-terminal helper methods for V0 runtime code and SHALL fail fast when seller-terminal context is required but absent.

#### Scenario: POS context exposes seller terminal id

- **GIVEN** bootstrap attached a seller-terminal POS actor
- **WHEN** `TchContextFilter` builds request context
- **THEN** request context contains actor type `SELLER_TERMINAL`
- **AND** it contains non-null tenant id and seller terminal id.

#### Scenario: Required seller terminal helper fails for admin request

- **GIVEN** an authenticated tenant admin request has no seller-terminal actor
- **WHEN** code calls `sellerTerminalIdRequired()` or `trustedSellerTerminalContextRequired()`
- **THEN** request context fails fast instead of silently deriving seller-terminal context.

#### Scenario: Admin does not accidentally acquire seller-terminal context

- **GIVEN** an authenticated tenant admin request includes seller-terminal-looking headers
- **WHEN** request context is built
- **THEN** admin actor context remains admin app-user context
- **AND** seller-terminal trusted context is not attached unless a documented admin/POS development mode explicitly allows it.
