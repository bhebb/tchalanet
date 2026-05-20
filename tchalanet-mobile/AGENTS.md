# Tchalanet Mobile — Agent Rules

> **Status**: NORMATIVE  
> **Scope**: AI agents and automated contributions to `tchalanet-mobile/`

---

Read first:

- `../AGENTS.md`
- `../VERSIONS.md`
- `CLAUDE.md`
- `openspec/project.md`

Canonical local docs:

- `CLAUDE.md`
- `openspec/`
- Flutter project docs and README files near touched code.

OpenSpec:

- Use `tchalanet-mobile/openspec/` for mobile changes.
- Use root `openspec/` only for cross-project coordination.

Validation:

- Use existing Flutter/Dart checks for the touched app surface.
- Document Android/iOS permission impact when relevant.

Context rule:

- Load root rules, local mobile router, one relevant mobile design doc, and touched feature files.

## 1. External architecture reference

Before introducing a new mobile architecture pattern, agents MUST check the official Flutter architecture guidance:

- https://docs.flutter.dev/app-architecture/concepts
- https://docs.flutter.dev/app-architecture/guide
- https://docs.flutter.dev/app-architecture/recommendations
- https://docs.flutter.dev/app-architecture/design-patterns/offline-first

Tchalanet follows a feature-first adaptation of Flutter's recommended layered architecture:

```text
UI layer -> optional application/domain logic -> data layer
```

Agents MUST NOT introduce Clean Architecture boilerplate blindly.
Agents MUST prefer simple feature slices and add layers only when complexity justifies them.

---

## 2. Non-negotiable rules

- Do not call Dio from UI.
- Do not access secure storage from features directly.
- Do not put everything in global `models/`, `screens/`, or `services/` folders.
- Do not duplicate backend critical business rules.
- Do not treat offline submissions as confirmed tickets.
- Do not introduce direct feature-to-feature dependencies.
- Do not create vague `utils.dart`, `helpers.dart`, or `manager.dart` files.

---

## 3. Required architecture

Use:

```text
lib/app
lib/core
lib/features
```

Feature code goes in:

```text
lib/features/<feature_key>
```

Start simple. Add subfolders only when the feature grows.

---

## 4. Mobile/backend boundary

Backend remains source of truth for:

- tenant isolation
- authorization
- payout finality
- final ticket acceptance
- final limit decisions
- final draw cutoff decisions
- fraud/review decisions

Mobile may provide local UX validation and offline queueing only.

---

## 5. PR checklist for agents

- [ ] Followed `docs/ARCHITECTURE.md`.
- [ ] Followed `docs/NAMING.md`.
- [ ] No forbidden global folders introduced.
- [ ] No raw HTTP in UI/ViewModel.
- [ ] API parsing centralized in `core/api`.
- [ ] Offline states are explicit if offline is involved.
- [ ] Tests added or updated.
- [ ] Any new architecture deviation documented.
