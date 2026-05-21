# Spec — PageModel cashier dashboard split

## MODIFIED Requirements

### Requirement: Existing cashier dashboard becomes web-only

The existing `private.dashboard.cashier` PageModel SHALL be repositioned as a web dashboard model.

#### Scenario: Reposition cashier PageModel

- **GIVEN** the existing PageModel contains many rows such as identity, overview, quick_sale, top_selections, recent_tickets, pending_approvals, next_draws, session, and limits
- **WHEN** migrating to the new surface model
- **THEN** the web model is renamed or copied to `private.dashboard.cashier.web`
- **AND** it is not used as the mobile POS home.

### Requirement: PageModel is layout/config, not operational truth

PageModel SHALL NOT decide real-time POS readiness.

#### Scenario: POS runtime state

- **GIVEN** the app needs to know if the seller can sell
- **WHEN** rendering mobile POS home
- **THEN** it uses the cashier home BFF response for operational context, session, draw, and action availability
- **AND** it does not infer those values from PageModel.

### Requirement: Pending approvals are not shown in mobile POS V1

Mobile POS V1 SHALL NOT expose pending approval workflow.

#### Scenario: Old dashboard had pending approvals

- **GIVEN** the old PageModel includes `dashboard.cashier.pending_approvals`
- **WHEN** creating mobile POS home
- **THEN** pending approvals are excluded
- **AND** any approval-required sale is surfaced as a required change in the sale flow, not as POS home state.
