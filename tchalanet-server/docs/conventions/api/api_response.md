# Standardized API Response Pattern

## Package Structure

### common.web.api (Stable HTTP Contract)

Contains the core API response types consumed by the frontend:

- `ApiResponse<T>`: Generic response wrapper
- `ApiStatus`: SUCCESS, SUCCESS_WITH_WARNINGS, PENDING, PARTIAL
- `ApiNotice`: Structured notices with code, message, domain, severity, meta
- `NoticeSeverity`: INFO, WARN
- `ServiceStatus`: Service health information
- `ServiceHealth`: UP, DOWN, DEGRADED

**Rules**: Pure POJOs/records, no Spring dependencies, stable contract.

### common.web.advice (Transitional Web Infrastructure)

Contains Spring Web components for automatic response wrapping:

- `ApiResponseBodyAdvice`: ResponseBodyAdvice for auto-wrapping
- `ApiResponseContext`: ThreadLocal context for notices/services
- `ApiResponseContextFilter`: Cleanup filter for ThreadLocal

**Rules**: Spring Web dependencies, temporary/transitional, no business logic.

## Global Wrapping

- **Automatic**: All 2xx responses are automatically wrapped using `ApiResponseBodyAdvice`
- **Context-Based**: Notices and service statuses are collected in `ApiResponseContext` (ThreadLocal)
- **Transparent**: Controllers return normal response objects; wrapping happens automatically

## Context Collection

Use `ApiResponseContext` to add notices and service statuses:

```java
// In any handler/service
ApiResponseContext.get().addNotice("LIMIT_WARN", "Stake exceeds limit", "limitpolicy", WARN);
ApiResponseContext.get().addServiceStatus("meilisearch", DOWN, "Search unavailable");
```

For BFF and partial-result flows, prefer the helper so standard metadata is attached consistently:

```java
ApiResponseNotices.warn(
    PlatformIdentityNoticeCodes.ACTIVATION_ERROR,
    "Identity activation could not be completed.",
    "platform.identity",
    NoticeSource.of("identityActivation")
        .service("keycloak")
        .operation("completeFirstLogin"),
    ex
);
```

The helper writes to `ApiResponseContext`; `ApiResponseBodyAdvice` assembles the final
`ApiResponse<T>` envelope.

For endpoints that aggregate multiple slices, prefer `BffSlices` so the required/optional decision
is explicit at the call site:

```java
var profile = BffSlices.required(() -> profileService.currentProfile(ctx));

var activation = BffSlices.optional(
    BffSlicePolicy.warn(
        PlatformIdentityNoticeCodes.ACTIVATION_ERROR,
        "Identity activation could not be completed.",
        "platform.identity",
        NoticeSource.of("identityActivation")
            .service("keycloak")
            .operation("activateIfNeeded"),
        IdentityActivationView.unavailable()
    ).serviceStatus(ServiceHealth.DEGRADED, "Identity provider unavailable"),
    () -> identityActivation.activateIfNeeded(ctx)
);
```

`required` preserves the normal exception flow. Stable-code exceptions are rendered by
`GlobalErrorHandler` as `ProblemDetail`. `optional` catches the slice failure, adds a standardized
notice, optionally adds a degraded service status, and returns the declared fallback.

Reserved `ApiNotice.meta` keys:

| Key | Meaning |
|---|---|
| `source` | Fine-grained emitter/slice inside the domain |
| `surface` | UI owner: `shell`, `page`, `section`, or `field` |
| `placement` | UI placement: `top`, `inline`, or `summary` |
| `target` | Stable UI target such as `dashboard.commissions` or `profile.email` |
| `field` | Form control name for field-level validation/feedback |
| `service` | Downstream service or provider when relevant |
| `operation` | Stable operation name useful for support |
| `requestId` | Request correlation id |
| `traceId` | Distributed trace id |
| `spanId` | Distributed span id |
| `errorId` | Server-generated id for a caught optional failure |

Domain-specific metadata is allowed, but frontend behavior must rely on stable fields above and the
notice `code`.

Frontend rendering ownership:

- `surface=shell`: cross-cutting feedback only;
- `surface=page`: page-level top error;
- `surface=section`: block/widget/card-level top error, usually with `target`;
- `surface=field`: form-control inline error, usually with `field`.

Every error should have one UI owner. Do not emit a section/field notice expecting it to also become
a shell banner.

## Response Structure

All 2xx responses now return `ApiResponse<T>`:

