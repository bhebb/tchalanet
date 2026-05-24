# Spec — Entitlement E2E

## Requirement: Tenant capabilities are resolved from subscription + plan

### Scenario: STARTER tenant receives STARTER capabilities
Given a tenant has an ACTIVE subscription with plan code `STARTER`
When the tenant calls `GET /tenant/me/capabilities`
Then the response includes `planCode = STARTER`
And `subscriptionActive = true`
And feature `sales.ticket.sell` is available
And feature `offline.sales.basic` is not available
And limit `limits.terminals.max` equals `2`

### Scenario: PRO tenant receives PRO capabilities
Given a tenant has an ACTIVE subscription with plan code `PRO`
When the tenant calls `GET /tenant/me/capabilities`
Then feature `offline.sales.basic` is available
And feature `promotion.rules.basic` is available
And limit `limits.terminals.max` equals `30`

### Scenario: Suspended subscription disables capabilities
Given a tenant has a SUSPENDED subscription
When the tenant calls `GET /tenant/me/capabilities`
Then `subscriptionActive = false`
And paid features are unavailable

### Scenario: Missing subscription returns safe empty snapshot
Given a tenant has no subscription
When the tenant calls `GET /tenant/me/capabilities`
Then the response is safe
And `subscriptionActive = false`
And features are empty or false
And limits are empty or zero/default according to API contract

## Requirement: Subscription changes invalidate entitlement cache

### Scenario: Change plan invalidates cached snapshot
Given tenant A has plan `STARTER`
And capabilities have already been loaded
When platform changes tenant A subscription to `PRO`
Then the next capabilities request returns `planCode = PRO`
And `offline.sales.basic = true`

### Scenario: Tenant snapshots are isolated
Given tenant A has `STARTER`
And tenant B has `PRO`
When both tenants request capabilities
Then tenant A does not receive tenant B features
And tenant B does not receive tenant A limits

## Requirement: Quota gates enforce configured limits

### Scenario: Terminal quota blocks creation
Given tenant has `limits.terminals.max = 2`
And tenant already has 2 active terminals
When tenant attempts to create another terminal
Then the API returns an entitlement limit error

### Scenario: User quota blocks creation
Given tenant has `limits.users.max = 5`
And tenant already has 5 active users
When tenant attempts to create another user
Then the API returns an entitlement limit error

### Scenario: Outlet quota blocks creation
Given tenant has `limits.outlets.max = 1`
And tenant already has 1 active outlet
When tenant attempts to create another outlet
Then the API returns an entitlement limit error
