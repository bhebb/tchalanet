# Tasks — Introduce Platform Modulith

## 0. Decision freeze

- [x] Confirm Java layer name: `platform`.
- [x] Document `platform layer` vs `platform admin scope`.
- [x] Add rule: no new stateful transversal code in `common`.
- [x] Add rule: no new non-core transversal modules in `core`.

## 1. Maven macro modules

- [x] Create parent aggregator POM.
- [x] Create `tchalanet-common`.
- [x] Create `tchalanet-catalog`.
- [x] Create `tchalanet-platform`.
- [x] Create `tchalanet-core`.
- [x] Create `tchalanet-features`.
- [x] Create `tchalanet-app` as the only executable Spring Boot app.
- [x] Move app bootstrap/main class to `tchalanet-app`.
- [ ] Verify targeted builds:
  - [ ] `./mvnw -pl tchalanet-common verify`
  - [ ] `./mvnw -pl tchalanet-platform -am verify`
  - [ ] `./mvnw -pl tchalanet-core -am verify`
  - [ ] `./mvnw -pl tchalanet-app -am verify`
- [ ] Verify full build: `./mvnw clean verify`.

## 2. Create platform structure

- [x] Create `com.tchalanet.server.platform` root.
- [x] Add package-info templates for Spring Modulith modules.
- [x] Create initial empty platform capabilities:
  - [x] `platform.audit`
  - [x] `platform.accesscontrol`
  - [x] `platform.identity`
  - [x] `platform.tenantconfig`
  - [x] `platform.tenanttheme`
  - [x] `platform.document`
  - [x] `platform.communication`
  - [x] `platform.notification`
  - [x] `platform.idempotence`
- [x] Each capability has `api/` and `internal/`.

## 3. Add gates

- [x] Add Spring Modulith verification test.
- [x] Add ArchUnit test: no cross-module internal imports.
- [x] Add ArchUnit test: `common` independence.
- [x] Add ArchUnit test: `catalog` cannot depend on platform/core/features.
- [x] Add ArchUnit test: `platform` cannot depend on core/features.
- [x] Add ArchUnit test: `core` cannot depend on features.
- [x] Add ArchUnit test: features are leaf modules.
- [x] Add ArchUnit test: core must not listen to platform events.
- [x] Add ArchUnit test: no forbidden legacy core modules when migration complete.
- [x] Add temporary allowlists with TODO + removal condition.

## 4. Defatten common

- [ ] Inventory common packages.
- [ ] Keep only technical primitives in common.
- [ ] Split security:
  - [ ] `common.security` = technical glue.
  - [ ] `platform.accesscontrol` = permission decisions.
- [ ] Split idempotence:
  - [ ] `common.idempotence` = annotations/keys/interfaces.
  - [ ] `platform.idempotence` = persistence/workflow.
- [ ] Move document workflow/state to `platform.document`.
- [ ] Move communication workflow/state to `platform.communication`.

## 5. Migrate platform candidates

### 5.1 Document / communication

- [ ] Create `DocumentApi`.
- [ ] Move document implementation under `platform.document.internal`.
- [ ] Create `CommunicationApi`.
- [ ] Move communication implementation under `platform.communication.internal`.
- [ ] Update consumers to use APIs only.

### 5.2 Audit

- [ ] Create `platform.audit.api.AuditApi`.
- [ ] Move audit implementation from `core.audit` to `platform.audit.internal`.
- [ ] Preserve after-commit success audit behavior.
- [ ] Preserve failure audit exception semantics where required.
- [ ] Delete `core.audit` after imports are migrated.

### 5.3 Tenant theme

- [ ] Create `platform.tenanttheme.api`.
- [ ] Move effective tenant theme resolution to `platform.tenanttheme.internal`.
- [ ] Keep global presets in `catalog.theme` if applicable.
- [ ] Delete `core.tenanttheme`.

### 5.4 Tenant config

High fan-in migration:

