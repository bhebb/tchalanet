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

## Response Structure

All 2xx responses now return `ApiResponse<T>`:

```json
{
  "status": "SUCCESS_WITH_WARNINGS",
  "data": { /* original response */ },
  "notices": [
    {
      "code": "LIMIT_WARN",
      "message": "Stake exceeds recommended limit",
      "domain": "limitpolicy",
      "severity": "WARN",
      "meta": {
        "ruleKey": "MAX_STAKE_PER_LINE",
        "currentValue": 50.00,
        "limitValue": 25.00
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

## Benefits

- **Consistent**: All endpoints follow the same response pattern
- **Automatic**: No manual wrapping required for existing endpoints
- **Flexible**: Easy to add notices/service status anywhere in the request lifecycle
- **Backward Compatible**: Existing error handling unchanged
- **Future-Proof**: Clear migration path to explicit `ApiResponse` usage

This architecture provides both immediate automatic wrapping and a clear path for explicit adoption in new development.
