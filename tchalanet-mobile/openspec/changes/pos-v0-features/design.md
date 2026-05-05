# Design — `pos-v0-features`

> References: [proposal](proposal.md) · [Spec 01 BFF endpoints](specs/bff-endpoints/spec.md) · [Spec 02 multi-entry](specs/sales-multi-entry/spec.md) · [Spec 03 Flutter contract](specs/flutter-integration/spec.md) · [Spec 04 Screens](specs/pos-screens/spec.md)

---

## Context

`pos-v0-foundation` delivered the data model for sessions, outlets, terminals, users, limit policies, autonomy rules, settings, permissions, and audit. No HTTP surface was exposed yet.

This change ships the **operational layer** in two parts:

1. **Backend** — `features.pos` BFF (17 endpoints), `core.sales` extension (multi-entry `Ticket`/`TicketEntry` + 3 command handlers + read projections), Flyway migration, BFF-level cache.
2. **Flutter** — 10 screens, Riverpod providers/repositories, `AppSession` store, error mapping, build flavors.

Primary constraint: the Flutter POS app runs on Android 10 phones and PHYSICAL terminal devices. The BFF must be a **single aggregation point** per screen — no client fan-out.

---

## Goals / Non-Goals

**Goals:**

- Ship 17 BFF endpoints covering all 10 POS screens under `/api/v1/tenant/pos/...`
- Extend `core.sales` with the multi-entry `Ticket`/`TicketEntry` domain model and 3 command handlers (`PlaceTicket`, `ApproveBlockedTicket`, `CancelTicket`)
- 4 read-model projections (dashboard KPIs, draw-with-stats, recent results, last ticket) maintained by event listeners
- Flutter: 10 screens, complete server-driven authorization, idempotent mutations, offline degradation banner
- BFF-level cache with named caches and event-driven eviction

**Non-Goals:**

- Offline sales (v1)
- Landscape optimization for tablets (v1)
- WhatsApp / PUSH notification channels (marked as unimplemented in edge service)
- Payout flow, draw settlement — handled by other changes
- Multi-currency support (HTG only in v0)
- Reporting screen beyond Results S8 (v1)

---

## Decisions

### D1 — `drawId` moves from `ticket` to `ticket_entry`

**Decision**: `Ticket` is the aggregate; `TicketEntry` hosts `drawId`, `betTypeCode`, `selection`, `amountHtg`. One ticket can target multiple draws.

**Rationale**: Multi-entry is a product requirement (agent places NY Bolet + FL Borlette in one transaction). Keeping `drawId` on `ticket` would require either multiple tickets per UX intent or a fake "combo draw" — both worse.

**Alternative considered**: Multiple single-entry tickets per tap on "Ajouter". Rejected: fragments idempotency, complicates the approval flow (which block do we show?), and breaks the UX receipt which shows all entries together.

---

### D2 — New Flyway migration for `ticket` and `ticket_entry`

**Decision**: `ticket` and `ticket_entry` are new tables in a new migration file (next available version). They are NOT renames of any existing table.

**Rationale**: No existing `ticket` table in the schema. Clean slate avoids ALTER complexity and makes rollback trivial (drop tables).

**Migration also includes**: `ticket_aud`, `ticket_entry_aud` (Envers), all indexes, RLS policies.

---

### D3 — BFF services delegate 100% to CommandBus / QueryBus — no domain logic

**Decision**: `PosSaleBffService.place(...)` calls `commandBus.send(PlaceTicketCommand(...))` and maps the result to a DTO. No business rule evaluation in BFF layer.

**Rationale**: Architecture rule (AGENTS.md §3 + Spec 01). BFF services are orchestration adapters, not a second domain layer. All invariants live in `core.sales` handlers.

**Alternative considered**: BFF service computing fees or calling limit policy directly. Rejected: creates hidden duplicate logic path and breaks testability of the domain.

---

### D4 — PENDING_APPROVAL returned inline, not as exception

**Decision**: `PlaceTicketCommand` handler returns a `PlaceTicketResult` discriminated union: `PLACED | PENDING_APPROVAL | BLOCKED`. The BFF maps `PENDING_APPROVAL` to HTTP 200 with `state = PENDING_APPROVAL` in the response body.

