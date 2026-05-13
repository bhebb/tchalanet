# Spec — platform.identity

## Requirement

`platform.identity` SHALL own persistent application identity data.

## Package

```text
com.tchalanet.server.platform.identity
  api
  internal.service
  internal.persistence
  internal.web
  internal.event
  internal.cache
```

## Owns

- app user;
- user profile;
- user preferences;
- tenant membership;
- Keycloak/IdP subject mapping;
- user bootstrap for current principal;
- user admin operations.

## Does not own

- `TchRequestContext`;
- operational context;
- permissions/roles decisions (use `platform.accesscontrol`);
- terminal/outlet/session validation.

## Controllers

Move existing user controllers into:

```text
platform.identity.internal.web.admin.UserAdminController
platform.identity.internal.web.admin.TenantUserAdminController
platform.identity.internal.web.me.CurrentUserProfileController
```

Routes may remain unchanged during migration.

## API

Create `IdentityApi` with a minimal public surface consumed outside the module.
Admin-only operations should remain internal unless another module truly consumes them.

## Acceptance criteria

- No user/profile/membership controller remains under `core.user` or `core.tenantuser`.
- No module imports `platform.identity.internal..`.
- Role/permission assignments go through `platform.accesscontrol.api`.
