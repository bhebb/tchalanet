# Tasks - archive ticket retention policy v1

## 1. Contract

- [x] 1.1 Analyze the current archive, ticket purge and domain purge behavior.
- [x] 1.2 Define ticket retention classes for losing, winning and unresolved tickets.
- [x] 1.3 Define draw, draw-result and draw-channel retention boundaries.
- [x] 1.4 Add the `platform.archive` specification delta.

## 2. Implementation

- [ ] 2.1 Add a ticket retention policy service owned by `core.sales`.
- [ ] 2.2 Add a ticket purge candidate query that selects ticket IDs by retention class and cutoff.
- [ ] 2.3 Extend platform purge orchestration to purge candidate ticket IDs only after verified archive lookup exists.
- [ ] 2.4 Keep charge, line and ticket deletion in FK-safe bounded batches.
- [ ] 2.5 Add draw and draw-result purge checks that respect ticket and draw dependencies.
- [ ] 2.6 Keep `draw_channel` online and expose only deactivate/version/archive-history behavior if needed.
- [ ] 2.7 Make ticket verification return an archived/expired outcome instead of a silent not-found when hot rows were purged.

## 3. Verification

- [ ] 3.1 Test losing approved no-payout ticket eligibility after the short retention cutoff.
- [ ] 3.2 Test winning, payout-pending, paid, reversed and unresolved tickets are not short-retention candidates.
- [ ] 3.3 Test legal hold blocks candidate purge.
- [ ] 3.4 Test DELETE mode is refused unless matching archive objects are VERIFIED and candidate counts match.
- [ ] 3.5 Test purge deletes charges, lines and tickets in safe order.
- [ ] 3.6 Test draw purge is blocked while hot tickets reference the draw.
- [ ] 3.7 Test draw-result purge is blocked while draws reference the result.
- [ ] 3.8 Test public/POS ticket verification behavior for a purged losing ticket.
- [ ] 3.9 Validate the OpenSpec change.
