# Tasks

## 1. Normative rules and inventory

- [x] Publish Separation of Concerns as the primary principle in `docs/ARCHITECTURE.md`.
- [x] Update architecture rules with the MVVM screen contract, effects, and dependency boundaries.
- [x] Publish the general Riverpod state-management policy in `docs/ARCHITECTURE.md`.
- [x] Document state categories, provider selection, lifetimes, disposal, persistence, invalidation, errors, and effects.
- [x] Reconcile conflicting mobile theme/token documents against the web Tchalanet preset.
- [x] Update the mobile i18n convention: `ht` default/fallback, `fr` and `en` supported.
- [x] Inventory routed screens, their ViewModels, state owners, direct data-source calls, hardcoded strings, and hardcoded visual values.
- [x] Define the incremental migration matrix and completion criteria per screen.

## 2. Automated guardrails

- [x] Add architecture tests for forbidden dependency directions and layer imports.
- [x] Add state-management guards for approved Riverpod provider roles and immutable state.
- [x] Add guards for screen-provider disposal and app/session reset behavior.
- [ ] Add a guard requiring a screen-level ViewModel for every routed screen.
  - [x] Inventory every route and enforce the complete ViewModel contract for migrated screens.
  - [ ] Migrate remaining routed screens, then remove the progressive route-contract exceptions.
- [x] Add a guard against detectable user-visible hardcoded strings.
- [x] Add a guard against hardcoded brand colors in feature Views.
- [x] Add locale key-parity validation for `ht`, `fr`, and `en`.
- [x] Add tests for default/fallback locale resolution.
- [ ] Add tests for one-shot effects and explicit asynchronous state transitions.
  - [x] Test that late auth restoration cannot overwrite a completed login.
  - [x] Add a typed cross-screen notification queue and consumption test.
  - [ ] Add tests for feature-owned typed one-shot effects.

## 3. Default Tchalanet Material 3 theme

- [x] Define the validated Flutter semantic token contract.
- [x] Align the local default theme to navy `#1A1B4B` and gold tertiary `#FECB00`.
- [x] Generate or define the complete Material 3 light `ColorScheme`.
- [ ] Decide and implement the approved mobile Plus Jakarta Sans font packaging/fallback.
- [x] Expand and test backend runtime token mapping to supported Flutter roles.
- [ ] Align spacing, radius, elevation, typography, and adaptive breakpoint tokens.
- [ ] Add compact-phone and POS golden tests for the default theme.

## 4. Shared Flutter components

- [x] Define component ownership and APIs adapted from web `ui/components`.
- [x] Implement shared semantic action buttons.
- [x] Implement loading, empty, error, offline, blocked, and success states.
- [x] Implement shared status badge, section header, card/surface, and field-error primitives.
- [x] Implement shared shell/navigation primitives for compact phone and POS.
- [x] Implement the root semantic notification host and notification banner.
- [x] Separate internal announcement, global news, and API notice/error contracts.
- [x] Retain hidden API trace IDs and expose a copy-support-reference action.
- [x] Implement the persisted `platform.notification` mobile Repository.
- [x] Implement authenticated summary polling, foreground refresh, manual refresh, and logout reset.
- [x] Implement the POS notification center UI with read/archive actions.
- [x] Integrate `ApiResponse.notices` parsing with response `X-Request-Id`.
- [x] Document how Stitch references are translated into tokens/components.

## 5. Haitian Creole-first i18n

- [x] Add complete `assets/i18n/ht.json`.
- [x] Wire root locale, supported locales, delegates, and localized app title.
- [x] Persist the selected locale and resolve startup locale with `ht` fallback.
- [ ] Migrate shared/root user-visible strings to i18n.
- [ ] Define typed parameter interpolation and backend message fallback behavior.
- [ ] Replace the temporary French Material/Cupertino framework fallback with native Haitian Creole delegates.
- [ ] Add tests for hot locale switching, missing keys, and text scaling.
  - [x] Test startup resolution, persistence, and hot locale switching.
  - [ ] Add missing-key and text-scaling coverage.

## 6. SoC + MVVM feature migration

- [ ] Migrate auth screens and declare their state owners.
  - [x] Migrate `/forbidden` to a screen ViewModel, i18n, and shared feedback state.
  - [x] Replace raw login exceptions with a typed i18n failure key.
  - [x] Move successful-login redirection ownership exclusively to GoRouter.
  - [ ] Separate public draw presentation from `/login` auth responsibilities.
  - [ ] Finish login and public draw copy i18n migration.
- [ ] Migrate cashier home and operational-context screens.
- [ ] Migrate session screens.
- [ ] Migrate sell, preview, and success screens.
- [ ] Migrate history, ticket detail, print/share, and scan screens.
- [ ] Remove View-to-Service/storage calls and ViewModel-to-Service/storage calls.
- [ ] Remove `core -> features` imports.
- [ ] Add Repository and ViewModel tests for each migrated critical flow.
- [ ] Remove competing/duplicated state and direct multi-provider orchestration from Views.

## 7. Verification and documentation

- [ ] Verify every routed screen uses i18n and semantic design tokens.
- [ ] Verify every routed screen has one screen-level ViewModel and respects component responsibilities.
- [ ] Verify every provider has an intentional category, lifetime, disposal, and reset policy.
- [ ] Run `dart format --set-exit-if-changed .`.
- [x] Run `flutter analyze`.
- [x] Run `flutter test`.
- [ ] Review representative screens against the supplied Stitch reference, when available.
- [x] Update affected durable docs and mark all completed migration tasks.
