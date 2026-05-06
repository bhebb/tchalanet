# Tchalanet V1 — Flows critiques & Analyse d'architecture

**Date**: 2026-05-05  
**Scope**: Core domains: `sales`, `draw`, `drawresult`, `uslottery`, `haiti`  
**Focus**: V1 critical paths, typed IDs, CQRS handlers, after-commit events

---

## 1. Vue d'ensemble de l'architecture

### Couches (4-layer hexagonal)

```
common/         ← Transversal (Bus, EventPublisher, TypedIDs, Cache, TchContext)
catalog/        ← Read-mostly reference data (drawchannel, resultslot, i18n, settings)
core/           ← Business domains (sales, draw, drawresult, uslottery, haiti)
features/       ← Vertical slices / BFF (ops, tenantadmin, publicdraw, reporting)
```

### Infrastructure (top-level)

- **API version**: `/api/v1`
- **Request context**: `TchRequestContext` (tenant, user, roles)
- **Security**: OAuth2 (Keycloak) + RLS (PostgreSQL Row-Level Security)
- **Transactions**: `@TchTx` annotation → marked as critical write path
- **Events**: Published via `AfterCommit.run()` to avoid transaction conflicts

---

## 2. Domaines critiques & Flows principaux

### 2.1 SALES — Ticket placement & lifecycle

**Domaine**: `core/sales` (tenantId: tenant-scoped)

**Entités clés**:

- `Ticket` (agregate root, JPA entity, soft-delete capable)
- `TicketLine` (value object, multiplex selection/stakes/odds)
- `TicketSaleStatus` (enum: PLACED, PENDING_APPROVAL, APPROVED, VOID, PAID, etc.)

**Flows critiques**:

#### 2.1.1 Sell Ticket Flow

```
Command:    SellTicketCommand(tenantId, terminalId, drawId, currency, lines[])
Handler:    SellTicketCommandHandler (@TchTx)
Port Out:   TicketWriterPort (JPA adapter)

Steps:
1. prepareSale()       → policy check (limits, session, draw state, currency)
2. Check breach outcome:
   - BLOCK           → newPendingApprovalTicket() + ApiNotice
   - WARN            → newSoldTicket() + SellTicketOutcome.SUCCESS_WITH_WARNINGS
   - OK              → newSoldTicket() + SellTicketOutcome.SUCCESS
3. Validate single game_code per ticket (MVP constraint)
4. Map to TicketPlacedEvent (in cents, with lines detail)
5. AfterCommit.run(() → publish(TicketPlacedEvent))
6. Return SellTicketResult(ticket, outcome, approvalRequestId?)

Typed IDs used:
- TenantId, TicketId, TerminalId, DrawId, EventId, AgentId
- Never: raw UUID except in domain.event and JPA entities

Event published after commit:
  TicketPlacedEvent(
    eventId:EventId, occurredAt:Instant,
    tenantId:TenantId, ticketId:TicketId, outletId:OutletId,
    agentId:AgentId, terminalId:TerminalId, sessionId:SessionId,
    drawId:DrawId, drawChannelId:DrawChannelId,
    gameCode:String, totalAmount:long (cents), currency,
    lines: List<Line> {betType, selection, stake, payout, betOption}
  )

Listeners downstream:
  - Ledger (accounting side-effect)
  - Stats aggregation
  - Notifications (optional)
```

#### 2.1.2 Approve/Reject Ticket Flow

```
Commands:
  - ApproveTicketSaleCommand(tenantId, ticketId)
  - RejectTicketSaleCommand(tenantId, ticketId, reason)

Handlers: ApproveTicketSaleCommandHandler, RejectTicketSaleCommandHandler (@TchTx)

Steps:
1. Load ticket (PENDING_APPROVAL state)
2. Transition to APPROVED or REJECTED
3. Publish event after commit

Events:
  - TicketPaymentPendingEvent (on approve)
  - TicketCancelledEvent (on reject)
```

#### 2.1.3 Record Draw Results (Settlement-scoped)

