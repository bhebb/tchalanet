# Tchalanet Mobile Architecture

> **Status**: NORMATIVE  
> **Scope**: `tchalanet-mobile/`  
> **Applies to**: Flutter application code, tests, and AI agent contributions  
> **Reference implementation style**: feature-first adaptation of Flutter's official app architecture guidance

---

## 0. Main external reference

Tchalanet Mobile follows a **feature-first adaptation** of Flutter's official app architecture recommendations.

Primary references:

- Flutter App Architecture — Concepts  
  https://docs.flutter.dev/app-architecture/concepts
- Flutter App Architecture — Guide  
  https://docs.flutter.dev/app-architecture/guide
- Flutter Architecture Recommendations  
  https://docs.flutter.dev/app-architecture/recommendations
- Flutter Offline-first support  
  https://docs.flutter.dev/app-architecture/design-patterns/offline-first
- Flutter Persistent storage architecture: SQL  
  https://docs.flutter.dev/app-architecture/design-patterns/sql

These references are authoritative for general Flutter architecture principles.  
Tchalanet rules below adapt them to our domain: POS, offline ticket selling, sync, secure session handling, tenant-aware backend APIs, and operational seller context.

Agents MUST NOT introduce architecture patterns that contradict these references unless an ADR explicitly approves the deviation.

---

## 0.1 Key principles

These principles drive every architecture decision in this app. If a class or pattern violates one of them, it is wrong regardless of what it is called.

### 1 — Separation of Concerns

The application is split into a UI layer and a Data layer, following Flutter's
official architecture guide. Each component has a distinct responsibility, interface,
boundary, and dependency direction:

```text
UI layer:   Views + ViewModels
Data layer: Repositories + Services
Optional:   Domain Use Cases for complex cross-repository orchestration
```

### 2 — MVVM: one View, one ViewModel

Every screen has exactly one ViewModel. The View displays state; the ViewModel owns logic.

```
View  →  watches state   →  ViewModel
View  →  triggers action →  ViewModel
ViewModel  →  calls    →  Repository / Use Case
```

### 3 — Views contain no business logic

A View (screen/widget) may only:
- render data provided by its ViewModel
- forward user gestures to the ViewModel
- run layout/animation logic

A View must NOT: call Dio, parse JSON, access secure storage, evaluate tenant rules, or decide whether a sale is allowed.

### 4 — Repositories are the single source of truth

One Repository per data type. The Repository decides:
- remote vs. local read
- cache policy
- offline queue
- retry strategy
- DTO → model mapping

A Repository exposes typed domain models, never raw API responses.

### 5 — Services are stateless data sources

A Service (or DataSource) wraps one external source: an HTTP endpoint, a local DB table, or the platform Keychain. It is stateless and returns raw data. It does not cache or combine sources.

### 6 — `domain/` is for use cases, not models

Models live in `data/models/`. The `domain/` folder is optional and exists only for use cases that orchestrate multiple repositories. If a feature has no cross-repository orchestration, skip `domain/` entirely.

---

## 1. Architectural style

Tchalanet Mobile uses:

- **Feature-first / slice-first organization**
- **MVVM-style UI** with Riverpod providers
- **Repository-driven data layer**
- **Optional application/domain layer per feature**
- **Explicit offline support** for POS-critical workflows
- **Riverpod-only application state management and dependency injection**

The app is NOT organized around global folders like `screens/`, `services/`, `models/`, or `utils/`.

### State-management policy

The durable policy lives in
[`docs/conventions/state_management.md`](conventions/state_management.md).

Summary:

- View-local state is limited to focus, animation, scroll and text controllers.
- ViewModels expose immutable typed screen state and commands.
- Repositories own persisted, cached, offline and remote application data.
- Screen providers are automatically disposed by default.
- App/session state resets on logout and tenant change.
- Riverpod memory state is not persistence.
- Navigation, dialogs, SnackBars, clipboard, printing and platform calls are one-shot
  effects, separate from durable state.

---

## 2. Top-level structure

```text
lib/
  app/           ← composition, router, theme, bootstrap
  core/          ← shared technical primitives (no feature logic)
    config/
    network/
    storage/
    settings/
    i18n/
  design_system/ ← tokens, theme, shared UI primitives
    tokens/
    theme/
  features/      ← one folder per user-facing slice
    auth/
    pos/
    settings/
    sync/
    ...
  main.dart
```

