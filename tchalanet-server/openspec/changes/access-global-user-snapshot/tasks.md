# Tasks

## Backend

- [x] Add DB projection/repository method for user access rows.
- [x] Add active override batch lookup by user.
- [x] Implement `AccessControlSnapshotResolver.resolveUserAccess(UserId)`.
- [x] Add `ApiScope.IDENTITY` and `/identity/**` path handling.
- [x] Use access snapshot in `AccessResolutionStepImpl`.
- [x] Use access snapshot in runtime/bootstrap entry-route decision.
- [x] Update first-login endpoint authorization to `isAuthenticated()`.
- [ ] Add focused tests or compile validation.

## Web

- [x] Ensure post-login routing uses backend-provided entry route.
