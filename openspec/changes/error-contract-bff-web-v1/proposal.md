# Change: error-contract-bff-web-v1

## Why

BFF endpoints can aggregate several backend slices. Some slice failures are blocking and must fail the
whole request. Other slice failures are non-blocking and should return successful primary data with a
clear warning, trace reference, and deterministic user message.

Today this boundary is not explicit enough:

- blocking failures and partial failures can be rendered similarly by the web app;
- backend error text can leak into user-facing UI;
- fallback translations are useful, but too many errors cannot be fallback-only;
- support correlation must be available for both blocking `ProblemDetail` errors and non-blocking
  `ApiResponse` notices.

## What changes

- Clarify the existing shared backend/web error contract for BFF aggregation.
- Require each BFF slice call to classify failures as blocking or non-blocking.
- Use `ProblemDetail` with stable `code` and trace identifiers for blocking failures.
- Use `ApiResponse.notices` and/or `services` with stable codes, severity, domain/source, and trace
  metadata for non-blocking failures.
- Require frontend translation by stable code first, category fallback second, and generic fallback
  only as a last resort.
- Establish a central error-code catalog so controllers/services do not invent unrelated strings.
- Extend the existing `ApiResponse` documentation with simple examples and helpers so teams do not
  have to remember low-level notice metadata by hand.

## Impact

- Backend: `common` web error/api contracts and `features` BFF orchestration conventions.
- Web: Angular API contracts, error normalizer, shell/page/section rendering, i18n keys.
- No support ticket submission flow is introduced.

## Non-goals

- No immediate migration of every existing endpoint.
- No redesign of the existing backend response contract:
  `2xx = ApiResponse<T>` and `4xx/5xx = ProblemDetail`.
- No exposure of backend stack traces or raw exception messages to users.
- No backend domain decision inside controllers; BFF orchestration remains in `features`.

## Context packs

- `openspec/context/10-non-negotiables.md`
- `openspec/context/20-backend-rules.md`
- `openspec/context/30-frontend-rules.md`

## Near-code references

- `tchalanet-server/tchalanet-common/src/main/java/com/tchalanet/server/common/web/error/GlobalErrorHandler.java`
- `tchalanet-server/tchalanet-common/src/main/java/com/tchalanet/server/common/web/advice/ApiResponseBodyAdvice.java`
- `tchalanet-server/tchalanet-common/src/main/java/com/tchalanet/server/common/web/api/ApiNotice.java`
- `tchalanet-server/docs/conventions/api/api_response.md`
- `tchalanet-server/openspec/context/76-api-response-rules.md`
- `tchalanet-web/libs/api/src/lib/contracts/api.types.ts`
- `tchalanet-web/libs/api/src/lib/http/api-feedback.interceptor.ts`
