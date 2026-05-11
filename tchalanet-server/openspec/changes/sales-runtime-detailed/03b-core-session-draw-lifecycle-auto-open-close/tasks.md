# Tasks

## 1. Add outlet/session config needed for automation

- [ ] Ensure `outlet` has:
  - `auto_open_session`
  - `auto_close_session`
  - `require_opening_float`
  - `default_opening_float_cents` optional
  - `auto_session_user_id` optional, if using one system/seller user
  - `auto_session_terminal_id` optional, usually virtual terminal
  - `business_day_cutoff`
  - `timezone`
- [ ] If one outlet can auto-open sessions for multiple sellers, create explicit config:
  - `outlet_auto_session_seller` table or reuse outlet-user assignment with a flag.
- [ ] Decide MVP default:
  - one auto session per outlet/system seller, OR
  - one auto session per assigned seller.
- [ ] Recommended MVP:
  - one auto session per seller only if that seller is assigned and enabled for auto-session
  - otherwise no auto-open.

## 2. Define auto-open trigger

Auto-open can be triggered in two ways:

### 2.1 Draw lifecycle hook

- [ ] When `OpenDueDrawsCommand` or draw scheduler opens the first sellable draw for the outlet/tenant business day, publish/use an event or call a session command after commit.
- [ ] Event example:
  - `DrawOpenedEvent`
  - or existing draw lifecycle event if available
- [ ] Listener in `core.session.infra.event` maps draw opened → `AutoOpenSalesSessionsCommand`.

### 2.2 Scheduler tick

- [ ] Add scheduler/ops command as safer MVP:
  - `AutoOpenSalesSessionsForBusinessDayCommand`
- [ ] Scheduler runs after draw open scheduler.
- [ ] It scans outlets with auto-open enabled and open sellable draws.
- [ ] It creates missing sessions only.

## 3. Auto-open command

- [ ] Add `AutoOpenSalesSessionsCommand`.
- [ ] Inputs:
  - `TenantId`
  - optional `OutletId`
  - `LocalDate businessDate`
  - `SessionAutomationReason`
  - `force` boolean for Ops only
- [ ] Handler must:
  - load outlet operational config
  - check `auto_open_session=true`
  - check outlet sales not blocked/day not closed
  - check at least one OPEN draw for the outlet/tenant business date
  - resolve sellers eligible for auto-open
  - resolve default/virtual terminal per seller/outlet
  - skip if seller already has OPEN session
  - create SalesSession with `source=SCHEDULER`
  - set opening amount using config/default
  - audit/record skipped reasons
- [ ] Do not fail whole batch if one outlet/seller is invalid; return result summary.

## 4. Manual open remains primary override

- [ ] Seller can call `POST /tenant/sessions/open` before scheduler.
- [ ] If session exists, return current session instead of duplicate or return conflict with clear code.
- [ ] Manual opening amount wins.
- [ ] Scheduler must skip sellers with existing OPEN sessions.

## 5. Determining “last slot / last draw”

Important: do not guess by provider name. Use tenant draw schedule data.

Define “last relevant draw” for an outlet/tenant business day as:

```text
There are no OPEN sellable draws remaining for the tenant/outlet business date
AND all draws scheduled for that business date that were OPEN have reached CLOSED/RESULTED/SETTLED/CANCELLED
AND no sell operation is currently in progress/pending approval for that seller/session.
```

Implementation options:

### 5.1 Query-based preferred MVP

- [ ] Add core.draw query:
  - `HasOpenDrawsForTenantBusinessDateQuery`
  - `ListOpenDrawsForTenantBusinessDateQuery`
  - `HasFutureSellableDrawsForTenantBusinessDateQuery`
  - `GetLastSellableDrawCloseTimeQuery` optional
- [ ] The session auto-close handler asks draw:
  - are there open draws for this tenant/date?
  - are there future sellable draws that should open later today?
- [ ] If no open or future sellable draws remain, session may auto-close.

### 5.2 Slot/channel-based calculation

- [ ] Use draw_channel/result_slot schedule:
  - draw date
  - draw time
  - timezone
  - cutoff
  - status
- [ ] Last slot is max scheduled/cutoff time among active draw channels for tenant business date.
- [ ] Final decision still checks actual draw statuses, not only clock.

## 6. Auto-close trigger

Auto-close can be triggered:

### 6.1 Draw close/apply/settle lifecycle

