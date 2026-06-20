# Access Global User Snapshot

## Why

Runtime/bootstrap needs a DB-backed view of the current user's complete access instead of deriving actor type, roles, permissions, or entry routes from request paths or frontend URLs.

## What

- Add `AccessControlSnapshotResolver.resolveUserAccess(UserId)`.
- Resolve platform, tenant, and seller-terminal access from persisted roles, memberships, permissions, and overrides.
- Add `ApiScope.IDENTITY` so `/identity/**` endpoints are authenticated but not tenant-scoped.
- Use the access snapshot in request access resolution and runtime bootstrap entry selection.

## Impact

- Backend access-control repositories/services.
- Security request context resolution.
- Private runtime/bootstrap response entry route.
- Minimal web use of backend-provided `entryRoute` if needed.
