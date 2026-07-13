# Spec: Public Portal E2E

Base URL :4301. No authentication. Assertions are UI-observable only.

## ADDED Requirements

### Requirement: Public shell renders anonymously

The public home SHALL load and render the public shell without authentication.

#### Scenario: Home loads

- **WHEN** an anonymous visitor navigates to `/`
- **THEN** the response is OK and the public shell is visible.

### Requirement: Public runtime navigation renders resolved content

Navigating from home to a public page-model route SHALL render its resolved
content, and the DOM SHALL NOT expose private (tenant/cashier/platform) provider
sources.

#### Scenario: Navigate to a public page-model route

- **WHEN** the visitor follows a public navigation destination
- **THEN** the target page renders its content
- **AND** no private provider source identifier appears in the DOM.

### Requirement: Public ticket verification shows a state

Submitting a ticket code on the public verification UI SHALL show a result state
or a not-found state. The test SHALL assert the state shown, not the verification
computation.

#### Scenario: Verify an unknown code

- **WHEN** the visitor submits a code that does not exist
- **THEN** a controlled "not found" state is displayed (no crash, no raw error).

### Requirement: Login entry surfaces failures inline

`/login` SHALL render, and invalid credentials SHALL surface an inline error
without navigating away.

#### Scenario: Invalid credentials

- **WHEN** the visitor submits wrong credentials on `/login`
- **THEN** an inline error is shown and the URL stays on `/login`.
