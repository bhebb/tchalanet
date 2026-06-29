# Tasks — error-contract-bff-web-v1

## 1. Contract design

- [x] Inspect backend `GlobalErrorHandler`, `ApiResponseBodyAdvice`, `ApiNotice`, and response context.
- [x] Inspect web `ApiResponse`, `ProblemDetail`, and shell feedback mapping.
- [x] Confirm this change clarifies the existing `ApiResponse`/`ProblemDetail` contract rather than
      redesigning it.
- [x] Decide whether `ApiNotice.meta` remains the correlation carrier or whether a typed
      diagnostic field is added to the API contract.
- [x] Define central backend error-code catalog placement per layer/domain.
- [x] Document that code centralization is owner-based, not one global enum.
- [x] Update `docs/conventions/api/api_response.md` with the BFF notice shape, reserved `meta` keys,
      and concrete blocking/non-blocking examples.

## 2. Backend implementation plan

- [x] Add/standardize constants for transversal error codes.
- [x] Add a small `ApiResponseNotices`/notice-source helper so BFF code does not manually assemble
      every notice metadata map.
- [x] Ensure blocking exceptions only carry stable code/context and final HTTP shape is assembled by
      `GlobalErrorHandler`.
- [x] Ensure non-blocking notices only carry stable code/context and final response shape is assembled
      by `ApiResponseBodyAdvice`.
- [x] Add BFF helper/model for required vs optional slice aggregation policy.
- [x] Ensure non-blocking slice failures add `ApiNotice` with code, severity, domain/source, and trace
      metadata.
- [x] Ensure blocking failures keep `ProblemDetail` code and trace fields through `GlobalErrorHandler`.
- [x] Add focused backend tests for blocking and non-blocking BFF aggregation.

## 3. Web implementation plan

- [x] Align web API contracts with server `ApiStatus`, `ApiNotice`, `ServiceStatus`, and diagnostics.
- [x] Translate notices and errors by stable code before category fallback.
- [x] Render non-blocking `ApiResponse` notices as user-facing warnings/info without duplicating page
      errors.
- [x] Include trace/support reference in copied diagnostics for both `ProblemDetail` and notices.

## 4. Validation

- [x] Add contract examples for one blocking and one non-blocking BFF slice failure.
- [x] Verify clients treat `ApiResponse.notices` as non-blocking feedback, not HTTP errors.
- [x] Run focused backend tests.
- [x] Run focused web tests.
