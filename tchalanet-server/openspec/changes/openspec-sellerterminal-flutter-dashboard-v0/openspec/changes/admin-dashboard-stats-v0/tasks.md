# Tasks: Admin Dashboard & Stats V0

## 1. Menu / page model

- [ ] Rename cashier/admin concepts in UI where needed.
- [ ] Add `Terminaux / Vendeurs`.
- [ ] Add `Résultats`.
- [ ] Add `Gagnants / Paiements`.
- [ ] Add `Contrôle`.
- [ ] Add `Rapports`.
- [ ] Keep `Succursales` optional.
- [ ] Use `Fiches / Tickets` labels where useful.

## 2. Dashboard endpoint

- [ ] Create `GET /admin/dashboard`.
- [ ] Return:
  - total sales today;
  - amount to pay;
  - commissions;
  - terminal status counts;
  - top terminals;
  - recent sales;
  - pending results;
  - unpaid winners;
  - exposure warnings.
- [ ] Use `features.dashboard` BFF.
- [ ] Use QueryBus/core read contracts.
- [ ] Do not query repositories from controller.

## 3. Reports

- [ ] Sales report by period.
- [ ] Sales by SellerTerminal.
- [ ] Sales by draw.
- [ ] Sales by game.
- [ ] Sold tickets report.
- [ ] Winning tickets report.
- [ ] Paid tickets report.
- [ ] Eliminated/perdant tickets report.
- [ ] Add pagination and sort allowlists.

## 4. Control views

- [ ] Terminal control report.
- [ ] Blocked terminals list.
- [ ] Exposure by number/draw/game.
- [ ] Limit exceeded/refused sales list if available.

## 5. Permissions

- [ ] Add/verify `report.read`.
- [ ] Add/verify `sales.report.read`.
- [ ] Add/verify `terminal.read`.
- [ ] Add/verify `limit.read`.
- [ ] Protect endpoints with method security.

## 6. Data access

- [ ] Add query models and handlers.
- [ ] Ensure tenant comes from `TchRequestContext`.
- [ ] Add indexes for dashboard/report queries.
- [ ] Keep result sets bounded.
- [ ] Avoid in-memory pagination.

## 7. Tests

- [ ] Tenant admin with `report.read` can access dashboard.
- [ ] Tenant admin without `report.read` is denied.
- [ ] SellerTerminal actor cannot access dashboard.
- [ ] Dashboard only returns current tenant data.
- [ ] Report pagination works.
- [ ] Sort allowlist rejects unknown sort.
- [ ] Commission totals match ticket snapshots.