```
Command:    RecordDrawTicketsResultCommand(tenantId, drawId, resultInfo)
Handler:    RecordDrawTicketsResultCommandHandler (@TchTx)

Flow:
1. For each ticket matching drawId:
   - Load draw result (via drawresult.domain.model.DrawResult)
   - Calculate winner status
   - Set ticketStatus to PAID or LOST
2. Publish TicketResultedEvent per ticket
3. Side-effect: Ledger + Payout queue

Event: TicketResultedEvent(tenantId, ticketId, drawId, resultStatus, payout)
```

**Key constraints**:

- ✅ Typed IDs everywhere (domain/application/dtos)
- ✅ @TchTx on all write handlers
- ✅ AfterCommit for events
- ✅ RLS filters by tenant_id at DB level
- ✅ Soft-delete compatible (is_deleted flag)
- ✅ No raw UUID outside persistence layer

---

### 2.2 DRAW — Draw lifecycle & scheduling

**Domaine**: `core/draw` (tenantId: tenant-scoped)

**Entités clés**:

- `Draw` (aggregate root, JPA entity)
- `DrawStatus` (enum: SCHEDULED, OPEN, CLOSED, RESULTED, SETTLED, CANCELED)

**Flows critiques**:

#### 2.2.1 Generate Draws Flow

```
Command:    GenerateDrawsForRangeCommand(tenantId, fromDate, toDate, drawChannelId)
Handler:    GenerateDrawsForRangeCommandHandler (@TchTx)

Steps:
1. Load drawChannel from catalog
2. For each date in range:
   - Create Draw aggregate per slot (if not exist)
   - Persist via DrawLifecyclePort
3. Return list of created draw IDs
4. No event published (batch operation)
```

#### 2.2.2 Open/Close Draws Flow

```
Commands:
  - OpenDueDrawsCommand(tenantId, baseDateTime)
  - CloseDueDrawsCommand(tenantId, baseDateTime)

Handlers: OpenDueDrawsCommandHandler, CloseDueDrawsCommandHandler (@TchTx)

Steps:
1. Query draws with status SCHEDULED (open) or OPEN (close)
2. Check draw.dueAt <= baseDateTime
3. Transition draw.status
4. Bulk save via DrawLifecyclePort
5. No event published (batch operation)
```

#### 2.2.3 Apply Draw Result Flow

```
Command:    ApplyExternalResultsWindowCommand(tenantId, resultSlotKey, resultId)
Handler:    ApplyExternalResultsWindowCommandHandler (@TchTx)

Steps:
1. Load DrawResult from drawresult domain
2. Load Draw aggregate (tenant-scoped via RLS)
3. Attach draw_result_id to draw
4. Set draw.status = RESULTED
5. Save draw
6. Publish DrawResultAppliedEvent after commit

Event: DrawResultAppliedEvent(
  eventId, occurredAt,
  tenantId, drawId, drawChannelId, resultSlotId, drawResultId,
  drawDate, scheduledAt
)

Listeners downstream:
  - Ticket settlement job
  - Draw stats
```

#### 2.2.4 Settle Draw Flow

```
Command:    SettleDrawCommand(tenantId, drawId)
Handler:    SettleDrawCommandHandler (@TchTx) [VoidCommandHandler]

Steps:
1. Load DrawSummary (cached view with result status)
2. Validate: result != null && result.status == CONFIRMED
3. Reload Draw aggregate (pessimistic lock candidate)
4. Call draw.settle(now)
5. Save draw via DrawLifecyclePort
6. If draw was RESULTED before: publish DrawSettledEvent after commit

Event: DrawSettledEvent(
  eventId, occurredAt,
  tenantId, drawId, drawChannelId, resultSlotId, drawResultId,
  drawDate, scheduledAt
)

Listeners downstream:
  - Ticket settlement finalization
  - Payout ledger
```

**Key constraints**:

