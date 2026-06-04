# public-admin-minimal-scope

## ADDED Requirements

### Requirement: Weekly scope is limited to three surfaces

The weekly web/backend implementation SHALL include only the public widget page,
the SUPER_ADMIN tenant bootstrap dashboard, and the TENANT_ADMIN seller onboarding dashboard.
It SHALL establish reusable integration patterns for future surfaces without implementing those future surfaces in this slice.

#### Scenario: Cashier dashboard is requested during this slice

- **WHEN** a task asks to add cashier dashboard behavior
- **THEN** the task is treated as out of scope for this change

#### Scenario: Future integration needs a dashboard or public widget

- **WHEN** a later integration adds richer dashboards or page widgets
- **THEN** it reuses or extends the widget/action contract pattern established by this slice

### Requirement: Public page uses a widget engine

The public page SHALL render from a backend-provided widget/page payload rather than hard-coded page sections.

#### Scenario: Public payload contains unsupported widget

- **WHEN** the web renderer receives a widget type it does not support
- **THEN** the web UI shows a contained widget fallback without breaking the whole page

### Requirement: SUPER_ADMIN dashboard is action-only

The SUPER_ADMIN dashboard SHALL expose tenant creation and tenant-admin creation only.

#### Scenario: SUPER_ADMIN opens dashboard

- **WHEN** a SUPER_ADMIN opens the dashboard
- **THEN** the available primary actions are create tenant and create tenant admin

### Requirement: TENANT_ADMIN dashboard is seller-onboarding only

The TENANT_ADMIN dashboard SHALL expose seller onboarding only.

#### Scenario: TENANT_ADMIN opens dashboard

- **WHEN** a TENANT_ADMIN opens the dashboard
- **THEN** the available primary action is onboard seller
