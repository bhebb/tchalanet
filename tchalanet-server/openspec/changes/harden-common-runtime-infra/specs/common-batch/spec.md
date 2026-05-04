# common-batch Specification Delta

## MODIFIED Requirements

### Requirement: Common batch infrastructure SHALL be métier-independent

The `common.batch` package SHALL provide technical batch infrastructure only. It SHALL NOT depend directly on `catalog.*`, `core.*`, `features.*`, or internal persistence repositories.

#### Scenario: Tenant bootstrap is required for tenant-scoped job

- **GIVEN** a TENANT-scoped batch job starts
- **WHEN** `BatchTchContextBinder` needs tenant metadata
- **THEN** it SHALL call a common technical port such as `BatchTenantBootstrapProvider`
- **AND** it SHALL NOT import `TenantCatalog`
- **AND** it SHALL NOT import catalog tenant DTOs directly
- **AND** it SHALL NOT import catalog persistence classes

#### Scenario: Tenant bootstrap implementation uses catalog

- **GIVEN** the tenant bootstrap data is stored or exposed by catalog tenant
- **WHEN** the application wires the runtime adapter
- **THEN** the adapter implementation MAY use `TenantCatalog`
- **AND** the adapter SHALL live outside `common`
- **AND** the dependency direction SHALL remain outer layer/domain adapter -> common port

#### Scenario: Batch gate flag lookup is required

- **GIVEN** `BatchGateResolver` needs a tenant or global job flag
- **WHEN** it resolves a flag
- **THEN** it SHALL call a common technical port such as `BatchGateFlagStore`
- **AND** `common.batch` SHALL NOT import catalog settings internal persistence

#### Scenario: Batch gate setting implementation uses settings storage

- **GIVEN** batch gate flags are stored as settings
- **WHEN** the settings-backed implementation is wired
- **THEN** that implementation SHALL live outside `common`
- **AND** it MAY use catalog settings APIs or persistence according to package rules
- **AND** `common.batch` SHALL only depend on the common port

---

### Requirement: Batch jobs SHALL declare explicit execution scope

Every batch job registered for Ops or scheduler execution SHALL declare whether it is `TENANT` or `GLOBAL`.

#### Scenario: Tenant job is registered

- **GIVEN** a batch job mutates or reads tenant-scoped state
- **WHEN** it is registered in the job allowlist
- **THEN** its scope SHALL be `TENANT`
- **AND** `tenant_id` SHALL be required
- **AND** the job SHALL bind a tenant `TchRequestContext`

#### Scenario: Global job is registered

- **GIVEN** a batch job operates on global/platform state
- **WHEN** it is registered in the job allowlist
- **THEN** its scope SHALL be `GLOBAL`
- **AND** `tenant_id` SHALL NOT be required
- **AND** the job SHALL bind a platform/system context if context is needed

#### Scenario: External result fetch is registered

- **GIVEN** external result fetch writes global `draw_result` records by `result_slot`
- **WHEN** `RESULTS_EXTERNAL_FETCH` is registered
- **THEN** it SHALL be registered as `GLOBAL`
- **AND** it SHALL NOT require `tenant_id`
- **AND** it SHALL NOT fetch once per tenant

#### Scenario: External result apply is registered

- **GIVEN** external result apply attaches global results to tenant draws
- **WHEN** `RESULTS_EXTERNAL_APPLY` is registered
- **THEN** it SHALL be registered as `TENANT`
- **AND** it SHALL require `tenant_id`

#### Scenario: External refresh is registered

- **GIVEN** refresh may orchestrate fetch and apply
- **WHEN** `RESULTS_EXTERNAL_REFRESH` is registered
- **THEN** its scope SHALL be explicitly documented
- **AND** it SHALL NOT repeatedly perform global fetch once per tenant
- **AND** if tenant-scoped, it SHALL only perform tenant-specific apply or use already-fetched global results
- **AND** if global-scoped, it SHALL orchestrate global fetch and tenant apply intentionally

---

### Requirement: Batch context SHALL be compatible with RLS

Batch execution SHALL bind a `TchRequestContext` that matches job scope and RLS requirements.

#### Scenario: Tenant context is bound

- **GIVEN** a TENANT job starts with a valid `tenant_id`
- **WHEN** the listener binds context
- **THEN** it SHALL bind `ApiScope.TENANT`
- **AND** it SHALL bind the runtime tenant id
- **AND** it SHALL bind tenant timezone and currency
- **AND** it SHALL bind a SYSTEM actor/role unless overridden by a validated actor param
- **AND** it SHALL set MDC values for observability

#### Scenario: Platform context is bound

- **GIVEN** a GLOBAL job starts without `tenant_id`
- **WHEN** the listener binds context
- **THEN** it SHALL bind `ApiScope.PLATFORM`
- **AND** it SHALL bind a SYSTEM actor/role
- **AND** it SHALL set the platform/super-admin-equivalent flag required by RLS for platform reads
- **AND** it SHALL not bind a runtime tenant id
- **AND** it SHALL use UTC as safe runtime zone unless another explicit platform zone is required

#### Scenario: Binding fails

- **GIVEN** context binding partially succeeds
- **WHEN** an exception occurs
- **THEN** the binder/listener SHALL clear context and MDC before rethrowing

#### Scenario: Job completes

- **GIVEN** a job completes successfully or fails
- **WHEN** `afterJob` runs
- **THEN** it SHALL clear `TchContext`
- **AND** it SHALL clear MDC
- **AND** it SHALL log status and duration

---

### Requirement: Batch gate SHALL resolve flags deterministically

Batch gate resolution SHALL follow the order tenant override, global flag, default.

#### Scenario: Tenant override exists

- **GIVEN** a tenant-scoped job
- **AND** a tenant override exists for the job key
- **WHEN** the gate resolves the job
- **THEN** the tenant override SHALL be used
- **AND** global flag SHALL NOT override it

#### Scenario: Global flag exists

- **GIVEN** no tenant override exists
- **AND** a global flag exists for the job key
- **WHEN** the gate resolves the job
- **THEN** the global flag SHALL be used

#### Scenario: No flag exists

- **GIVEN** no tenant or global flag exists
- **WHEN** the gate resolves the job
- **THEN** the documented default SHALL be used
- **AND** if default is enabled, this SHALL be considered safe only because jobs are allowlisted

---

### Requirement: Batch gate cache SHALL support explicit invalidation

The batch gate cache SHALL expose invalidation methods for tenant, global and full reset.

#### Scenario: Tenant gate flag changes

- **WHEN** a tenant-level gate flag changes
- **THEN** the tenant cache entry SHALL be evicted or updated after commit
- **AND** subsequent gate checks SHALL observe the committed value

#### Scenario: Global gate flag changes

- **WHEN** a global gate flag changes
- **THEN** the global cache entry SHALL be evicted or updated after commit
- **AND** subsequent gate checks SHALL observe the committed value

#### Scenario: Ops clears batch gate cache

- **WHEN** an authorized Ops user clears batch gate cache
- **THEN** the system SHALL support full eviction
- **AND** the operation SHALL be auditable

---

### Requirement: Invalid batch gate settings SHALL be observable

Invalid batch gate setting values SHALL not fail the job path, but SHALL be visible to operators.

#### Scenario: Invalid boolean value is stored

- **GIVEN** a setting value is invalid
- **WHEN** batch gate resolution loads the setting
- **THEN** the value SHALL be ignored
- **AND** the resolution SHALL continue to the next source or default
- **AND** a WARN log SHALL include setting key and raw value
