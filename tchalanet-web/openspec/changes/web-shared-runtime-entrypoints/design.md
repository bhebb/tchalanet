# Design

## Boundary

Use one physical Nx project, `libs/web`, with capability entrypoints:

```text
libs/web/src/lib/core
libs/web/src/lib/auth
libs/web/src/lib/i18n
libs/web/src/lib/errors
libs/web/src/lib/shell
```

This keeps the workspace small while making imports explicit:

```ts
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
```

If a capability later needs independent build/test/release ownership, it can be promoted to its own
Nx project without changing the conceptual boundary.

## Responsibility Split

`@tch/api` owns:

- backend contracts;
- `TchBackendClient`;
- API response unwrapping;
- ProblemDetail / ApiResponse / ApiNotice mapping;
- HTTP primitives and low-level interceptors that do not depend on an app shell.

`@tch/web/errors` owns:

- frontend-safe error view models;
- stable-code translation lookup;
- page/section/field selection helpers;
- server field-error application helpers;
- error presentation components when they are moved out of `@tch/ui/components`.

`@tch/web/shell` owns reusable shell feedback primitives once they no longer depend on a single app.

`@tch/web/auth` owns provider-neutral session/guard contracts once they are extracted from app core.

`@tch/web/i18n` owns runtime loader/language-state abstractions once they are extracted from app core.

App `core` owns:

- provider registration;
- shell-specific store wiring;
- app route guards while they still depend on app routes;
- app composition choices.

## Error Names

New reusable symbols use the `Tch` prefix:

```text
TchErrorFeedbackCopy
TchErrorViewModel
TchTranslateLookup
```

Compatibility aliases may remain during migration:

```text
ErrorFeedbackCopy
ErrorViewModel
TranslateLookup
```

## Component Placement

Error-domain components belong under `@tch/web/errors`, not under generic UI, once their rendering
depends on the normalized error contract. Generic buttons, cards, badges, loading, and layout
components stay in `@tch/ui/components`.
