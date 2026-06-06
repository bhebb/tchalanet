# public-admin-widget-bff

## ADDED Requirements

### Requirement: Backend contracts establish future integration rails

Backend contracts consumed by this slice SHALL be documented and tested in a way that later web integrations can reuse.

#### Scenario: Later web surface needs backend data

- **WHEN** a later surface needs public widgets, role dashboards, or admin actions
- **THEN** it extends the established PageModel/widget and action endpoint patterns instead of creating an unrelated contract style

### Requirement: Backend public widget page payload is reused

The web slice SHALL reuse the existing backend public page payload endpoint.
The backend payload SHALL include stable widget ids/types, translation keys, theme-compatible configuration, and contained widget errors when applicable.

#### Scenario: Anonymous user loads public page

- **WHEN** an anonymous user requests the public page payload
- **THEN** the backend returns ordered widgets and payloads for supported public widgets

#### Scenario: Widget provider fails

- **WHEN** one public widget provider fails
- **THEN** the backend returns a contained widget error for that widget and does not fail the whole page when the remaining page can still render

#### Scenario: Translation value is not resolved

- **WHEN** a widget has a title, label, or description key without a resolved value
- **THEN** the backend still returns the key/fallback contract so the frontend can render directly

#### Scenario: Widget payload is cacheable

- **WHEN** the backend caches a PageModel/template payload
- **THEN** the cache is only an optimization and does not decide action authorization or mutation validity

### Requirement: SUPER_ADMIN tenant provisioning is reused

The web slice SHALL reuse the existing SUPER_ADMIN tenant provisioning endpoint.

#### Scenario: SUPER_ADMIN creates tenant

- **WHEN** a SUPER_ADMIN submits valid tenant provisioning data
- **THEN** the backend creates the tenant and returns the provisioning result

### Requirement: SUPER_ADMIN can create initial tenant admin during provisioning

The web slice SHALL use `initialAdminEmail` during tenant provisioning for the first tenant admin.

#### Scenario: SUPER_ADMIN creates tenant admin

- **WHEN** a SUPER_ADMIN includes `initialAdminEmail` in tenant provisioning
- **THEN** the backend provisions the tenant admin and returns `initialAdminUserId`

### Requirement: TENANT_ADMIN seller onboarding is reused

The web slice SHALL reuse the existing identity user creation endpoint for seller onboarding in the current tenant.

#### Scenario: TENANT_ADMIN onboards seller

- **WHEN** a TENANT_ADMIN submits valid seller onboarding data
- **THEN** the backend creates or provisions the seller in the tenant from request context

#### Scenario: TENANT_ADMIN attempts tenant override

- **WHEN** a TENANT_ADMIN request includes a tenant override
- **THEN** the backend ignores or rejects the override and uses only the tenant from request context
