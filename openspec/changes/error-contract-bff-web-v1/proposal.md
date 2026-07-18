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
  `ApiResponse` notices;
- the web resolver has exact-code lookup logic, but the real locale bundles do not yet prove that
  known backend codes are translated in Haitian Creole, French, and English;
- the mobile client can display raw `ProblemDetail`/notice text, and neither client family has a
  common ownership policy for BFF degradation, form errors, and page recovery.

## What changes

- Clarify the existing shared backend/web/mobile error contract for BFF aggregation.
- Require each BFF slice call to classify failures as blocking or non-blocking.
- Use `ProblemDetail` with stable `code` and trace identifiers for blocking failures.
- Use `ApiResponse.notices` and/or `services` with stable codes, severity, domain/source, and trace
  metadata for non-blocking failures.
- Require every client to translate stable codes from an actually shipped, locale-complete catalog
  before category fallback and generic fallback.
- Define feedback ownership: shell, page, BFF section, form, field, or feature-owned business
  notice. This prevents a response envelope from being silently discarded or shown twice.
- Define a mobile-first error-recovery surface for blocking page failures, plus deterministic focus
  behavior after a successful mutation, a block error, or field validation failure.
- Establish a central error-code catalog so controllers/services do not invent unrelated strings.
- Extend the existing `ApiResponse` documentation with simple examples and helpers so teams do not
  have to remember low-level notice metadata by hand.

## Impact

- Backend: `common` web error/api contracts, code catalogs, BFF orchestration conventions, and
  contract fixtures.
- Web: public, admin, and platform portals; Angular API-envelope retention, error normalizer,
  shell/page/section/form rendering, i18n catalogs, and recovery UI.
- Mobile: API exception/notice mapping, localized feedback, POS/form ownership, and recovery UI.
- No support ticket submission flow is introduced.

## Non-goals

- No immediate migration of every existing endpoint or every legacy screen; the inventory defines
  an explicit migration order and a gate for new BFFs.
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
- `tchalanet-web/libs/api/src/lib/backend-client/tch-backend-client.ts`
- `tchalanet-web/libs/web/errors/src/lib/routing/local-error-routing.ts`
- `tchalanet-mobile/lib/core/network/api_client.dart`
- `tchalanet-mobile/lib/core/network/api_notice_interceptor.dart`
