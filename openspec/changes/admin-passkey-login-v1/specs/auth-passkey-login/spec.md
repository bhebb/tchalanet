# Specification: Admin passkey login

## ADDED Requirements

### Requirement: Passkeys are scoped to APP_USER credentials

The system SHALL model passkeys as WebAuthn credentials registered to an existing `APP_USER`.

SellerTerminal POS actors SHALL NOT enroll or use passkeys.

#### Scenario: Credential is not a trusted device record

- **GIVEN** an app user registers a WebAuthn credential
- **WHEN** the credential is stored
- **THEN** the system SHALL store it as a credential record for one `APP_USER`
- **AND** it SHALL NOT assume that the credential maps to exactly one physical device.

#### Scenario: SellerTerminal remains unchanged

- **GIVEN** a SellerTerminal signs in from the POS client
- **WHEN** it authenticates
- **THEN** it SHALL continue using the existing Firebase email/PIN flow
- **AND** it SHALL NOT enroll or use passkeys.

### Requirement: Enrollment requires recent authentication

The system SHALL allow passkey enrollment only from a recently authenticated active `APP_USER`
session.

A restored long-lived session alone SHALL NOT be sufficient.

#### Scenario: Successful passkey enrollment

- **GIVEN** an active authenticated app user satisfies the recent-authentication policy
- **AND** the request uses an approved RP ID and origin
- **WHEN** the user chooses to add a passkey
- **THEN** the backend SHALL issue a short-lived single-use registration challenge
- **AND** the browser SHALL create a credential with user verification required
- **AND** the backend SHALL verify RP ID, origin, challenge, user verification, and credential data
- **AND** the backend SHALL persist the verified credential and audit record.

#### Scenario: Enrollment with stale session

- **GIVEN** an app user has only a restored long-lived session
- **WHEN** a registration challenge is requested
- **THEN** the backend SHALL require password reauthentication, an existing passkey, or another
  approved step-up before enrollment.

### Requirement: Admin and super admin users can login with passkeys

The admin and platform login surfaces SHALL offer passkey login as an optional method when the
browser supports WebAuthn.

#### Scenario: Discoverable passkey login

- **GIVEN** an active app user has a registered discoverable credential
- **WHEN** the user chooses "Continue with a passkey"
- **THEN** the backend SHALL issue a short-lived single-use authentication challenge
- **AND** the browser SHALL allow the user to select an available credential
- **AND** the backend SHALL validate assertion, RP ID, origin, challenge, credential status, and
  user verification
- **AND** the backend SHALL resolve the owning active `APP_USER` and existing Firebase identity
- **AND** the selected adapter SHALL establish Firebase authentication
- **AND** the web app SHALL load `/runtime/private`.

#### Scenario: Username-first remains possible

- **GIVEN** the product chooses a username-first passkey flow later
- **WHEN** the user enters username/email before WebAuthn
- **THEN** the server MAY restrict allowed credential IDs for that app user
- **AND** passkey login SHALL still not depend on username lookup V0 as its only supported model.

#### Scenario: Passkey unavailable

- **GIVEN** the browser does not support WebAuthn or no passkey exists on the device
- **WHEN** the login page renders
- **THEN** username/email + password login SHALL remain available.

### Requirement: Firebase remains the final token issuer

Passkey authentication SHALL establish the normal Firebase-backed app session.

#### Scenario: Successful WebAuthn assertion

- **GIVEN** a WebAuthn assertion is cryptographically valid
- **WHEN** the backend resolves the credential owner
- **THEN** it SHALL require the `APP_USER` to remain active
- **AND** it SHALL require a usable existing Firebase identity
- **AND** it SHALL establish Firebase authentication for that existing identity
- **AND** the normal Tchalanet bootstrap SHALL verify actor status, tenant, roles, and permissions.

#### Scenario: Blocked user has a valid credential

- **GIVEN** an `APP_USER` is suspended or otherwise blocked
- **AND** the user's passkey assertion is valid
- **WHEN** passkey login is verified
- **THEN** the login SHALL fail with a generic authentication error.

### Requirement: Credential persistence is explicit and auditable

The backend SHALL persist passkey credentials with enough metadata for future WebAuthn ceremonies,
revocation, and audit.

#### Scenario: Credential is stored

- **GIVEN** enrollment verification succeeds
- **WHEN** the backend stores the credential
- **THEN** it SHALL store a globally unique credential ID, typed `APP_USER` ID, public key,
  signature counter, transports, authenticator metadata, backup eligibility/state when available,
  display name, created timestamp, last-used timestamp, revoked timestamp, and creation request ID
- **AND** it SHALL NOT store biometrics, device PINs, private keys, or local unlock secrets.

#### Scenario: Credential is revoked

- **GIVEN** a credential is logically revoked
- **WHEN** it is used for authentication
- **THEN** the backend SHALL reject it with a generic authentication error.

### Requirement: Challenges are short-lived and single-use

The backend SHALL manage registration and authentication challenges as security state.

#### Scenario: Challenge is consumed once

- **GIVEN** a challenge has already been used
- **WHEN** the same challenge is submitted again
- **THEN** the backend SHALL reject the request.

#### Scenario: Challenge ceremony mismatch

- **GIVEN** a registration challenge is issued
- **WHEN** it is submitted to an authentication verification endpoint
- **THEN** the backend SHALL reject the request.

#### Scenario: Origin or RP mismatch

- **GIVEN** a challenge was issued for an expected RP ID and origin
- **WHEN** verification receives a different RP ID or origin
- **THEN** the backend SHALL reject the request.

### Requirement: Passkey attempts are protected and auditable

The backend SHALL rate-limit and audit passkey registration, authentication, and revocation attempts.

Sensitive logs SHALL exclude challenges, assertions, client data, and Firebase custom tokens.

#### Scenario: Repeated failed passkey assertions

- **GIVEN** repeated passkey authentication attempts fail for the same client context
- **WHEN** they exceed the configured threshold
- **THEN** the backend SHALL reject or slow down further attempts
- **AND** it SHALL record an audit event without exposing whether the target account exists.

### Requirement: Recovery and support do not bypass user presence

The system SHALL preserve recovery options while preventing support from enrolling credentials for
users.

#### Scenario: User loses authenticator

- **GIVEN** a user loses access to all passkey authenticators
- **WHEN** the user recovers through username/email + password reset
- **THEN** the recovery policy SHALL define whether existing passkeys are revoked or preserved only
  after stronger reauthentication.

#### Scenario: Support revokes credential

- **GIVEN** an authorized support actor handles an audited recovery request
- **WHEN** support revokes one or more credentials
- **THEN** the affected credentials SHALL no longer authenticate.

#### Scenario: Support cannot enroll credential

- **GIVEN** support is acting on behalf of a user
- **WHEN** support attempts to create a passkey for that user
- **THEN** the system SHALL not provide any flow that bypasses the user's own enrollment ceremony.
