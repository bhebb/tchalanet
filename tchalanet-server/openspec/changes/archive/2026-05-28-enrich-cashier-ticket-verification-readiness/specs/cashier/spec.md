# Spec: Cashier POS verification and readiness

## ADDED Requirements

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
