# features-cashier Specification

## Purpose
TBD - created by archiving change tchalanet-cashier-sales-betoption. Update Purpose after archive.
## Requirements
### Requirement: Cashier exposes seller-facing game option metadata

The cashier feature SHALL expose:

```http
GET /tenant/cashier/games/available
```

The endpoint SHALL return cashier game choices with game labels, bet type
labels, option labels, option descriptions, and selection hints.

#### Scenario: Loto 4 options include labels and hints

When the cashier requests `/tenant/cashier/games/available`
Then the response includes `HT_LOTO4`
And it includes options for exact, box, front pair, and back pair
And each option includes a seller-facing label and selection hint.

### Requirement: POS payload remains stable

Cashier preview and sell payloads SHALL continue to send raw seller input as
`selection` and numeric option code as `betOption`.

#### Scenario: Loto 4 front pair payload is canonicalized by backend

Given the POS sends `HT_LOTO4`, `LOTTO4_PATTERN`, bet option `3`, and selection
`12`
When backend evaluates the line
Then backend canonicalizes it as `12**`
And seller-facing responses display `12` with the front-pair label.

### Requirement: POS does not depend on technical enum names

The POS SHALL use labels and hints from the game-options endpoint for display.

#### Scenario: Cashier sees a human option label

Given the available-games response contains Loto 3 option code `2`
When the POS renders the option
Then it displays `Désordre / Box`
And does not display only `2` or `LOTTO3_BOX`.

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

### Requirement: POS ticket verification accepts public scan payloads

`features.cashier` SHALL expose an authenticated POS endpoint that accepts a scanned ticket value and normalizes it to a public ticket code.

#### Scenario: cashier scans full public verification URL

- **WHEN** the cashier submits `https://tchalanet.com/v/TCH-8F4K-29PL`
- **THEN** the backend SHALL extract `TCH-8F4K-29PL`
- **AND** resolve the ticket through Sales using a public-code lookup
- **AND** return a contextual POS verification response.

#### Scenario: cashier scans raw public ticket code

- **WHEN** the cashier submits `TCH-8F4K-29PL`
- **THEN** the backend SHALL use it as the public ticket code.

### Requirement: verification returns translation keys and actions

The POS verification response SHALL return status, severity, title key, message key, params, and available actions.

#### Scenario: ticket is winning and claim is payable

- **GIVEN** a ticket is settled as won
- **AND** a payout claim exists with status `OPEN`
- **WHEN** the cashier verifies the ticket
- **THEN** the response SHALL include status `PAYABLE`
- **AND** action `EXECUTE_PAYOUT`
- **AND** amount/currency params.

#### Scenario: ticket is already paid

- **GIVEN** a payout claim is `PAID`
- **WHEN** the cashier verifies the ticket
- **THEN** the response SHALL include status `ALREADY_PAID`
- **AND** SHALL NOT include enabled `EXECUTE_PAYOUT`.

### Requirement: readiness returns lightweight attention only

Cashier readiness SHALL return non-blocking badges/notifications for previous unpaid payouts and SHALL NOT require acknowledgement in V1.

#### Scenario: previous unpaid payout claims exist

- **GIVEN** previous draws have payout claims `OPEN` or `BLOCKED`
- **WHEN** the cashier readiness endpoint is loaded
- **THEN** the response SHALL include a notification of type `PREVIOUS_UNPAID_PAYOUTS`
- **AND** action `VIEW_PAYOUTS_TO_PROCESS`.

#### Scenario: no previous unpaid payout exists

- **GIVEN** there are no old payout claims requiring attention
- **WHEN** readiness is loaded
- **THEN** the response SHALL not include payout attention noise.

### Requirement: Cashier does not own payout truth

Cashier SHALL NOT create payout claims, mark claims as paid directly, or compute winning amounts.

#### Scenario: cashier executes payout

- **WHEN** the cashier confirms payout execution
- **THEN** Cashier SHALL dispatch or call the existing payout execution path
- **AND** `core.payout` SHALL own the actual payment transition.

### Requirement: Features must not own business invariants

Cashier home services SHALL compose public core/platform/catalog APIs and bus queries.

#### Scenario: Home service data dependencies

- **GIVEN** cashier home needs session/draw/sales/profile data
- **WHEN** implementing the service
- **THEN** it uses CommandBus/QueryBus or public `api/` contracts
- **AND** it does not import `core.*.internal`
- **AND** it does not call repositories.

