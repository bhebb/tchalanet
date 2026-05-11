# core.terminal spec delta

## ADDED Requirements

### Requirement: GetCurrentOperationalContextQueryHandler

`core.terminal` SHALL provide a handler for `GetCurrentOperationalContextQuery(tenantId, userId, terminalIdHeader, deviceId, terminalBinding)` returning `CurrentOperationalContextView(terminalId, outletId, salesSessionId, source)`.

#### Scenario: Signed device binding wins

- **GIVEN** a request carrying a verified signed device binding
- **WHEN** the handler runs
- **THEN** it SHALL resolve `terminalId` from the binding, `outletId` from the terminal, and a candidate `salesSessionId` from the terminal+user
- **AND** `source` SHALL be `SIGNED_DEVICE_BINDING`

#### Scenario: Server bootstrap

- **GIVEN** an authenticated user assigned to exactly one terminal with an `OPEN` session
- **WHEN** the handler runs without a signed binding
- **THEN** `source` SHALL be `SERVER_BOOTSTRAP`

#### Scenario: Admin selection

- **GIVEN** a `TENANT_ADMIN` with an active admin POS selection
- **WHEN** the handler runs
- **THEN** the view SHALL reflect the selected terminal/outlet/session
- **AND** `source` SHALL be `ADMIN_SELECTION`

#### Scenario: Client claim fallback

- **GIVEN** a request that carries only an unverified client-supplied terminal id
- **WHEN** the handler runs
- **THEN** `source` SHALL be `CLIENT_CLAIM`
- **AND** the response SHALL NOT be used for sensitive operations

#### Scenario: No resolvable context

- **GIVEN** no signed binding, no single-terminal assignment, no admin selection, no client claim
- **WHEN** the handler runs
- **THEN** it SHALL return a view with `source = NONE` (or the upstream resolver SHALL emit `Optional.empty()`)

### Requirement: Resolution does not check operational state

The handler SHALL NOT check `terminal.locked`, outlet block flags, or session status. Those checks belong to use-case validators.

#### Scenario: Locked terminal still resolves

- **GIVEN** a verified device binding pointing to a `LOCKED` terminal
- **WHEN** the handler runs
- **THEN** it SHALL still return the terminal/outlet/session view
- **AND** the downstream use-case validator SHALL reject the operation with `TERMINAL_LOCKED`
