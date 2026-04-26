# Proposal: Align Draw Events and Conventions

**Date**: 2026-04-26  
**Status**: COMPLETED / ARCHIVED  
**Author**: Agent

## 1. Context & Goals

Following the audit of the draw pipeline (2026-04-25), several critical issues were identified regarding event naming, architectural leakage, security gaps, and inconsistent practices across `core.draw` and `core.drawresult`.

This proposal aims to:

- Standardize event naming and ownership.
- Enforce strict domain isolation.
- Align controllers with `ApiResponse<T>` and `@PreAuthorize` conventions.
- Remove legacy "magic numbers" and centralize date/time resolution.

## 2. Changes

### 2.1 Events

- Rename `DrawResultedAppliedEvent` (owned by `core.drawresult`) to `DrawResultIngestedEvent`.
- Create `DrawResultAppliedEvent` (owned by `core.draw`) to signal when a result is attached to a draw.
- Fix `DrawDomainEventListener` to use `AFTER_COMMIT` and check for idempotency.

### 2.2 API & Controllers

- Migrate all draw-related controllers to `ApiResponse<T>`.
- Refactor `CreateDrawCommand` to return a `DrawSummary` directly.
- Secure `DrawAdminController` (uncomment `@PreAuthorize`).
- Move `overrideResult` from `DrawAdminController` to `DrawResultsOpsController`.

### 2.3 Technical Debt

- Replace manual date/time calculations in providers with `OccurredAtResolver`.
- Remove raw `ResponseEntity` returns.
- Replace `IllegalStateException` in ops controllers with `gate.assertEnabledOrThrow()`.
