# Tasks

## 1. Target model

- [ ] Replace `UUID targetId` in controllers/ports with typed target model.
- [ ] Create canonical `PolicyTarget` or separate `LimitTarget`/`AutonomyTarget`.
- [ ] Supported MVP target types:
  - TENANT
  - OUTLET
  - USER
- [ ] Remove TERMINAL from MVP policy target handling.
- [ ] For TENANT target, `targetId` is implicit/current tenant.

## 2. LimitDefinition

- [ ] Decide `limit_definition` is global/system rule definition.
- [ ] Queries:
  - `ListLimitDefinitionsQuery`
  - `GetLimitDefinitionByIdQuery`
  - `GetLimitDefinitionByRuleKeyQuery`
- [ ] Commands:
  - `UpsertLimitDefinitionCommand` restricted SUPER_ADMIN/platform if global.
  - `DeleteLimitDefinitionCommand` restricted.
- [ ] Add admin/platform endpoints as appropriate.

## 3. LimitAssignment

- [ ] Tenant-scoped assignments.
- [ ] Queries:
  - `ListLimitAssignmentsByTargetQuery`
  - `GetEffectiveLimitPolicyQuery`
  - `ListEffectiveLimitPoliciesQuery`
- [ ] Commands:
  - `UpsertLimitAssignmentCommand`
  - `DeleteLimitAssignmentCommand`
- [ ] Use tenant from context.
- [ ] Add audit logs.

## 4. AutonomyPolicyRule

- [ ] Tenant-scoped rules.
- [ ] Queries:
  - `GetAutonomyOverviewQuery`
  - `GetEffectiveAutonomyPolicyQuery`
  - `ListAutonomyRulesQuery`
- [ ] Commands:
  - `UpsertAutonomyPolicyRuleCommand`
  - `DeleteAutonomyPolicyRuleCommand`
- [ ] Use tenant from context.
- [ ] Add audit logs.

## 5. Evaluation service

- [ ] Create application service:
  - `EvaluateSalePolicyCommand/Query` or `SalePolicyEvaluator`
  - `EvaluatePayoutPolicyCommand/Query` or `PayoutPolicyEvaluator`
- [ ] Combined output:
  - decision: ALLOW/WARN/REQUIRE_APPROVAL/BLOCK
  - reasons
  - requiredApprovalRole
  - effective target chain
  - policy ids involved
- [ ] Sales and payout consume this service.
- [ ] No duplicate limit calculations in features.

## 6. Controller cleanup

- [ ] Use `CommandBus`, never direct handler injection.
- [ ] Use `@CurrentContext`.
- [ ] No raw UUID target params.
- [ ] No actor/tenant from request body.
- [ ] Return `ApiResponse<T>`.

## 7. Port cleanup

- [ ] Remove `Instant.now()` from default methods.
- [ ] Inject `Clock` in handlers/services.
- [ ] Ensure read ports are read-only.
- [ ] Ensure writer ports do not expose persistence details.

## 8. HTTP endpoints

Core endpoints remain canonical; feature overview may aggregate them.

- [ ] `GET /admin/policies/limits/definitions`
- [ ] `PUT /admin/policies/limits/definitions`
- [ ] `DELETE /admin/policies/limits/definitions/{id}`
- [ ] `GET /admin/policies/limits/assignments`
- [ ] `PUT /admin/policies/limits/assignments`
- [ ] `DELETE /admin/policies/limits/assignments/{id}`
- [ ] `GET /admin/policies/autonomy`
- [ ] `PUT /admin/policies/autonomy`
- [ ] `DELETE /admin/policies/autonomy/{id}`
- [ ] `GET /admin/policies/effective?targetType=...&targetId=...`

## 9. Tests

- [ ] Tenant default applies when no outlet/user override.
- [ ] Outlet override wins over tenant default.
- [ ] User override wins over outlet default where intended.
- [ ] REQUIRE_APPROVAL returns approval role from autonomy.
- [ ] BLOCK with requireApprovalOnBlock produces approval request where configured.
