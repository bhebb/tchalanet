# PR Checklist — Tenant Admin Users

This checklist is historical for OpenSpec change `82-tenantadmin-users-20260128`.
Tenant Admin users have since migrated out of `features/tenantadmin/users` and are now owned by `core.tenantuser.infra.web.admin`.

Before requesting review

- [ ] The change is implemented in `core.tenantuser` unless it is a composite BFF screen.
- [ ] Any composite feature code includes only orchestration code (no repositories, entities, EntityManager, or SQL).
- [ ] Public signatures (controllers, request/response) use Typed IDs (e.g. `UserId`) — no `java.util.UUID`.
- [ ] Any new domain behavior is implemented in the owning core (core.user / core.tenantuser). If you added a handler to a core, reference the core PR.
- [ ] Composite feature code calls core handlers only via CommandBus/QueryBus or ports; no direct repository access.
- [ ] Tests added/updated for admin controller behavior and any composite orchestration/mapping.

For reviewers

- [ ] Confirm `features/tenantadmin/users` was not reintroduced for mono-domain CRUD.
- [ ] Confirm there are no imports from `jakarta.persistence`, `org.springframework.data`, or direct JPA/Hibernate classes in any tenant-admin feature package.
- [ ] Confirm controller signatures use typed IDs, not `UUID` or `String` for IDs.
- [ ] Confirm any write goes through a core command (see OpenSpec 82 for required core handlers).
- [ ] Ensure any feature code only composes read models (XxxView / XxxRow) and doesn't implement business invariants.
- [ ] Run unit tests + `FeatureArchitectureTest` locally (mvn -DskipTests=false test-compile test) if possible.

If ArchUnit detects violations

- Create follow-up tasks to fix violations, referencing the ArchUnit rule name and the change-id `82-tenantadmin-users-20260128`.

References

- OpenSpec: `openspec/context/82-feature-tenant-admin-users.md`
- Change proposal: `openspec/changes/82-tenantadmin-users-20260128/proposal.md`
- Matrix: `docs/tenant-admin/FEATURE_TENANT_ADMIN_MATRIX.md`
