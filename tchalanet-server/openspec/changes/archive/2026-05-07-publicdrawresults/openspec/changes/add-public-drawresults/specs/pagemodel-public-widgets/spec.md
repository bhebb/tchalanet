# Spec — PageModel public widgets

## MODIFIED Requirements

### Requirement: Public draw result widget uses core.drawresult query

The public PageModel draw results widget SHALL load global public draw results from `core.drawresult`, not tenant-scoped draw queries.

#### Scenario: Home widget loads public draw results

- **GIVEN** the public home PageModel contains a widget with source `public_draw_results`
- **WHEN** the provider loads the widget
- **THEN** it SHALL call `ListPublicDrawResultSlotsQuery`
- **AND** it SHALL pass `includeHistory=false`
- **AND** it SHALL pass `historyLimit=0`
- **AND** it SHALL receive slot metadata, latest result, and next expected result/countdown only.

### Requirement: PageModel provider does not require tenant context

The public PageModel draw result provider SHALL NOT require a tenant context.

#### Scenario: Public home without tenant context

- **GIVEN** no `TchContext` tenant is available
- **WHEN** the PageModel provider loads `public_draw_results`
- **THEN** it SHALL still load data from `core.drawresult`
- **AND** it SHALL NOT call tenant-scoped `core.draw` queries.

### Requirement: Widget response omits history content

The public home widget response SHALL not include historical results payload.

#### Scenario: Query returns empty history lists

- **GIVEN** the provider calls `ListPublicDrawResultSlotsQuery(includeHistory=false)`
- **WHEN** the provider maps query results to the widget payload
- **THEN** it SHALL either omit `history` from the widget payload or keep it as an empty list
- **AND** it SHALL NOT include 5/10 historical results.

### Requirement: PageModel source naming

The PageModel widget source SHALL use a clear public result source name.

#### Scenario: Widget configuration

- **GIVEN** a public home PageModel
- **WHEN** configuring the result widget
- **THEN** it SHOULD use source `public_draw_results`
- **AND** it SHOULD avoid ambiguous source `draws` for global public results.

Example:

```json
{
  "id": "home.draw_results",
  "type": "results_by_slot",
  "source": "public_draw_results",
  "props": {
    "slot_keys": ["NY_MID", "FL_EVE"],
    "show_countdown": true
  }
}
```
