# Tasks

## 1. Boundaries

- [x] Define the library dependency direction and composition-root responsibility.
- [x] Define an incremental strategy for app-coupled public shell pieces.

## 2. PageModel Library

- [x] Scaffold `libs/page-model` and add the `@tch/page-model` alias.
- [x] Move runtime contracts, API client, renderer, label pipe, and widget abstractions.
- [x] Replace the concrete registry dependency with an injectable registry abstraction.
- [x] Migrate PageModel tests and portal imports.
- [x] Validate PageModel library tests.

## 3. Widgets Library

- [x] Scaffold `libs/widgets` and add the `@tch/widgets` alias.
- [x] Move concrete widgets and expose a registry provider.
- [x] Compose the widget registry in the portal app.
- [x] Migrate widget tests and portal imports.
- [x] Validate widgets library tests.

## 4. Web Library

- [x] Scaffold `libs/web` and add the `@tch/web` alias.
- [x] Move reusable public shell presentation without importing app-owned services.
- [x] Migrate portal imports and preserve shell behavior.
- [x] Validate web library lint/tests.

## 5. Verification

- [x] Update web architecture, conventions, PageModel, placement, naming, state management, Nx
      boundaries, and quickstart documentation.
- [ ] Run portal production/development build. Typecheck, lint, and tests pass; Angular/esbuild
      currently deadlocks during the application build.
- [x] Record remaining work in a handoff if any task remains open.