- [ ] After `CloseDueDrawsCommand`, run/session command:
  - `AutoCloseSalesSessionsCommand`
- [ ] Optionally after settlement tick too, but do not wait for payout.

### 6.2 Scheduler tick

- [ ] Add scheduler/ops command:
  - `AutoCloseSalesSessionsForBusinessDayCommand`
- [ ] Runs after close/apply/settle windows.
- [ ] It can run every 5-15 minutes during close window.

## 7. Auto-close command

- [ ] Inputs:
  - `TenantId`
  - optional `OutletId`
  - `LocalDate businessDate`
  - `force`
  - `reason`
- [ ] Handler must:
  - list OPEN sessions with `source=SCHEDULER` or auto-close eligible manual sessions if allowed by config
  - check outlet `auto_close_session=true`
  - check no OPEN draws remain for business date
  - check no future sellable draws remain for business date
  - check no ticket sale command in progress if idempotency table exposes in-progress status
  - check no pending approval that must block session closure, if sales approvals are session-bound
  - close session with `source=SCHEDULER`
  - compute closing amount:
    - for auto sessions: default/calculated summary
    - for manual sessions: do not auto-close unless configured
- [ ] Do not wait for payout.
- [ ] Return summary with closed/skipped reasons.

## 8. Manual close remains allowed

- [ ] Seller can close any time if business rules allow.
- [ ] Manual close should not require all draws settled.
- [ ] If seller closes while draws still open:
  - no new sales can be made by that session
  - seller may open a new session if allowed
- [ ] Tickets remain attached to original session.

## 9. Pending work checks

Before auto-close, check:

- [ ] no open draw remains for the seller/outlet business day
- [ ] no sale idempotency in progress for that seller/session
- [ ] no pending sale approval tied to that session if closing would orphan required action
- [ ] no offline unsynced ticket sale for that terminal/session if offline MVP supports local ticket creation

MVP fallback:

- [ ] If offline sync is not fully implemented, skip auto-close for sessions with terminal `sync_state=SYNC_PENDING` or `SYNC_CONFLICT`.

## 10. Ops endpoints

Add restricted endpoints, not seller endpoints:

- [ ] `POST /admin/sessions/auto-open`
- [ ] `POST /admin/sessions/auto-close`
- [ ] `GET /admin/sessions/automation/preview`
- [ ] `GET /admin/sessions/automation/status`

Request fields:

- `outletId` optional
- `businessDate` optional; default tenant/outlet today
- `force`
- `reason`
- `dryRun`

## 11. Scheduler placement

- [ ] Scheduler lives in `core.session.infra.batch` or `core.session.infra.scheduler`.
- [ ] Scheduler does not contain business logic.
- [ ] Scheduler calls command bus.
- [ ] Scheduler checks `BatchGate`/feature flag:
  - `tch.session.auto-open.active`
  - `tch.session.auto-close.active`
- [ ] Scheduler uses tenant/outlet timezone explicitly.
- [ ] Scheduler uses injected `Clock`.

## 12. Events

- [ ] Publish `SalesSessionOpenedEvent` after commit.
- [ ] Publish `SalesSessionClosedEvent` after commit.
- [ ] Include:
  - tenantId
  - sessionId
  - outletId
  - userId
  - source MANUAL/SCHEDULER/OPS
  - occurredAt
- [ ] Stats/projections consume idempotently.

## 13. Read endpoints required for operators

Do not rely on overview only.

- [ ] `GET /admin/sessions/open`
- [ ] `GET /admin/sessions?status=OPEN&outletId=...`
- [ ] `GET /admin/sessions/{sessionId}`
- [ ] `GET /admin/sessions/{sessionId}/totals`
- [ ] `GET /admin/sessions/automation/status`
- [ ] `GET /admin/sessions/automation/preview`

## 14. Tests

- [ ] Auto-open creates session when first draw is open.
- [ ] Auto-open skips if seller already has OPEN session.
- [ ] Auto-open skips if outlet requires opening float and no default allowed.
- [ ] Manual open before scheduler is preserved.
- [ ] Auto-close skips while open draw remains.
- [ ] Auto-close closes after last draw of business day is closed.
- [ ] Auto-close does not wait for payout.
- [ ] Auto-close skips if terminal sync pending/conflict.
- [ ] Tickets remain attached to session after close.
- [ ] Seller can manually close before auto-close.