- [ ] PR1: Create `platform.tenantconfig.api` bridge to legacy `core.tenantconfig`.
- [ ] PR2: Flip consumers to `platform.tenantconfig.api`.
- [ ] PR3: Move implementation to `platform.tenantconfig.internal`.
- [ ] Delete `core.tenantconfig`.

### 5.5 Access control

High fan-in migration:

- [ ] PR1: Create `platform.accesscontrol.api` bridge.
- [ ] PR2: Flip permission evaluator/controllers/features/core consumers.
- [ ] PR3: Move implementation to `platform.accesscontrol.internal`.
- [ ] Delete `core.accesscontrol`.

### 5.6 Identity (tenant user)

High fan-in migration:

- [ ] PR1: Create `platform.identity.api` bridge to legacy `core.tenantuser`.
- [ ] PR2: Flip consumers by area.
- [ ] PR3: Move implementation to `platform.identity.internal`.
- [ ] PR4: Delete `core.tenantuser`.

### 5.7 Notification

- [ ] If `core.notification` exists and is not business-core, migrate to `platform.notification`.
- [ ] Separate notification state/preferences/inbox from provider delivery.
- [ ] Use `platform.communication` for delivery providers.
- [ ] Delete `core.notification` unless ADR exception exists.

## 6. Align core to Modulith

For each core domain:

- [ ] Create `api/command`.
- [ ] Create `api/query`.
- [ ] Create `api/event` for public integration events.
- [ ] Create `api/model` for read/result models.
- [ ] Move handlers/domain/ports/infra under `internal/`.
- [ ] Ensure public API does not expose aggregates, ports, handlers, repositories, or JPA entities.
- [ ] Verify core inter-domain calls use APIs/events/bus rules.

## 7. Features cleanup

- [ ] Ensure no `features.<x>.api` unless ADR exception exists.
- [ ] Ensure features are leaves.
- [ ] Ensure shared reusable behavior is moved to common/catalog/platform/core.
- [ ] Ensure features use only public APIs of core/platform/catalog.

## 8. Finalize gates

- [ ] Remove legacy allowlists.
- [ ] Enforce absence of:
  - [ ] `core.audit`
  - [ ] `core.accesscontrol`
  - [ ] `core.tenantuser`
  - [ ] `core.tenantconfig`
  - [ ] `core.tenanttheme`
  - [ ] `core.notification`
- [ ] Run Spring Modulith verification.
- [ ] Run ArchUnit tests.
- [ ] Run Flyway + ddl validation.
- [ ] Run `./mvnw clean verify`.

## 9. Update project documentation

After migration and gate cleanup:

- [ ] Update `docs/ARCHITECTURE.md`: reflect 6-layer structure, remove any reference to legacy `core` buckets.
- [ ] Update `docs/PLAYBOOK.md`: update module creation guidelines to reference `platform` layer.
- [ ] Mark `ADR-001-modulith-platform-layer` as **Accepted** (Spring Modulith + ArchUnit gates passing, no legacy allowlists).
- [ ] Mark `ADR-002-initial-application-service-migration-scope` as **Accepted** or **Superseded** as appropriate.
- [ ] Update `docs/modules/platform.md`, `docs/modules/core.md`, `docs/modules/common.md`, `docs/modules/catalog.md`, `docs/modules/features.md`: align with final module shapes.
- [ ] Update `docs/reference/platform-modules.md`: mark as finalized (remove "living inventory" qualifier if stable).
- [ ] Update `docs/architecture/IMPLEMENTATION_PLAN.md`: mark all phases complete.
- [ ] Update `docs/architecture/ARCHITECTURE_MODULITH.md`: remove any "Proposed" or provisional language.
- [ ] Verify `docs/ARCHITECTURE.md` does not mention `core.tenantuser`, `core.audit`, `core.accesscontrol`, `core.tenantconfig`, `core.tenanttheme`.
- [ ] Final doc audit: `grep -r "usercontext\|tenantuser\|core\.audit\|core\.accesscontrol" docs/` → zero results expected.
