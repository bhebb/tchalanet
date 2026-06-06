# Extract PageModel, Widgets, and Web Libraries

## Why

The public runtime is implemented inside `apps/tch-portal`, which makes PageModel contracts,
rendering, widgets, and reusable web shell pieces app-owned and difficult to reuse or test in
isolation.

## What

- Extract the PageModel runtime contracts, API client, renderer, and widget registry abstraction
  into `libs/page-model`.
- Extract concrete runtime widgets and their registry provider into `libs/widgets`.
- Extract reusable public web shell pieces into `libs/web`.
- Keep the portal app as the composition root and avoid dependency cycles.

## Impact

The portal imports stable `@tch/page-model`, `@tch/widgets`, and `@tch/web` entry points. Existing
runtime behavior and backend contracts remain unchanged.

## Non-goals

- Redesigning widgets or the public shell.
- Changing PageModel backend endpoints or payloads.
- Extracting app-specific authentication and i18n state management.
