# Change: complete-apiresponse-notices

> Archived: superseded by `openspec/changes/error-contract-bff-web-v1`.
> Keep this file only as historical context. The root contract is now the source of truth for
> `ApiResponse` notices, service statuses, `PARTIAL`, `PENDING`, and `ProblemDetail` boundaries.

## Why

The API response wrapper already supports notices and service statuses. The body advice must consistently merge context notices/services, compute response status and keep errors unwrapped.

## What changes

- Complete `ApiResponseBodyAdvice`.
- Add/complete `ApiResponseContext` and cleanup filter.
- Add `ApiStatusResolver`.
- Define strict difference between ApiNotice and persistent notification.

## Impact

- HTTP 2xx JSON responses are consistently wrapped/enriched.
- ProblemDetail errors remain unchanged.
