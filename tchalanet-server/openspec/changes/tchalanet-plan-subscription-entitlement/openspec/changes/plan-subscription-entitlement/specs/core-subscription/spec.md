# core.subscription Requirements

## ADDED Requirements

### Requirement: Subscription handlers validate target plans

Apply and change plan handlers SHALL validate target plan existence and active status through `PlanCatalog`.

#### Scenario: Apply active plan

- GIVEN plan PRO exists and is active
- WHEN a tenant plan is applied with code PRO
- THEN the subscription is created or updated with planCode PRO

#### Scenario: Reject missing plan

- GIVEN plan UNKNOWN does not exist
- WHEN apply/change plan is requested
- THEN the handler fails with `ProblemDetail` not found or bad request

#### Scenario: Reject inactive plan

- GIVEN plan PRO exists but active=false
- WHEN apply/change plan is requested
- THEN the handler fails with `subscription.plan_inactive`

### Requirement: Subscription domain is time deterministic

The `Subscription` domain model SHALL NOT call `Instant.now()`.

#### Scenario: Cancel uses handler-provided time

- GIVEN a subscription is ACTIVE
- WHEN `cancelNow(now)` is called
- THEN `endsAt`, `canceledAt`, and `updatedAt` equal `now`

### Requirement: Subscription events publish after commit

Subscription lifecycle changes SHALL publish after-commit events for downstream consumers.

#### Scenario: Change plan publishes event

- WHEN a subscription plan is changed from STARTER to PRO
- THEN `TenantSubscriptionPlanChangedEvent` is published after commit
- AND it includes tenantId, subscriptionId, oldPlanCode, newPlanCode, occurredAt

## MODIFIED Requirements

### Requirement: Tenant-scoped subscription controllers use context tenant

Tenant/admin subscription endpoints SHALL derive tenant id from `TchRequestContext` and SHALL NOT bind tenant id from request body.

#### Scenario: Tenant admin changes current subscription

- WHEN `/admin/subscription/change` is called
- THEN controller uses `ctx.effectiveTenantIdRequired()`
- AND maps request DTO to command

### Requirement: Platform subscription controllers may target tenant by path

Platform/SUPER_ADMIN subscription routes MAY use `{tenantId}` path variables and MUST be audited.

#### Scenario: Super admin applies tenant plan

- WHEN `/platform/tenants/{tenantId}/subscription/apply` is called
- THEN the target tenant comes from the path variable
- AND authorization requires SUPER_ADMIN/platform scope

## REMOVED Requirements

None.
