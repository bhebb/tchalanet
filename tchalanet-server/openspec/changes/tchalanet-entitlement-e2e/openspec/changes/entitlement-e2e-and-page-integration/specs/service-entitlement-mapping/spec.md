# Spec — Service-to-Entitlement Mapping

## Requirement: Business services declare entitlement needs

### Scenario: Terminal creation checks feature and quota
When creating a terminal
Then the HTTP boundary should require `terminal.licensing`
And quota should check `limits.terminals.max`
And critical handler should recheck quota if callable outside HTTP

### Scenario: Outlet creation checks quota
When creating an outlet
Then the HTTP boundary should check `limits.outlets.max`
And handler should recheck if creation is possible outside HTTP

### Scenario: User creation checks quota
When creating a tenant user
Then the HTTP boundary should check `limits.users.max`
And handler should recheck if creation is possible outside HTTP

### Scenario: Promotion creation checks feature
When creating a basic promotion rule
Then the API should require `promotion.rules.basic`
And, if plan limits promotion count, check `limits.promotion_rules.max`

### Scenario: Offline flow checks feature
When creating offline grant or accepting offline sync
Then API/handler should require relevant offline feature
And critical sync acceptance must validate in handler/application service

## Requirement: New API/handler review rule

### Scenario: Developer adds new API or handler
When designing a new API or handler
Then the developer must answer:
“Does this action depend on a paid feature or quota?”
And document the answer in the PR or OpenSpec task
And add entitlement checks if applicable
