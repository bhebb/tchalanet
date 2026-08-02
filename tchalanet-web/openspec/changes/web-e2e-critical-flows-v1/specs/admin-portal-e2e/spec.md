# Spec: Admin Portal E2E

Base URL :4302. Roles: `admin` (TENANT_ADMIN), `cashier`. UI-observable only —
no backend business assertions (limit/payout/idempotency stay in the pyramid).

## ADDED Requirements

### Requirement: Admin login dispatches to the admin space

A TENANT_ADMIN signing in SHALL be routed by `spaceDispatchGuard` to
`/app/admin/dashboard`, not to the platform space or forbidden.

#### Scenario: Admin lands on dashboard

- **WHEN** an activated TENANT_ADMIN completes sign-in
- **THEN** the browser lands on `/app/admin/dashboard`.

### Requirement: First-login activation guard

An un-activated TENANT_ADMIN reaching an app route SHALL be routed to
`/account/activation`; an activated one SHALL NOT.

#### Scenario: Un-activated admin is sent to activation

- **WHEN** an un-activated TENANT_ADMIN navigates to an `/app/admin/*` route
- **THEN** the browser is redirected to `/account/activation`.

### Requirement: Setup console renders its sections

`/app/admin/setup` SHALL render the setup/readiness sections and they SHALL be
navigable. The test SHALL NOT assert readiness computation.

#### Scenario: Setup sections present

- **WHEN** the admin opens `/app/admin/setup`
- **THEN** the setup/readiness sections are visible and navigable.

### Requirement: Limits screen renders and gives form feedback

`/app/admin/limits` SHALL render the policy list, and submitting an invalid value
in the editor SHALL show inline validation. The test SHALL assert the feedback,
not the limit engine result.

#### Scenario: Invalid limit value is rejected in the UI

- **WHEN** the admin submits an invalid value in the limit editor
- **THEN** inline validation is shown and the form is not accepted.

### Requirement: Cashier is isolated from tenant-admin routes

A `cashier` signed into the admin portal SHALL NOT reach a tenant-admin-only
route; the UI guard SHALL redirect or show `ForbiddenPage`.

#### Scenario: Cashier blocked from an admin-only route

- **WHEN** a cashier navigates to a tenant-admin-only `/app/admin/*` route
- **THEN** the browser shows forbidden or is redirected away from the route.

### Requirement: POS sale happy path shows a receipt in the UI

A cashier building and confirming a ticket on `/app/admin/pos/sale` SHALL see a
visible success/receipt feedback and the form SHALL reset for the next sale.
Correctness of the receipt content is owned by Unit/Integration.

#### Scenario: Confirmed sale gives visible feedback

- **WHEN** the cashier confirms a valid ticket
- **THEN** a success/receipt feedback is visible
- **AND** the sale form resets for the next entry.

### Requirement: POS rejected sale surfaces a rejection state

A sale the API rejects SHALL surface a rejection toast/state in the POS UI. The
test SHALL assert the UI reaction, not the rule that caused the rejection.

#### Scenario: Rejected sale is shown

- **WHEN** the API rejects a submitted sale
- **THEN** the POS UI shows a rejection state and does not show a receipt.

### Requirement: Seller pricing configuration supports override lifecycle

`/app/admin/seller-terminals/commissions` SHALL expose the seller's pricing
configuration entry point. The override screen SHALL allow an admin to save an
override and return the seller to tenant inheritance by removing it.

#### Scenario: Seller override is saved and removed

- **WHEN** the admin opens a seller's barèmes, changes a numeric value, and saves
- **THEN** the UI shows the override as active and sends the update request
- **WHEN** the admin removes the override
- **THEN** the UI shows that the seller inherits the tenant value.

### Requirement: Cross-origin portal handoff lands the authenticated shell

Following `/login/handoff` after signing in SHALL land the target portal's
authenticated shell without a re-login loop.

#### Scenario: Handoff completes

- **WHEN** a signed-in user follows the portal handoff
- **THEN** the target portal's authenticated shell renders and no re-login is
  prompted.
