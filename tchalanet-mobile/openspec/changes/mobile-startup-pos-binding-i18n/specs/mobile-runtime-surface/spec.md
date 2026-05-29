# Spec: Mobile runtime surface and responsive guardrails

## ADDED Requirements

### Requirement: Runtime surface must be explicit
The Flutter app SHALL maintain a runtime profile containing both business surface and screen constraints.

#### Scenario: POS surface overrides width heuristics
- **GIVEN** the app is running on an enrolled POS device
- **AND** the screen width is similar to a normal Android phone
- **WHEN** the cashier home page is rendered
- **THEN** the app SHALL render the POS handheld layout
- **AND** SHALL NOT choose the richer mobile layout only because the width is large enough

#### Scenario: Mobile phone uses mobile layout
- **GIVEN** the app is running on an Android phone with `TchSurface.MOBILE`
- **WHEN** the cashier home page is rendered
- **THEN** the app SHALL render the mobile seller layout

### Requirement: MediaQuery and LayoutBuilder must be used for different purposes
The Flutter app SHALL use `MediaQuery` for global screen/runtime characteristics and `LayoutBuilder` for local widget constraints.

#### Scenario: Global profile resolution
- **GIVEN** the app starts
- **WHEN** `MaterialApp.router.builder` runs
- **THEN** the app SHALL compute a `TchRuntimeProfile` from `MediaQuery`
- **AND** SHALL expose it through a top-level scope/context extension

#### Scenario: Local component adaptation
- **GIVEN** a component is placed in a constrained area
- **WHEN** the component needs to choose compact vs expanded internal layout
- **THEN** the component SHALL use `LayoutBuilder`
- **AND** SHALL NOT rely only on the full screen width

### Requirement: SafeArea must protect POS/mobile pages
Top-level mobile/POS pages SHALL render under `SafeArea` or equivalent safe insets handling.

#### Scenario: POS with system bars
- **GIVEN** an Android POS has system navigation/status bars
- **WHEN** the cashier sell page is displayed
- **THEN** primary controls SHALL NOT be obscured by system bars

### Requirement: Surface must be sent to backend as display hint
The mobile app SHALL send `X-Tch-Surface` on API requests where backend can adapt payload size or labels.

#### Scenario: POS calls profile current
- **GIVEN** the runtime profile surface is `POS_HANDHELD`
- **WHEN** the app calls `GET /tenant/profile/me/current`
- **THEN** it SHALL send `X-Tch-Surface: POS_HANDHELD`

#### Scenario: Header is not security proof
- **GIVEN** a client sends `X-Tch-Surface: POS_HANDHELD`
- **WHEN** the backend handles a critical action
- **THEN** the backend SHALL NOT treat the header alone as trusted POS authorization

### Requirement: POS pages must prioritize transaction flow
POS handheld layouts SHALL optimize for speed, few visible choices, and sticky critical actions.

#### Scenario: Ticket with many selections
- **GIVEN** a ticket draft has 40 selections
- **WHEN** the POS sell page is displayed
- **THEN** the page SHALL show current entry, a compact preview, line count, total, and a sticky validate/print action
- **AND** SHALL NOT require scrolling to reach the final sell action

#### Scenario: Edit visible selection
- **GIVEN** line 3 is visible in the POS selection preview
- **WHEN** the seller taps the line or edit icon
- **THEN** the app SHALL open a compact edit panel/bottom sheet
- **AND** SHALL update totals immediately after saving

#### Scenario: Edit hidden selection
- **GIVEN** a selection is not visible in the compact preview
- **WHEN** the seller opens all selections
- **THEN** the app SHALL allow editing or deleting by stable `lineId`
- **AND** SHALL NOT use list index as the canonical identity of the line

### Requirement: Mobile and POS may share response schema
The app SHALL support a shared response schema where widgets/actions are optional and surface-aware.

#### Scenario: Same cashier home contract
- **GIVEN** backend returns `CashierHomeResponse`
- **WHEN** POS and mobile clients consume it
- **THEN** both clients SHALL accept the same schema
- **AND** each client MAY ignore unsupported widgets/actions

#### Scenario: Backend filters payload by surface
- **GIVEN** a POS client requests cashier home
- **WHEN** the backend has surface information
- **THEN** the backend MAY omit mobile-only widgets while preserving the same response schema
