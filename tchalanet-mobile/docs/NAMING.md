# Tchalanet Mobile Naming Rules

> **Status**: NORMATIVE  
> **Scope**: Dart files, classes, providers, routes, and feature keys

---

## 1. Principles

- Names must be searchable.
- Names must encode intent.
- Avoid vague terms: `utils`, `helper`, `manager`, `service` unless the role is precise.
- One concept = one term.

---

## 2. Feature keys

Use lowercase snake_case.

Examples:

```text
auth
shell
pos_session
sell_ticket
ticket_verify
payout
sync
settings
```

Forbidden:

```text
SellTicket
sell-ticket
salesFeature
misc
common_feature
```

---

## 3. File names

Use snake_case.

```text
sell_ticket_screen.dart
sell_ticket_view_model.dart
sell_ticket_state.dart
sell_ticket_repository.dart
sell_ticket_remote_data_source.dart
sell_ticket_local_data_source.dart
sell_ticket_request_dto.dart
sell_ticket_response_dto.dart
sell_ticket_mapper.dart
```

---

## 4. Class names

Use PascalCase.

```dart
SellTicketScreen
SellTicketViewModel
SellTicketState
SellTicketRepository
SellTicketRemoteDataSource
SellTicketLocalDataSource
SellTicketRequestDto
SellTicketResponseDto
SellTicketMapper
```

---

## 5. Providers

Use lowerCamelCase and include the role.

```dart
sellTicketRepositoryProvider
sellTicketViewModelProvider
apiClientProvider
sessionManagerProvider
```

Forbidden:

```dart
sellProvider
managerProvider
serviceProvider
```

---

## 6. Routes

Use kebab-case route paths.

```text
/sell-ticket
/tickets/:ticketId
/sync
/session
/settings
```

Route names should be lowerCamelCase:

```dart
sellTicketRoute
ticketDetailsRoute
syncRoute
```

---

## 7. DTOs vs models

- `Dto` is allowed only in the data layer for API/local persistence shapes.
- UI-facing models use `View`, `State`, `Item`, or feature-specific names.
- Domain/client concepts use meaningful names: `TicketDraft`, `OfflineSubmission`, `Money`.

Forbidden:

```dart
TicketModel
GenericDto
DataModel
```

---

## 8. Anti-patterns

Forbidden names:

```text
utils.dart
helpers.dart
common.dart
base_service.dart
app_manager.dart
misc.dart
```

Allowed only if scoped:

```text
money_formatter.dart
api_error_mapper.dart
offline_submission_mapper.dart
```
