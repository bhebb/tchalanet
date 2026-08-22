# Tasks: seller-terminal-client-diagnostics-v1

## 0. Scope

- [x] Confirm this is cross-project and belongs under root `openspec/changes/`.
- [x] Define proposal, design, tasks, and spec deltas.
- [x] Refine ownership to `platform.clientdiagnostics`, strict expiry, closed event schema, bounded batching, and normative retention.

## 1. Backend policy and ingestion

- [ ] Add `platform.clientdiagnostics` module with public `api/` and internal policy, ingestion, persistence, and web packages.
  - [x] Initial `api/model`, ingestion service, and tenant web endpoint scaffold added.
  - [x] DB-backed policy, event persistence, and web endpoints added.
  - [x] Internal service and persistence responsibilities separated to match platform module architecture.
- [x] Add diagnostic policy persistence scoped to tenant + seller terminal.
- [x] Add platform ops commands to enable/disable diagnostics with expiry and reason.
- [x] Return the active diagnostics policy in POS profile/bootstrap for the current terminal.
- [x] Add `POST /api/v1/tenant/client-diagnostics/events` for event batches.
- [ ] Add diagnostics endpoints under `/api/v1/admin/seller-terminals/{sellerTerminalId}/diagnostics` and `/api/v1/platform/ops/client-diagnostics`.
  - [x] Admin read-only policy endpoint added at `/api/v1/admin/seller-terminals/{sellerTerminalId}/diagnostics`.
  - [x] DB-backed platform ops enable/disable endpoint added.
- [x] Resolve tenant, seller terminal, and seller from request context, not client payload.
- [x] Add `client_diagnostics.write` to the hardcoded V0 seller-terminal permission set if still required.
- [ ] Enforce closed schema, body-size, stack-frame bounds, idempotency, rate-limit, `maxEvents`, max activation duration, and 7-day retention rules.
  - [x] Server-side category whitelist, idempotent event insert, max activation duration, per-minute ingestion limit, server-side `maxEvents`, and 7-day retention job added.
  - [x] Basic PII/secret rejection added for message, identifiers, endpoint key, and stack frames.
  - [ ] Request body-size hard limit still needs gateway/app config verification.
- [ ] Add admin read endpoints for recent events and event detail.
  - [x] Recent event list endpoint added.
  - [x] Full event detail endpoint added.
- [ ] Add focused backend tests for policy expiry, ingestion authorization, redaction rejection, idempotency, and tenant isolation.

## 2. Mobile diagnostics client

- [x] Extend POS profile/runtime models with diagnostics policy.
- [x] Add a diagnostic reporter service with whitelisted event construction and bounded retry queue.
  - [x] Queue, HTTP transport, and reporter orchestration separated under `core/client_diagnostics`.
- [x] Capture API errors from `ApiClient`, including request ID and problem code.
- [x] Classify mobile connectivity and sale-process API failures separately from generic API errors.
- [x] Capture auth restore/login/build-session failures as connectivity diagnostics when policy allows it.
- [x] Capture print failures from `PrinterService` and `printTicket`.
- [x] Capture Flutter framework and uncaught async errors.
- [x] Batch diagnostic submissions in groups of 5-20 events.
- [x] Drop oldest events first when the local queue is full.
- [x] Suppress duplicate mobile diagnostics with a short fingerprint-based dedupe window.
- [x] Destroy queued events when diagnostics are disabled or `expiresAt` passes.
- [x] Ensure diagnostic submission never blocks sale, print, scan, or navigation flows.
- [ ] Add focused mobile tests for disabled policy, expired policy, redaction, queue bounds, and print/API event payloads.

## 3. Admin web

- [ ] Add diagnostics controls to platform ops, with admin seller terminal detail read-only.
  - [x] Read-only diagnostics state card added to seller terminal detail.
  - [x] Enable/disable controls added to platform ops.
  - [x] Shared platform seller-terminal target picker extracted for tenant-scoped terminal search.
- [ ] Require duration and reason before enabling diagnostics.
  - [x] Platform ops control collects duration, reason, and categories.
  - [x] Client-side category validation added before enable.
- [x] Display active/expired/off state clearly.
- [ ] Add recent diagnostic events table and event detail view.
  - [x] Recent diagnostic events view added under platform ops.
  - [x] Full event detail view added with device, printer, app, endpoint, exception, stack frames, and sanitized payload.
- [ ] Keep the diagnostics panel compact: state, expiry, terminal/app summary, controls, recent events, and sanitized detail.
- [ ] Add web tests for permissions, enable/disable flow, expiry display, and redacted event rendering.

## 4. Operations

- [ ] Document a support workflow: enable diagnostics, reproduce issue, inspect events, disable diagnostics.
- [ ] Define max activation duration, 7-day retention, max events, queue size, and rate limits in ops docs or near-code docs.
- [ ] Add rollout notes explaining that this is not always-on analytics.

## 5. Validation

- [ ] Backend focused tests pass.
- [ ] Mobile focused tests pass.
- [ ] Web focused tests pass.
- [ ] Manual staging smoke: enable diagnostics for one terminal, reproduce a print error, inspect event, disable diagnostics.
