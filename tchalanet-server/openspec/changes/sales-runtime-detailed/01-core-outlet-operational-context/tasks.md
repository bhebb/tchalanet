# Tasks

## 1. Domain model

- [ ] Create/refactor `Outlet` aggregate.
- [ ] Include:
  - `OutletId`
  - `TenantId`
  - name
  - slug
  - addressId
  - timezone
  - dayClosed
  - salesBlocked
  - salesBlockReason
  - receipt config
  - requireOpeningFloat
  - autoOpenSession
  - autoCloseSession
- [ ] Add domain methods:
  - `blockSales(reason, actorId, clock)`
  - `unblockSales(actorId, clock)`
  - `updateConfig(patch)`
  - `canSell(now)` returning explicit decision/reason
- [ ] Ensure domain uses typed IDs only.

## 2. Commands

- [ ] `CreateOutletCommand`
- [ ] `UpdateOutletConfigCommand`
- [ ] `BlockOutletSalesCommand`
- [ ] `UnblockOutletSalesCommand`
- [ ] `AssignUserToOutletCommand`
- [ ] `RemoveUserFromOutletCommand`
- [ ] `AssignTerminalToOutletCommand` if ownership belongs here; otherwise terminal command calls outlet validation.

## 3. Queries / read endpoints

Do not rely only on feature overview. Add core outlet reads.

- [ ] `GetOutletByIdQuery`
- [ ] `ListOutletsQuery`
- [ ] `GetOutletOperationalContextQuery`
- [ ] `ListOutletUsersQuery`
- [ ] `ListOutletTerminalsQuery`
- [ ] `GetOutletSalesCapabilityQuery`

## 4. Admin HTTP endpoints

- [ ] `GET /admin/outlets`
  - paginated with `@TchPaging`
  - filters: q, active, salesBlocked, dayClosed
- [ ] `GET /admin/outlets/{outletId}`
- [ ] `POST /admin/outlets`
- [ ] `PATCH /admin/outlets/{outletId}/config`
- [ ] `POST /admin/outlets/{outletId}/block-sales`
- [ ] `POST /admin/outlets/{outletId}/unblock-sales`
- [ ] `GET /admin/outlets/{outletId}/users`
- [ ] `POST /admin/outlets/{outletId}/users`
- [ ] `DELETE /admin/outlets/{outletId}/users/{userId}`
- [ ] `GET /admin/outlets/{outletId}/terminals`
- [ ] Return `ApiResponse<T>`.

## 5. Ops/read endpoints

- [ ] `GET /admin/outlets/{outletId}/operational-context`
- [ ] `GET /admin/outlets/{outletId}/sales-capability`
- [ ] Optional: `GET /platform/outlets?tenantId=...` for SUPER_ADMIN support if existing pattern allows.

## 6. Persistence

- [ ] Implement JPA entity extending tenant base.
- [ ] Implement mapper with `CommonIdMapper`.
- [ ] Implement reader/writer ports.
- [ ] No business logic in repository/adapters.
- [ ] RLS-first reads; avoid Java tenant filters for read side unless write-specific.

## 7. Audit/events

- [ ] Audit create/update/block/unblock/user assignment.
- [ ] Publish `OutletSalesBlockedEvent` and `OutletConfigChangedEvent` after commit only if consumers exist.
- [ ] Evict cache after commit.

## 8. Tests

- [ ] Domain tests for sales capability.
- [ ] Handler tests with in-memory ports.
- [ ] Controller mapping tests if needed.
