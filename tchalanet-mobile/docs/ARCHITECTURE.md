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

## 1. Architectural style

Tchalanet Mobile uses:

- **Feature-first / slice-first organization**
- **MVVM-style UI** with Riverpod providers
- **Repository-driven data layer**
- **Optional application/domain layer per feature**
- **Explicit offline support** for POS-critical workflows

The app is NOT organized around global folders like `screens/`, `services/`, `models/`, or `utils/`.

---

## 2. Top-level structure

```text
lib/
  app/
    bootstrap/
    config/
    router/
    theme/
    localization/
    error/
  core/
    api/
    auth/
    storage/
    network/
    result/
    pagination/
    ids/
    money/
    time/
    offline/
  features/
    auth/
    shell/
    pos_session/
    sell_ticket/
    ticket_verify/
    payout/
    sync/
    settings/
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

Large feature:

```text
features/<feature_key>/
  ui/
    xxx_screen.dart
    xxx_view_model.dart
    xxx_state.dart
  application/
    xxx_use_case.dart
    xxx_orchestrator.dart
  domain/
    xxx.dart
    xxx_policy.dart
  data/
    xxx_repository.dart
    xxx_remote_data_source.dart
    xxx_local_data_source.dart
    dto/
      xxx_request_dto.dart
      xxx_response_dto.dart
    mapper/
      xxx_mapper.dart
```

Small feature:

```text
features/<feature_key>/
  xxx_screen.dart
  xxx_view_model.dart
  xxx_state.dart
  xxx_repository.dart
```

Apply the **Rule of 3**:

- fewer than 3 files for a role: keep flat
- 3 or more files for a role: create `ui/`, `data/`, `application/`, etc.

---

## 4. Dependency direction

Canonical dependency direction:

```text
ui -> application -> data
ui -> data is allowed only for simple read/write flows
application -> domain
application -> data
data -> core/api, core/storage, core/network
domain -> core value primitives only
```

Forbidden:

- `data -> ui`
- `core -> features`
- direct feature-to-feature dependencies
- UI calling Dio directly
- UI reading secure storage directly
- ViewModel parsing raw JSON or backend errors directly

---

## 5. UI layer rules

UI layer contains:

- screens/widgets
- view models/controllers
- UI state records/classes
- UI-only formatting helpers

UI layer responsibilities:

- display state
- collect user input
- call ViewModel actions
- show loading/error/success states

UI layer MUST NOT:

- call Dio directly
- access local database directly
- access secure storage directly
- parse `ApiResponse` or `ProblemDetail`
- implement backend business rules
- decide tenant/session truth

---

## 6. ViewModel rules

ViewModels:

- expose immutable UI state
- receive user actions
- call use cases or repositories
- translate domain/application results into UI state
- are testable without Flutter widgets where possible

ViewModels MUST NOT:

- construct raw API URLs
- parse JSON
- mutate local DB directly
- hide long-running sync side effects
- contain critical business decisions that belong to backend

---

## 7. Application layer rules

`application/` is optional.

Create it when a feature needs:

- orchestration across multiple repositories
- multi-step flow logic
- command-like mobile actions
- sync planning
- reusable client-side workflow logic

Examples:

- `StartPosSessionUseCase`
- `SellTicketUseCase`
- `SyncOfflineSubmissionsUseCase`
- `ResolveSellerOperationContextUseCase`

Application layer MUST NOT duplicate backend critical rules such as final limits, final draw cutoff, payout finality, fraud decisions, or tenant isolation.

---

## 8. Domain layer rules

`domain/` is optional.

Use it only for pure client-side concepts:

- value objects
- local drafts
- offline submission models
- client-side policies for UX
- calculations needed for display or local validation

Domain objects must be pure Dart:

- no Flutter widgets
- no Riverpod
- no Dio
- no database APIs
- no secure storage

Critical business truth stays on the backend.

---

## 9. Data layer rules

Data layer contains:

- repositories
- remote data sources
- local data sources
- DTOs
- mappers

Repository is the client-side source of truth for a feature's data.

Repositories decide:

- remote vs local reads
- cache fallback
- offline queueing
- sync status
- retry strategy
- DTO/model mapping

Remote data sources:

- use `core/api`
- contain endpoint-specific HTTP calls
- return DTOs or low-level API results

Local data sources:

- use local DB/secure storage through `core/storage`
- never expose raw database rows to UI

---

## 10. Backend contract alignment

Backend 2xx responses use `ApiResponse<T>`.  
Backend 4xx/5xx responses use `ProblemDetail`.

Mobile must centralize parsing in:

```text
core/api/api_response.dart
core/api/problem_detail.dart
core/api/api_client.dart
```

Feature code must receive typed results, not raw maps.

---

## 11. Offline architecture

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
  ui/
  application/
  data/
```

Rules:

- `core/offline` contains primitives only.
- Feature-specific offline records stay inside the feature.
- Offline submissions are not treated as server-confirmed tickets.
- Sync must surface clear states: pending, syncing, synced, rejected, needs_review, failed.
- Server remains the final source of truth.

---

## 12. Security and session rules

- Tokens live in `core/auth` + secure storage.
- Features never read tokens directly.
- Seller operational context must be explicit: outlet, terminal, session.
- Client-provided operational context is convenience, not final trust.
- Backend validates final authorization and operational eligibility.

---

## 13. Testing expectations

Minimum tests:

- ViewModel tests for screen states
- Repository tests with fake remote/local data sources
- Mapper tests for DTO conversion
- Offline sync use-case tests for pending/success/failure cases

Widget tests are added for critical screens and flows.

---

## 14. Mental model

- `app` = composition
- `core` = technical primitives
- `features` = user-facing slices
- `ui` = state and presentation
- `application` = mobile orchestration
- `domain` = pure client concepts, optional
- `data` = repository and IO boundary

If a class does not clearly fit one role, it is misplaced.
