# Tchalanet Mobile Offline Rules

> **Status**: NORMATIVE  
> **Scope**: offline POS, ticket selling, local submissions, and sync flows

---

## 1. Principle

Offline support is a first-class mobile concern, but the backend remains the final source of truth.

An offline sale submission is NOT a confirmed server ticket.

---

## 2. Offline states

Use explicit states:

```text
draft
pending_sync
syncing
synced
rejected
needs_review
failed
cancelled_locally
```

Do not collapse all failures into a generic error.

---

## 3. Core offline primitives

```text
lib/core/offline/
  sync_operation.dart
  sync_queue.dart
  sync_status.dart
  sync_failure.dart
  conflict_policy.dart
```

These are generic primitives only.

---

## 4. Feature-specific offline data

Feature-specific offline records stay inside their feature.

Example:

```text
features/sell_ticket/data/
  offline_ticket_submission_store.dart
  offline_ticket_submission_repository.dart
  offline_ticket_submission_dto.dart
```

---

## 5. Sell ticket offline flow

```text
Seller enters ticket
  -> client validates structural/local rules
  -> app creates local OfflineTicketSubmission
  -> app queues SyncOperation
  -> UI prints/displays local proof with offline status
  -> later sync sends submission to backend
  -> backend accepts/rejects/flags needs_review
  -> app updates local state
```

---

## 6. Forbidden

- Do not mark offline submission as confirmed ticket before server acceptance.
- Do not silently discard rejected submissions.
- Do not hide sync failures from seller/admin flows.
- Do not compute final limits/cutoff/fraud decisions as final truth on device.
- Do not allow local clock to become final source of sale time truth.

---

## 7. Required UX visibility

Seller must be able to see:

- number of pending submissions
- last sync result
- rejected submissions
- submissions requiring review
- whether current device is offline/online

---

## 8. Testing

Offline tests must cover:

- local submission creation
- duplicate sync attempt
- network unavailable
- server rejection
- server accepted
- needs review
- app restart with pending queue
