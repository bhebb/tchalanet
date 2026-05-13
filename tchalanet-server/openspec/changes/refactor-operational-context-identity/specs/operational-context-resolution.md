# Spec — Operational Context Resolution

## Requirement

Operational context resolution SHALL validate runtime operation frames such as seller POS context.

## Seller operation context

### Inputs

- `TchRequestContext`
- `TerminalId`
- optional `OutletId`
- optional `SessionId`
- operation type, e.g. `SELL`, `SYNC_OFFLINE_SALE`, `PAYOUT_EXECUTE`

### Resolution sources

- `platform.identity.api.IdentityApi`
- `platform.accesscontrol.api.AccessControlApi`
- `core.terminal.api` queries
- `core.outlet.api` queries
- `core.session.api` queries

### Output

`SellerOperationalContext` or `SellerOperationContext` containing:

- tenant id;
- actor user id;
- terminal id;
- outlet id;
- session id when required;
- timezone/locale/currency if effective context needs them;
- authorization snapshot if useful.

## Rules

- Resolver must not mutate business state.
- Resolver must not bypass RLS.
- Resolver returns a value object / result, not JPA entities.
- Business use cases remain responsible for business invariants.

## Failure cases

Return/throw problem-compatible application exceptions for:

- missing terminal;
- terminal inactive;
- terminal not in tenant/outlet;
- outlet inactive;
- session missing when required;
- session closed;
- actor not allowed to operate on session/terminal/outlet;
- permission denied.
