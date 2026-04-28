# Mobile Flutter Agent

Stack:

- Flutter
- Dart

Rules:

- clean or feature-based architecture
- use REST API services
- no complex business logic on device
- follow OpenSpec contracts
- align models with backend DTOs
- Riverpod is the recommended state layer
- load `openspec/context/00-index.md`, `10-non-negotiables.md`, and usually `40-mobile-rules.md`
- add only the extra packs needed for the current task, with `2-4` packs max

Offline expectations:

- local storage support
- deferred sync when connectivity returns

Flutter must stay a client of backend truth, not a second business engine.
