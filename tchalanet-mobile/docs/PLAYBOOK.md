# Tchalanet Mobile Playbook

> **Status**: NORMATIVE  
> **Scope**: day-to-day Flutter development and AI agent contributions

---

## 1. Before adding code

Ask:

1. Is this a user screen or flow? → `features/<feature_key>`
2. Is this app composition? → `app/`
3. Is this reusable technical infrastructure? → `core/`
4. Is this backend-critical business truth? → do not implement as final truth on mobile

---

## 2. Adding a new feature

1. Create `lib/features/<feature_key>/`.
2. Start simple with UI + repository.
3. Add `application/` only for orchestration.
4. Add `domain/` only for pure client models/rules.
5. Add tests.
6. Update feature documentation if the flow is important.

Recommended first structure:

```text
features/<feature_key>/
  <feature_key>_screen.dart
  <feature_key>_view_model.dart
  <feature_key>_state.dart
  <feature_key>_repository.dart
```

Grow into folders only when needed.

---

## 3. Do

- Use Riverpod providers.
- Use GoRouter for navigation.
- Use repositories as source of truth.
- Use remote/local data sources behind repositories.
- Parse `ApiResponse` and `ProblemDetail` centrally.
- Keep UI state explicit and immutable.
- Use typed IDs/value objects for important identifiers.
- Keep offline states visible and auditable.
- Write tests for ViewModels and repositories.

---

## 4. Don't

- Do not call Dio from UI.
- Do not access secure storage directly from features.
- Do not put all models in a global `models/` folder.
- Do not create vague `utils.dart` or `helper.dart` files.
- Do not duplicate backend critical business rules.
- Do not create abstract interfaces for every class by default.
- Do not hide sync side effects in UI widgets.
- Do not treat offline submissions as confirmed tickets.

---

## 5. Common workflows

### Online read flow

```text
Screen -> ViewModel -> Repository -> RemoteDataSource -> ApiClient
```

### Offline-capable read flow

```text
Screen -> ViewModel -> Repository
                         ├─ LocalDataSource
                         └─ RemoteDataSource
```

### Offline write flow

```text
Screen -> ViewModel -> UseCase -> Repository
                                 ├─ Local submission store
                                 └─ Sync queue
```

### Sync flow

```text
SyncScreen -> SyncViewModel -> SyncOfflineSubmissionsUseCase -> SyncRepository
```

---

## 6. Agent contribution checklist

Before submitting code, verify:

- [ ] No Dio usage outside data sources/core API.
- [ ] No secure storage usage outside core/auth/core/storage.
- [ ] No feature imports another feature internals.
- [ ] Repository returns typed results, not raw maps.
- [ ] DTO mapping is explicit.
- [ ] Offline states are represented explicitly.
- [ ] Tests added or updated for ViewModel/repository logic.
- [ ] New architecture pattern is documented.
