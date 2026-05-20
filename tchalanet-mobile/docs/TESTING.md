# Tchalanet Mobile Testing Rules

> **Status**: NORMATIVE

---

## 1. Testing pyramid

Prioritize:

1. pure Dart tests for mappers, value objects, policies
2. repository tests with fake remote/local data sources
3. ViewModel tests
4. widget tests for critical screens
5. integration tests for critical flows

---

## 2. ViewModel tests

ViewModel tests must verify state transitions:

```text
initial -> loading -> success
initial -> loading -> failure
pending_sync -> syncing -> synced
pending_sync -> syncing -> rejected
```

---

## 3. Repository tests

Repository tests must use fake data sources.

Do not hit real network or real backend in unit tests.

---

## 4. Mapper tests

Every non-trivial DTO mapper needs tests.

Especially:

- API response to UI model
- ProblemDetail to AppError
- offline submission DTO to domain/client model

---

## 5. Offline tests

Offline sync must be tested as a first-class workflow.

Required cases:

- create local submission
- queue sync operation
- retry failed operation
- handle server rejection
- handle accepted submission
- keep pending operations after app restart
