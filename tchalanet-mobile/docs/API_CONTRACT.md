# Tchalanet Mobile API Contract

> **Status**: NORMATIVE  
> **Scope**: communication with `tchalanet-server`

---

## 1. Backend response contract

Tchalanet backend uses:

- `ApiResponse<T>` for 2xx responses
- `ProblemDetail` for 4xx/5xx errors

Mobile must parse these centrally.

---

## 2. Core API package

```text
lib/core/api/
  api_client.dart
  api_response.dart
  api_notice.dart
  service_status.dart
  problem_detail.dart
  api_result.dart
  auth_interceptor.dart
```

---

## 3. Result shape

Recommended mobile abstraction:

```dart
sealed class ApiResult<T> {
  const ApiResult();
}

final class ApiSuccess<T> extends ApiResult<T> {
  const ApiSuccess({
    required this.data,
    this.notices = const [],
  });

  final T data;
  final List<ApiNotice> notices;
}

final class ApiFailure<T> extends ApiResult<T> {
  const ApiFailure(this.problem);

  final ProblemDetail problem;
}
```

Feature code should depend on typed results, not raw JSON maps.

---

## 4. Data source rule

Remote data sources call `ApiClient`.

Repositories call data sources.

ViewModels call repositories or use cases.

Widgets never call `ApiClient`, Dio, or raw HTTP.

---

## 5. Error handling

- `ProblemDetail` must be mapped to user-friendly UI errors at ViewModel/application boundary.
- Technical details must not be exposed directly to users.
- Mobile must retain `ProblemDetail.traceId` or the `X-Request-Id` response header in
  the typed API error.
- API error UI may expose a copy-support-reference action. The copied reference
  contains trace ID, error ID, stable error code, and HTTP status when available;
  those details remain hidden from the visible user message.
- Authentication/session errors must trigger centralized session handling.
- Validation errors should be shown near the affected input when possible.

### Notices are not internal notifications

- `ApiResponse.notices` belong only to the current HTTP response.
- They may create a temporary POS notice through `AppNotificationHost`.
- Their parser must associate the response `X-Request-Id` with each notice so the
  seller can copy a hidden support reference.
- Tenant/platform internal notifications are persisted and have read/unread/archive
  lifecycle.
- Global sector news is not displayed on the POS.

### POS persistent notification pull

- Runtime summary source: `GET /tenant/runtime/state`.
- Notification center endpoint: `GET /tenant/me/notifications`.
- Mutations: `POST /tenant/me/notifications/{id}/read` and `.../{id}/archive`.
- Mobile receives the initial summary from `GET /tenant/runtime/bootstrap`.
- Mobile refreshes the summary through runtime-state polling; it must not start a
  duplicate periodic notification-summary poll.
- Full notification pages are fetched only when the notification center is opened.

---

## 6. Pagination

Mobile must represent backend paginated responses as a stable client type:

```dart
final class TchPage<T> {
  const TchPage({
    required this.items,
    required this.page,
    required this.size,
    required this.total,
    required this.totalPages,
  });

  final List<T> items;
  final int page;
  final int size;
  final int total;
  final int totalPages;
}
```

Do not expose backend raw maps to UI.
