# Spec Delta: portal-auth-handoff

## ADDED Requirements

### Requirement: Cross-origin public login handoff

The system SHALL use a server-backed portal handoff when a user authenticates from public-portal and
the resolved target portal is hosted on a different origin.

#### Scenario: Super admin logs in from public host

- **GIVEN** public-portal is served from an origin different from platform-portal
- **AND** the user authenticates successfully as `SUPER_ADMIN`
- **WHEN** login completes
- **THEN** public-portal SHALL request a one-time portal handoff for platform-portal
- **AND** the browser SHALL be redirected to the platform-portal handoff route
- **AND** platform-portal SHALL consume the handoff and land the user in the platform app
- **AND** no Firebase ID token or long-lived application token SHALL appear in the URL
- **AND** the handoff secret SHALL be carried in the URL fragment, not as a query parameter

#### Scenario: Tenant admin logs in from public host

- **GIVEN** public-portal is served from an origin different from admin-portal
- **AND** the user authenticates successfully as `TENANT_ADMIN` or `TENANT_OWNER`
- **WHEN** login completes
- **THEN** public-portal SHALL request a one-time portal handoff for admin-portal
- **AND** the browser SHALL be redirected to the admin-portal handoff route
- **AND** admin-portal SHALL consume the handoff and land the user in the tenant admin app
- **AND** the first private runtime request in admin-portal SHALL be authenticated

#### Scenario: Same-origin private portal

- **GIVEN** public-portal and the resolved target portal share the same origin
- **WHEN** login completes
- **THEN** the web app MAY navigate directly to the target route without creating a server handoff
- **AND** the private runtime request SHALL remain authenticated

#### Scenario: Direct private entry with local session

- **GIVEN** admin-portal or platform-portal has a valid local identity-provider session
- **WHEN** the browser opens a private app route directly
- **THEN** the existing landing/session guard SHALL allow entry without creating a handoff
- **AND** the private runtime request SHALL remain authenticated

#### Scenario: Direct private entry without local session

- **GIVEN** admin-portal or platform-portal has no valid local identity-provider session
- **WHEN** the browser opens a private app route directly
- **THEN** the existing shared login page SHALL handle local sign-in
- **AND** local same-origin sign-in SHALL NOT require a portal handoff

### Requirement: One-time handoff security

Portal handoff codes SHALL be opaque, short-lived, target-bound, and single-use.

#### Scenario: Handoff is created

- **GIVEN** an authenticated user may access the requested target portal
- **AND** the requested entry route is in the target portal relative-path allow-list
- **WHEN** the backend creates a portal handoff
- **THEN** it SHALL generate a secret with at least 128 bits of entropy
- **AND** it SHALL persist only a SHA-256 hash of the secret
- **AND** it SHALL return the raw secret only in the creation response
- **AND** it SHALL derive `targetUrl` from trusted server runtime configuration

#### Scenario: Handoff creation is not authorized

- **GIVEN** an authenticated user may not access the requested target portal
- **WHEN** the user requests a portal handoff for that target portal
- **THEN** the backend SHALL reject creation
- **AND** no handoff secret SHALL be issued

#### Scenario: Handoff entry route is not allowed

- **GIVEN** an authenticated user requests a handoff with an absolute URL or disallowed relative path
- **WHEN** the backend validates the handoff request
- **THEN** it SHALL reject creation
- **AND** no handoff secret SHALL be issued

#### Scenario: Handoff code is reused

- **GIVEN** a handoff code has already been consumed
- **WHEN** another request attempts to consume the same code
- **THEN** the backend SHALL reject the request
- **AND** the backend SHOULD return `410 Gone`
- **AND** the target app SHALL show a recoverable login error

#### Scenario: Handoff code is expired

- **GIVEN** a handoff code is older than its short TTL
- **WHEN** a target app attempts to consume it
- **THEN** the backend SHALL reject the request
- **AND** the backend SHOULD return `410 Gone`
- **AND** the target app SHALL show a recoverable login error

#### Scenario: Handoff target mismatch

- **GIVEN** a handoff was created for admin-portal
- **WHEN** platform-portal attempts to consume it
- **THEN** the backend SHALL reject the request
- **AND** the code SHALL NOT grant access to platform-portal

#### Scenario: Concurrent handoff consumption

- **GIVEN** two requests attempt to consume the same valid handoff concurrently
- **WHEN** the backend processes the requests
- **THEN** exactly one request SHALL succeed
- **AND** all other requests SHALL be treated as replay

#### Scenario: Anonymous target consume

- **GIVEN** the target portal origin has no local identity-provider session
- **WHEN** it posts a valid handoff id and code to consume the handoff
- **THEN** the backend SHALL allow the consume request without an existing bearer token
- **AND** the response SHALL provide the approved target-app session bootstrap material
- **AND** the target app SHALL establish local auth before calling `/runtime/private`

#### Scenario: Consume is rate limited

- **GIVEN** repeated invalid consume attempts occur for the same IP or handoff id
- **WHEN** the rate limit threshold is exceeded
- **THEN** the backend SHALL reject further attempts for the configured window

### Requirement: Portal handoff audit trail

The system SHALL audit security-relevant handoff lifecycle events.

#### Scenario: Handoff lifecycle events occur