- ✅ Draw is tenant-scoped (RLS enforced)
- ✅ Draw domain pure (no Spring/JPA, only aggregates)
- ✅ Typed IDs (DrawId, TenantId, DrawChannelId, ResultSlotId)
- ✅ @TchTx on all write handlers
- ✅ After-commit event publishing for side-effects
- ✅ Result attachment via result_slot, not by channel_code

---

### 2.3 DRAWRESULT — Global result ingestion & orchestration

**Domaine**: `core/drawresult` (global, not tenant-scoped)

**Entités clés**:

- `DrawResult` (aggregate root, JPA entity, global scope)
- `DrawResultStatus` (enum: PROVISIONAL, CONFIRMED, OVERRIDDEN)
- `DrawResultUpsertOutcome` (created, updated, skipped, skippedConfirmed, skippedOverridden)

**Flows critiques**:

#### 2.3.1 Fetch External Results Window Flow

```
Command:    FetchExternalResultsWindowCommand(
              baseDate, daysBack, maxSlots, slotKeys[],
              includeRaw, force, dryRun, reason
            )
Handler:    FetchExternalResultsWindowCommandHandler (@TchTx)

Steps (per slot, per date):
1. Resolve ResultSlot from catalog (by slotKey, validate active)
2. Resolve source config (which provider, which games)
3. Fetch external result via ExternalResultFetcher (HTTP)
4. Project to haiti normalization (numbers, meta)
5. Assemble DrawResultPersistence (source, haiti, raw, flags, quality, sourceHash)
6. Upsert draw_result via DrawResultWriterPort
   - Key: (resultSlotId, occurredAt)
   - Idempotency: sourceHash deduplication
   - Validation: skip if CONFIRMED (unless force=true)
7. Count: inserted, updated, skipped, errors
8. Return FetchExternalResultsWindowResult(counters)

Key validations:
- daysBack clamped to props.limits.hardDaysBack (e.g., 7)
- maxSlots clamped to props.limits.maxSlotsPerTick (e.g., 100)
- normalizeKey(slotKey) → upper-trim
- dry-run mode counts without persisting

Idempotency mechanism:
- Upsert by (result_slot_id, occurred_at)
- Source hash detects duplicates / version updates
- Status CONFIRMED blocks re-fetch (unless force=true for manual override)

No events published (raw fetch stage)
```

#### 2.3.2 Record Manual Draw Result Flow

```
Command:    RecordManualDrawResultCommand(tenantId, resultSlotId, manualNumbers, force, reason)
Handler:    RecordManualDrawResultCommandHandler (@TchTx)

Steps:
1. Validate slot exists and active
2. Validate manual numbers
3. Assemble DrawResult from manual input
4. Upsert with DrawSource.MANUAL (vs EXTERNAL)
5. Return upsert outcome
6. Publish DrawResultIngestedEvent after commit (if created/updated)
```

#### 2.3.3 Override Draw Result Flow

```
Command:    OverrideDrawResultCommand(resultSlotId, overrideNumbers, reason)
Handler:    OverrideDrawResultCommandHandler (@TchTx)

Steps:
1. Load existing DrawResult
2. Update status to OVERRIDDEN
3. Update numbers to override values
4. Audit trail (reason, timestamp)
5. Publish DrawResultIngestedEvent
6. Trigger eviction of related caches (tenant draw caches)

Event: DrawResultIngestedEvent (published after commit)
```

**Key constraints**:

- ✅ DrawResult is global, not tenant-scoped
- ✅ Upsert by (result_slot_id, occurred_at)
- ✅ Fetch by result_slot_key, not draw_channel_code
- ✅ Source hash for idempotency
- ✅ Force override only with reason (audit)
- ✅ CONFIRMED status is terminal (blocks re-fetch unless explicit force)
- ✅ No tenant-scoping on fetch/persist logic

---

### 2.4 USLOTTERY — US Lottery provider integration

**Domaine**: `core/uslottery` (application/integration layer only; no domain model yet)

**Config**: `application-uslottery.yaml`

