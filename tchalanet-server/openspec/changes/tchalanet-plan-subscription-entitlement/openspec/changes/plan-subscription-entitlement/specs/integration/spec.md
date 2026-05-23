# entitlement integration Requirements

## ADDED Requirements

### Requirement: Business code does not parse plan JSON directly

Business modules SHALL use `EntitlementApi` for capability checks and SHALL NOT parse `PlanView.featuresJson` or `PlanView.limitsJson` directly.

#### Scenario: Offline sales check

- WHEN offline sales acceptance needs to verify availability
- THEN it calls `EntitlementApi.requireFeature(tenantId, "offline.sales.basic")`
- AND it does not call `PlanCatalog` directly

### Requirement: Initial quota enforcement points

V1 SHALL enforce quotas only at critical creation/bind operations.

#### Scenario: Terminal create quota

- WHEN a terminal is created
- THEN handler checks `limits.terminals.max` against active terminal count

#### Scenario: User create quota

- WHEN a user is invited or created
- THEN handler checks `limits.users.max` against active user count

#### Scenario: Outlet create quota

- WHEN an outlet is created
- THEN handler checks `limits.outlets.max` against active outlet count

#### Scenario: Mobile device bind quota

- WHEN a mobile device is bound
- THEN handler checks `limits.mobile_devices.max` against active bound mobile devices

### Requirement: Optional modules use feature gates

V1 SHALL gate optional modules at coarse boundaries.

#### Scenario: Promotion basic create

- WHEN a promotion rule is created
- THEN backend requires `promotion.rules.basic`
- AND checks `limits.promotion_rules.max` if applicable

#### Scenario: Payout approval workflow

- WHEN payout approval workflow action is requested
- THEN backend requires `payout.approval.workflow`

#### Scenario: Email notification delivery

- WHEN email notification delivery is requested for tenant feature path
- THEN backend requires `notification.email`

### Requirement: UI receives capabilities

Tenant UI bootstrap or page models SHALL be able to include tenant capabilities to hide/disable actions.

#### Scenario: POS hides unavailable offline mode

- GIVEN tenant does not have `offline.sales.basic`
- WHEN POS/mobile bootstrap reads capabilities
- THEN offline actions are hidden or disabled

## MODIFIED Requirements

None.

## REMOVED Requirements

None.