**Rationale**: An approval requirement is a valid business outcome, not an error. Throwing a 422 forces the Flutter client to parse exceptions for flow logic. A 200 with `state` keeps the protocol clean.

**Implication**: `TicketPlacedEvent` is NOT emitted on `PENDING_APPROVAL`; emitted only after `ApproveBlockedTicketCommand` succeeds.

---

### D5 — Read projections maintained by listeners, not live queries

**Decision**: Dashboard KPIs (`salesDayHtg`, `ticketCount`, `avgTicketHtg`, `balanceHtg`) are stored in `sales_session_counters` (provisioned by `pos-v0-foundation`). They are updated by `SalesSessionTicketCountersListener` on `TicketPlacedEvent`, `TicketCancelledEvent`, `SalesSessionClosedEvent`. The BFF reads projections; it does not aggregate on-the-fly.

**Rationale**: Live aggregation of all tickets per session on every dashboard load is O(n) and incompatible with the 30 s refresh requirement under load. Listener updates are O(1) per event.

**Trade-off**: Read projections are eventually consistent (± one event processing delay). Acceptable for a dashboard KPI; exact totals are on the z-report.

---

### D6 — BFF-level cache with event-driven eviction (5 named caches)

**Decision**:

| Cache                   | Key                                         | TTL   | Eviction trigger                              |
| ----------------------- | ------------------------------------------- | ----- | --------------------------------------------- |
| `pos.bff.dashboard`     | `(tenantId, agentId)`                       | 30 s  | `TicketPlacedEvent`, `DrawSettledEvent`       |
| `pos.bff.sale_context`  | `(tenantId, outletId)`                      | 60 s  | `SettingChangedEvent`, `DrawClosedEvent`      |
| `pos.bff.ticket_detail` | `ticketId`                                  | 60 s  | `TicketCancelledEvent`, `TicketApprovedEvent` |
| `pos.bff.results`       | `(tenantId, date, lotteryCode)`             | 5 min | `DrawSettledEvent`                            |
| `pos.bff.settings`      | `(tenantId, outletId, terminalId, agentId)` | 60 s  | `SettingChangedEvent`                         |

Caffeine (L1) + Redis (L2). Eviction listeners run after-commit.

**Rationale**: Dashboard is refreshed every 30 s by every active agent. Without cache, this is a cross-domain query cascade on every tick. Cache reduces this to a Redis hit.

---

### D7 — `X-Tch-Platform: MOBILE` triggers lazy VIRTUAL terminal provisioning on bootstrap

**Decision**: `GET /bff/bootstrap` with `X-Tch-Platform: MOBILE` dispatches `ProvisionVirtualTerminalCommand` only if no ACTIVE VIRTUAL terminal exists for the agent. The newly provisioned terminal is returned inline in the bootstrap response.

**Rationale**: Agents on phones shouldn't need an administrator to pre-create a terminal. PHYSICAL terminals must already exist (registered by admin) — the header distinguishes the two paths.

---

### D8 — Flutter: Riverpod `StateNotifier` for `DraftEntry` list; `AsyncNotifierProvider` for all BFF reads

**Decision**:

- `DraftEntryNotifier extends StateNotifier<List<DraftEntry>>` — local only, no persistence.
- All BFF reads use `AsyncNotifierProvider` (auto-dispose on screen unmount).
- `AppSessionNotifier extends StateNotifier<AppSession?>` — global, persists until logout.
- All HTTP calls go through a single `ApiClient` (Dio) that injects `Authorization`, `X-Tch-Platform`, and `Idempotency-Key` via interceptors.

**Rationale**: Consistent with `apps/tchalanet-mobile/CLAUDE.md` (Riverpod only, no Provider legacy). `AsyncNotifierProvider` provides loading/error/data states natively.

---

### D9 — Idempotency key generated by Flutter before the first call, stored in `DraftEntryNotifier`

**Decision**: The UUID is generated when the user adds the first entry. On retry (network error), the same key is reused. The key is cleared when `entries` is cleared (after PLACED or on explicit reset).

**Rationale**: Prevents duplicate tickets on connectivity drops. Aligns with Spec 01 (Idempotency-Key mandatory on B7) and Spec 03.

