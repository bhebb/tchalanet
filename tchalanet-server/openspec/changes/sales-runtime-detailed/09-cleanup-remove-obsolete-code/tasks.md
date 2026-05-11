# Tasks

## 1. Remove old/broken controller patterns

- [ ] Remove controllers using `TchContextResolver` manually when `@CurrentContext` is available.
- [ ] Remove controllers injecting handlers directly.
- [ ] Remove controllers taking tenantId or actorId from request body.
- [ ] Remove controllers returning domain models.
- [ ] Remove raw UUID controller params outside low-level infra.

## 2. Remove duplicate endpoints

- [ ] Ensure outlet CRUD exists only in `core.outlet`.
- [ ] Ensure terminal CRUD/runtime exists only in `core.terminal`.
- [ ] Ensure session core endpoints exist only in `core.session`.
- [ ] Ensure tenantadmin feature only has overview/onboarding/orchestration.
- [ ] Ensure cashier feature only has UX orchestration.

## 3. Rename models/commands

- [ ] Use `SalesSession` naming in core.session.
- [ ] Replace POS naming where terminal is meant.
- [ ] Replace `AgentId` with `UserId` or agreed seller typed id where appropriate.
- [ ] Replace `TargetType` with policy-specific target enum/model if needed.

## 4. Remove obsolete persistence

- [ ] Delete replaced Flyway scripts if DB reset accepted.
- [ ] Delete unused JPA entities/repositories.
- [ ] Delete unused mappers.
- [ ] Delete unused ports.

## 5. Documentation

- [ ] Update `DOMAIN_OUTLET.md`.
- [ ] Update `DOMAIN_TERMINAL.md`.
- [ ] Update `DOMAIN_SESSION.md`.
- [ ] Update `DOMAIN_SALES.md`.
- [ ] Update `DOMAIN_PAYOUT.md`.
- [ ] Update `DOMAIN_LIMITPOLICY.md`.
- [ ] Update `FEATURE_TENANTADMIN.md`.
- [ ] Update `FEATURE_CASHIER.md`.

## 6. Validation

- [ ] Compile.
- [ ] Run unit tests.
- [ ] Run Flyway migrate.
- [ ] Run ddl validate.
- [ ] Run architecture grep checks:
  - no raw UUID in application/domain/web DTOs
  - no handler direct injection in controllers
  - no TchContextResolver in controllers
  - no domain model returned by controllers
