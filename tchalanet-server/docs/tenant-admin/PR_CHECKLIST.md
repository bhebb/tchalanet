# PR Checklist — Tenant Admin (users)

This checklist supports OpenSpec change `82-tenantadmin-users-20260128` and helps reviewers validate PRs touching `features/tenantadmin/users`.

Before requesting review

- [ ] The change includes only orchestration code in the feature (no repositories, entities, EntityManager, or SQL).
- [ ] Public signatures (controllers, request/response) use Typed IDs (e.g. `UserId`) — no `java.util.UUID`.
- [ ] Any new domain behavior is implemented in the owning core (core.user / core.tenantuser). If you added a handler to a core, reference the core PR.
- [ ] The feature calls core handlers only via CommandBus/QueryBus or ports; no direct repository access.
- [ ] Tests added/updated for orchestrator logic (unit tests) and mapping (mappers).

For reviewers

- [ ] Confirm there are no imports from `jakarta.persistence`, `org.springframework.data`, or direct JPA/Hibernate classes in `features/tenantadmin/users`.
- [ ] Confirm controller signatures use typed IDs, not `UUID` or `String` for IDs.
- [ ] Confirm any write goes through a core command (see OpenSpec 82 for required core handlers).
- [ ] Ensure feature only composes read models (XxxView / XxxRow) and doesn't implement business invariants.
- [ ] Run unit tests + `FeatureArchitectureTest` locally (mvn -DskipTests=false test-compile test) if possible.

If ArchUnit detects violations

- Create follow-up tasks to fix violations, referencing the ArchUnit rule name and the change-id `82-tenantadmin-users-20260128`.

References

- OpenSpec: `openspec/context/82-feature-tenant-admin-users.md`
- Change proposal: `openspec/changes/82-tenantadmin-users-20260128/proposal.md`
- Matrix: `docs/tenant-admin/FEATURE_TENANT_ADMIN_MATRIX.md`
