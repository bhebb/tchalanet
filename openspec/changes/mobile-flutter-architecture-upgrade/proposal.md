# Change: Align Flutter mobile with official MVVM architecture and latest tooling

## Status

Proposed

## Owner

Tchalanet mobile

## Context

Tchalanet mobile is being moved to a standalone Flutter application outside Nx. The first OpenSpec focuses on removing the legacy Nx/Ionic mobile app and creating the new Flutter app. This follow-up change passes behind that work to standardize the Flutter architecture, dependency choices, version policy, and local developer setup before feature development starts.

The mobile app must start cleanly with the latest stable Flutter SDK and recent package versions. If the developer machine has an old Flutter SDK, Claude must instruct the developer to update or install a clean stable SDK before generating app code.

## Why

- Avoid starting the new mobile app with an outdated Flutter SDK.
- Align code with Flutter/Google architecture guidance: MVVM, repositories, services, UI/data separation.
- Keep mobile isolated from server, infra, edge, and web code after the Nx/Ionic cleanup.
- Establish conventions before implementing auth, home, dashboard, POS/sell, ticket verification, offline, and printing.

## What

- Adopt Flutter official architecture guidance as baseline: MVVM + repositories + services.
- Implement Tchalanet mobile with feature-first folders and Riverpod-based ViewModels.
- Use the latest stable Flutter SDK available on the developer machine, after `flutter upgrade` or a clean SDK installation if needed.
- Install current compatible dependency versions using `flutter pub add`, not hardcoded stale versions.
- Configure routing, auth state, HTTP, secure token storage, environment config, theme, linting, and basic tests.
- Add local Android test documentation.
- Update `VERSIONS.md` with the actual Flutter/Dart/tooling versions used.

## Non-goals

- Do not implement the sell process yet.
- Do not implement the full dashboard yet.
- Do not implement offline sync yet.
- Do not implement Bluetooth/ESC-POS printing yet.
- Do not change Spring Boot backend contracts unless a missing endpoint is explicitly confirmed.
- Do not scan or refactor `tchalanet-server`, `tchalanet-infra`, `tchalanet-edge-service`, or Angular web code.

## Strict repository boundaries for Claude

After the legacy Nx/Ionic cleanup is complete, Claude must stay inside these paths only:

- `tchalanet-mobile/**`
- `VERSIONS.md`
- `openspec/changes/mobile-flutter-architecture-upgrade/**`
- root files only when strictly required for mobile documentation or cleanup validation

Claude must not scan, index, grep, refactor, or edit:

- `tchalanet-server/**`
- `tchalanet-infra/**`
- `tchalanet-edge-service/**`
- `apps/tchalanet-web/**`
- unrelated backend, infra, edge, or web libraries

Allowed exception: Claude may inspect root-level workspace files such as `package.json`, `nx.json`, `.github/**`, and `VERSIONS.md` only to remove stale references to the deleted legacy mobile app. Once that cleanup is done, Claude must stop scanning root/web/server/infra/edge and continue only inside `tchalanet-mobile/**`.

## Impact

- New mobile code follows official Flutter architectural guidance.
- Developer machines are normalized around recent stable Flutter tooling.
- Future features can be implemented feature-by-feature without mixing UI, network, storage, and domain logic.
