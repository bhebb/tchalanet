# PR Checklist — Platform Modulith Migration

## Every PR

- [ ] Scope is one migration step, not a big bang.
- [ ] `platform/` vs `/api/v1/platform/**` wording is clear in docs/comments.
- [ ] No new stateful transversal code added to `common`.
- [ ] No new non-core transversal module added to `core`.
- [ ] No module imports another module's `internal` package.
- [ ] Tests for moved code migrated with the code.
- [ ] Integration tests of dependent modules audited.
- [ ] Targeted Maven build passes.
- [ ] Full `./mvnw clean verify` passes before merge.

## For Maven module PRs

- [ ] Dependency graph matches `MAVEN_MODULES.md`.
- [ ] Only `tchalanet-app` is executable.
- [ ] Versions remain centralized.

## For platform migration PRs

- [ ] Public contracts are under `platform.<capability>.api`.
- [ ] Implementation is under `platform.<capability>.internal`.
- [ ] API does not expose JPA entities, repositories, internal services, or Spring MVC DTOs.
- [ ] Transaction/context behavior is documented if non-default.
- [ ] Cross-platform imports are avoided unless ADR exception exists.

## For core migration PRs

- [ ] Public command/query/event/model types are under `core.<domain>.api`.
- [ ] Handlers/domain/ports/infra are under `core.<domain>.internal`.
- [ ] Core does not listen to platform events.
- [ ] No domain/application import of infra.

## For final cleanup PRs

- [ ] Legacy packages removed:
  - [ ] `core.audit`
  - [ ] `core.accesscontrol`
  - [ ] `core.tenantuser`
  - [ ] `core.tenantconfig`
  - [ ] `core.tenanttheme`
  - [ ] `core.notification`
- [ ] Legacy allowlists removed from ArchUnit/Modulith tests.
