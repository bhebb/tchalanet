# Tasks — commercial-network-v1

## 1. Outlet

- [ ] Add/confirm `OutletKind`.
- [ ] Allow outlet to represent partner institutions / bank branches / mobile points.
- [ ] Add optional partner metadata only if needed.
- [ ] Update outlet docs and admin UI labels.

## 2. Seller

- [x] Create `core.seller` (renamed from `core.agent` — dropped zones/hierarchy/mandates).
- [x] Add typed IDs: `SellerId`, `SellerOutletAssignmentId`, `SellerCommissionPolicyId`.
- [x] Add aggregate `Seller`.
- [x] Add `SellerOutletAssignment`.
- [x] Add `SellerCommissionPolicy`.
- [x] Add commands and queries.
- [x] Add admin controllers.
- [x] Add read models.
- [x] Add events.
- [ ] Add tests for assignment history.

## 3. Sales

- [x] Resolve seller during sell (stub via `SaleSellerContextResolver` — TODO: wire `ResolveSellerForOperationQuery`).
- [x] Snapshot seller_id + seller_assignment_id on ticket (nullable).
- [ ] Revalidate seller/assignment in transaction before ticket creation.
- [ ] Snapshot seller commission policy.
- [ ] Pass seller scope to limitpolicy.
- [ ] Add promotion fields to TicketLine.
- [ ] Add charge waiver snapshot support.

## 4. Promotion

- [ ] Keep V1 effects only: FREE_GAME_LINE, BOOST_ODDS, WAIVE_CHARGE.
- [ ] Add `HT_MARYAJ_GRATUIT`.
- [ ] Integrate Sales applier.
- [ ] Add runtime active campaign cache and invalidation.

## 5. LimitPolicy

- [ ] Add/confirm scope SELLER.
- [ ] Ensure Sales can evaluate seller limit before ticket creation.
- [ ] Keep prepaid as limit policy V1, not ledger.

## 6. Cashier

- [ ] Cashier home resolves seller.
- [ ] Cashier sell flow dispatches Sales command.
- [ ] Display ApiNotice warnings.

## 7. Notification

- [ ] Use platform.notification for in-app alerts only.
- [ ] Do not use platform.communication for these app notices.

## 8. Tests

- [ ] Seller can exist without user.
- [ ] Seller can be linked to user later.
- [ ] Seller assignment history is preserved.
- [ ] Suspended seller cannot sell.
- [ ] Seller moved to another outlet keeps historical ticket assignment.
- [ ] Promotion snapshots are stable after promotion config changes.
- [ ] Waived charge is visible in money breakdown.
