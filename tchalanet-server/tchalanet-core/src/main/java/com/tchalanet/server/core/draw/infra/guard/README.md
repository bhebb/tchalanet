# Draw Sales Guard

## Purpose

Validates draw operations against sales/payout/settlement state.

## Current Implementation

**NoOpDrawSalesGuardAdapter** (⚠️ Temporary)

- All validations return immediately without checks
- Logs warnings to indicate no validation is performed
- **NOT SAFE FOR PRODUCTION**

## Future Implementation

Replace with **RealDrawSalesGuardAdapter** that:

- Queries sales, payout, settlement repositories
- Enforces all business rules documented in NoOpDrawSalesGuardAdapter
- Throws `ProblemRest` when operations are blocked

## Dependencies Required

- `TicketReaderPort` (core.sales)
- `PayoutReaderPort` (core.payout)
- `SettlementReaderPort` (features.settlement)

## See Also

- `DRAW_SALES_GUARD_SUMMARY.md` (root) for complete rules documentation
