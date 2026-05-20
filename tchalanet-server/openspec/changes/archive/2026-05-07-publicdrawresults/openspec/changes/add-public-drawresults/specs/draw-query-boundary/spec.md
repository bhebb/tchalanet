# Spec — draw query boundary

## MODIFIED Requirements

### Requirement: Public global result display does not use core.draw

Public global result display SHALL use `core.drawresult` queries rather than tenant-scoped `core.draw` queries.

#### Scenario: Public home displays latest results

- **GIVEN** a public home PageModel result widget
- **WHEN** the widget loads latest results
- **THEN** it SHALL use `ListPublicDrawResultSlotsQuery`
- **AND** it SHALL NOT use `ListLatestDrawsWithResultsQuery` if that query depends on tenant draw lifecycle.

### Requirement: core.draw remains available for tenant lifecycle views

`core.draw` queries SHALL remain valid for tenant-specific views where the tenant draw lifecycle matters.

#### Scenario: Vendor dashboard needs next tenant draws

- **GIVEN** a vendor dashboard needs the next sellable tenant draws
- **WHEN** it queries draw data
- **THEN** it MAY use `core.draw` tenant-scoped queries
- **AND** those queries MAY require tenant context.

#### Scenario: Admin dashboard needs draw lifecycle statuses

- **GIVEN** an admin dashboard needs SCHEDULED/OPEN/CLOSED/RESULTED/SETTLED draw lifecycle information
- **WHEN** it queries draw data
- **THEN** it SHOULD use `core.draw` queries.

### Requirement: Existing draw queries are reviewed, not removed by default

This change SHALL NOT force immediate deletion of existing `core.draw` latest/next queries.

#### Scenario: Keeping queries during transition

- **GIVEN** existing `core.draw` queries are still referenced by private dashboard or admin flows
- **WHEN** this change is implemented
- **THEN** those queries MAY remain
- **AND** public PageModel/publicdrawresults SHALL migrate away from them first.