- **GIVEN** a handoff is created, consumed, expired, replayed, or consumed by the wrong target
- **WHEN** the backend handles the event
- **THEN** it SHALL emit the matching audit event:
  `PORTAL_HANDOFF_CREATED`, `PORTAL_HANDOFF_CONSUMED`, `PORTAL_HANDOFF_EXPIRED`,
  `PORTAL_HANDOFF_REPLAY_DETECTED`, or `PORTAL_HANDOFF_TARGET_MISMATCH`
- **AND** the audit metadata SHALL NOT include the raw handoff secret

### Requirement: Cross-origin platform support access handoff

The system SHALL use a server-backed support access handoff when a super admin starts tenant support
access from platform-portal and admin-portal is hosted on a different origin.

#### Scenario: Super admin opens tenant admin in support mode

- **GIVEN** platform-portal is served from an origin different from admin-portal
- **AND** a `SUPER_ADMIN` starts support access for an active tenant
- **WHEN** support access is accepted
- **THEN** backend SHALL create a server-backed support access session
- **AND** platform-portal SHALL redirect to admin-portal using a one-time handoff
- **AND** admin-portal SHALL restore the support context after consuming the handoff
- **AND** tenant admin API calls SHALL be authorized for the selected tenant support session

#### Scenario: Support access is restored after handoff

- **GIVEN** admin-portal has consumed a handoff bound to a support access session
- **WHEN** admin-portal calls `GET /platform/tenants/admin-access/current`
- **THEN** the backend SHALL return the active support access session for the current super admin
- **AND** admin-portal SHALL update its support access store from the response
- **AND** the store SHALL act only as a client cache

#### Scenario: Support access is hydrated during app lifecycle

- **GIVEN** admin-portal is running for a super admin
- **WHEN** admin-portal bootstraps, completes handoff consumption, regains window focus, or becomes
  visible again
- **THEN** admin-portal SHALL hydrate `SupportAccessStore` from
  `GET /platform/tenants/admin-access/current`
- **AND** absent-session or expired-session responses SHALL clear support mode UI state

#### Scenario: Support mode UI is derived from store

- **GIVEN** admin-portal displays support-mode affordances such as sidebar state, banner, tenant
  indicator, stop button, or back-to-platform link
- **WHEN** support access state changes
- **THEN** those UI affordances SHALL derive exclusively from `SupportAccessStore`
- **AND** they SHALL clear when support access is stopped or expires

#### Scenario: Support access round trip reuses active session

- **GIVEN** a super admin has an active server-side support access session
- **WHEN** the user navigates platform -> admin -> platform -> admin while the target portal keeps a
  valid local session
- **THEN** admin-portal SHALL restore support UI from the current support access endpoint
- **AND** it SHALL NOT require a new handoff solely to rediscover the active support session

#### Scenario: Support tenant switch is reflected

- **GIVEN** a super admin changes the active support tenant from platform-portal
- **WHEN** admin-portal next hydrates the current support access session
- **THEN** admin-portal SHALL reflect the latest tenant from the backend response
- **AND** stale browser cache SHALL NOT remain the source of truth

#### Scenario: Effective tenant resolves from support session

- **GIVEN** a `SUPER_ADMIN` has an active server-side support access session for a tenant
- **WHEN** the user calls a tenant admin API in support mode
- **THEN** backend tenant resolution SHALL use the active support access session
- **AND** expired or cleared sessions SHALL NOT authorize support mode

#### Scenario: Support access is stopped

- **GIVEN** admin-portal is running in platform support mode
- **WHEN** the super admin stops support access
- **THEN** backend SHALL clear the current support access session
- **AND** admin-portal SHALL stop sending or applying support tenant override context

### Requirement: Support access audit trail

The system SHALL audit support access lifecycle events.

#### Scenario: Support access lifecycle events occur

- **GIVEN** support access is started, restored, stopped, or expires
- **WHEN** the backend handles the event
- **THEN** it SHALL emit the matching audit event:
  `SUPPORT_ACCESS_STARTED`, `SUPPORT_ACCESS_RESTORED`, `SUPPORT_ACCESS_STOPPED`, or
  `SUPPORT_ACCESS_EXPIRED`

### Requirement: Browser storage is not cross-origin source of truth

The system SHALL NOT depend on `localStorage`, `sessionStorage`, or identity-provider local
persistence to transfer authentication or support access context between origins.

#### Scenario: Target portal starts with empty browser storage

- **GIVEN** the target portal origin has empty `localStorage` and `sessionStorage`
- **WHEN** it receives a valid handoff code
- **THEN** it SHALL still be able to establish the correct authenticated app session
- **AND** it SHALL load the private runtime without a 401 loop

### Requirement: Handoff failure UX

The target portal SHALL handle failed handoff consumption without leaking the handoff secret.

#### Scenario: Handoff consume fails

- **GIVEN** a target portal cannot consume a handoff
- **WHEN** it handles the failure
- **THEN** it SHALL remove the handoff fragment from browser history
- **AND** it SHALL navigate to the local login page with only a non-sensitive error code
- **AND** it SHALL NOT redirect to another portal with the handoff code

### Requirement: Handoff route and guard ordering

The target portals SHALL keep handoff routing reachable without a pre-existing local session.

#### Scenario: Handoff route is opened without local session

- **GIVEN** admin-portal or platform-portal has no valid local identity-provider session
- **WHEN** the browser opens `/login/handoff` with a handoff fragment
- **THEN** the handoff route SHALL be reachable before private auth guards redirect to login
- **AND** existing landing/session guard behavior for non-handoff routes SHALL remain unchanged
