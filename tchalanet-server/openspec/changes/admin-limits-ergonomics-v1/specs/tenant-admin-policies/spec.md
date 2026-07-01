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