### `app/`

Application composition and runtime wiring:

- bootstrap
- router
- theme
- localization
- dependency container/provider root
- top-level error boundary

`app/` must not contain feature business logic.

### `core/`

Shared technical primitives:

- API client
- response/error contracts
- auth/session/token storage
- typed IDs/value objects
- money/time primitives
- storage/network/offline infrastructure
- result/error abstractions

`core/` must remain domain-light and reusable. It must not contain feature workflows.

### `features/`

User-facing slices. A feature exists because there is a screen, flow, menu entry, or mobile use case.

Examples:

- `auth`
- `pos_session`
- `sell_ticket`
- `ticket_verify`
- `payout`
- `sync`
- `settings`

---

## 3. Canonical feature structure

This structure is the direct application of the Flutter app architecture guide (see §0 references) to our feature-first organization.

Full feature:

```text
features/<feature_key>/
  data/
    models/                   ← typed domain models (no raw maps)
      xxx.dart
    repositories/             ← interface + implementation
      xxx_repository.dart
      xxx_repository_impl.dart
    services/                 ← stateless data sources (HTTP, local DB)
      xxx_service.dart
  domain/                     ← OPTIONAL — use cases only
    use_cases/
      xxx_use_case.dart
  presentation/
    view_models/              ← Riverpod Notifiers / ViewModels
      xxx_controller.dart
    views/                    ← screens and widgets
      xxx_page.dart
```

Small feature (Rule of 3: fewer than 3 files per role → keep flat):

```text
features/<feature_key>/
  xxx_page.dart
  xxx_controller.dart
  xxx_repository.dart
```

Apply the **Rule of 3**:

- fewer than 3 files for a role → keep flat in the feature folder
- 3 or more files for a role → create the sub-folder (`data/`, `presentation/`, `domain/`)

Skip `domain/` entirely if the feature has no cross-repository orchestration.

---

## 4. Dependency direction

```text
presentation/views
    ↓
presentation/view_models   ←→   domain/use_cases (optional)
    ↓                                  ↓
data/repositories  ←────────────────────┘
    ↓
data/services
    ↓
core/ (network, storage, config)
```

Allowed:
- `views → view_models` (watches state, triggers actions)
- `view_models → repositories` (direct, for simple features)
- `view_models → domain/use_cases` (when orchestration exists)
- `use_cases → repositories` (compose multiple repos)
- `repositories → services` (delegate IO)
- `services → core` (network, storage)

Forbidden:
- `data → presentation` (any direction upward)
- `core → features`
- direct feature-to-feature imports
- View calling a Service or Dio directly
- View reading secure storage directly
- ViewModel parsing raw JSON or `Map<String, dynamic>`
- ViewModel implementing backend business rules

### Automated architecture enforcement

`test/architecture/architecture_guard_test.dart` enforces these boundaries as a
progressive baseline:

- known legacy violations are listed explicitly and cannot grow;
- removing a known violation requires removing its baseline entry, so it cannot return;
- every routed screen is inventoried;
- screens marked migrated must expose one immutable screen ViewModel through an
  auto-disposed Riverpod provider;
- application/session state keeps an explicit reset path.
- detectable user-visible literals in legacy Views are tracked by an exact
  decreasing baseline, while migrated/new Views must resolve copy through i18n;
- feature Views cannot use raw colors or Material palette colors; they use
  `ColorScheme` roles or approved semantic Tchalanet tokens.

Updating a debt baseline is an architecture decision, not a routine way to make a test
pass. The migration inventory must explain any deliberate exception.

---

## 5. Presentation layer rules

### Views (`presentation/views/`)

A View:
- renders data from its ViewModel
- forwards user gestures to the ViewModel
- runs layout and animation logic

A View must NOT:
- call Dio or any Service directly
- access local DB or secure storage
- parse `ApiResponse` or `ProblemDetail`
- implement backend business rules
- decide tenant/session truth

### ViewModels (`presentation/view_models/`)

A ViewModel:
- holds the UI state as a Riverpod `Notifier` or `AsyncNotifier`
- exposes commands (methods) for user interactions
- calls Repositories or Use Cases
- translates results into UI state
- is testable without Flutter widgets

