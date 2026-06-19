## MODIFIED Requirements

### Requirement: Provider-Neutral Web Session

The web application session SHALL be managed through provider-neutral auth orchestration while Firebase remains the selected identity-provider adapter.

#### Scenario: Persisted Firebase user returns to a private route

- **GIVEN** Firebase has a persisted authenticated user
- **WHEN** the user opens a private Tchalanet route after closing and reopening the browser
- **THEN** the Firebase adapter waits for provider auth state restoration
- **AND** the application session is rebuilt from the private runtime bootstrap
- **AND** the user is not redirected to login solely because Firebase emitted a transient anonymous state during startup

#### Scenario: User logs out

- **GIVEN** the user is authenticated
- **WHEN** the user requests logout
- **THEN** the configured auth client signs out from the provider
- **AND** the application session is reset to anonymous state

### Requirement: Authenticated Application API Calls

Non-public Tchalanet application API calls SHALL receive a Firebase bearer token when a provider session exists.

#### Scenario: Private runtime bootstrap is requested

- **GIVEN** Firebase can provide an access token
- **WHEN** the frontend calls the private runtime bootstrap endpoint
- **THEN** the auth bearer interceptor attaches `Authorization: Bearer <token>`
- **AND** public runtime endpoints remain anonymous
