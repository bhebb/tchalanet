# Error Management Convention

> Status: NORMATIVE v1
> Scope: Flutter core networking, session recovery, notices, localization, and feature UI

Mobile consumes the backend contract. It does not display server diagnostic prose directly and it
does not reimplement backend business rules.

## Runtime pipeline

```text
Dio
  -> interceptors (request id, auth, operation context, notices)
  -> mapDioException
  -> ApiException / ApiNotice
  -> repository or use case
  -> Riverpod ViewModel state
  -> localized widget feedback
```

The core implementation lives in:

- `lib/core/network/api_client.dart`: Dio setup, headers, response parsing, and transport mapping;
- `lib/core/network/api_exception.dart`: typed blocking failure and locale-key resolution;
- `lib/core/network/api_notice.dart`: non-blocking response notice parsing;
- `lib/core/network/api_notice_interceptor.dart`: current-response notice delivery;
- `lib/core/network/request_id_interceptor.dart`: request correlation;
- `lib/core/network/session_invalidation_controller.dart`: centralized auth invalidation;
- `lib/core/notifications/support_reference.dart`: safe copyable support reference;
- `lib/core/observability/diagnostic_info.dart`: safe diagnostic context.

Widgets and feature code must not call Dio or parse raw JSON maps.

## Blocking failures

For a `4xx/5xx` response, `mapDioException` creates an `ApiException` with the transport-safe
fields available from the body and headers:

- HTTP `statusCode`;
- stable `code` and `category`;
- `retryPolicy` and derived `retryable`;
- approved `params`;
- `requestId`, `traceId`, `spanId`, and `errorId`.

The response `detail`, `title`, Dio message, provider text, and exception text are diagnostic only.
The visible message is resolved with the active locale bundle using this order:

1. `common.errors.codes.<code>.message`;
2. `common.errors.categories.<category>.message`;
3. status/client fallback such as timeout or network unavailable;
4. `common.error.unknown`.

Use `userErrorTranslationKeys` and `localizedUserError`; do not render `ApiException.message`
directly. A feature maps the resulting failure into its own ViewModel state and owns the error UI.

## Transport failures

Client-originated failures use namespaced client codes:

| Transport condition | Code | Default recovery |
| --- | --- | --- |
| connect/send/receive timeout | `client.network.timeout` | user retry or backoff |
| connection failure | `client.network.unavailable` | retry after connectivity returns |
| cancelled request | `client.request.cancelled` | no visible error unless the feature needs it |
| unknown Dio failure | `client.unexpected` | generic fallback and diagnostics |

Connectivity and retry UI belong to the feature or shared app notification host, never to a raw
Dio interceptor. Automatic retry is allowed only for an explicitly idempotent read or an operation
whose server contract guarantees the same intent; never automatically repeat a sale or other
non-idempotent mutation.

## Notices and partial responses

`ApiResponse.notices` describe the current response only. `ApiNoticeInterceptor` parses them and
adds them to the temporary app notification host. A notice retains:

- stable `code`, `domain`, `kind`, and severity;
- functional `target` and structured `params`;
- source/service/operation context when supplied;
- request, trace, span, and error correlation.

Notices are not persisted notifications. Persistent tenant/platform notifications use the dedicated
notification endpoints and lifecycle. A partial response must keep available data usable; a notice
must not be promoted to a blocking page error unless the feature cannot render meaningful content.

## Authentication and session recovery

The auth interceptor owns token attachment and refresh. When the backend returns an unrecoverable
authentication failure, it calls the centralized `SessionInvalidationController`. Features observe
the session state and route through the normal login/session restoration flow.

Do not expose whether a tenant, seller, account, or provider exists. Do not show token, Firebase,
identity-provider, or authorization diagnostics in a user message.

## Forms and field validation

Repositories preserve structured validation information. ViewModels map violations to typed field
state when a target can be resolved; unresolved violations belong in a form-level summary. Client
validators run before submission, while server violations remain visible until the user edits the
affected field or dismisses the form.

Field copy is translated by stable violation code and safe params. Server prose is never the
translation key or the user-facing fallback.

## Support and diagnostics

Visible errors stay short and actionable. A support action may copy a `SupportReference` containing
`traceId`, `errorId`, stable `code`, and HTTP status. Diagnostic copies must not include request
payloads, credentials, PINs, bearer tokens, personal data, or provider responses.

The diagnostic repository may retain request/trace context for support and observability. It must
respect the mobile retention policy and must not become a second user-facing error store.

## Testing requirements

Core tests must cover:

- ProblemDetail body and header parsing;
- byte-buffer JSON error decoding;
- code/category/retry/params preservation;
- request/trace/span/error correlation;
- localized key priority and generic fallback;
- notice parsing and request correlation;
- session invalidation on auth failure;
- redaction of server and transport diagnostic prose.

## Cross-project references

- [Backend error management](../../../tchalanet-server/docs/conventions/error-management.md)
- [Web error management](../../../tchalanet-web/docs/conventions/error-management.md)
- [Mobile API contract](../API_CONTRACT.md)
- [Shared API error contract](../../../openspec/changes/error-contract-bff-web-v1/specs/api-error-contract/spec.md)
