# tenant-admin-policies Spec Delta

## ADDED Requirements

### Requirement: Tenant Admin Policies Overview

The server SHALL expose a tenantadmin BFF overview for limits and policies.

#### Scenario: Tenant admin opens limits overview

- **WHEN** an authenticated tenant admin with `limit.read` opens the limits overview
- **THEN** the server SHALL return a single overview payload containing summary metrics, navigation cards, task action links, alerts, and active tenant-level limit rules
- **AND** the web app SHALL NOT need to compose the overview from multiple low-level limit endpoints

#### Scenario: Tenant admin needs a common limit task

- **WHEN** the overview payload is requested
- **THEN** it SHALL include action links for common tenant admin tasks such as blocking a number, limiting a draw/channel, and limiting a seller
- **AND** each action link SHALL include a business label, a short description, an icon, and a route.

### Requirement: PageModel Boundary

PageModel SHALL remain responsible for shell/navigation metadata and SHALL NOT be the owner of interactive limits overview data.

#### Scenario: Sidebar overview link is clicked

- **WHEN** the sidebar routes to `/app/admin/limits`
- **THEN** Angular SHALL render the limits overview page
- **AND** the page SHALL load business data from the tenantadmin policies overview endpoint

### Requirement: Ergonomic Number Limit Creation

Tenant admin number-limit creation SHALL be centered on the business action, not
on the technical rule catalog.

#### Scenario: Tenant admin blocks or limits a number globally

- **WHEN** a tenant admin opens the number limits page
- **THEN** the primary action SHALL let them enter one or more selections, choose
  block or limit behavior, set the amount/count when required, and choose a
  duration
- **AND** the main flow SHALL expose two business intentions: block numbers and
  limit stake exposure per number
- **AND** potential payout exposure SHALL NOT be part of the primary V0 number
  flow until real payout/coverage calculation is exposed clearly to admins
- **AND** potential payout limit definitions SHALL NOT be published in the V0
  admin rule catalog
- **AND** the page SHALL default to the tenant/global scope
- **AND** long explanatory text SHALL NOT appear before the active limits list.

#### Scenario: Tenant admin needs a channel-specific number limit

- **WHEN** a tenant admin wants the same number rule only for one draw channel
- **THEN** the UI SHALL make the scope explicit and route or apply the rule
  against the selected draw-channel ID
- **AND** the API call SHALL NOT send channel display codes where the backend
  expects draw-channel IDs.

### Requirement: Limit Definitions And Simulation

The rules/support page SHALL be the pedagogical surface for limits.

#### Scenario: Tenant admin wants to understand limit definitions

- **WHEN** a tenant admin opens the limit definitions page
- **THEN** it SHALL explain the supported rules and their impact in a concise,
  educational way
- **AND** it SHALL include a simulation area for inspecting currently active
  rules by supported scope.

### Requirement: Supported Limit Scopes After Outlet Removal

Tenant admin limits SHALL expose only runtime-supported scopes.

#### Scenario: Tenant admin configures scoped limits

- **WHEN** the admin UI lists, creates, updates, or deletes limit assignments
- **THEN** it SHALL use `TENANT`, `DRAW_CHANNEL`, `SELLER_TERMINAL`, or `AGENT`
  scopes only
- **AND** seller-terminal scoped requests SHALL use seller-terminal IDs
- **AND** draw-channel scoped requests SHALL use draw-channel IDs
- **AND** `OUTLET` SHALL NOT be offered as a tenant admin limit scope in V0.
