# Change: Mobile SoC, MVVM, Tchalanet brand, and Haitian Creole foundation

## Status
Proposed

## Why

The mobile application needs one enforceable architecture, one default Tchalanet
visual language across web and Flutter, and one complete localization rule before
additional POS capabilities multiply the current inconsistencies.

Without this foundation, each new screen increases direct data-source coupling,
duplicated state, hardcoded styles, and untranslated user-facing text.

## What Changes

- Establish Separation of Concerns (SoC) with feature-first MVVM.
- Establish a repository-wide Riverpod state-management policy.
- Enforce Repository and dependency boundaries.
- Align the default Flutter Material 3 theme with the web Tchalanet navy/gold brand.
- Adapt web theme/style/component responsibilities into typed Flutter conventions.
- Make Haitian Creole (`ht`) the default and fallback locale.
- Require i18n, semantic tokens, shared components, and automated guardrails across
  all screens.

## Context

The Flutter application already uses Riverpod, GoRouter, Material 3, a feature-first
folder structure, runtime theme primitives, and local/backend i18n foundations.
However, the implementation and the normative mobile documentation currently drift
from the intended architecture and from the Tchalanet web brand.

The web application is the current visual reference for the default Tchalanet brand:

- deep navy primary: `#1A1B4B`;
- navy primary container: `#2E3192`;
- gold accent / Material 3 tertiary: `#FECB00`;
- light background: `#F9F9FC`;
- white card surface;
- Plus Jakarta Sans brand typography;
- semantic Material 3 and `--tch-*` tokens rather than feature-level hardcoded values.

The mobile application must adapt this language to Flutter and POS constraints. It
must remain recognizably Tchalanet without copying dense web layouts onto small
screens.

## Current findings

### Architecture

- MVVM is documented but not consistently enforced.
- Several Views call Services or secure storage directly.
- Most ViewModels call Services directly instead of Repositories.
- Some screens have no dedicated ViewModel; some ViewModels are declared inside View
  files.
- Mutable state ownership is distributed between widgets, providers, controllers,
  Services, and secure storage without an explicit ownership rule.
- `core/network` imports a cashier feature, reversing the documented dependency
  direction.
- Several Views are very large and combine rendering, orchestration, validation, and
  navigation effects.

### Design system

- Mobile normative documents disagree on the default palette.
- The current mobile design token document declares purple `#3525CD` as primary,
  while the web reference declares navy `#1A1B4B` and gold `#FECB00`.
- Runtime theme mapping supports only a small subset of the semantic roles used by
  the web.
- Screens still contain hardcoded colors, sizes, strings, and one-off visual
  decisions.
- Shared mobile components do not yet cover all recurring screen states and actions.

### I18n

- Mobile currently declares French as the default locale.
- Haitian Creole (`ht`) has no bundled mobile locale.
- Most user-visible strings are hardcoded in Flutter Views.
- `MaterialApp.router` is not fully wired to the active locale.
- Existing local bundles cover only a small part of the visible application.

### Quality guardrails

- Static analysis passes but cannot detect architectural boundary violations.
- Tests cover only two login widgets.
- No architecture tests guard forbidden imports, View-to-Service calls, hardcoded
  user-facing strings, or hardcoded brand colors.

## Goals

1. Make MVVM the enforceable architecture for every Flutter screen.
2. Make Separation of Concerns (SoC) the primary architecture principle.
3. Define how local UI, screen, shared application, persisted, and remote state are
   owned and managed with Riverpod.
4. Make Repositories the single source of truth between ViewModels and data sources.
5. Align the default Flutter Material 3 theme with the Tchalanet web brand.
6. Define a Flutter token and shared-component convention adapted from
   `tchalanet-web/libs/ui/theme`, `ui/styles`, and `ui/components`.
7. Make Haitian Creole (`ht`) the default and fallback mobile locale.
8. Require all user-visible mobile text to use i18n keys.
9. Add automated guardrails for architecture, state management, tokens, i18n, and critical ViewModel
   behavior.
10. Keep POS usability requirements stronger than visual parity with the web.

## Non-goals