```yaml
tch.us-lottery:
  enabled: true
  providers:
    ny:
      enabled: true
      base-url: https://data.ny.gov/api/v3/views/...
      app-token: <token>
    fl:
      enabled: true
      base-url: https://apim-website-prod-eastus.azure-api.net
      headers: { Accept, x-partner, Origin, Referer, User-Agent }
    ga:
      enabled: true
      base-url: https://www.galottery.com
    tx:
      enabled: true
      base-url: https://www.texaslottery.com
```

**Flows**:

#### 2.4.1 Provider Query Flow

```
Query:      UsLotteryProviderQuery(providerCode, resultSlotKey)
Handler:    (Not yet in codebase; port-driven)
Port Out:   UsLotteryProviderQuery (interface)

Design:
- Hydrate provider config (URL, headers, credentials)
- Fetch external result (HTTP, with circuit-breaker)
- Parse JSON/XML to normalized { numbers, meta }
- Return or throw external exception

Not called directly; used by drawresult.ExternalResultFetcher
```

**Key constraints**:

- ✅ Provider config driven by application-uslottery.yaml
- ✅ No tenant-scoping (shared provider queries)
- ✅ Circuit-breaker / retry patterns (if any)
- ✅ External calls outside transaction (if possible) or early in @TchTx

---

### 2.5 HAITI — Number normalization & dream-to-numbers mapping

**Domaine**: `core/haiti` (sub-domains: `tchala`, `lottery`)

**Sub-domains**:

1. **Tchala**: Dream number dictionary, user submissions, approvals

   - Entities: TchalaEntry, TchalaSuggestion
   - Flows: Import, Approve, Reject, Merge, Delete

2. **Lottery**: Result numbers, dream normalization
   - Service: HaitiProjectionService (projects external results to haiti space)

**Flows**:

#### 2.5.1 Import Tchala Entries Flow

```
Command:    ImportTchalaEntriesCommand(tenantId, entries[])
Handler:    ImportTchalaEntriesCommandHandler (@TchTx)

Steps:
1. Bulk load entries from CSV/JSON
2. Validate dream numbers (range, format)
3. Create TchalaEntry per entry
4. Set status to PENDING_APPROVAL
5. Persist via TchalaWriterPort
6. Return import summary (created, errors)

No events published
```

#### 2.5.2 Approve Tchala Entry Flow

```
Command:    ApproveTchalaEntryCommand(tenantId, entryId)
Handler:    ApproveTchalaEntryCommandHandler (@TchTx)

Steps:
1. Load entry (PENDING_APPROVAL status)
2. Validate entry
3. Set status to APPROVED
4. Save

No events published (internal state)
```

#### 2.5.3 Query Tchala by Dream or Number

```
Query:      GetTchalaByNumberQuery(number) or SearchTchalaQuery(dreamText)
Handler:    GetTchalaByNumberQueryHandler, SearchTchalaQueryHandler

Steps:
1. Query TchalaRepository (read-only, RLS applied if tenant-scoped)
2. Return matching TchalaEntryView (DTO, cached potentially)
```

#### 2.5.4 Haiti Projection Service (Core business logic)

```
Service:    HaitiProjectionService
Method:     project(resultSlot, date, externalResult) → HaitiResult

Purpose:
- Normalize external result numbers to haiti space
- Apply dream-to-number mapping
- Validate against schema

Used by: FetchExternalResultsWindowCommandHandler (step 4)
```

**Key constraints**:

- ✅ Tchala: tenant-scoped (approvals per tenant)
- ✅ Lottery: global (shared normalization logic)
- ✅ HaitiProjectionService: pure function, no I/O
- ✅ No events published (reference data lifecycle)
- ✅ Typed IDs: TenantId, TchalaEntryId

---

## 3. Configuration & Profiles

### 3.1 Base Configuration (`application.yaml`)

