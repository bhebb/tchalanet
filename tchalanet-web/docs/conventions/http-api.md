# HTTP API Convention

> Status: DRAFT v0.1  
> Scope: API clients, response contracts, interceptors, errors, headers

## Rule

HTTP access is a platform boundary. Components do not call `HttpClient` directly. API clients own endpoint calls, response unwrapping, and request-specific headers.

## Placement

Library placement:

```text
libs/api/src/lib/contracts        transverse backend contracts
libs/api/src/lib/http             interceptors, request context, query helpers, error mapping
libs/api/src/lib/backend-client   TchBackendClient wrapper around Angular HttpClient
```

Feature placement:

```text
apps/<portal>/src/app/features/**/data-access/*-api.service.ts
```

## API Client Decorator

Use Angular `@Service()` for new feature API clients when they are root singleton services and use
`inject()` for dependencies:

```ts
import { Service, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';

@Service()
export class SellerTerminalApi {
  private readonly backend = inject(TchBackendClient);
}
```

Keep `@Injectable()` only when the service needs constructor-based dependency injection, advanced
provider configuration (`useClass`, `useValue`, `useExisting`, `useFactory`), or non-root scopes.
This follows the Angular v22 DI guidance:
<https://angular.dev/guide/di/creating-and-using-services#when-to-use-service-vs-injectable>.

Reusable web runtime placement:

```text
libs/web/errors   frontend-safe error view models, copy, page/section/field helpers
libs/core/auth    provider-neutral auth/session contracts, guards, provider adapter wiring
libs/core/i18n    runtime i18n loader/language contracts
libs/web/shell    reusable shell feedback/layout primitives
```

App `core` wires providers and shell-specific stores. It should not be the permanent owner of code
that another web app must reuse.

## Response Contracts

Successful backend responses use:

```text
ApiResponse<T>
```

Paged responses use:

```text
TchPage<T>
```

`TchPage<T>` mirrors the backend record
`tchalanet-common/common.web.paging.TchPage`:

```ts
{
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}
```

Do not type Tchalanet backend lists as Spring `Page<T>` (`content`, `number`) in web code. If an
endpoint still returns a local non-`TchPage` shape, keep a local feature type and do not alias it to
`TchPage<T>`.

Errors map to:

```text
ProblemDetail
```

The web consumes the backend contract; it does not redefine it:

- `2xx` means `ApiResponse<T>` or an explicitly documented raw payload/download;
- `4xx/5xx` means `ProblemDetail`;
- non-blocking BFF degradation stays in `ApiResponse.notices` and, when useful, service metadata;
- blocking failures stay as `ProblemDetail`.

Data-only APIs (`get`, `getPage`, `post`, `put`, `patch`, `delete`, multipart variants) unwrap
successful responses for legacy/simple endpoints. They are not suitable for a BFF, dashboard,
mutation, or other flow that can return notices, service state, `PARTIAL`, or trace metadata.

Those flows use the retained family instead: `getApiResponse`, `getPageApiResponse`,
`postApiResponse`, `putApiResponse`, `patchApiResponse`, `deleteApiResponse`,
`deleteWithBodyApiResponse`, multipart `*ApiResponse`, or `getApiResponseResource`. The feature
then owns routing its envelope feedback to exactly one surface.

Declare local ownership with `TchRequestOptions.feedback`:

```ts
feedback: { owner: 'section', mode: 'local', target: 'dashboard.commissions' }
```

`owner` is one of `shell`, `page`, `section`, `form`, `field`, or `feature`; `mode` is `inherit`,
`local`, or `silent`. `suppressShellFeedback` is deprecated and maps to `{ mode: 'local' }` during
migration. The context currently records intent only; do not assume it displays or suppresses UI
until the shell feedback router is implemented.

Backend notices intended for web display should include stable metadata:

```text
code
severity
domain
source
service
operation
meta.surface    shell | page | section | field
meta.placement  top | inline | summary
meta.target     stable UI target when surface is section/page
meta.field      stable field/control name when surface is field
traceId/requestId/errorId/spanId when available
```

Frontend code translates stable `code` values first, category fallback second, and generic fallback
last. It must not display raw exception messages, provider messages, SQL text, stack traces, backend
`title`, or backend `detail` directly to public/minimal users.

## Error Handling

See [`error-management.md`](./error-management.md) for the shell/page/section/field ownership
model and the backend metadata expected for each error type.

Central error mapping should preserve diagnostics for support correlation:

```text
status
code/type
instance/path
errorId/requestId/traceId/spanId
timestamp
```

Components consume mapped errors or view-state errors. They do not inspect raw `HttpErrorResponse` unless they are an API boundary.

Presentation components render one normalized UI model from `@tch/web/errors` (`TchErrorViewModel`,
`tch-error-panel`, `tch-section-error`, or `tch-field-error`) and must not parse `ProblemDetail` or
`ApiResponse` directly. API clients, stores, or the page controller own that mapping.

## Interceptors

Global interceptors may add:

- auth token;
- correlation/request id;
- app/API metadata headers;
- centralized error mapping.

### Ordre des intercepteurs

Pour les portails privés, déclarer les intercepteurs ainsi :

```text
correlation → problem-detail → shell-feedback → auth-bearer → support-access
```

Les erreurs traversent la chaîne en sens inverse. `auth-bearer` doit donc rester plus près du
transport que `problem-detail` : il voit un `HttpErrorResponse` 401 brut, renouvelle le JWT, puis
rejoue la requête avant toute normalisation ou présentation de feedback. Le portail public omet
simplement `support-access`.

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