- Pixel-for-pixel reproduction of web pages on mobile.
- Redesigning the web theme system.
- Implementing every existing mobile screen in this planning change.
- Implementing backend translation endpoints.
- Defining tenant-specific visual identities beyond the existing runtime theme
  contract.
- Treating a Stitch mockup as a source of runtime tokens or business behavior.

## Proposed approach

### Architecture

- Use feature-first MVVM.
- Require one screen-level ViewModel per routed screen.
- Views render state and forward intents only.
- ViewModels depend on Repositories or optional Use Cases, never directly on Dio,
  Services, secure storage, or another feature's presentation provider.
- Repositories own data coordination, mapping, cache, offline policy, and retry
  policy.
- Services remain stateless external data sources.
- Keep state and behavior inside the component responsible for them: Views for
  presentation-only concerns, ViewModels for UI state and commands, Repositories for
  application data truth, and Services for external APIs.
- Use unidirectional flow:

```text
View intent -> ViewModel command -> Repository / Use Case -> new immutable state -> View
```

### State management

- Use Riverpod as the only application state-management and dependency-injection
  mechanism.
- Keep widget-local state only for ephemeral presentation concerns.
- Use immutable typed UI state owned by ViewModels.
- Use Repositories as the source of truth for persisted, cached, offline, and remote
  application data.
- Scope providers to their actual lifetime; screen state is disposed by default.
- Model loading, empty, success, failure, offline, and pending states explicitly.
- Handle one-shot effects separately from durable UI state.

### Default Tchalanet Material 3 brand

- Use the web Tchalanet preset as the default brand reference.
- Map navy to Material 3 `primary`.
- Map gold to Material 3 `tertiary` / accent, not `secondary`.
- Generate or define the complete Flutter `ColorScheme`; screens consume semantic
  roles only.
- Keep status colors semantic and distinct from the gold brand accent.
- Adapt typography, spacing, radius, elevation, breakpoints, and reusable components
  to Flutter and POS touch requirements.

### I18n

- Add `assets/i18n/ht.json`.
- Make `ht` the startup fallback and default locale.
- Keep `fr` and `en` supported.
- Route every user-visible string through the mobile i18n abstraction.
- Add locale delegates/configuration to the root app.
- Keep backend-provided messages compatible with `messageKey`, fallback, and params.

### Visual references

A Stitch design may be supplied as an implementation reference. It must:

- respect the default navy/gold Tchalanet palette;
- use Material 3 semantics;
- preserve POS accessibility and touch sizing;
- be translated into shared tokens/components rather than copied as one-off styles.

## Impact

### Mobile files and documents

- `docs/ARCHITECTURE.md`
- `docs/conventions/theme.md`
- `docs/conventions/i18n.md`
- `docs/mobile/02_mobile_design_tokens.md`
- `docs/mobile/03_mobile_components.md`
- `docs/mobile/ui-rules.md`
- `lib/app/`
- `lib/core/`
- `lib/design_system/`
- `lib/features/`
- `assets/i18n/`
- `test/`

### Reference-only web sources

- `tchalanet-web/docs/conventions/theme.md`
- `tchalanet-web/docs/conventions/style.md`
- `tchalanet-web/libs/ui/theme/`
- `tchalanet-web/libs/ui/styles/`
- `tchalanet-web/libs/ui/components/`

No web code is changed by this mobile-owned change.

## Risks

- A broad migration can create long-lived mixed architecture if executed screen by
  screen without guardrails first.
- Literal visual parity with the web can reduce POS usability.
- Converting every hardcoded string at once can create incomplete translations.
- Runtime tenant theme overrides can drift from the default local theme unless token
  contracts are tested.

## Rollout

1. Resolve and publish normative architecture, theme, and i18n rules.
2. Add automated architecture/token/i18n guardrails.
3. Establish the default Tchalanet Material 3 theme and shared components.
4. Wire Haitian Creole as default and migrate shared/root UI strings.
5. Migrate features incrementally, starting with auth and cashier critical flows.
6. Verify each migrated feature with ViewModel, repository, widget, and golden tests.