```yaml
app:
  version: 0.1.0
  api-version: v1
  base-path: /api
  default-tenant: tchalanet
  zone-id: UTC

spring:
  datasource: PostgreSQL (Hikari pool, 10 max)
  jpa: Hibernate (ddl-auto=validate, defer-init=true)
  batch: Job disabled by default (launch via API)
  flyway: Migration from classpath:db/migration
  security: OAuth2 resource server (Keycloak)

tch.cache.redis.enabled: false (opt-in)
```

### 3.2 Draw Profile (`application-draw.yaml`)

```yaml
tch.draw:
  scheduler:
    active: true
    windows:
      fetch_results: "12:00-14:00,20:00-23:00" (NY timezone)
      apply_results: "12:00-14:30,20:00-23:30"
      settle_draws: "12:00-15:00,20:00-23:30"
      close_draws: "11:30-14:00,19:30-23:00"
      open_draws: "02:00-06:00"

  lifecycle:
    generate_cron: "0 0 5 * * *" (5am UTC daily)
    generation_days: 7 (lookahead)

  results:
    scheduler: Cron-based fetch (5-min intervals)
    limits:
      max_slots_per_tick: 100
      hard_days_back: 7
```

### 3.3 US Lottery Profile (`application-uslottery.yaml`)

```yaml
tch.us-lottery:
  providers: ny, fl, ga, tx (each with base-url, headers, creds)
```

---

## 4. Erreurs évidentes & Améliorations

### 4.1 Erreurs détectées

#### Fichier corrompu: `ARCHITECTURE.md`

**Issue**: Duplication massive et corruption du texte aux lignes 49-200.

```
Exemple (ligne 49-62):
## Règl# Tchalanet – Architecture Applicadr##deCe document décrit l'architecture...
[CORRUPTION: duplication, caractères inversés, texte fragmenté]
```

**Action**: ✅ Reconstruire le fichier (voir section 5 ci-dessous)

#### Missing CLAUDE.md files

- `core/haiti/CLAUDE.md` → À créer
- `core/uslottery/CLAUDE.md` → À créer

#### Naming: Prefix inconsistency in QueryHandler classes

- `GetDrawByIdQueryHandler` → OK
- `ListDrawsHandler` → Non-standard (should be `ListDrawsQueryHandler`)

**Action**: Renommer pour cohérence

---

### 4.2 Améliorations architecturales (non-urgentes)

1. **Pessimistic locking on SettleDrawCommandHandler**

   - Comment suggests: "Ideally this should be pessimistic lock / FOR UPDATE"
   - Action: Add Hibernate @Lock(LockModeType.PESSIMISTIC_WRITE) on load

2. **Cache strategy review**

   - Currently Redis disabled by default
   - Recommendation: Enable for draw/tchala lookups (read-mostly)

3. **Batch job scheduling**
   - Currently no auto-run; driven via ops endpoints
   - Status: OK for V1 (manual control better for operations)

---

## 5. Fixes proposées

### 5.1 Fix ARCHITECTURE.md corruption

**File**: `tchalanet-server/docs/ARCHITECTURE.md`

**Action**: Clean up duplicated/corrupted lines (49-200)

The file has massive text duplication starting at line 49. Rebuilding section by section is recommended.

### 5.2 Rename List*Handler → List*QueryHandler

**Files to rename**:

- `core/draw/application/query/handler/ListDrawsHandler.java`
  → `ListDrawsQueryHandler.java`

**Rationale**: Consistency with naming convention (all query handlers should end in `QueryHandler`)

### 5.3 Create missing CLAUDE.md files

**File**: `core/haiti/CLAUDE.md`

```markdown
# Claude — core.haiti

Scope:

- Tchala (dream-to-number mapping)
- Lottery (result normalization)
- Tenant-scoped approvals (Tchala)
- Global normalization (Lottery)

Rules:

- Use HaitiProjectionService for result projection
- No HTTP calls (external fetching is uslottery/drawresult concern)
- Approve/reject flow is tenant-scoped
- Use typed IDs (TenantId, TchalaEntryId)

Before editing:

- Load CLAUDE.md in core/sales for ticket result matching
- Load core/drawresult for HaitiProjectionService usage
```

