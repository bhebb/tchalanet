# PR Checklist — Commercial Network V1

## Boundaries

- [ ] `Seller` is not `User`.
- [ ] `Cashier` is not a core domain.
- [ ] Partner institutions are modeled as outlets in V1.
- [ ] No generic `core.compensation` introduced.
- [ ] No prepaid ledger introduced unless explicitly required.

## Seller

- [ ] Seller can exist without user.
- [ ] Seller assignment is historized.
- [ ] Seller suspension blocks sale.
- [ ] Seller commission policy is versioned.
- [ ] Sales snapshots seller commission policy.

## Sales

- [ ] Seller is resolved from user + outlet + session.
- [ ] Seller/assignment is revalidated in transaction.
- [ ] Seller fields snapshot on ticket.
- [ ] LimitPolicy scope SELLER called.
- [ ] Promotion snapshots are persisted.
- [ ] Charges/waivers are snapshot.

## Promotion

- [ ] Only V1 effects implemented.
- [ ] FREE_GAME_LINE does not mutate pricing odds.
- [ ] BOOST_ODDS snapshots odds override.
- [ ] WAIVE_CHARGE impacts MoneyBreakdown/charge snapshot.

## Events

- [ ] Cross-domain effects after-commit only.
- [ ] Consumers idempotent.
