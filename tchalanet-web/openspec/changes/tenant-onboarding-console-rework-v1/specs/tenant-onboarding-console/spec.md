## ADDED Requirements

### Requirement: Admin setup checklist is built from stateless section components

The `admin-portal` setup checklist page (`admin-complete-tenant-config.page`) SHALL compose its
per-domain status cards from dedicated `components/` under its own feature folder, not from
duplicated inline template blocks.

#### Scenario: Page renders a domain status card

- **GIVEN** the setup overview has loaded a set of readiness sections (identity, address,
  games/pricing, draw channels, theme, promotions, seller-terminal)
- **WHEN** the page renders the checklist
- **THEN** each card is rendered by a single stateless `setup-checklist-card` component receiving
  its id, label, icon, status, badge kind, description, CTA route, and blocking flag via `input()`
- **AND** no card markup is duplicated inline in the page template.
- **AND** seeded/default domains such as Maryaj gratis/promotions and subscription are not shown as
  required setup work.

#### Scenario: Section fails independently

- **GIVEN** one readiness section's data failed to load (per `error-management.md`, section-owned
  notices)
- **WHEN** the page renders the checklist
- **THEN** only the affected `setup-checklist-card` shows its section error
- **AND** the rest of the checklist stays usable.
- **AND** merged cards retain every relevant target (for example identity + address, and draw
  channels + draw-sales-matrix) so no section-owned notice is hidden.

### Requirement: Platform provisioning vocabulary matches the current backend entitlement model

The platform tenant provisioning page's domain/status/step label maps SHALL reflect the backend's
current `TenantProvisioningOrchestrator` vocabulary (`domainStatuses`, `nextSteps`), with no stale
label for a domain, status, or step the backend no longer returns, and no missing label for one it
does.

#### Scenario: Backend returns a current domain/status/step code

- **GIVEN** `POST /platform/tenant-onboarding/preview` or `/provision` returns a domain, status, or
  next-step code
- **WHEN** the provisioning page renders the health card or next-steps card
- **THEN** the code resolves to a translated, human-readable label
- **AND** no raw backend code is shown to the user.

#### Scenario: Backend no longer returns a retired code

- **GIVEN** a domain/status/step code was retired by a backend change (e.g. quota renames dropping
  outlet/cashier-era vocabulary)
- **WHEN** the provisioning page's label maps are audited
- **THEN** the retired code's label entry is removed from the page.

### Requirement: Extracted console components stay presentational

Components extracted from the setup checklist or provisioning pages SHALL receive all data via
`input()` and emit via `output()` only; they SHALL NOT call an API/data-access service or perform
navigation logic beyond rendering a `routerLink`.

#### Scenario: A component needs data it does not have

- **GIVEN** a `components/` component under `setup/` or `tenants/onboarding/` needs additional data
- **WHEN** the component is implemented or reviewed
- **THEN** the data is passed down from the owning page via `input()`
- **AND** the component does not inject an API or data-access service to fetch it itself.
