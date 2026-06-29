# Design — web-error-management-v1

## Target model

All web errors are normalized before UI rendering:

```text
Backend ProblemDetail / HttpErrorResponse / frontend Error / validation state
  -> normalizeWebAppError()
  -> WebAppError
  -> route to page, section, field, or shell feedback owner
```

This change consumes the backend contract. It does not redefine it:

```text
2xx JSON -> ApiResponse<T>
4xx/5xx -> ProblemDetail
```

`ApiResponse.notices` and service metadata are non-blocking feedback for successful or partial
responses. They must not be converted into blocking HTTP errors.

## WebAppError

`WebAppError` is a frontend presentation model. It does not replace backend `ProblemDetail`.
It belongs at the API/web boundary. Reusable UI components should receive already-normalized inputs
and should not own normalization, routing, or app-wide error state.

```ts
export type WebErrorOrigin = 'backend' | 'frontend' | 'network' | 'validation' | 'auth';
export type WebErrorCategory =
  | 'auth_required'
  | 'access_denied'
  | 'validation'
  | 'not_found'
  | 'conflict'
  | 'rate_limited'
  | 'network_unavailable'
  | 'service_unavailable'
  | 'unexpected';

export type WebErrorSurface = 'page' | 'section' | 'field' | 'shell';

export interface WebAppError {
  readonly id: string;
  readonly origin: WebErrorOrigin;
  readonly category: WebErrorCategory;
  readonly severity: 'info' | 'warn' | 'error';
  readonly surface: WebErrorSurface;
  readonly userTitleKey: string;
  readonly userMessageKey: string;
  readonly code?: string;
  readonly status?: number;
  readonly traceId?: string;
  readonly requestId?: string;
  readonly errorId?: string;
  readonly source?: string;
  readonly retryable: boolean;
  readonly dedupeKey: string;
}
```

## Deterministic category mapping

Mapping order:

1. backend `ProblemDetail.code`, if present;
2. backend `ProblemDetail.type`, if it is a stable application type;
3. HTTP status fallback;
4. frontend/network fallback.

Status fallbacks:

| Status / condition | Category | Severity | Default surface |
|---|---|---|---|
| `0` or offline | `network_unavailable` | `warn` | `shell` or `section` |
| `400`, `422` | `validation` | `warn` | `field` or `section` |
| `401` | `auth_required` | `warn` | `page` or `shell` |
| `403` | `access_denied` | `warn` | `page` or `section` |
| `404` | `not_found` | `warn` | `page` or `section` |
| `409` | `conflict` | `warn` | `section` |
| `429` | `rate_limited` | `warn` | `shell` |
| `500`-`599` | `service_unavailable` | `error` | `shell` or `page` |
| thrown frontend error | `unexpected` | `error` | `shell` |

Feature services may override the default surface only when the error has a clear local owner.

## Backend code contract dependency

The frontend should consume stable backend error identifiers without parsing human-readable text.

Expected backend fields, in priority order:

- `code`: stable machine code such as `TENANT_NOT_FOUND`, `DRAW_CLOSED`, or `ACCESS_DENIED`;
- `type`: stable URI or namespaced type when `code` is absent;
- `traceId`, `requestId`, or `errorId`: support correlation identifiers;
- `title` and `detail`: optional fallback text, not primary UI copy.

Frontend UI copy is keyed by category and known code. Unknown codes fall back to category messages.

The cross-project contract is tracked in:

```text
openspec/changes/error-contract-bff-web-v1
```

## BFF blocking vs non-blocking failures

BFF endpoints can call several backend slices. The web must distinguish two backend channels:

- blocking failure: HTTP error with `ProblemDetail`;
- non-blocking failure: successful `ApiResponse<T>` with `notices` and/or degraded service metadata.

Blocking failures can own a page, section, or shell error depending on route context. Non-blocking
failures should not replace the primary data view. They should render as warning/info/error feedback
close to the affected surface when possible, or in shell feedback when the failure is cross-cutting.

`ApiNotice.code` is treated the same way as `ProblemDetail.code` for translation priority.
`ApiNotice.severity` controls presentation severity. Notice diagnostics are read from typed fields
when available, or from metadata agreed by the shared API contract.

