# Security Auth & Access Control Refactor

## Status

PROPOSED

---

## Context

The current server already has:

- JWT authentication through Spring Security resource server;
- role extraction from Keycloak tokens;
- `TchContextFilter` building `TchRequestContext`;
- `TchPermissionEvaluator` and `core.accesscontrol` concepts;
- RLS context support.

However, the current implementation has gaps and inconsistencies:

- no runtime `UserBootstrapFilter`, so `app_user` is not guaranteed;
- `TchContextFilter` attempts lookup but does not synchronize `app_user`;
- `TchPermissionEvaluator` assumes the Spring principal is `TchRequestContext`, but the current principal is `Jwt`;
- two permission-entry mechanisms exist (`@PreAuthorize` and `@RequiresPermission` aspect);
- `SecurityConfig` still contains role-based rules that should remain only coarse gates;
- RLS should receive full context variables, including API scope and super-admin status;
- sensitive override headers need stricter handling.

---

## Goals

- Introduce a clean runtime authentication/context pipeline.
- Guarantee local `app_user` existence for authenticated protected requests.
- Standardize permission evaluation on Spring Method Security + `TchPermissionEvaluator`.
- Route authorization decisions through `core.accesscontrol`.
- Align RLS with `TchRequestContext`.
- Define safe super-admin tenant/deleted-visibility override behavior.
- Document the flows in `tchalanet-docs/docs/01-architecture/flows/`.

---

## Non-goals

- Full DB-driven RBAC V2 implementation.
- Permission management UI.
- Complete audit flow implementation.
- Replacing Keycloak.
- Changing public/mobile authentication model.

---

## References

- `tchalanet-docs/docs/01-architecture/flows/authentication-flow.md`
- `tchalanet-docs/docs/01-architecture/flows/permission-flow.md`
- `openspec/context/90-security-flows-guide.md`
