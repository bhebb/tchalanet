# Design - draw and ticket notification lifecycle

## Audience boundary

| Audience | Recipients | Scope | External channels |
| --- | --- | --- | --- |
| Tenant administration | Active `TENANT_OWNER` and `TENANT_ADMIN` members of the affected tenant | Tenant notification center | WEB only by default |
| Platform supervision | Active `SUPER_ADMIN` users | Platform notification center | WEB and Slack for action-required incidents |

`SUPER_ADMIN` is not an implicit tenant administrator. A platform notification has no tenant id;
a tenant notice carries exactly one tenant id. Seller terminals and end customers are out of scope.

## Lifecycle matrix

| Domain event or state | Tenant owner/admin | Superadmin | Channel and intent | Status |
| --- | --- | --- | --- | --- |
| Draw generated, scheduled, opened or normally closed | None | None | State is visible in draw list/dashboard | Policy |
| Result missing, provider overdue, or provisional result stuck | None | Action required | WEB + Slack, deduplicated by result slot, draw date and reason | Implemented |
| Confirmed global result available for an affected tenant draw | Information: result available | None | WEB; links to tenant draw results; does not claim settlement or payment | Implemented |
| Global result corrected | Warning: result corrected, recheck results | Action required/audit warning | Tenant WEB and platform WEB; platform Slack when mapped by communication policy | Implemented |
| Result applied to a tenant draw | None | None | Processing state stays visible on draw detail | Policy |
| Ticket result calculated | None | None | High-volume technical event; outcomes remain on ticket/draw detail | Policy |
| Draw settled successfully | None | None | Normal terminal state; visible in draw detail and reports | Policy |
| Ticket sold, approved, cancelled, paid normally | None | None | Immediate command response, receipts, lists and reports are authoritative | Policy |
| Settlement/payout processing blocked after retry threshold | Warning only when tenant action is needed | Action required | WEB + Slack for platform; tenant WEB must identify the affected draw, never individual foreign tickets | Planned |

The normal path intentionally generates no per-ticket messages. A tenant with thousands of ticket
sales must not receive thousands of notifications. The result-available notification is the single
tenant-level lifecycle signal for a normal draw outcome.

## Invariants

1. Every notification listener runs after commit in a new transaction and uses the processed-event
   or notification-trigger idempotency boundary.
2. Dedupe keys use the smallest business correlation: `resultSlotId + drawDate + reason` for
   platform result incidents, and `drawResultId + tenantId + audience` for tenant result notices.
3. A result-available notice states only that a result is available. It never states that tickets
   are settled, winnings are paid, or a payout is final.
4. A ticket-level failure is aggregated by tenant draw. Per-ticket technical failures are recorded
   in observability and reconciliation, not emitted to every tenant administrator.
5. A correction keeps a separate notification from initial availability. It is never represented
   as a second "result available" notice.

## Existing implementation

`core.drawresult` currently creates the three implemented notices from global result events:

- `DRAW_RESULT_ACTION_REQUIRED` for missing/manual/overdue/provisional results, platform only;
- `DRAW_RESULT_AVAILABLE` for every affected tenant administration audience;
- `DRAW_RESULT_CORRECTED` for platform operations and each affected tenant administration audience.

The role-parity change ensures a tenant audience includes both owner and admin. The communication
bridge determines whether the platform correction warning has a Slack mapping; the notification
module itself does not send Slack.

## Planned settlement exception

The future settlement listener will emit one aggregated `DRAW_SETTLEMENT_ATTENTION_REQUIRED`
notice only after a bounded processing retry threshold. It must contain tenant draw id, result slot,
draw date, count of unfinished tickets and a route to the draw/reconciliation surface. It must not
include ticket numbers, selections, public codes, personal data, or payout amounts in a platform
notice unless the operator navigates through an authorized detail flow.
