# Tasks — platform-identity-admin-users

- [ ] Rename/merge admin controllers into `IdentityUserAdminController`.
- [ ] Change path to `/admin/identity/users`.
- [ ] Remove `/admin/users/tenant/{tenantId}`.
- [ ] Remove `/admin/membership/bootstrap`.
- [ ] Add `@Valid` to request bodies.
- [ ] Add `@Operation` docs.
- [ ] Add `@AuditLog` to all write endpoints.
- [ ] Fix approve user flow to use actor from `TchRequestContext`.
- [ ] Add Keycloak sync status fields to responses.
- [ ] Add `send-invitation` endpoint.
- [ ] Add `resync-keycloak` endpoint.
- [ ] Enforce role creation/assignment restrictions.
- [ ] Add tests for tenant scoping and forbidden cross-tenant management.
