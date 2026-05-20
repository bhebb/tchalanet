# apiresponse-notices Spec

## ADDED Requirements

### Requirement: Api notices are immediate response feedback

`ApiResponse.notices` SHALL represent only feedback for the current HTTP response.

#### Scenario: Sale accepted with warning

- **GIVEN** a sale is accepted but near a limit
- **WHEN** the handler adds `LIMIT_WARN` to `ApiResponseContext`
- **THEN** the HTTP response contains the notice
- **AND** no persistent notification is created solely because of the notice

### Requirement: ApiResponseBodyAdvice wraps 2xx JSON

The body advice SHALL wrap/enrich successful JSON responses in `ApiResponse<T>`.

#### Scenario: Controller returns DTO

- **GIVEN** a controller returns a normal DTO
- **WHEN** selected media type is JSON
- **THEN** the body advice returns `ApiResponse` with DTO as `data`

### Requirement: Existing ApiResponse is enriched

The body advice SHALL merge context notices/services into an existing `ApiResponse` body.

#### Scenario: Controller returns ApiResponse with notice

- **GIVEN** controller returns `ApiResponse.success(data)` with existing notice
- **AND** `ApiResponseContext` contains another notice
- **WHEN** body advice runs
- **THEN** both notices are present

### Requirement: Status resolution

The system SHALL compute status:

- `PARTIAL` if any service is DOWN or DEGRADED;
- `SUCCESS_WITH_WARNINGS` if any notice severity is WARN;
- `PENDING` if data is null and approval/pending notice exists;
- `SUCCESS` otherwise.

#### Scenario: Service degraded

- **GIVEN** `ApiResponseContext` has service status DEGRADED
- **WHEN** response is wrapped
- **THEN** status is `PARTIAL`

### Requirement: ProblemDetail is never wrapped

Errors SHALL remain `ProblemDetail` and SHALL NOT be wrapped in `ApiResponse`.

#### Scenario: Business error

- **GIVEN** a handler throws `ProblemRest.forbidden`
- **WHEN** exception handler returns `ProblemDetail`
- **THEN** body advice does not wrap it

### Requirement: Files and streams are not wrapped

Binary/file responses SHALL NOT be wrapped.

#### Scenario: PDF receipt endpoint

- **GIVEN** controller returns `Resource` or `byte[]`
- **WHEN** body advice runs
- **THEN** the raw file response is preserved
