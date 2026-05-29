# Spec — Dashboard Usage Counts

## Requirement: Tenant admin dashboard returns plan usage counts

### Scenario: Dashboard shows terminal usage
Given tenant has `limits.terminals.max = 10`
And tenant has 4 active terminals
When admin dashboard loads
Then it returns terminals usage `4`
And terminal limit `10`

### Scenario: Dashboard shows outlet usage
Given tenant has `limits.outlets.max = 3`
And tenant has 2 active outlets
When admin dashboard loads
Then it returns outlets usage `2`
And outlet limit `3`

### Scenario: Dashboard shows user usage
Given tenant has `limits.users.max = 15`
And tenant has 7 active users
When admin dashboard loads
Then it returns users usage `7`
And user limit `15`

## Requirement: Dashboard counts are not enforcement

### Scenario: Enforcement still happens at write boundary
Given dashboard displays remaining terminal slots
When another request creates terminals concurrently
Then terminal creation handler still validates quota before saving
