# Spec — Outlet API/Fixes for Operational Context

## Requirement

`core.outlet` SHALL expose a stable API/query to verify outlet eligibility for operational context.

## Needed contract

`GetOutletOperationalEligibilityQuery` or equivalent returns:

- outlet id;
- tenant id if explicit multi-tenant use;
- active/deleted status;
- timezone/currency if relevant;
- optional display label.

## Rules

- No other module imports outlet persistence.
- Return read model, not aggregate/JPA entity.