```json
{
  "status": "SUCCESS_WITH_WARNINGS",
  "data": {
    /* original response */
  },
  "notices": [
    {
      "code": "LIMIT_WARN",
      "message": "Stake exceeds recommended limit",
      "domain": "limitpolicy",
      "severity": "WARN",
      "meta": {
        "ruleKey": "MAX_STAKE_PER_LINE",
        "currentValue": 50.0,
        "limitValue": 25.0
      }
    }
  ],
  "services": [
    {
      "service": "meilisearch",
      "status": "DOWN",
      "message": "Search service unavailable"
    }
  ]
}
```

## Status Determination

Status is automatically determined by `ResponseBodyAdvice`:

- `PENDING`: Body is null and contains approval notices
- `PARTIAL`: Services contain DOWN/DEGRADED status
- `SUCCESS_WITH_WARNINGS`: Notices contain WARN severity
- `SUCCESS`: Default for clean responses

## Controller Changes

Controllers no longer manually wrap responses:

```java
// Before
return ResponseEntity.created(uri).body(ApiResponse.success(ticket));

// After
return ResponseEntity.created(uri).body(ticket); // Automatically wrapped
```

## Error Handling

4xx/5xx errors continue to return `ProblemDetail` format unchanged:

```json
{
  "type": "about:blank",
  "title": "Limit blocked",
  "status": 409,
  "detail": "Limit breach blocked",
  "operationType": "SALE"
}
```

## Migration Policy

### New Endpoints

All new endpoints must return: `ResponseEntity<ApiResponse<T>>`

### Existing Endpoints

- Remain unchanged in code
- Automatically wrapped by `ApiResponseBodyAdvice`
- No manual intervention required

### Errors

- Continue to return `ProblemDetail`
- Never wrapped in `ApiResponse`

## BFF Refactoring Strategy

BFF endpoints (public/private) are ideal candidates for 100% `ApiResponse` adoption because:

- They aggregate multiple services → need `PARTIAL` status
- They are consumed directly by frontend → stable contract
- They can collect notices from multiple backend calls

Example BFF endpoint:

```java
@PostMapping("/search")
public ResponseEntity<ApiResponse<SearchResult>> search(@RequestBody SearchRequest request) {
    // Collect notices from multiple services
    ApiResponseContext.get().addServiceStatus("meilisearch", DOWN, "Search unavailable");

    var results = searchService.search(request);
    return ResponseEntity.ok(ApiResponse.partial(results, List.of(serviceStatus), notices));
}
```

### Blocking vs non-blocking BFF examples

Required slice failure: throw a stable-code exception. `GlobalErrorHandler` emits `ProblemDetail`.

```java
var sale = BffSlices.required(() -> commandBus.execute(command));
return ApiResponse.success(mapper.toSellResponse(sale));
```

Optional slice failure: preserve primary data and add an immediate notice.

```java
var activationView = BffSlices.optional(
    BffSlicePolicy.warn(
        PlatformIdentityNoticeCodes.ACTIVATION_ERROR,
        "Identity activation could not be completed.",
        "platform.identity",
        NoticeSource.of("identityActivation")
            .service("keycloak")
            .operation("activateIfNeeded"),
        IdentityActivationView.unavailable()
    ),
    () -> identityActivation.activateIfNeeded(ctx)
);

return dashboardView.withActivation(activationView);
```

Section-level dashboard warning: preserve the dashboard response and target the failing block.

```java
ApiResponseNotices.add(
    "dashboard.commissions.unavailable",
    "Commissions are temporarily unavailable.",
    "dashboard",
    NoticeSeverity.WARN,
    NoticeSource.of("commissions").service("commission-service"),
    ex,
    Map.of(
        "surface", "section",
        "placement", "top",
        "target", "dashboard.commissions"
    )
);
```

Optional provider degradation: add both a notice for user copy and a service status for the partial
response status.

```java
var providerResults = BffSlices.optional(
    BffSlicePolicy.warn(
        "features.dashboard.provider_results.degraded",
        "Some provider results are temporarily unavailable.",
        "features.dashboard",
        NoticeSource.of("providerResults")
            .service("uslottery")
            .operation("latestResults"),
        ProviderResults.unavailable()
    ).serviceStatus(ServiceHealth.DEGRADED, "Latest results unavailable"),
    () -> providerResultsClient.latestResults()
);
```

## Benefits

- **Consistent**: All endpoints follow the same response pattern
- **Automatic**: No manual wrapping required for existing endpoints
- **Flexible**: Easy to add notices/service status anywhere in the request lifecycle
- **Backward Compatible**: Existing error handling unchanged
- **Future-Proof**: Clear migration path to explicit `ApiResponse` usage

This architecture provides both immediate automatic wrapping and a clear path for explicit adoption in new development.
