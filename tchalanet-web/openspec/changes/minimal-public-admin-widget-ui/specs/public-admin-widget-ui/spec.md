# public-admin-widget-ui

## ADDED Requirements

### Requirement: Web implementation establishes reusable integration patterns

The web implementation SHALL establish reusable patterns for future PageModel, dashboard, and action integrations.

#### Scenario: Later dashboard is added

- **WHEN** a later dashboard is implemented
- **THEN** it reuses the typed API service, role route, widget renderer, and action-form patterns established by this slice when applicable

### Requirement: Public page renders backend widgets

The web app SHALL render the public page from a backend-provided widget/page payload.

#### Scenario: Public page receives supported widgets

- **WHEN** the public page loads a payload with supported widget types
- **THEN** the web app renders the widgets in backend-provided order

#### Scenario: Public page receives unsupported widget

- **WHEN** the public page receives an unsupported widget type
- **THEN** the web app renders a contained fallback for that widget

### Requirement: SUPER_ADMIN dashboard exposes only tenant bootstrap actions

The SUPER_ADMIN dashboard SHALL expose tenant onboarding with initial tenant-admin creation only.

#### Scenario: SUPER_ADMIN opens dashboard

- **WHEN** a SUPER_ADMIN opens the dashboard
- **THEN** the UI shows tenant provisioning and initial admin email fields
- **AND** the UI does not show analytics, jobs, audit, flags, release notes, or service health widgets

### Requirement: TENANT_ADMIN dashboard exposes only seller onboarding

The TENANT_ADMIN dashboard SHALL expose seller onboarding only.

#### Scenario: TENANT_ADMIN opens dashboard

- **WHEN** a TENANT_ADMIN opens the dashboard
- **THEN** the UI shows the seller onboarding workflow
- **AND** the UI does not show tenant id override controls

### Requirement: Role routing uses authenticated role

The web app SHALL route authenticated users to the minimal dashboard matching their role.

#### Scenario: Wrong role accesses dashboard

- **WHEN** a user without the required role attempts to access a dashboard
- **THEN** the route guard blocks access or redirects according to the existing web auth pattern
