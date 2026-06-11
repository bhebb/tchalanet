# AGENTS.md — Tchalanet Mobile

Mobile agent router for `tchalanet-mobile/` (Flutter).

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`

Canonical docs (source of truth — do not duplicate here):

- `openspec/` — active specs and mobile context.
- `docs/ARCHITECTURE.md` — SoC/MVVM layers and dependency boundaries.
- `docs/conventions/README.md` — convention router.
- `docs/conventions/state_management.md` — Riverpod state policy.
- `docs/conventions/material3.md` — Material 3 usage.
- `docs/conventions/theme.md` — single Tchalanet mobile theme.
- `docs/conventions/style.md` — typed tokens and feature styling.
- `docs/conventions/components.md` — shared component rules.
- `docs/conventions/i18n.md` — Haitian Creole-first localization.
- Flutter architecture guidance: https://docs.flutter.dev/app-architecture/guide

OpenSpec:

- Mobile changes: `tchalanet-mobile/openspec/`.
- Root `openspec/` only for cross-project coordination.

Architecture: feature-first SoC/MVVM adaptation of Flutter's layered architecture
(`lib/app`, `lib/core`, `lib/design_system`, `lib/features/<feature_key>`). Riverpod
is the only application state-management/DI mechanism. Add layers only when
complexity justifies. Do not duplicate backend business rules. Treat offline
submissions as pending, never confirmed.

Shared workflow skills: `.agents/skills/` (see `.agents/README.md`).

Validation:

- Existing Flutter/Dart checks for the touched surface (`flutter analyze`, `flutter test`).
- Document Android/iOS permission impact when relevant.

Context rule:

- Load root rules, this router, the one relevant convention, and touched feature files.
