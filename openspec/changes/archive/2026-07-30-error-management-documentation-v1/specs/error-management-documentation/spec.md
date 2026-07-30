# Error management documentation

## ADDED Requirements

### Requirement: Cross-project error handling is documented from the shared contract to each client

The repository SHALL provide linked normative documentation for the backend producer boundary, web
consumer boundary, and mobile consumer boundary. The documentation SHALL describe blocking
`ProblemDetail`, non-blocking `ApiResponse.notices`, stable codes, safe parameters, correlation,
retry safety, localization, and exactly-one UI ownership.

#### Scenario: Contributor implements a blocking backend failure

- **WHEN** a contributor needs to add a required API failure
- **THEN** the backend guide points to `ErrorDescriptor`, `ProblemRest`, and `GlobalErrorHandler`
- **AND** the client guides explain how the stable code and correlation fields are consumed

#### Scenario: Contributor implements an optional dashboard slice

- **WHEN** a contributor needs to degrade one slice without breaking the page
- **THEN** the backend guide points to `BffSlices.optional` and structured `ApiNotice`
- **AND** the web and mobile guides explain local notice ownership and fallback rendering

#### Scenario: Contributor exposes an error to a user

- **WHEN** a failure reaches a web or mobile view
- **THEN** the client guide requires localized safe copy by stable code/category
- **AND** raw server, provider, transport, credential, and stack-trace prose is excluded
