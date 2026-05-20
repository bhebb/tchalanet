# Spec — Session API/Fixes for Operational Context

## Requirement

`core.session` SHALL expose a stable API/query to verify sales session eligibility.

## Needed contract

`GetSalesSessionOperationalEligibilityQuery` or equivalent returns:

- session id;
- terminal id;
- outlet id;
- seller/user id if bound;
- open/closed status;
- openedAt/closedAt.

## Rules

- Closed session blocks sell/offline promotion.
- Session/terminal/outlet mismatch blocks mutation.
- No other module imports session persistence.
