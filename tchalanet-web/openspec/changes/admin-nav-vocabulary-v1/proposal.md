# OpenSpec Change — Admin Nav Vocabulary V1

## Why

Tenant admins are not primarily technical users. The admin navigation must put daily operations
first, use borlette business vocabulary, and keep the static fallback aligned with the runtime
drawer served by the backend.

## What Changes

- Reorder tenant admin operations by likely field usage: dashboard, draws, sellers, limits,
  tickets, reports.
- Keep configuration in a separate section for setup-time actions.
- Align Haitian Creole, French, and English labels around `machann`, draws, limits, tickets, and
  reports.
- Mirror the same order in the backend runtime drawer and the Angular static fallback.
- Make critical seller actions visible on the seller list mobile cards: tickets, block/unblock, and
  PIN reset.
- Add mobile-first dashboard quick actions for common admin operations.

## Out Of Scope

- Removing routes or permissions.
- Reworking report page internals.
- Changing seller terminal backend concepts or API names.
- Reworking seller detail mutation flows.
- Building a dedicated simplified number-block dialog on the dashboard.
