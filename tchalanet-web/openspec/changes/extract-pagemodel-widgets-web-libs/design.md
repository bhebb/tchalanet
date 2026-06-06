# Design

## Dependency Direction

- `page-model` owns runtime contracts, API access, rendering, shared widget helpers, and an
  injectable widget registry abstraction.
- `widgets` depends on `page-model` and provides the concrete widget registry.
- `web` depends on `page-model` and UI libraries for reusable shell presentation.
- `tch-portal` composes providers and keeps app-specific authentication and i18n orchestration.

`page-model` must never import `widgets`; this prevents a renderer/widget dependency cycle.

## Incremental Web Extraction

Public shell pieces that do not depend on app services move first. App-coupled header and shell
composition move only after their authentication and language-switcher integration is represented
by library-safe inputs, outputs, or injection tokens.