**File**: `core/uslottery/CLAUDE.md`

```markdown
# Claude — core.uslottery

Scope:

- Provider HTTP clients (NY, FL, GA, TX)
- Result parsing and normalization
- No persistence (query ports only)

Rules:

- Config via application-uslottery.yaml
- Use circuit-breaker for external calls
- Never tenant-scoped
- Return normalized result (not persisted locally)

Before editing:

- Load FetchExternalResultsWindowCommandHandler for integration points
- Load HaitiProjectionService for normalization contract
```

---

## 6. Flows critiques V1 (résumé visuels)

### Flow 1: Ticket Sale → Settlement

```
[SellTicketCommand]
    ↓ (SellTicketCommandHandler @TchTx)
[TicketPlacedEvent] → AfterCommit → Ledger, Stats
    ↓
[RecordDrawTicketsResultCommand]
    ↓ (RecordDrawTicketsResultCommandHandler @TchTx)
[TicketResultedEvent] → AfterCommit → Payout
```

### Flow 2: Draw Result → Draw Settlement

```
[FetchExternalResultsWindowCommand]
    ↓ (FetchExternalResultsWindowCommandHandler @TchTx)
[DrawResult upserted] (global, PROVISIONAL)
    ↓
[ApplyExternalResultsWindowCommand]
    ↓ (ApplyExternalResultsWindowCommandHandler @TchTx)
[DrawResultAppliedEvent] → AfterCommit
    ↓
[SettleDrawCommand]
    ↓ (SettleDrawCommandHandler @TchTx)
[DrawSettledEvent] → AfterCommit → Settlement finalization
```

### Flow 3: Draw Lifecycle Scheduler

```
[Cron: 5am UTC]
    ↓
[GenerateDrawsForRangeCommand] (7-day lookahead)
    ↓
[Cron: 5-min intervals]
    ↓
[OpenDueDrawsCommand / CloseDueDrawsCommand] (batch)
    ↓
[DrawStatus transitions] (SCHEDULED→OPEN→CLOSED→RESULTED→SETTLED)
```

### Flow 4: Result Fetch & Normalization

```
[Cron: 5-min intervals, 12:00-14:00, 20:00-23:00]
    ↓
[FetchExternalResultsWindowCommand]
    ↓
[ExternalResultFetcher] (HTTP calls to NY, FL, GA, TX)
    ↓
[HaitiProjectionService] (normalize to dream space)
    ↓
[DrawResult upsert] (by result_slot_id, occurred_at)
```

---

## 7. Checklist de validation

- [x] Typed IDs everywhere (domain/application/dtos)
- [x] @TchTx on all write handlers
- [x] AfterCommit.run() for cross-domain side-effects
- [x] RLS row-level filtering (PostgreSQL tenant_id policies)
- [x] No raw UUID outside persistence
- [x] Controllers thin (validation + dispatch only)
- [x] CQRS separation (Command/Query handlers)
- [x] Event publishing after commit (idempotency safe)
- [ ] ARCHITECTURE.md needs repair (corruption lines 49-200)
- [ ] ListDrawsHandler → ListDrawsQueryHandler (naming consistency)
- [ ] haiti/CLAUDE.md missing
- [ ] uslottery/CLAUDE.md missing

---

## 8. Prochaines étapes

1. **Fix ARCHITECTURE.md** (priority: HIGH)

   - Remove duplication, restore clarity
   - Estimated effort: 1-2 hours

2. **Rename query handlers** (priority: MEDIUM)

   - Single file rename
   - Estimated effort: 15 mins + tests

3. **Create CLAUDE.md files** (priority: MEDIUM)

   - haiti and uslottery scoping
   - Estimated effort: 30 mins

4. **Review pessimistic locking** (priority: LOW)
   - SettleDrawCommandHandler optimization
   - Estimated effort: 1 hour + testing

---

**Generated by**: Claude Code  
**Status**: Analysis complete, ready for fixes
