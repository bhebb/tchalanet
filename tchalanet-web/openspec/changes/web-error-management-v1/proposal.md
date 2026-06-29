# Change: web-error-management-v1

## Why

The current web error experience is noisy and ambiguous:

- multiple global errors can stack on the same page;
- users may see technical messages that do not explain what they can do next;
- a support/trace action is unclear and can navigate an authenticated user back to the public area;
- page, section, field, and global shell errors are not consistently separated;
- frontend behavior depends too much on raw backend messages instead of deterministic error categories.

We need a deterministic web consumption model that keeps users oriented, gives support enough
correlation data, and prevents duplicated or overly technical error surfaces. This is not a backend
contract redesign: the backend contract remains `2xx = ApiResponse<T>` and `4xx/5xx = ProblemDetail`.

## What changes

- Define a canonical `WebAppError` model derived from `ProblemDetail`, frontend runtime failures,
  validation failures, and network failures.
- Define deterministic frontend error categories and severity policy from backend `code/type`,
  HTTP status, and frontend fallback conditions.
- Consume the shared backend/web BFF error contract from
  `openspec/changes/error-contract-bff-web-v1`.
- Render non-blocking backend slice failures from `ApiResponse.notices/services` as actionable
  user warnings/info, not as thrown page failures.
- Add an explicit single-owner rendering policy so one failure cannot appear simultaneously as shell,
  page, section, snackbar, and form feedback.
- Replace stacked shell banners with a bounded, deduplicated queue and summary behavior.
- Standardize which UI layer owns each error:
  - page-level unrecoverable failures;
  - section/widget/card failures;
  - field/form validation failures;
  - global shell feedback for cross-cutting operational notices only.
- Replace any ambiguous support action with explicit non-navigation actions:
  copy diagnostic reference, retry when safe, dismiss, and optional contact/support link only when it
  is role-aware and keeps the user in the correct shell.
- Separate public support and private support destinations when support navigation exists, so public
  users stay public and authenticated users stay in the private shell.
- Keep public users on public routes and authenticated users inside the private shell when an error
  action is triggered.
- Add i18n-ready user messages and suppress technical details outside privileged diagnostics.

## Impact

- Scope: `tchalanet-web` only.
- Main areas: `libs/api` error mapping, `apps/tch-portal/src/app/core/feedback`, page/section error
  components, i18n error messages, and focused tests.
- Backend API dependency: backend errors should expose stable `ProblemDetail.code` or `type` values,
  plus `traceId/requestId/errorId` when available. Backend non-blocking slice failures should expose
  stable `ApiNotice.code`, severity, source/domain, and trace metadata. This change consumes that
  contract but does not implement backend code generation.

## Non-goals

- No backend implementation in this change.
- No support ticket submission workflow.
- No exposure of stack traces, JWTs, request bodies, or personally sensitive data in copied details.
- No visual redesign of the shell or full-page layouts.
- No broad migration of every feature-specific inline error unless required to stop duplicated global
  errors for the same failure.
- No generic app-wide error store that becomes a second state-management system.
- No backend contract redesign.
- No global retry behavior.
