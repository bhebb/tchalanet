# Error Management Convention

> Status: NORMATIVE v1
> Scope: `tchalanet-common` HTTP error and degradation contract

This document is the backend source of truth for failures crossing the HTTP boundary. It is
consumed by the web and mobile clients; it does not define their visual copy or widgets.

## Contract at a glance

| Situation | HTTP shape | Client responsibility |
| --- | --- | --- |
| Successful endpoint | `ApiResponse<T>` | Read `data`, then handle `notices` and `services` |
| Required operation fails | `ProblemDetail` with `application/problem+json` | Render one blocking error owned by the relevant surface |
| Optional BFF slice fails | `ApiResponse<T>` with a `DEGRADATION` notice | Keep available slices usable and show one local warning |
| Field validation fails | `ProblemDetail` with `violations[]` | Map each safe violation to its field and keep unconsumed violations in a summary |
| Authentication/session failure | `ProblemDetail` with auth category | Re-authenticate or invalidate the client session centrally |

Errors are never wrapped in `ApiResponse`. Binary downloads, SSE, and empty responses use their
transport-specific adapter and must not promise a JSON error body when the transport cannot carry it.

## Blocking `ProblemDetail`

Every required failure is serialized as RFC 9457-compatible `ProblemDetail` with these stable
properties when available:

- `status`: HTTP status;
- `code`: lowercase, namespaced stable code such as `sales.draw.cutoff_passed`;
- `category`: `auth_required`, `access_denied`, `validation`, `not_found`, `conflict`,
  `business_rule`, `rate_limited`, `service_unavailable`, or `unexpected`;
- `retryPolicy`: `NEVER`, `AFTER_USER_ACTION`, `AFTER_REAUTH`, `AFTER_DELAY`, or
  `RETRY_SAME_INTENT`;
- `retryable`: derived from the policy;
- `params`: descriptor-approved, display-safe values only;
- `violations`: structured validation failures when applicable;
- correlation: `requestId`, `traceId`, `spanId`, and generated `errorId` when available.

`title` and `detail` are compatibility and diagnostic fields. They are neutral server text, not a
translation contract. Clients translate by `code`, then category, then a generic fallback.

## Producing a blocking error

New producers are code-first:

1. Declare an `ErrorDescriptor` in the owning capability.
2. Define its expected status, category, retry policy, audiences, and public parameter types.
3. Throw a typed domain exception or use `ProblemRest.of(descriptor, params)`.
4. Let `GlobalErrorHandler` decorate and serialize the response.

`ErrorDescriptor` validates lowercase dotted codes and rejects undeclared or unsafe parameters.
Message-first `ProblemRest` factories are migration bridges only. Do not put exception messages,
provider responses, SQL text, credentials, PINs, tokens, or internal identifiers in the payload.

## Global error boundary

`GlobalErrorHandler` is the only common HTTP boundary for application failures. It handles:

- typed Tchalanet exceptions (`not found`, validation, forbidden, conflict, business rule);
- Spring validation and malformed request bodies;
- access denials;
- legacy `ProblemRestException` producers through a normalization firewall;
- unexpected exceptions through `internal.unexpected`.

The handler always returns the common media type, attaches request/trace correlation, and logs the
stable code with the request context. It does not expose the root cause to the client.

Authentication failures occurring in the filter chain must use the same descriptor-backed writer and
must not enumerate users, tenants, providers, policies, or token details.

## Non-blocking slices

An endpoint that owns several independent reads must make the required/optional decision at the
slice boundary:

- use `BffSlices.required(...)` when the slice is necessary to make the response meaningful;
- use `BffSlices.optional(...)` when the page can remain useful without it;
- emit one `ApiNotice` with stable `code`, `kind=DEGRADATION`, domain, source, functional `target`,
  safe `params`, severity, and correlation;
- optionally emit a `ServiceStatus` to describe the overall dependency health;
- return an explicit fallback value for the missing slice.

The target is a functional client target such as `tenant_admin_dashboard.commission`, never a Java
class, internal service name, or visual component. The backend does not choose `shell`, `page`,
`section`, or `field` placement.

`ApiNotice.message` and `meta` remain migration bridges. New producers should use the structured
fields (`source`, `target`, `params`, `trace`) and let clients resolve localized copy from the code.

## Validation and safe parameters

Validation output uses a `violations[]` array. Each violation contains a stable constraint code,
field/target path, and only approved public parameters. Nested paths and indexed paths are valid,
for example `lines[2].amount`.

Do not serialize Bean Validation prose, Jackson messages, rejected credentials, raw request bodies,
or arbitrary object values. The server logs enough structured context for support without logging
payloads or secret material.

## Retry policy

The backend declares recovery intent; clients decide whether and when to offer it:

- `NEVER`: no retry action;
- `AFTER_USER_ACTION`: user must correct or confirm something first;
- `AFTER_REAUTH`: restore authentication before retrying;
- `AFTER_DELAY`: user may retry after a service/backoff delay;
- `RETRY_SAME_INTENT`: only safe for an idempotent request with the same intent.

Clients must never automatically repeat a non-idempotent sale, payment, result, or lifecycle
mutation because `retryable=true`.

## Observability and tests

Every error boundary preserves correlation identifiers and uses the stable code in logs. Tests must
cover:

- `ProblemDetail` content type and unwrapped error shape;
- stable code/category/status/retry policy;
- redaction of exception/provider/SQL/credential prose;
- structured validation violations;
- optional slice degradation without converting it to an HTTP error;
- request/trace/error correlation;
- auth failures without enumeration.

Canonical fixtures live under `tchalanet-server/testing/contracts/error-contract/v1/`.

## Cross-project references

- [Web error management](../../../tchalanet-web/docs/conventions/error-management.md)
- [Mobile error management](../../../tchalanet-mobile/docs/conventions/error-management.md)
- [Shared API error contract](../../../openspec/changes/error-contract-bff-web-v1/specs/api-error-contract/spec.md)
- [Server API response convention](./api_response.md)
