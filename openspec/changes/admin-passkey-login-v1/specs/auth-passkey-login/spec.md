# Specification: Admin passkey login

## ADDED Requirements

### Requirement: Authenticated app users can enroll passkeys

The system SHALL allow an authenticated `APP_USER` to enroll a passkey for the current device.

SellerTerminal POS actors SHALL NOT enroll or use passkeys.

#### Scenario: Successful passkey enrollment

- **GIVEN** an authenticated active app user opens profile/security
- **WHEN** the user chooses to add this device
- **THEN** the backend SHALL issue a WebAuthn registration challenge
- **AND** the browser SHALL create a passkey using the platform authenticator when available
- **AND** the backend SHALL persist the verified credential for that app user.

#### Scenario: Enrollment requires an active session

- **GIVEN** no authenticated app session exists
- **WHEN** a registration challenge is requested
- **THEN** the backend SHALL reject the request.

### Requirement: Admin and super admin users can login with passkeys

The admin and platform login surfaces SHALL offer passkey login as an optional method when the
browser supports WebAuthn.

#### Scenario: Successful passkey login

- **GIVEN** an active app user has a registered passkey
- **WHEN** the user chooses passkey login and completes the device prompt
- **THEN** the backend SHALL verify the WebAuthn assertion
- **AND** the web app SHALL establish the normal authenticated application session
- **AND** the app SHALL call `/runtime/private` and redirect using the existing post-login rules.

#### Scenario: Passkey unavailable

- **GIVEN** the browser does not support WebAuthn or no passkey exists on the device
- **WHEN** the login page renders
- **THEN** username/email + password login SHALL remain available.

#### Scenario: Password recovery remains the fallback

- **GIVEN** a user loses access to a passkey device
- **WHEN** the user needs to recover access
- **THEN** the existing username/email password reset flow SHALL remain available.

### Requirement: Passkey attempts are protected and auditable

The backend SHALL rate-limit and audit passkey registration and authentication attempts.

#### Scenario: Repeated failed passkey assertions

- **GIVEN** repeated passkey authentication attempts fail for the same client context
- **WHEN** they exceed the configured threshold
- **THEN** the backend SHALL reject or slow down further attempts
- **AND** it SHALL record an audit event without exposing whether the target account exists.

