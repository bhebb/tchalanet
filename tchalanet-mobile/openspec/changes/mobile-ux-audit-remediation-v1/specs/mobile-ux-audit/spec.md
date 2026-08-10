# Specification — Mobile UX Audit Remediation

## ADDED Requirements

### Requirement: Frequent seller controls provide immediate feedback

Game chips, bet options, verification, and primary navigation SHALL expose
pressed, disabled, loading, and error states appropriate to their action.

#### Scenario: Seller selects a game chip

- **WHEN** the seller taps a game or bet chip
- **THEN** the control shows a Material pressed/ripple state
- **AND** the selected state is visible and accessible
- **AND** the layout remains stable on a narrow device.

#### Scenario: Seller tries to verify an empty ticket

- **WHEN** the verification code or sale input is empty/invalid
- **THEN** the primary action is disabled
- **AND** no API request is sent.

### Requirement: Completion supports the next sale efficiently

Completion SHALL make the next sale and ticket completion actions immediate,
localized, and unambiguous.

#### Scenario: Seller starts a new ticket

- **WHEN** the seller taps `Nouvo tikè` after a successful sale
- **THEN** mobile navigates directly to the sale screen
- **AND** preserves a valid selected draw when available
- **AND** does not force a return through the dashboard.

#### Scenario: Completion labels follow the active locale

- **WHEN** the completion screen is rendered in HT, FR, or EN
- **THEN** copy, print, new-ticket, error, and fallback labels use i18n keys
- **AND** no hardcoded French action label is displayed.

### Requirement: Data screens reveal incomplete data states

Dashboard and list screens SHALL distinguish loading, empty, error, and
paginated states.

#### Scenario: Latest ticket has no data

- **WHEN** latest-ticket loading returns empty or fails
- **THEN** the dashboard shows an explicit localized empty/error state
- **AND** it does not leave a blank section under the title.

#### Scenario: History exceeds the first page

- **WHEN** more than 50 tickets match the history query
- **THEN** mobile exposes pagination or load more
- **AND** it does not silently present the first 50 as the complete result.

#### Scenario: Seller searches ticket history

- **WHEN** the seller types a ticket code
- **THEN** mobile applies a debounced search or clearly submits the query
- **AND** loading, empty, and error states remain visible.

### Requirement: Results and reports preserve filter meaning

Results and report filters SHALL preserve a visible and truthful relationship
between the selected filter and displayed totals.

#### Scenario: Seller filters results

- **WHEN** result filters are displayed on a small screen
- **THEN** the filter controls remain usable without consuming the result view
- **AND** the active date range is visible.

#### Scenario: Seller filters a report by draw

- **WHEN** a draw filter is active
- **THEN** totals either update to that filter
- **OR** the UI labels them explicitly as whole-day totals.

### Requirement: Unsupported functionality is not advertised

The UI SHALL not present controls that imply an unavailable capability.

#### Scenario: QR scan is unavailable

- **GIVEN** no scanner implementation is active
- **WHEN** the seller opens ticket verification
- **THEN** mobile does not present a misleading QR scanner placeholder
- **AND** manual code verification remains available.

### Requirement: Settings and account actions are observable

Settings and account surfaces SHALL expose the state and outcome of actions.

#### Scenario: Settings update is submitted

- **WHEN** a seller changes a setting
- **THEN** the control shows saving/disabled state
- **AND** mobile shows localized success or error feedback
- **AND** the server result is reflected in the displayed value.

#### Scenario: Seller is blocked

- **WHEN** the backend provides a blocked reason or admin contact
- **THEN** the forbidden screen displays that reason and contact action
- **AND** logout remains available.

### Requirement: POS PDF behavior remains isolated from UX sharing

Explicit document sharing MAY use a system surface, but automatic POS printing
SHALL remain inside the Tchalanet workflow.

#### Scenario: Report or ticket PDF is explicitly requested

- **WHEN** the seller explicitly chooses download, share, or PDF fallback
- **THEN** the system PDF/share surface may open
- **AND** the action is not treated as an automatic POS print.

#### Scenario: POS auto-print is executed

- **WHEN** a physical POS printer mode is active after a sale
- **THEN** `PrinterService` dispatches ESC/POS or the vendor adapter
- **AND** mobile does not call `Printing.layoutPdf`
- **AND** Tchalanet remains the foreground application.
