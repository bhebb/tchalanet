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
- the legacy server boundary uses the same `ProblemDetail.detail` field for stable-looking codes,
  human prose, and hybrid strings containing internal data. A client cannot safely infer a product
  contract from that field.
- several compatibility paths can misclassify a technical failure as a business `422`, or express a
  missing primary resource as a successful envelope with an error notice.

## Delivery order

This is a contract-first migration. It SHALL not be delivered as parallel UI normalization work.

1. Establish a safe server boundary: no parser/provider/exception prose or Java class names in a
   client response, with regression tests before broad migration.
2. Make the contract executable: owner-defined code-first descriptors, central validation of their
   metadata, versioned fixtures, and a static guard against new message-first producers.
3. Migrate and prove the sale decision vertical first: sales, limits, tenant-game availability,
   access, identity, and the POS HTTP adapters.
4. Resolve BFF composition semantics and the legacy response-envelope contradictions.
5. Migrate Angular and Flutter consumers only when their backend path has a tested stable code and
   locale-complete copy.

Legacy producers SHALL be classified before conversion as one of: an existing stable code, a human
business sentence, a hybrid code/prose or dynamic-value string, or a framework/technical producer.
The conversion outcome differs by class: retain and register an existing stable code; introduce an
owner code for a business sentence; split a hybrid into a code plus approved safe parameters or
server-only diagnostics; and map technical producers to approved generic or integration codes.

## What changes

- Clarify the existing shared backend/web/mobile error contract for BFF aggregation.
- Make the backend contract code-first and enforce it before further portal or mobile screen
  migration. Concrete codes remain owned by their domains; a common validator collects and checks
  descriptors rather than creating one giant business enum.
- Define typed public parameter specifications and an explicit retry policy. A descriptor allowlist
  is not merely a list of parameter names: it also constrains value type and display safety, so an
  internal identifier cannot become visible merely because its key was approved.
- Add a redaction regression gate before migrating existing producers. `detail`, `title`, notices,
  service metadata, and support references are never a vehicle for exception, provider, parser,
  credential, PIN, token, or Java-class prose.
- Require each BFF slice call to classify failures as blocking or non-blocking.
- Use `ProblemDetail` with stable `code` and trace identifiers for blocking failures.
- Use `ApiResponse.notices` and/or `services` with stable codes, severity, domain/source, and trace
  metadata for non-blocking failures.
- Represent independently recoverable BFF sections with a typed availability state. A nullable
  business value alone cannot distinguish an intentionally absent value from an unavailable slice.
- Require every client to translate stable codes from an actually shipped, locale-complete catalog
  before category fallback and generic fallback. The catalog gate applies to Haitian Creole, French,
  and English for each client audience that can receive the code.
- Define feedback ownership: shell, page, BFF section, form, field, or feature-owned business
  notice. This prevents a response envelope from being silently discarded or shown twice.
- Define a mobile-first error-recovery surface for blocking page failures, plus deterministic focus
  behavior after a successful mutation, a block error, or field validation failure.
- Establish a central error-code catalog so controllers/services do not invent unrelated strings.
- Resolve the ownership and failure semantics of the existing BFF slice helper. A required technical
  failure must never become a legacy business `422`; a helper with feature-composition policy cannot
  remain an accidental `common.web.advice` contract.
- Remove semantic contradictions in compatibility APIs, including a `PENDING` sentinel embedded in
  transport advice and any `notFound` factory that represents an unusable primary resource as
  `SUCCESS` plus an error notice.
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
- No additional screen-level error migration based on parsing legacy `detail` text. Temporary
  compatibility bridges are measured, allowlisted, and removed once their producers are code-first.
- No redesign of the existing backend response contract:
  `2xx = ApiResponse<T>` and `4xx/5xx = ProblemDetail`.
- No exposure of backend stack traces or raw exception messages to users.
- No backend domain decision inside controllers; BFF orchestration remains in `features`.
- No production client-side diagnostic store, support reference, or telemetry event carries raw
  server prose. It carries only redacted structured context and correlation identifiers.

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
