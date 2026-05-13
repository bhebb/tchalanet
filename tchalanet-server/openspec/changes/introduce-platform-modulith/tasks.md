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
  - [ ] `platform.usercontext`
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
- [ ] Add temporary allowlists with TODO + removal condition.

## 4. Defatten common

- [x] Inventory common packages.
- [ ] Keep only technical primitives in common.
- [ ] Split security:
  - [ ] `common.security` = technical glue.
  - [ ] `platform.accesscontrol` = permission decisions.
- [ ] Split idempotence:
  - [ ] `common.idempotence` = annotations/keys/interfaces.
  - [ ] `platform.idempotence` = persistence/workflow.
- [x] Move document workflow/state to `platform.document`.
- [x] Move communication workflow/state to `platform.communication`.

## 5. Migrate platform candidates

### 5.1 Document / communication

- [x] Create `DocumentApi`.
- [x] Move document implementation under `platform.document.internal`.
- [x] Create `CommunicationApi`.
- [x] Move communication implementation under `platform.communication.internal`.
- [ ] Update consumers to use APIs only.

### 5.2 Audit

- [x] Create `platform.audit.api.AuditApi`.
- [x] Move audit implementation from `core.audit` to `platform.audit.internal`.
- [ ] Preserve after-commit success audit behavior.
- [ ] Preserve failure audit exception semantics where required.
- [x] Delete `core.audit` after imports are migrated.

### 5.3 Tenant theme

- [x] Create `platform.tenanttheme.api`.
- [x] Move effective tenant theme resolution to `platform.tenanttheme.internal`.
- [x] Keep global presets in `catalog.theme` if applicable.
- [x] Delete `core.tenanttheme`.

### 5.4 Tenant config

High fan-in migration:

- [x] PR1: Create `platform.tenantconfig.api` bridge to legacy `core.tenantconfig`.
- [x] PR2: Flip consumers to `platform.tenantconfig.api`.
- [x] PR3: Move implementation to `platform.tenantconfig.internal`.
- [x] Delete `core.tenantconfig`.

### 5.5 Access control

High fan-in migration:

- [x] PR1: Create `platform.accesscontrol.api` bridge.
- [x] PR2: Flip permission evaluator/controllers/features/core consumers.
- [x] PR3: Move implementation to `platform.accesscontrol.internal`.
- [x] Delete `core.accesscontrol`.

### 5.6 Tenant user / user context

High fan-in migration:

- [ ] PR1: Create `platform.usercontext.api` bridge to legacy `core.tenantuser`.
- [ ] PR2: Flip consumers by area.
- [ ] PR3: Move implementation to `platform.usercontext.internal`.
- [ ] PR4: Delete `core.tenantuser`.

### 5.7 Notification

- [x] If `core.notification` exists and is not business-core, migrate to `platform.notification`.
- [x] Separate notification state/preferences/inbox from provider delivery.
- [x] Use `platform.communication` for delivery providers.
- [x] Delete `core.notification` unless ADR exception exists.

## 6. Align core to Modulith

For each core domain:

- [x] Create `api/command`.
- [x] Create `api/query`.
- [ ] Create `api/event` for public integration events.
- [x] Create `api/model` for read/result models.
- [x] Move handlers/domain/ports/infra under `internal/`.
- [ ] Ensure public API does not expose aggregates, ports, handlers, repositories, or JPA entities.
- [ ] Verify core inter-domain calls use APIs/events/bus rules.

## 7. Features cleanup

- [x] Ensure no `features.<x>.api` unless ADR exception exists.
- [x] Ensure features are leaves.
- [ ] Ensure shared reusable behavior is moved to common/catalog/platform/core.
- [ ] Ensure features use only public APIs of core/platform/catalog.

## 8. Finalize gates

- [ ] Remove legacy allowlists.
- [x] Enforce absence of:
  - [x] `core.audit`
  - [x] `core.accesscontrol`
  - [x] `core.tenantuser`
  - [x] `core.tenantconfig`
  - [x] `core.tenanttheme`
  - [x] `core.notification`
- [ ] Run Spring Modulith verification.
- [ ] Run ArchUnit tests.
- [ ] Run Flyway + ddl validation.
- [ ] Run `./mvnw clean verify`.
