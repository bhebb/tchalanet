# tenant-admin-limits — active control page

## ADDED Requirements

### Requirement: Active Limit Control Page

The tenant admin Limit page SHALL show active and disabled limit assignments grouped by business intent rather than by technical scope.

#### Scenario: Admin sees active limits grouped by type

- **GIVEN** the tenant has active global, draw-channel, and seller-terminal limit assignments
- **WHEN** the admin opens `/app/admin/limits`
- **THEN** the page shows grouped sections for blocked numbers, number caps, ticket limits, seller limits, and advanced limits
- **AND** each limit shows a business target label such as `Tout santral`, `Texas · 10:00`, or `POS-005`
- **AND** the default view does not expose scope scores or repository/entity terminology.

#### Scenario: No active limits

- **GIVEN** the tenant has no active limit assignments
- **WHEN** the admin opens `/app/admin/limits`
- **THEN** the page shows an empty state explaining that no special restriction is active
- **AND** the primary action is to block a number
- **AND** the secondary action allows adding another limit.

### Requirement: Active Limit Read Model

The backend SHALL expose enough read-only data in the tenant admin policies overview for the web page to render active limits without additional per-row label lookups.

#### Scenario: Draw channel target label is resolved

- **GIVEN** a `DRAW_CHANNEL` limit assignment exists for a tenant draw channel
- **WHEN** the policies overview is requested
- **THEN** the active limit item includes the draw channel target id
- **AND** the active limit item includes a human-readable draw channel label from the owning draw/channel contract.

#### Scenario: Seller terminal target label is resolved

- **GIVEN** a `SELLER_TERMINAL` limit assignment exists
- **WHEN** the policies overview is requested
- **THEN** the active limit item includes the terminal target id
- **AND** the active limit item includes a human-readable terminal label from the seller terminal owner.

#### Scenario: Feature aggregation respects module ownership

- **GIVEN** the tenant admin policies feature needs labels from draw channels or seller terminals
- **WHEN** it builds the active limit overview
- **THEN** it uses stable APIs/query buses
- **AND** it does not access another module's repositories or SQL tables directly.

### Requirement: Quick Number Blocking Defaults

The block-number quick action SHALL optimize for blocking a number on a draw channel for the current day.

#### Scenario: Admin opens block number from the Limit page

- **GIVEN** the admin opens the block-number quick action from `/app/admin/limits`
- **WHEN** the dialog opens
- **THEN** draw channel scope is selected by default
- **AND** the date/duration defaults to today
- **AND** active/open sellable channels are prioritized in the channel selector.

#### Scenario: Admin opens block number from a draw channel context

- **GIVEN** the admin opens block-number from a draw or draw-channel detail page
- **WHEN** the dialog opens
- **THEN** the relevant draw channel is preselected and locked
- **AND** the admin does not need to search for that channel again.

### Requirement: Active Limit Actions

Each active limit row SHALL expose one clear set of supported actions.

#### Scenario: Disable a limit

- **GIVEN** an active limit is visible in the grouped list
- **WHEN** the admin chooses disable
- **THEN** the UI asks for confirmation
- **AND** the assignment is updated to `enabled = false`
- **AND** the list refreshes after success.

#### Scenario: Delete a limit

- **GIVEN** a limit assignment is visible in the grouped list
- **WHEN** the admin chooses delete
- **THEN** the UI asks for confirmation
- **AND** the assignment is removed using the supported backend API
- **AND** the list refreshes after success.

#### Scenario: Edit a limit

- **GIVEN** a limit assignment is visible in the grouped list
- **WHEN** the admin chooses edit
- **THEN** the UI opens the relevant focused dialog with the current values prefilled
- **AND** it does not send the admin to a generic technical rule form unless no focused editor exists.

### Requirement: Contextual Effective Limits

Draw, draw channel, and seller terminal detail pages SHALL show the limits that effectively apply to that context before the full configuration editor.

#### Scenario: Draw shows inherited blocking reasons

- **GIVEN** a draw channel has an active number block
- **AND** the tenant has an inherited active number cap
- **AND** draw exposure contains numbers near a configured cap
- **WHEN** the admin opens a generated draw detail page for that channel
- **THEN** the page shows static effective limits affecting that draw
- **AND** it identifies whether each limit comes from the draw channel or from the tenant/global scope
- **AND** number blocks are displayed as a simple business label such as `Numéro bloqué : 12, 45`
- **AND** runtime hot exposures are displayed separately from static configured rules
- **AND** it shows the outcome such as block, warning, or approval required.

#### Scenario: Draw channel shows channel blocking reasons

- **GIVEN** a draw channel has an active number block
- **AND** the tenant has an inherited active number cap
- **WHEN** the admin opens the draw channel detail page
- **THEN** the limits section first shows effective limits for that channel
- **AND** it identifies whether each limit comes from the channel or from the tenant/global scope.

#### Scenario: Contextual page does not replace central list

- **GIVEN** the tenant has active limits across multiple channels and seller terminals
- **WHEN** the admin opens one draw or draw channel detail page
- **THEN** the effective summary shows only limits affecting that draw or channel context
- **AND** `/app/admin/limits` remains the place to see all active limits.

### Requirement: Mobile-First Limit Overview

The active limits page SHALL remain usable at 360 dp width.

#### Scenario: Admin uses the Limit page on a phone

- **GIVEN** the viewport is approximately 360 dp wide
- **WHEN** the admin opens `/app/admin/limits`
- **THEN** quick actions and active limits stack vertically
- **AND** target, duration, status, and primary actions remain visible without horizontal scrolling.