## Surface ownership

Only one surface owns a given failure.

- Page-level errors render when the main route cannot continue.
- Section/widget/card errors render inside the affected bounded surface.
- Field errors render next to the relevant input.
- Shell feedback is reserved for cross-cutting notices, backend service health, network loss,
  background operation failures, and unhandled runtime errors.

If a feature handles an error locally, it must set `suppressShellFeedback: true` or otherwise prevent
the same failure from producing a global shell banner.

Ownership decision order:

1. Field/form owner when the error maps to a specific input or validation group.
2. Section/widget/card owner when a bounded part of the page failed.
3. Page owner when the routed content cannot continue.
4. Shell feedback only for cross-cutting notices, degraded services, background failures, network
   loss, and unhandled runtime errors.

Shell feedback is not the default renderer for all API failures. Feature state remains in feature
stores, runtime state remains in its owning runtime capability, and global app state remains minimal.

## Shell feedback queue

The shell feedback store should behave as a deduplicated queue:

- dedupe by deterministic `dedupeKey`, preferring `code + status + source + route`, then
  `category + surface + source + route`;
- repeated identical errors increment a count instead of adding another visible banner;
- visible shell errors are capped lower than the current noisy state, with a summary entry when more
  errors exist;
- dismissing an item removes that dedupe group;
- clearing feedback is available only inside the current shell context.

No shell feedback action may navigate an authenticated user to a public route.

## User actions

Supported actions:

- `Retry`, only for explicitly retryable operations;
- `Copy reference`, for standard/verbose private diagnostics;
- `Dismiss`;
- `Contact support`, only if configured as a role-aware destination that preserves shell context.

The old ambiguous "send trace to support" action should be removed unless a real support submission
flow exists. Copying diagnostics must use clear text such as "Copier la référence support".

Retry is operation-owned. Generic retry must not be added globally because only the owning feature
or store knows whether retrying is safe, especially for non-idempotent commands.

## Support destinations

Support navigation, if implemented, should stay simple and shell-specific:

- public support route for anonymous/public users;
- private support route or private help surface for authenticated users;
- no shared action that sends an authenticated user back into the public shell;
- no "send trace" wording unless a real submission endpoint exists.

The default action remains "copy support reference". A support page can explain where to paste that
reference, but it should not imply automatic ticket submission unless that workflow exists.

## Diagnostic copy format

Copied details are support-safe and deterministic:

```text
Tchalanet support reference
Time: <ISO timestamp>
Route: <current route>
Category: <category>
Code: <code>
Severity: <severity>
Status: <status>
Request ID: <requestId>
Trace ID: <traceId>
Error ID: <errorId>
Source: <source>
```

Rules:

- no stack trace;
- no JWT/session token;
- no request or response body;
- no user-entered personal data;
- no backend `detail` if it may include sensitive operational content.

## i18n

Error copy must be localized through stable keys:

```text
common.errors.categories.<category>.title
common.errors.categories.<category>.message
common.errors.codes.platform.identity.activation.error.title
common.errors.codes.platform.identity.activation.error.message
```

Fallback exists for resilience, not as the normal state for known product errors. Product flows such
as identity activation, sales, tenant provisioning, dashboards, and cashier BFF responses should get
explicit code translations once their backend codes are stable.

Backend `title/detail` can be used only as a diagnostic fallback in privileged verbose views, not as
the default public-facing message.

Do not add exact-code translations for temporary, invented, or raw backend strings. Exact-code i18n
belongs only to stable backend codes. Public/minimal users never see raw backend `title`, `detail`,
exception messages, provider messages, SQL messages, or stack traces.

## Validation

Validation should include focused unit tests for:

- `ProblemDetail` to `WebAppError` normalization;
- `ApiResponse.notices` to `WebAppError`/shell feedback normalization;
- backend code/type precedence over status fallback;
- deduplication and count increment in shell feedback;
- shell action routing for public vs authenticated users;
- diagnostic copy redaction;
- `suppressShellFeedback` preventing duplicate local/global errors.
- one representative public flow and one authenticated private flow before broad migration.
