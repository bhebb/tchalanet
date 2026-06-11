# Design: Mobile SoC, MVVM, brand, and i18n foundation

## 1. Decision summary

This change establishes four coupled foundations:

1. feature-first MVVM;
2. explicit component responsibilities and boundaries;
3. a repository-wide Riverpod state-management policy;
4. a Flutter Material 3 adaptation of the Tchalanet web design system;
5. Haitian Creole-first i18n.

These foundations are implemented together because screen structure, reusable
components, visual tokens, and localized state must share the same boundaries.

## 2. Separation of Concerns and MVVM

### 2.1 Component responsibilities

Tchalanet Mobile follows Flutter's Separation of Concerns guidance. The application
is split into UI and Data layers, with optional Domain/Use Case components only when
complexity justifies them.

State and behavior belong to the component whose documented responsibility requires
them:

| State | Owner |
| --- | --- |
| Screen loading/data/error state | Screen ViewModel |
| Form draft and validation state | Screen/flow ViewModel |
| Remote/local entity truth | Repository |
| Authenticated session | Auth Repository exposed through Auth ViewModel |
| Active locale | Locale controller |
| Active runtime theme | Theme controller/repository |
| Navigation stack | GoRouter |
| Ephemeral animation/focus/controller state | View |

Components must not take over responsibilities assigned to another component.

### 2.2 Screen contract

Each routed screen has one screen-level ViewModel:

```text
View
  watches immutable UiState
  sends user intents
  renders transient UI effects

ViewModel
  owns screen state
  exposes commands
  calls Repository / Use Case
  maps failures to typed UI failures

Repository
  is single source of truth for its data type
  coordinates remote/local/cache/offline/retry

Service / DataSource
  wraps exactly one external source
  is stateless
```

Reusable presentational widgets may remain stateless or own strictly ephemeral UI
state. They do not get independent business ViewModels.

### 2.3 Dependency direction

```text
app composition
  -> feature presentation
     -> feature domain/use cases (optional)
        -> feature repositories
           -> feature services/data sources
              -> core technical primitives
```

Rules:

- Views never call Services, Dio, repositories, secure storage, or platform plugins.
- ViewModels never call Services, Dio, or secure storage directly.
- `core/` never imports `features/`.
- Features do not import another feature's presentation layer.
- Cross-feature orchestration belongs in an app-level coordinator or explicit Use
  Case with interfaces at the correct boundary.
- Models crossing layers are typed; raw maps remain in Services/data mapping.

### 2.4 Effects

Navigation, SnackBars, dialogs, printing, clipboard, and platform actions are effects.
The ViewModel decides that an effect is required and exposes a typed effect/event.
The View performs the Flutter/platform call.

Effects must not become a second owner of screen state.

## 3. State-management policy

### 3.1 Technology decision

Riverpod is the only application state-management and dependency-injection mechanism
for Tchalanet Mobile.

Do not introduce a second state-management framework. Flutter primitives such as
`StatefulWidget`, `TextEditingController`, `FocusNode`, and animation controllers
remain allowed only for ephemeral presentation state.

### 3.2 State categories

Every state value must fit one category:

| Category | Examples | Responsible component | Lifetime |
| --- | --- | --- | --- |
| Ephemeral View state | focus, animation, scroll position, text controller | View | widget lifetime |
| Screen UI state | loading, form draft, validation, selected filter, error | ViewModel | routed screen/flow |
| Shared application state | auth session, locale, runtime theme, connectivity | dedicated app-level controller/repository | app/session |
| Application data state | tickets, draws, session data, offline queue | Repository | repository policy |
| Navigation state | route stack, route parameters | GoRouter | navigation lifetime |

State must not be duplicated across categories. A View may derive presentation values
from ViewModel state but must not maintain a competing copy of business or application
state.

### 3.3 Riverpod provider policy

Use providers according to responsibility:

| Provider type | Approved use |
| --- | --- |
| `Provider` | dependency injection and pure synchronous derived values |
| `NotifierProvider` | synchronous immutable UI/application state with commands |
| `AsyncNotifierProvider` | asynchronous UI/application state with commands and lifecycle |
| `StreamProvider` | genuine external streams exposed by a Repository |
| `FutureProvider` | simple read-only derived/query state with no commands |

