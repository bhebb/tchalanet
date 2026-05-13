# Tasks — Introduce Platform Modulith

## 0. Decision freeze

- [ ] Confirm Java layer name: `platform`.
- [ ] Document `platform layer` vs `platform admin scope`.
- [ ] Add rule: no new stateful transversal code in `common`.
- [ ] Add rule: no new non-core transversal modules in `core`.

## 1. Maven macro modules

- [ ] Create parent aggregator POM.
- [ ] Create `tchalanet-common`.
- [ ] Create `tchalanet-catalog`.
- [ ] Create `tchalanet-platform`.
- [ ] Create `tchalanet-core`.
- [ ] Create `tchalanet-features`.
- [ ] Create `tchalanet-app` as the only executable Spring Boot app.
- [ ] Move app bootstrap/main class to `tchalanet-app`.
- [ ] Verify targeted builds:
  - [ ] `./mvnw -pl tchalanet-common verify`
  - [ ] `./mvnw -pl tchalanet-platform -am verify`
  - [ ] `./mvnw -pl tchalanet-core -am verify`
  - [ ] `./mvnw -pl tchalanet-app -am verify`
- [ ] Verify full build: `./mvnw clean verify`.

## 2. Create platform structure

- [ ] Create `com.tchalanet.server.platform` root.
- [ ] Add package-info templates for Spring Modulith modules.
- [ ] Create initial empty platform capabilities:
  - [ ] `platform.audit`
  - [ ] `platform.accesscontrol`
  - [ ] `platform.usercontext`
  - [ ] `platform.tenantconfig`
  - [ ] `platform.tenanttheme`
  - [ ] `platform.document`
  - [ ] `platform.communication`
  - [ ] `platform.notification`
  - [ ] `platform.idempotence`
- [ ] Each capability has `api/` and `internal/`.

## 3. Add gates

- [ ] Add Spring Modulith verification test.
- [ ] Add ArchUnit test: no cross-module internal imports.
- [ ] Add ArchUnit test: `common` independence.
- [ ] Add ArchUnit test: `catalog` cannot depend on platform/core/features.
- [ ] Add ArchUnit test: `platform` cannot depend on core/features.
- [ ] Add ArchUnit test: `core` cannot depend on features.
- [ ] Add ArchUnit test: features are leaf modules.
- [ ] Add ArchUnit test: core must not listen to platform events.
- [ ] Add ArchUnit test: no forbidden legacy core modules when migration complete.
- [ ] Add temporary allowlists with TODO + removal condition.

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

### 5.6 Tenant user / user context

High fan-in migration:

- [ ] PR1: Create `platform.usercontext.api` bridge to legacy `core.tenantuser`.
- [ ] PR2: Flip consumers by area.
- [ ] PR3: Move implementation to `platform.usercontext.internal`.
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
