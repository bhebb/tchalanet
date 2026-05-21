# Tasks — platform-identity-admin-users

- [x] Rename/merge admin controllers into `IdentityUserAdminController`.
- [x] Change path to `/admin/identity/users`.
- [x] Remove `/admin/users/tenant/{tenantId}`.
- [x] Remove `/admin/membership/bootstrap`.
- [x] Add `@Valid` to request bodies.
- [x] Add `@Operation` docs.
- [x] Add `@AuditLog` to all write endpoints.
- [x] Fix approve user flow to use actor from `TchRequestContext`.
- [x] Add Keycloak sync status fields to responses.
- [x] Add `send-invitation` endpoint.
- [x] Add `resync-keycloak` endpoint.
- [x] Enforce role creation/assignment restrictions.
- [x] Add tests for tenant scoping and forbidden cross-tenant management.
