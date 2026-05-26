# Change Proposal — commercial-network-v1

## Summary

Introduce a cleaner V1 commercial network model:

- Replace ambiguous `agent` with `seller` for the transaction seller/machann.
- Expand `outlet` to represent physical, mobile, institution, bank, and partner sales channels.
- Keep `cashier` as feature/UI only.
- Keep Promotion V1 limited to three effects.
- Keep seller commissions simple and snapshot them in Sales.
- Treat seller limits/prepaid as `core.limitpolicy` scope SELLER for V1.
- Avoid `core.compensation`, `core.partner`, and seller payment until the need is real.

## Motivation

The previous `agent` concept mixed seller, commercial representative, partner institution, bank, prepaid, commission, payment, sub-agent/network.

## Non-goals

No generic compensation engine, partner settlement, seller payment flow, prepaid ledger, multi-level commission, or full promotion rules engine.

## Affected domains

`core.outlet`, `core.seller`, `core.promotion`, `core.sales`, `core.limitpolicy`, `features.cashier`, `platform.notification`.
