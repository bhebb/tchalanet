# Spec — Multitenant Onboarding + Entitlement

## Requirement: Onboarding assigns a default plan

### Scenario: New live tenant receives default STARTER plan
Given platform admin creates a live tenant
When onboarding completes
Then tenant has an ACTIVE subscription
And subscription plan code is `STARTER`
And tenant can load capabilities
And capabilities include STARTER features

### Scenario: Demo tenant receives DEMO plan
Given platform admin creates a demo tenant
When onboarding completes
Then tenant has an ACTIVE or TRIAL subscription with plan code `DEMO`
And tenant mode is `DEMO` or equivalent sandbox marker
And capabilities include demo features
And external delivery is mock/whitelisted

## Requirement: Onboarding remains tenant-isolated

### Scenario: Tenant A onboarding does not affect tenant B
Given tenant A is onboarded with STARTER
And tenant B is onboarded with PRO or DEMO
When capabilities are loaded for both tenants
Then each tenant receives only its own plan snapshot
