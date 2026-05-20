# Tasks — Sales Sell Operational Context

## 1. Define models

- [ ] Create `ResolveSellerOperationalContextRequest`.
- [ ] Create `SellerOperationalContext` result model.
- [ ] Include tenant/user/terminal/outlet/session/operation type.

## 2. Implement resolver

- [ ] Query identity membership if needed.
- [ ] Check permission through `AccessControlApi`.
- [ ] Query terminal API.
- [ ] Query outlet API.
- [ ] Query session API.
- [ ] Return a value object or throw application exception.

## 3. Update sell controller/handler

- [ ] Controller maps terminal/outlet/session ids.
- [ ] Resolver runs before `SellTicketCommand` mutation path.
- [ ] Sell handler receives validated operational context or ids derived from it.
- [ ] No direct repository access from controller.

## 4. Offline sync integration

- [ ] Promotion handler uses same resolver before sales command.
- [ ] Offline technical validation remains in `core.offlinesync`.

## 5. Tests

- [ ] Sell succeeds with valid context.
- [ ] Missing terminal fails.
- [ ] Closed session fails.
- [ ] Terminal/outlet mismatch fails.
- [ ] Permission denied fails.
