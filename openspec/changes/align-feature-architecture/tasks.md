## 1. Architecture Alignment

- [x] Remove feature-owned command handler from ops refresh orchestration.
- [x] Align ops web controller to dispatch core commands directly.
- [x] Align stats event listeners with after-commit side-effect policy.
- [x] Review publicdraw and document the bounded read-only SQL projection exception.
- [x] Update feature docs with explicit role and deviation rationale.
- [x] Add an architecture rule preventing command handlers under `features`.
- [x] Rename feature `*UseCase` classes to `*Service`.
- [x] Remove `features/**/infra/**` packages from aligned slices.
- [x] Add architecture rules preventing `features/**/infra/**` packages and `*UseCase` class names.
- [x] Apply feature package naming rules from `81-feature-rules.md` across aligned slices.
- [x] Rename feature `*Dto` classes/packages to feature model names.
- [x] Flatten role packages that contain fewer than three classes.
- [x] Replace publicdraw feature-owned query handlers with BFF services.
- [x] Replace reporting feature-owned query/handler classes with criteria and services.
- [x] Move stats dashboard criteria to model packages and replace app repositories with readers.

## 2. Validation

- [x] Compile backend.
- [x] Run targeted architecture/feature tests where available.
- [x] Validate OpenSpec change.
