# AGENTS.md — Tchalanet Mobile

Mobile agent router for `tchalanet-mobile/` (Flutter).

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`

Canonical docs (source of truth — do not duplicate here):

- `openspec/` — active specs and mobile context.
- README and design docs near touched code.
- Flutter architecture guidance: https://docs.flutter.dev/app-architecture/guide

OpenSpec:

- Mobile changes: `tchalanet-mobile/openspec/`.
- Root `openspec/` only for cross-project coordination.

Architecture: feature-first adaptation of Flutter's layered architecture (`lib/app`, `lib/core`, `lib/features/<feature_key>`). Add layers only when complexity justifies. Do not duplicate backend business rules. Treat offline submissions as pending, never confirmed.

Shared workflow skills: `.agents/skills/` (see `.agents/README.md`).

Validation:

- Existing Flutter/Dart checks for the touched surface (`flutter analyze`, `flutter test`).
- Document Android/iOS permission impact when relevant.

Context rule:

- Load root rules, this router, one relevant mobile doc, and touched feature files.
