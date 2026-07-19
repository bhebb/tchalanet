## ADDED Requirements

### Requirement: Seller terminal navigation has five bounded destinations
The authenticated seller terminal SHALL render one localized navigation bar with Home, Tickets,
Results, Reports, and Profile. It SHALL not present an operational drawer.

#### Scenario: A seller selects a destination
- **WHEN** the seller selects a destination in the bottom navigation
- **THEN** the app navigates to its canonical route
- **AND** the selected destination uses the Tchalanet active accent.

### Requirement: Profile owns secondary seller actions
The Profile surface SHALL own seller PIN change, locale selection, support, app/version details,
printer actions, and logout. Header avatar activation SHALL navigate to Profile.

#### Scenario: A seller logs out
- **WHEN** the seller confirms logout from Profile
- **THEN** the application clears the authenticated session
- **AND** navigates to login.

### Requirement: Results is backed by a real query
The application SHALL expose Results only when it can load a typed, searchable result collection.
It SHALL not render a static or placeholder results page.

#### Scenario: Results query is unavailable
- **WHEN** the result-search API contract is not available
- **THEN** the Results destination is not added to the released navigation.
