# OpenSpec — API Response Rules aligned to Modulith (76)

> Status: NORMATIVE

## 1. HTTP contract

All successful HTTP responses use `ApiResponse<T>` explicitly or through automatic wrapping.

Errors use `ProblemDetail` and are never wrapped in `ApiResponse`.

## 2. Module ownership

`common` owns the stable response primitives:

- `ApiResponse<T>`
- `ApiNotice`
- `ApiStatus`
- `ServiceStatus`
- Problem helpers

Platform/core/features/catalog controllers may use them.

## 3. Features

Features are BFF endpoints and may aggregate multiple services.

They may add notices/service statuses for partial results.

## 4. Platform

Platform APIs return Java models internally. Platform HTTP endpoints still follow `ApiResponse<T>`.

## 5. Core

Core controllers are thin adapters and return `ApiResponse<T>` or DTOs wrapped by advice.

Core API Java commands/queries/events are not HTTP response wrappers.