Rules:

- Prefer `Notifier` and `AsyncNotifier` for ViewModels.
- New code must not introduce legacy `StateNotifier` or mutable public state objects.
- Provider `build()` methods are declarative and must not trigger navigation,
  SnackBars, printing, writes, or unrelated invalidations.
- Providers expose typed state and domain/UI models, never raw JSON or Dio responses.
- Provider families use stable typed parameters with meaningful equality.
- Global providers are reserved for true app/session-scoped state and dependency
  composition.

### 3.4 Immutability and state transitions

- ViewModel state is immutable.
- Commands are the only public mutation entry points.
- Every asynchronous command exposes an explicit transition such as
  `idle -> loading -> data/error`.
- Domain-relevant states such as `pending_sync`, `rejected`, `blocked`, and
  `needs_review` remain explicit; they are not collapsed into a generic error.
- Derived values are computed from canonical state, not stored as synchronized
  duplicates.
- Do not mutate lists, maps, models, or state fields in place.

### 3.5 Lifetime and disposal

- Screen- and flow-scoped providers use automatic disposal by default.
- A provider may be kept alive only when its longer lifetime is documented and
  tested.
- App/session-scoped providers must reset when their scope ends, especially on
  logout or tenant change.
- Provider invalidation and refresh are initiated by the component responsible for
  the affected state; Views call ViewModel commands rather than coordinating several
  unrelated providers.

### 3.6 Persistence, cache, and offline state

- Riverpod memory state is not persistence.
- Secure storage, database, cache, and offline queue access go through Repositories.
- Repositories define cache freshness, refresh, retry, conflict, and offline policy.
- Persisted state is restored through a Repository before being exposed to
  ViewModels.
- A successful local write must not be presented as server-confirmed unless the
  Repository has server confirmation.

### 3.7 Errors and effects

- Repositories expose typed application failures rather than raw exceptions.
- ViewModels map application failures into localized UI state and allowed commands.
- Navigation, dialogs, SnackBars, clipboard, printing, and platform actions are
  one-shot effects, not durable data state.
- Effects must be consumable once and must not replay unexpectedly after rebuild,
  rotation, or navigation.

### 3.8 State testing

State tests must cover:

- initial state;
- command transitions for success and failure;
- retry and refresh;
- disposal and restoration where relevant;
- logout/tenant-change resets;
- duplicate command/idempotency behavior;
- offline and synchronization transitions;
- effects emitted once.

## 4. Flutter design-system adaptation

### 4.1 Responsibility mapping

The web responsibility split is adapted to Flutter:

| Web | Flutter mobile |
| --- | --- |
| `libs/ui/theme` | `lib/design_system/theme` + `lib/core/theme` |
| `libs/ui/styles` | `lib/design_system/tokens` + layout/typography helpers |
| `libs/ui/components` | `lib/design_system/components` |
| feature SCSS composition | feature Views using shared components/tokens |

Flutter does not reproduce CSS variables. It uses typed Dart tokens and Material 3
system roles.

### 4.2 Default brand roles

The default local theme must align with the web Tchalanet preset:

| Brand role | Material 3 role | Default |
| --- | --- | --- |
| Deep navy | `primary` | `#1A1B4B` |
| On navy | `onPrimary` | `#FFFFFF` |
| Lighter navy | `primaryContainer` | `#2E3192` |
| Gold accent | `tertiary` / accent | `#FECB00` |
| On gold | `onTertiary` | `#241A00` |
| Background | `surface` / background role | `#F9F9FC` |
| Card/widget | `surfaceContainerLowest` | `#FFFFFF` |
| Main text | `onSurface` | `#1A1C1E` |
| Secondary text | `onSurfaceVariant` | `#464652` |

Gold is not `secondary`. `secondary` remains a muted indigo generated from the M3
scheme.

### 4.3 Token rules

