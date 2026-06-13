# Firebase and Keycloak cutover requirements

## ADDED Requirements

### Requirement: Standard runtime uses Firebase without Keycloak

Production and staging SHALL use Firebase as the external authentication
provider and SHALL boot without a Keycloak service or Keycloak configuration.

#### Scenario: Production API starts

- **WHEN** the production API starts with valid Firebase configuration
- **THEN** it starts without resolving a Keycloak host, issuer, JWKS endpoint, or
  admin API

#### Scenario: Unsafe production provider is configured

- **WHEN** production is configured with `keycloak`, `local-jwt`, `local-perf`,
  or `firebase-emulator`
- **THEN** startup fails closed with an actionable configuration error

#### Scenario: Standard Spring Resource Server configuration is loaded

- **WHEN** the server starts in a standard Firebase or local-jwt profile
- **THEN** no Keycloak issuer, JWKS, audience, realm, or decoder resource is
  required or configured

### Requirement: Authorization remains server-owned

A verified Firebase identity SHALL be mapped to an AppUser before Tchalanet
resolves tenant memberships, roles, permissions, operational context, and RLS
context.

#### Scenario: Firebase tenant user calls an authorized endpoint

- **WHEN** a valid Firebase token maps to an active tenant AppUser
- **THEN** roles and permissions are loaded from Tchalanet
- **AND** `TchRequestContext` is created
- **AND** transaction-local RLS variables are bound

#### Scenario: Firebase claims contain authorization hints

- **WHEN** a Firebase token contains tenant or role-like claims
- **THEN** those claims do not replace Tchalanet membership and permission
  resolution

### Requirement: First-login linking is explicit and safe

Tchalanet SHALL support linking a Firebase subject to an admin-provisioned
AppUser using an explicit and audited policy.

#### Scenario: Phone-only user signs in for the first time

- **WHEN** an admin-provisioned AppUser has an eligible verified phone number
  and the matching Firebase user signs in
- **THEN** the Firebase subject can be linked without requiring email
- **AND** the link is audited

#### Scenario: Identity match is ambiguous

- **WHEN** email or phone evidence matches more than one AppUser
- **THEN** no automatic link is created
- **AND** an explicit administrator action is required

### Requirement: Web sends Firebase tokens only to Tchalanet

The web client SHALL attach Firebase ID tokens only to configured Tchalanet API
requests.

#### Scenario: Web calls the Tchalanet API

- **WHEN** an authenticated web user calls a configured Tchalanet API URL
- **THEN** the request includes a current Firebase ID token as a bearer token

#### Scenario: Web calls a third-party URL

- **WHEN** the web application calls an asset, Firebase, telemetry, or
  third-party URL
- **THEN** the Firebase ID token is not attached

### Requirement: Local testing preserves security paths

Local and E2E authentication SHALL use a provider-neutral path that preserves
Tchalanet authorization, context creation, and RLS.

#### Scenario: Local JWT calls a tenant endpoint

- **WHEN** a valid local JWT maps to an active AppUser
- **THEN** normal permission checks and RLS enforcement execute

### Requirement: Local IDE supports Firebase Auth Emulator and real Firebase

Local IDE SHALL switch explicitly between Firebase Auth Emulator and the real
Firebase staging instance without code changes.

#### Scenario: Local IDE uses Firebase Auth Emulator

- **WHEN** local IDE is configured with `firebase-emulator`
- **THEN** web, mobile, and backend connect to the configured Authentication
  Emulator
- **AND** the backend accepts emulator tokens only for the expected local
  project
- **AND** no Firebase Admin private credential is required

#### Scenario: Local IDE uses real Firebase staging

- **WHEN** local IDE is configured with `firebase`
- **THEN** clients use the Firebase staging project
- **AND** the backend verifies normally signed Firebase ID tokens
- **AND** no code change is required

#### Scenario: Emulator token reaches a non-emulator runtime

- **WHEN** an unsigned emulator token reaches Firebase staging or production
- **THEN** the token is rejected

### Requirement: Firebase Auth Emulator is not a production performance proxy

Firebase Auth Emulator SHALL be used for functional and integration testing,
while backend performance tests SHALL use deterministic local authentication.

#### Scenario: Backend load test runs

- **WHEN** a backend permission, context, RLS, or business-flow load test runs
- **THEN** it uses `local-jwt`
- **AND** results are not presented as Firebase production performance results

### Requirement: Authentication secrets are externalized

Firebase Admin private credentials SHALL NOT be committed, embedded in
application configuration, or exposed to the web client.

#### Scenario: Firebase server credentials are configured

- **WHEN** the server runs in Firebase mode
- **THEN** credentials are loaded from an approved environment or secret mount
- **AND** no workstation-specific absolute fallback path is used

### Requirement: Keycloak synchronization is removed

Tchalanet SHALL NOT synchronize AppUsers, memberships, roles, or permissions
with Keycloak.

#### Scenario: An administrator changes a tenant user

- **WHEN** an administrator creates, disables, or changes roles for an AppUser
- **THEN** Tchalanet persists and audits the change internally
- **AND** no Keycloak synchronization endpoint, service, listener, or job runs

### Requirement: Keycloak implementation is not retained

Tchalanet SHALL preserve a provider-neutral identity boundary without retaining
Keycloak implementation, configuration, infrastructure, tests, or current
documentation.

#### Scenario: A future identity provider is required

- **WHEN** a new external identity provider is proposed
- **THEN** it implements the provider-neutral identity contract as a new change
- **AND** Git history may be used as reference without restoring dormant
  Keycloak compatibility code

### Requirement: Mobile uses Firebase without owning authorization

The mobile client SHALL authenticate with Firebase and SHALL obtain
authorization and operational context from Tchalanet.

#### Scenario: Mobile calls the Tchalanet API

- **WHEN** an authenticated mobile user calls a configured Tchalanet API URL
- **THEN** the request includes a current Firebase ID token
- **AND** Tchalanet resolves roles, tenant context, and operational context

#### Scenario: Mobile calls a third-party URL

- **WHEN** the mobile application calls a non-Tchalanet URL
- **THEN** the Firebase ID token is not attached

### Requirement: Published documentation reflects the supported providers

Published documentation SHALL describe Firebase for production/staging and
local-jwt for deterministic local/E2E use without presenting Keycloak as a
standard runtime dependency.

#### Scenario: A new contributor follows the authentication setup guide

- **WHEN** the contributor configures a supported environment
- **THEN** the guide provides the required Firebase or local-jwt settings
- **AND** no standard setup step requires Keycloak
- **AND** canonical server implementation guidance is available under
  `tchalanet-server/docs`