---

### D10 — GoRouter routes for all 10 screens

**Decision**: All 10 screens are GoRouter routes. Navigation guards check `AppSession.currentSession`:

- `currentSession == null` → redirect `/` to `/no-session`
- Tabs (Reports, History, Settings) are sub-routes of the shell route.
- Bottom nav tabs are grayed out (disabled, not hidden) when no session active, except Settings.

**Rationale**: Centralised navigation guard prevents back-button exploitation to reach sales screens without a session.

---

## Risks / Trade-offs

**[R1] TicketPlacedEvent must carry `sessionId`** — `pos-v0-foundation` originally emitted events without `sessionId`. If the existing `TicketPlacedEvent` record doesn't include it, `SalesSessionTicketCountersListener` silently skips updates.
→ **Mitigation**: Spec 02 explicitly defines the event shape. Implementation task verifies the event record before building the listener.

**[R2] Read projection staleness on high concurrency** — Multiple agents on the same session (supervisor oversight) may see a stale KPI for up to the cache TTL (30 s).
→ **Mitigation**: Acceptable for v0 (single-agent sessions are the norm). TTL is intentionally short (30 s). Exact totals are on the z-report.

**[R3] Idempotency key collision unlikely but catastrophic** — Two different tickets with the same UUID from different clients would be a security issue.
→ **Mitigation**: Key is scoped to `(tenantId, appUserId, idempotencyKey)` in the unique index. UUID v4 collision probability is negligible.

**[R4] VIRTUAL terminal proliferation** — Each MOBILE bootstrap provisioning creates a terminal row. Agents who log in repeatedly without closing sessions could accumulate VIRTUAL terminals.
→ **Mitigation**: `ProvisionVirtualTerminalCommand` checks for an existing ACTIVE VIRTUAL terminal before creating a new one (idempotent by design from `pos-v0-foundation`).

**[R5] Flutter `package:decimal` not yet in `pubspec.yaml`** — Must be added before any amount rendering.
→ **Mitigation**: Task includes explicit `flutter pub add decimal` step.

**[R6] ESC/POS printer driver integration is device-specific** — The BFF returns `ESC_POS_TEXT` lines; the Flutter plugin to send them to a PHYSICAL printer may differ by terminal model (Motorola vs generic Android).
→ **Mitigation**: v0 uses a thin `PrinterService` abstraction stub; actual driver binding deferred to printer-specific integration task. Hidden behind `actions.canReprint`.

---

## Migration Plan

1. **Backend first**: run Flyway migration, verify `ddl-auto=validate` passes on staging.
2. **BFF controllers and services**: deploy behind feature flag `pos.bff.enabled` (Unleash) — not routable until flag is on.
3. **Event listeners + projections**: deploy with backend; listeners are no-op until tickets exist.
4. **Enable feature flag** on dev environment; run BFF smoke tests via `test_curl.sh`.
5. **Flutter build**: deploy `dev` flavor pointing at dev API; run manual screen-by-screen smoke test.
6. **Enable feature flag on staging**; regression pass.
7. **Rollback**: disable feature flag; drop `ticket` / `ticket_entry` tables via inverse migration if needed (tables are new, no existing data).

---

## Open Questions

| #    | Question                                                                                                           | Owner        | Status                                                              |
| ---- | ------------------------------------------------------------------------------------------------------------------ | ------------ | ------------------------------------------------------------------- |
| OQ-1 | Does `pos-v0-foundation`'s `TicketPlacedEvent` already include `sessionId`?                                        | Backend lead | ⚠️ Must verify before building `SalesSessionTicketCountersListener` |
| OQ-2 | Printer plugin for Motorola POS: `esc_pos_utils` + `print_bluetooth_thermal` sufficient?                           | Mobile lead  | Open                                                                |
| OQ-3 | Cancel window duration configured per tenant in settings, or hardcoded?                                            | Product      | Open — assume 5 min hardcoded for v0                                |
| OQ-4 | Should `B1 bootstrap` return a PARTIAL response if `pos-v0-foundation` outlet assignment is missing, or fail hard? | Product      | Open — current spec says PARTIAL with `serviceStatus`; confirm UX   |
