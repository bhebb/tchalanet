# Tasks

## Contract and persistence

- [x] Reuse `SettlementTermsSnapshot` as the immutable pricing coverage and settlement input.
- [ ] Add a typed applied-settlement snapshot record.
- [ ] Add JSONB persistence for the applied-settlement snapshot on the ticket line.
- [ ] Extend ticket placed/result events and POS/admin detail responses.
- [ ] Define compatibility behavior for tickets created before the new snapshots.

## Sale-time coverage

- [ ] Evolve the planner so `SettlementTermsSnapshot` contains complete commercial coverage for
  Bòlèt, Maryaj and Loto.
- [ ] Resolve each coverage rule with seller override -> tenant default precedence.
- [ ] Capture the effective value and source at sale confirmation.
- [ ] Add tests proving configuration changes do not alter an existing ticket.

## Result-time application

- [ ] Extend `TicketLineResult` to return the winning term(s), not only the total amount.
- [ ] Persist `appliedSettlementSnapshot` when official results are applied.
- [ ] Preserve alternative-best and cumulative payout semantics.
- [ ] Add Bòlèt lot 1/2/3 payout tests with a single selection.
- [ ] Add Maryaj exact/reverse and fixed-amount tests.
- [ ] Add replay/idempotency tests for applied snapshots and payout events.

## Consumers

- [ ] Update POS/admin ticket details to show coverage before results.
- [ ] Update POS/admin ticket details to show applied terms after results.
- [ ] Keep receipts free of potential payout values.
- [ ] Add web/mobile contract and e2e coverage for pending, won and lost states.

## Validation

- [ ] Run focused pricing, sales and settlement tests.
- [ ] Run the end-to-end sale -> result -> payout -> ticket detail flow.
- [ ] Validate the OpenSpec and review migration/event compatibility.
