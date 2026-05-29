# Spec: Mobile/POS terminal binding and startup flow

## ADDED Requirements

### Requirement: Profile current must bootstrap app security state
`GET /tenant/profile/me/current` SHALL be the post-login bootstrap endpoint for mobile/POS/web clients.

#### Scenario: Device not enrolled
- **GIVEN** a cashier authenticates successfully
- **AND** the request comes from a POS surface with no valid enrolled device binding
- **WHEN** the app calls `GET /tenant/profile/me/current`
- **THEN** the response SHALL be HTTP 200
- **AND** `startupState` SHALL be `DEVICE_NOT_ENROLLED`
- **AND** `operationalContext.trusted` SHALL be false
- **AND** `allowedNextActions` SHALL NOT contain `VIEW_CASHIER_HOME` or `SELL_TICKET`

#### Scenario: Session closed
- **GIVEN** a cashier authenticates successfully
- **AND** the device is enrolled, active, and bound to a terminal/outlet
- **AND** no sales session is open
- **WHEN** the app calls `GET /tenant/profile/me/current`
- **THEN** the response SHALL be HTTP 200
- **AND** `startupState` SHALL be `SESSION_CLOSED`
- **AND** `allowedNextActions` SHALL contain `OPEN_SESSION`
- **AND** `allowedNextActions` SHALL NOT contain `SELL_TICKET`

#### Scenario: Ready for cashier
- **GIVEN** a cashier authenticates successfully
- **AND** the device binding is trusted
- **AND** the terminal/outlet are valid
- **AND** a matching sales session is open
- **WHEN** the app calls `GET /tenant/profile/me/current`
- **THEN** `startupState` SHALL be `READY_FOR_CASHIER`
- **AND** `allowedNextActions` SHALL contain `VIEW_CASHIER_HOME` and `SELL_TICKET`

### Requirement: Cashier home must not be the security bootstrap endpoint
`GET /tenant/cashier/home` SHALL return screen content only after required context is valid.

#### Scenario: Device not enrolled calls cashier home directly
- **GIVEN** a valid cashier token
- **AND** no trusted device binding
- **WHEN** the client calls `GET /tenant/cashier/home` directly
- **THEN** the backend SHALL return `403 device.not_enrolled` or `403 operational_context.required`

#### Scenario: Session closed calls cashier home directly
- **GIVEN** a trusted device binding
- **AND** no open sales session
- **WHEN** the client calls `GET /tenant/cashier/home` directly
- **THEN** the backend SHALL return `409 session.required`

### Requirement: POS device binding must be admin-controlled
A seller SHALL NOT freely modify terminal or outlet assignment from the POS/mobile cashier app.

#### Scenario: POS terminal reassignment
- **GIVEN** a POS device is bound to terminal `POS-02`
- **WHEN** a seller attempts to change the terminal from the POS app
- **THEN** the app SHALL NOT expose a free terminal edit action
- **AND** the backend SHALL reject untrusted terminal claims for critical actions

#### Scenario: Admin rebinds terminal
- **GIVEN** a tenant admin has permission `device.bind_terminal`
- **WHEN** the admin binds a device to a new terminal
- **THEN** the backend SHALL audit the action
- **AND** subsequent POS context resolution SHALL use the new binding

### Requirement: Trusted operational context is mandatory for critical seller actions
Critical seller actions SHALL require an operational context sourced from `SIGNED_DEVICE_BINDING` or `ADMIN_SELECTION`.

#### Scenario: Direct sell without device binding
- **GIVEN** a valid cashier token
- **AND** no device binding headers
- **WHEN** the client calls `POST /tenant/seller/tickets/sell`
- **THEN** the backend SHALL return `403 operational_context.required`

#### Scenario: Direct sell with fake terminal headers
- **GIVEN** a valid cashier token
- **AND** client-supplied terminal/outlet/session headers that do not resolve to trusted binding
- **WHEN** the client calls `POST /tenant/seller/tickets/sell`
- **THEN** the backend SHALL return `403 operational_context.untrusted`

#### Scenario: Sell with trusted binding but closed session
- **GIVEN** a trusted device binding
- **AND** the sales session is closed
- **WHEN** the client calls `POST /tenant/seller/tickets/sell`
- **THEN** the backend SHALL return `409 session.required`

### Requirement: Tenant plan and entitlements must gate POS/mobile capabilities
Seller POS/mobile flows SHALL check tenant subscription, plan features, and quotas.

#### Scenario: Plan blocks POS selling
- **GIVEN** a tenant subscription is active
- **AND** the tenant plan does not enable `POS_SELLING`
- **WHEN** a POS client calls `GET /tenant/profile/me/current`
- **THEN** the response SHALL include `startupState=PLAN_FEATURE_BLOCKED`
- **AND** capability `posSelling` SHALL be false

#### Scenario: Plan blocks final sell
- **GIVEN** a tenant plan does not enable `POS_SELLING`
- **WHEN** a POS client calls `POST /tenant/seller/tickets/sell`
- **THEN** the backend SHALL return `403 plan.feature_required`

#### Scenario: Device quota exceeded
- **GIVEN** tenant plan `maxDevices=2`
- **AND** tenant already has 2 active devices
- **WHEN** a tenant admin calls device enrollment
- **THEN** the backend SHALL return `409 plan.quota_exceeded`

### Requirement: Preview and sell must be separate seller actions
The seller ticket flow SHALL support `PREVIEW_TICKET` and `SELL_TICKET` as distinct actions.

#### Scenario: Preview validates draft without creating ticket
- **GIVEN** a seller has a local ticket draft
- **WHEN** the app calls ticket preview
- **THEN** the backend SHALL return price, line count, warnings, blockers, language info, and next actions
- **AND** SHALL NOT create a final ticket resource

#### Scenario: Sell revalidates even after preview
- **GIVEN** preview returned valid
- **AND** the session closes before final sell
- **WHEN** the app calls sell
- **THEN** the backend SHALL reject the sale with `409 session.required`

#### Scenario: Sell can be called without preview
- **GIVEN** a client skips preview
- **WHEN** the client calls sell with a valid request and idempotency key
- **THEN** the backend SHALL perform all validations
- **AND** SHALL create the ticket only if all critical checks pass

### Requirement: Final sell must be idempotent
`SELL_TICKET` SHALL require idempotency protection.

#### Scenario: Missing idempotency key
- **GIVEN** a valid sell payload
- **WHEN** the client calls final sell without `Idempotency-Key`
- **THEN** the backend SHALL return `400 idempotency.missing`

#### Scenario: Same key same payload
- **GIVEN** a sell request succeeds with `Idempotency-Key=K1`
- **WHEN** the same request is retried with the same key and same payload
- **THEN** the backend SHALL return the same ticket result

#### Scenario: Same key different payload
- **GIVEN** a sell request succeeds or is in progress with `Idempotency-Key=K1`
- **WHEN** a different payload is sent with the same key
- **THEN** the backend SHALL return `409 idempotency.payload_mismatch`