A ViewModel must NOT:
- construct raw API URLs
- parse raw JSON or `Map<String, dynamic>`
- mutate local DB directly
- contain final backend decisions (limits, payout finality, fraud, tenant isolation)
- expose mutable state, raw exceptions, or raw data-source responses
- perform navigation, show dialogs/SnackBars, print, or invoke platform plugins

Provider policy:

- Prefer `NotifierProvider` and `AsyncNotifierProvider`.
- Use `Provider` for DI and pure synchronous derivations.
- Use `FutureProvider` only for simple read-only queries without commands.
- Use `StreamProvider` only for genuine Repository-backed streams.
- Do not introduce new `StateNotifier` implementations.
- Document and test every provider that is kept alive beyond its natural scope.

---

## 6. Domain layer rules (optional)

`domain/use_cases/` exists only when a feature needs to orchestrate multiple repositories or reuse a client-side workflow across ViewModels.

Examples:
- `OpenPosSessionUseCase` (coordinates terminal binding + session creation)
- `SyncOfflineSubmissionsUseCase` (reads queue, calls sync service, updates status)

Use Cases:
- take typed inputs, return typed outputs
- depend on Repositories only (never on ViewModels or Views)
- are pure Dart — no Flutter, no Riverpod, no Dio directly

Skip `domain/` if no orchestration is needed. Most simple features only need `view_model → repository`.

---

## 7. Data layer rules

### Models (`data/models/`)

- Typed Dart classes — no raw `Map<String, dynamic>` outside `fromJson`
- `fromJson` factory for deserialization
- No Flutter, no Riverpod, no Dio
- Sensitive fields (tokens) never appear in `toString()`

### Repositories (`data/repositories/`)

- One repository per data type or feature concern
- Abstract interface + concrete implementation (for testability)
- Owns: cache policy, offline queue, retry, DTO → model mapping
- Exposes typed domain models, never raw DTOs

### Services (`data/services/`)

- Stateless wrapper around one data source (HTTP endpoint, local DB table, Keychain)
- Returns raw data (DTO or `Map<String, dynamic>`)
- No caching, no combining sources
- One service per external source

---

## 8. Use case layer rules

See §6. Alias: the folder is `domain/use_cases/`, not `application/`.

---

## 9. Backend contract alignment

Backend 2xx responses use `ApiResponse<T>`.  
Backend 4xx/5xx responses use `ProblemDetail`.

Mobile centralizes parsing in:

```text
shared/models/api_response.dart
shared/models/problem_detail.dart
core/network/api_client.dart
```

Feature code receives typed results only — never raw maps.

---

## 10. Offline architecture

Offline support is explicit.

```text
core/offline/
  sync_operation.dart
  sync_queue.dart
  sync_status.dart
  conflict_policy.dart

features/sell_ticket/data/
  offline_ticket_submission_store.dart
  offline_ticket_submission_repository.dart

features/sync/
  data/
  domain/use_cases/
  presentation/
```

Rules:

- `core/offline` contains primitives only.
- Feature-specific offline records stay inside the feature.
- Offline submissions are not treated as server-confirmed tickets.
- Sync must surface clear states: pending, syncing, synced, rejected, needs_review, failed.
- Server remains the final source of truth.

---

## 11. Security and session rules

- Tokens live in `core/storage` + secure storage.
- Features never read tokens directly.
- Seller operational context must be explicit: outlet, terminal, session.
- Client-provided operational context is convenience, not final trust.
- Backend validates final authorization and operational eligibility.

---

## 12. Testing expectations

Minimum tests:

- ViewModel tests for screen states
- Repository tests with fake Services (no real HTTP)
- Use case tests for orchestration logic
- Offline sync use-case tests for pending/success/failure cases

Widget tests for critical screens and flows.

---

## 13. Mental model

| Folder | Role |
|---|---|
| `app/` | Composition — wires everything, no logic |
| `core/` | Technical primitives — no feature workflow |
| `design_system/` | Tokens, theme, shared UI components |
| `features/<x>/data/models/` | Typed domain models |
| `features/<x>/data/services/` | Stateless data sources (HTTP, DB) |
| `features/<x>/data/repositories/` | Single source of truth per data type |
| `features/<x>/domain/use_cases/` | Cross-repo orchestration (optional) |
| `features/<x>/presentation/view_models/` | UI state + user action handlers |
| `features/<x>/presentation/views/` | Screens and widgets — display only |

If a class does not clearly fit one row, it is misplaced.
