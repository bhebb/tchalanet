# platform-referentials Specification

## Purpose
TBD - created by archiving change platform-referentials-async-lists-v1. Update Purpose after archive.
## Requirements
### Requirement: Platform referential lists use resource-backed async views

Platform catalog referential list pages SHALL load read data through data-access resource methods and
render loading, empty, ready, and error states through the shared async view component.

#### Scenario: Platform operator opens a referential list

- **GIVEN** a platform operator is authenticated in the platform portal
- **WHEN** they open a catalog referential list page
- **THEN** the page requests data through the catalog data-access service
- **AND** the template renders the list through `tch-async-view`
- **AND** backend read errors are displayed through the shared async error view model.

### Requirement: Platform referential navigation exposes all implemented CRUD entries

The platform private navigation SHALL expose implemented catalog referential CRUD pages, including
sub-referentials that previously existed only as placeholder routes.

#### Scenario: Platform operator opens referentials navigation

- **GIVEN** the platform private shell navigation is loaded
- **WHEN** the operator expands the catalog referentials section
- **THEN** the navigation includes the implemented referential pages
- **AND** every listed entry resolves to an Angular route.

### Requirement: Tenant page model navigation is available under enterprise settings

The tenant admin private navigation SHALL expose tenant page model management under the enterprise
area so tenant admins can reach page model pages generated from page templates.

#### Scenario: Tenant admin opens enterprise navigation

- **GIVEN** the tenant admin private shell navigation is loaded
- **WHEN** the admin expands the enterprise section
- **THEN** a page models entry is visible
- **AND** it routes to the tenant admin page model area.

