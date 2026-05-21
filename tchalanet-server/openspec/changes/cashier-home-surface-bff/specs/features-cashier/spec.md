# Spec — features.cashier home

## ADDED Requirements

### Requirement: Mobile POS cashier home is action-first

`GET /tenant/cashier/home` SHALL return a compact action-first home response for `MOBILE_POS`.

#### Scenario: Ready POS context

- **GIVEN** the user is authenticated as a cashier
- **AND** operational context is trusted
- **AND** sales session is open
- **WHEN** the client calls `GET /tenant/cashier/home` with `X-Tch-Surface: MOBILE_POS`
- **THEN** the response includes `primaryAction.type = SELL_TICKET`
- **AND** `requiredStep = null`
- **AND** it includes session and primary draw summaries
- **AND** it does not include the long cashier dashboard widget list.

#### Scenario: Missing operational context

- **GIVEN** the user is authenticated as a cashier
- **AND** no trusted operational context is available
- **WHEN** the client calls `GET /tenant/cashier/home`
- **THEN** the response includes `requiredStep.type = SELECT_OPERATIONAL_CONTEXT`
- **AND** the primary action routes to operational context selection.

#### Scenario: Session closed

- **GIVEN** the user is authenticated as a cashier
- **AND** operational context is trusted
- **AND** no sales session is open
- **WHEN** the client calls `GET /tenant/cashier/home`
- **THEN** the response includes `requiredStep.type = OPEN_SESSION`
- **AND** the primary action routes to open session.

### Requirement: Web cashier home may return widgets

`GET /tenant/cashier/web-home` SHALL return a richer widget-based model for `CASHIER_WEB`.

#### Scenario: Web cashier home

- **GIVEN** the user can access cashier web
- **WHEN** the client calls `GET /tenant/cashier/web-home` with `X-Tch-Surface: CASHIER_WEB`
- **THEN** the response includes widgets for session, active draw, and recent tickets
- **AND** the primary sell action is visible at the top.

### Requirement: Features must not own business invariants

Cashier home services SHALL compose public core/platform/catalog APIs and bus queries.

#### Scenario: Home service data dependencies

- **GIVEN** cashier home needs session/draw/sales/profile data
- **WHEN** implementing the service
- **THEN** it uses CommandBus/QueryBus or public `api/` contracts
- **AND** it does not import `core.*.internal`
- **AND** it does not call repositories.
