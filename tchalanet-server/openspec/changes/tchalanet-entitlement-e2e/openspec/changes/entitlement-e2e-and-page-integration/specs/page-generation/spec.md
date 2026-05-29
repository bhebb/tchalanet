# Spec — Page Generation & Public Plan Display

## Requirement: Public pages can display active plans

### Scenario: Public pricing lists active plans
Given `catalog.plan` has active STARTER, STANDARD, PRO
When public pricing page model is loaded
Then the response includes active public plans
And inactive/deleted plans are excluded

### Scenario: Public pricing shows curated features
Given a plan has many raw feature keys
When public pricing page model is loaded
Then the response shows a curated feature list
And does not expose internal/demo/hidden feature keys by default

## Requirement: Tenant/admin page generation uses capabilities

### Scenario: Tenant admin actions reflect capabilities
Given tenant has STARTER capabilities
When admin dashboard page model is loaded
Then unavailable PRO actions are hidden or disabled according to presentation rule

### Scenario: POS/mobile hides unavailable seller actions
Given tenant does not have offline feature
When POS/mobile page model is loaded
Then offline sale/grant actions are hidden