- Feature Views use `Theme.of(context).colorScheme` and typed `Tch*` tokens.
- No feature View defines brand hex values.
- No feature View introduces repeated magic spacing, radius, elevation, or typography.
- Runtime backend tokens map to a validated mobile token contract.
- Unknown runtime tokens are ignored.
- The default local preset renders immediately; public/tenant runtime themes may
  override the validated subset.
- Plus Jakarta Sans is the default brand font when bundled and licensed for mobile;
  otherwise the implementation must document and use the approved fallback stack.

### 4.4 Shared component rules

Recurring UI roles must be implemented as shared components before repeated feature
copies:

- primary, secondary, tonal, destructive, and icon actions;
- loading, empty, error, offline, blocked, and success states;
- status badges;
- section headers;
- cards/surfaces;
- form fields and field errors;
- navigation and shell primitives.

Components expose typed Flutter parameters. They do not accept arbitrary brand colors
from features unless the parameter represents a semantic role.

### 4.5 POS adaptation

Visual consistency with web must not weaken mobile/POS ergonomics:

- minimum touch target: 44 logical pixels;
- primary POS actions: preferably 56 logical pixels or larger;
- one dominant action per critical screen;
- no dense web tables on compact surfaces;
- adaptive layouts use Material 3 window size classes and declared runtime surface;
- accessibility contrast and text scaling are required.

## 5. I18n design

### 5.1 Locale policy

Supported locales:

| Locale | Role |
| --- | --- |
| `ht` | Default and fallback |
| `fr` | Supported |
| `en` | Supported |

Startup resolution:

```text
saved user locale
  -> supported device locale
  -> ht
```

### 5.2 Text ownership

- Stable mobile UI labels live in bundled mobile locale files.
- Backend business/system messages use `messageKey`, fallback, and params.
- Ticket/PDF/print translation remains backend-owned.
- User-visible strings must not be hardcoded in Views, ViewModels, repositories, or
  Services.
- Technical strings such as routes, headers, storage keys, and API codes remain
  unlocalized.

### 5.3 Flutter root wiring

`MaterialApp.router` must receive:

- active locale;
- supported locales;
- localization delegates;
- localized app title where applicable.

Locale changes must update the complete visible application without restart.

### 5.4 Translation completeness

Every local key must exist in `ht`, `fr`, and `en`.

CI must fail when:

- a locale is missing a required key;
- a feature introduces a detectable hardcoded user-visible string;
- a translation key is malformed;
- `ht` is missing or is not configured as fallback.

## 6. Guardrails and verification

### Architecture tests

Guard:

- `core/` importing `features/`;
- Views importing Services, repositories, storage, Dio, or platform plugins;
- ViewModels importing Services, storage, Dio, or Flutter widgets;
- feature-to-feature presentation imports;
- routed screens without a screen-level ViewModel.
- competing state-management frameworks;
- ViewModels exposing mutable state;
- app/session-scoped providers without reset behavior.

### State-management tests

Guard:

- approved Riverpod provider roles and lifetimes;
- immutable typed ViewModel state;
- explicit asynchronous state transitions;
- persistence through Repositories only;
- effects emitted and consumed once;
- logout and tenant-change reset behavior.

### Design-system tests

Guard:

- default brand role values;
- backend token-to-mobile-role mapping;
- forbidden hardcoded brand colors in feature Views;
- shared component semantics;
- representative golden tests for compact phone and POS surfaces.

### I18n tests

Guard:

- locale key parity across `ht`, `fr`, and `en`;
- `ht` default/fallback behavior;
- hot locale change;
- representative critical screens with long translations and text scaling.

## 7. Migration order

1. Guardrails and normative docs.
2. Root theme/i18n composition.
3. Shared design-system components.
4. Auth.
5. Cashier home and operational context.
6. Session.
7. Sell/preview/success.
8. Ticket history/detail/print/share/scan.
9. Remaining screens.

Each migrated routed screen is complete only when it has:

- one ViewModel;
- responsibilities consistent with Separation of Concerns;
- state categories, lifetimes, and transitions consistent with the state-management
  policy;
- repository boundaries;
- no hardcoded user text or brand styling;
- ViewModel tests;
- widget/golden coverage appropriate to risk.
