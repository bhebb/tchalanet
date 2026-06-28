## ADDED Requirements

### Requirement: Tenant admin activation journey

The tenant admin UI SHALL provide a complete activation path that lets an admin finish the tenant onboarding checklist before operating sales.

#### Scenario: Admin completes the onboarding checklist

- **GIVEN** a tenant admin opens the administration area
- **WHEN** the tenant is not fully ready
- **THEN** the UI shows the seven onboarding steps with persisted status
- **AND** each step exposes a clear action to complete or review the missing setup
- **AND** completing the checklist leaves the tenant ready for draw generation, draw opening, seller setup, and ticket sale.

### Requirement: Maryaj gratis management

The tenant admin UI SHALL expose Maryaj gratis as a dedicated promotion surface, not a generic placeholder.

#### Scenario: Admin reviews or activates Maryaj gratis

- **GIVEN** a tenant admin opens Maryaj gratis
- **WHEN** the backend supports the default Maryaj gratis template
- **THEN** the page shows the current status and lets the admin activate the default configuration
- **AND** the page explains the business effect without exposing implementation jargon.

#### Scenario: Maryaj gratis backend action is unavailable

- **GIVEN** a tenant admin opens Maryaj gratis
- **WHEN** the activation endpoint is unavailable or returns an unsupported action
- **THEN** the action is disabled or fails with a clear recoverable message
- **AND** the page does not call known-missing endpoints blindly.

### Requirement: Seller terminal setup

The tenant admin UI SHALL let an admin create a seller terminal that can be used for selling.

#### Scenario: Admin creates a seller terminal

- **GIVEN** a tenant admin opens the new seller form
- **WHEN** the admin enters the required terminal information
- **THEN** the UI creates the seller terminal through the admin seller-terminal API
- **AND** the seller list shows the created terminal with status and actions
- **AND** the UI offers a path to use that terminal for selling.

### Requirement: Admin can act through a seller terminal

The tenant admin UI SHALL allow an admin to enter an explicit seller-terminal context for sale flows.

#### Scenario: Admin enters seller-terminal context

- **GIVEN** a tenant admin has at least one active seller terminal
- **WHEN** the admin selects a terminal to act with
- **THEN** the active seller-terminal context is visible in the sale surface
- **AND** POS calls include the seller-terminal context expected by the backend
- **AND** the admin can exit that seller-terminal context.

### Requirement: Admin sale uses POS capabilities

The tenant admin ticket sale UI SHALL reuse the POS web/API flow used by seller terminals instead of implementing a separate sale engine.

#### Scenario: Admin sells and prints a ticket

- **GIVEN** a tenant admin is acting through a seller terminal
- **AND** the tenant has open sellable draws
- **WHEN** the admin selects a draw, enters selections, and confirms the sale
- **THEN** the UI calls the POS sale capability
- **AND** the sold ticket is displayed with receipt information
- **AND** the admin can print the ticket.
