# catalog.plan Requirements

## ADDED Requirements

### Requirement: V1 plan mapping

`catalog.plan` SHALL provide active plan definitions for STARTER, STANDARD, PRO, and DEMO.

#### Scenario: V1 plans are available

- WHEN `PlanCatalog.listActive()` is called
- THEN it includes STARTER, STANDARD, PRO, and DEMO unless explicitly deactivated

#### Scenario: Plans are cumulative by data

- GIVEN STANDARD is active
- WHEN its `featuresJson` is read
- THEN it contains core, STARTER, and STANDARD features

### Requirement: Plan JSON fields are valid JSON objects

`featuresJson` and `limitsJson` SHALL be parsed and exposed as JSON object nodes in `PlanView`.

#### Scenario: Parse features JSON object

- GIVEN `featuresJson` contains `{"sales.ticket.sell": true}`
- WHEN mapped to `PlanView`
- THEN `featuresJson().get("sales.ticket.sell").asBoolean()` is true
- AND the node is not a text node

#### Scenario: Reject invalid plan JSON on admin write

- GIVEN admin creates or updates a plan with invalid JSON
- WHEN validation runs
- THEN the request fails with `ProblemDetail` 400

## MODIFIED Requirements

### Requirement: PlanCatalog remains read-only public API

`PlanCatalog` SHALL remain the only public Java API for plan reads.

#### Scenario: Core subscription validates plan by code

- WHEN `core.subscription` needs to apply or change plan
- THEN it calls `PlanCatalog.findByCode(code)`
- AND it does not import `catalog.plan.internal.*`

### Requirement: Plan admin writes evict caches

Plan admin create/update/deactivate/soft-delete SHALL evict plan caches.

#### Scenario: Update plan evicts caches

- WHEN a plan is updated
- THEN active plans, by-code, and by-id caches are evicted after the write

## REMOVED Requirements

None.
