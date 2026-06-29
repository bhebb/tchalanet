# HTTP API Convention

> Status: DRAFT v0.1  
> Scope: API clients, response contracts, interceptors, errors, headers

## Rule

HTTP access is a platform boundary. Components do not call `HttpClient` directly. API clients own endpoint calls, response unwrapping, and request-specific headers.

## Placement

Current app placement:

```text
apps/tch-portal/src/app/core/http/
apps/tch-portal/src/app/core/<capability>/*-api.service.ts
apps/tch-portal/src/app/shared/types/
```

Target library placement, once stable:

```text
libs/api/contracts
libs/api/http
libs/api/clients
```

## Response Contracts

Successful backend responses use:

```text
ApiResponse<T>
```

Paged responses use:

```text
TchPage<T>
```

Errors map to:

```text
ProblemDetail
```

API clients unwrap successful responses before exposing data to stores/components.

## Error Handling

See [`error-management.md`](./error-management.md) for the shell/page/section/field ownership
model and the backend metadata expected for each error type.

Central error mapping should preserve:

```text
status
code/type
title/message
detail
instance/path
errorId/requestId
timestamp
```

Components consume mapped errors or view-state errors. They do not inspect raw `HttpErrorResponse` unless they are an API boundary.

## Interceptors

Global interceptors may add:

- auth token;
- correlation/request id;
- app/API metadata headers;
- centralized error mapping.

Operational headers are scoped. Cashier sale context and terminal proof headers must be attached only by cashier/POS flows that own that operation.

## URL Rules

API URLs should be relative when targeting the configured backend proxy:

```text
/api/v1/...
```

Do not hardcode environment hosts in feature code.

Absolute API URLs are allowed only through approved runtime config/proxy rules.

## Header Rules

Use stable constants or dedicated helpers for shared headers.

Do not send tenant override or operational proof headers from generic interceptors.

For seller/cashier flows:

- context headers can travel with relevant cashier requests;
- operation validation happens on sensitive operations;
- sale proof/idempotency headers belong to the sale command, not every request.

## Anti-Patterns

Do not:

- inject `HttpClient` in presentation components;
- duplicate endpoint strings across pages;
- swallow errors silently in API clients;
- merge settings, i18n, theme, and PageModel into one opaque bootstrap response;
- use `any` at API boundaries when a contract exists;
- let interceptors become business logic.
