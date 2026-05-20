# Tasks

## 1. Domain model

- [ ] Refactor `Terminal` aggregate.
- [ ] Fields:
  - TerminalId
  - TenantId
  - OutletId
  - assigned UserId nullable
  - kind PHYSICAL/VIRTUAL
  - state REGISTERED/ACTIVE/LOCKED/OFFLINE/UNREGISTERED
  - activeForUser
  - lastSeen
  - label
  - inventoryTag
  - metadata
  - syncState ONLINE/OFFLINE/SYNC_PENDING/SYNC_CONFLICT
  - lock fields
- [ ] Domain methods:
  - register
  - activate
  - unregister
  - lock
  - unlock
  - heartbeat
  - assignOutlet
  - assignUser
  - setActiveForUser

## 2. Commands

- [ ] `RegisterTerminalCommand`
- [ ] `UpdateTerminalCommand`
- [ ] `AssignTerminalToOutletCommand`
- [ ] `AssignTerminalToUserCommand`
- [ ] `SetActiveTerminalForUserCommand`
- [ ] `LockTerminalCommand`
- [ ] `UnlockTerminalCommand`
- [ ] `UnregisterTerminalCommand`
- [ ] `SendTerminalHeartbeatCommand`
- [ ] `UpdateTerminalSyncStateCommand`

## 3. Queries / read endpoints

Do not rely only on overview.

- [ ] `GetTerminalByIdQuery`
- [ ] `ListTerminalsQuery`
- [ ] `ListTerminalsByOutletQuery`
- [ ] `ListTerminalsByUserQuery`
- [ ] `GetActiveTerminalForUserQuery`
- [ ] `GetTerminalStatusQuery`
- [ ] `GetTerminalSyncStatusQuery`
- [ ] `ListOfflineOrSyncPendingTerminalsQuery`

## 4. Admin HTTP endpoints

- [ ] `GET /admin/terminals`
  - paginated
  - filters: outletId, userId, state, syncState, kind
- [ ] `GET /admin/terminals/{terminalId}`
- [ ] `POST /admin/terminals`
- [ ] `PATCH /admin/terminals/{terminalId}`
- [ ] `POST /admin/terminals/{terminalId}/assign-outlet`
- [ ] `POST /admin/terminals/{terminalId}/assign-user`
- [ ] `POST /admin/terminals/{terminalId}/activate-for-user`
- [ ] `POST /admin/terminals/{terminalId}/lock`
- [ ] `POST /admin/terminals/{terminalId}/unlock`
- [ ] `DELETE /admin/terminals/{terminalId}`
- [ ] Return `ApiResponse<T>`.

## 5. Tenant/runtime endpoints

- [ ] `POST /tenant/terminals/{terminalId}/heartbeat`
- [ ] `GET /tenant/terminals/current`
  - current active terminal for current user
- [ ] `GET /tenant/terminals/{terminalId}/status`
- [ ] `POST /tenant/terminals/{terminalId}/sync-state`

## 6. Ops endpoints

- [ ] `GET /admin/terminals/offline`
- [ ] `GET /admin/terminals/sync-pending`
- [ ] Optional: `POST /admin/terminals/{terminalId}/force-sync-reset`

## 7. Controller cleanup

- [ ] Replace command models used as request body with web request models.
- [ ] Remove raw `UUID` from `TerminalResponse`.
- [ ] Use `@CurrentContext`, not manual context resolver.
- [ ] No actorId from request body; actor comes from context.
- [ ] Use `CommandBus` and `QueryBus`.

## 8. Persistence

- [ ] Implement JPA and mapper.
- [ ] Add partial unique index for one active terminal per user.
- [ ] Add RLS.
- [ ] Add targeted cache specs if needed.

## 9. Tests

- [ ] Domain tests for lock/unlock/heartbeat.
- [ ] Handler tests for active terminal uniqueness.
- [ ] Web tests for DTO mapping.
