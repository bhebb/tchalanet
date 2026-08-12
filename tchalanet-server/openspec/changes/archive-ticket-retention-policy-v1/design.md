# Design - archive ticket retention policy v1

## Current state

The archive execution path already exports `sales_ticket`, `sales_ticket_line`,
`sales_ticket_charge`, `draw` and `draw_result` datasets through owning-module archive providers.
The purge services are platform-admin only, dry-run first, legal-hold aware, and require verified
archive objects before deleting hot rows.

The current ticket purge is period-based. It compares hot row counts to archive row counts for the
whole period, then deletes all ticket charges, lines and headers for that period. That is safe for
complete-period cleanup, but it cannot support "purge only losing tickets after one week" because
the hot/archive count equality would no longer describe the same row set.

## Ticket retention classes

The implementation should classify tickets using `sales_ticket` lifecycle fields:

| Class | Predicate | Hot retention |
| --- | --- | --- |
| Losing/no-payout | `sale_status=APPROVED`, `result_status=LOST`, `settlement_status=NO_PAYOUT`, `winning_amount=0` | short, configurable; product target 7 days |
| Winning/unpaid | `result_status=WON`, `settlement_status=PAYOUT_PENDING` or `winning_amount>0` and not paid | long; at least payout expiry plus dispute buffer |
| Winning/paid | `result_status=WON`, `settlement_status=PAID` | long; configurable compliance buffer |
| Reversed/corrected | `settlement_status=REVERSED` or `result_status=OVERRIDDEN` | long; audit/dispute buffer |
| Unresolved | `result_status=NOT_RESULTED` or `PENDING`, or `settlement_status=NOT_SETTLED` | not purgeable |
| Cancelled/voided | `sale_status=CANCELLED` or `VOIDED` | separate policy; not part of short losing-ticket purge |

Short-retention purge must use ticket IDs selected by this policy. It must not delete every ticket
in a calendar period.

## Archive matching

Before deleting candidate tickets, the purge flow must prove each candidate is represented by a
verified archive object and lookup metadata. Acceptable designs:

1. candidate archive objects whose row counts match the candidate ID set; or
2. complete-period archive objects plus lookup/index verification for every candidate ticket.

V1 should prefer option 2 because it reuses the existing archive object model and avoids splitting
datasets into "losing" and "winning" variants.

## Deletion order

Deletion remains bounded and FK-safe:

1. `sales_ticket_charge`
2. `sales_ticket_line`
3. `sales_ticket`

The purge service must expose dry-run counts before DELETE mode and must keep reason, requester and
legal-hold checks mandatory.

## Draw and result retention

Draw cleanup is later than ticket cleanup. A draw is purgeable only after:

- the draw is terminal;
- verified draw archive exists;
- no hot ticket references the draw;
- no active legal hold overlaps the draw or period.

Draw results are public historical facts and payout inputs. A draw result is purgeable only after:

- verified draw-result archive exists;
- no hot draw references the result;
- the public result history retention window has elapsed;
- no active legal hold overlaps the result or period.

`draw_channel` is not a normal purge target. It is reference/configuration data needed to interpret
draw history. Operators should deactivate or version channels instead of deleting them.

## Verification after purge

Ticket verification by public code, QR URL or cashier scan must not silently degrade when a losing
ticket has been purged from hot storage. The read path should:

1. search hot ticket tables first;
2. if not found, search `archive_lookup_index`;
3. return an explicit archived/expired verification outcome, or assemble an archived ticket view
   when the product requires detailed dispute lookup.

The response must not expose storage object URIs.
