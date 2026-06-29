## ADDED Requirements

### Requirement: Independent deployable web apps

The workspace SHALL provide independently buildable and deployable Angular/Nx apps for public,
admin, and platform surfaces.

#### Scenario: Admin portal builds alone

- **WHEN** `admin-portal` is built or served
- **THEN** it does not require `public-portal` or `platform-portal`
- **AND** it can use login from `libs/core/auth`.

#### Scenario: Public portal builds alone

- **WHEN** `public-portal` is built or served
- **THEN** it does not import private admin, POS, or platform features.

#### Scenario: Platform portal builds alone

- **WHEN** `platform-portal` is built or served
- **THEN** it does not depend directly on `admin-portal` or `public-portal`.

#### Scenario: POS stays future

- **WHEN** web-apps-split-v1 is implemented
- **THEN** it does not create `apps/pos-portal`
- **AND** POS remains lazy under `admin-portal`.

### Requirement: API library is technical only

`libs/api` SHALL own backend contracts, HTTP primitives, and the generic backend client as folders
inside one Nx library in V0. It SHALL NOT own feature business clients.

#### Scenario: Shared backend contracts are needed

- **WHEN** frontend code needs `ApiResponse<T>`, `ProblemDetail`, `TchPage<T>`, `ApiNotice`,
  `ServiceStatus`, `ActionItem`, or `NavigationDestination`
- **THEN** those contracts are owned by the `libs/api/src/lib/contracts` folder.

#### Scenario: Feature business endpoint is added

- **WHEN** a feature needs a business endpoint client
- **THEN** the client is placed in that feature's `data-access/`
- **AND** it is not added to `libs/api/clients`.

### Requirement: Core libraries own reusable runtime capabilities

Reusable authentication and i18n runtime behavior SHALL live under `libs/core`.

#### Scenario: A portal needs login

- **WHEN** `admin-portal`, `platform-portal`, or another portal needs login
- **THEN** it uses the shared login page/routes from `libs/core/auth`.

#### Scenario: A portal needs i18n runtime

- **WHEN** a portal needs locale state, translation loading, or bundle merge behavior
- **THEN** it uses `libs/core/i18n`.

### Requirement: Web UX libraries own shell, errors, and navigation

Reusable web UX capabilities SHALL live under `libs/web`.

#### Scenario: A portal renders a shell

- **WHEN** public, admin/private, or platform shell layout is shared
- **THEN** shared shell code lives in `libs/web/shell`
- **AND** no separate `libs/shell` is created.

#### Scenario: A portal renders API errors

- **WHEN** a page, section, field, or API error presenter is shared
- **THEN** it lives in `libs/web/errors`.

#### Scenario: Web navigation is shared by shells

- **WHEN** navigation code is reusable but still belongs to shell layout
- **THEN** it lives in `libs/web/shell`
- **AND** no separate `libs/web/navigation` library is created in V0.

### Requirement: Feature structure is page-first

Features SHALL be organized by surface, feature, page/flow, components, and data-access.

#### Scenario: Routed page is added

- **WHEN** a routed page is added
- **THEN** it lives in its own folder
- **AND** it uses separate `.page.ts`, `.page.html`, and `.page.scss` files.

#### Scenario: Page has local state

- **WHEN** page state is specific to one page
- **THEN** the store lives beside the page as `*.page.store.ts`
- **AND** it is not placed in `data-access/`.

#### Scenario: Component is extracted

- **WHEN** a component is only used by one page
- **THEN** it lives under that page's `components/`
- **AND** it is promoted to feature/global only after real reuse exists.

### Requirement: Seller-terminal is lazy under admin

The admin portal SHALL keep seller-terminal/POS code lazy-loaded in V0.

#### Scenario: Admin does not use sale

- **WHEN** an admin user does not navigate to POS/seller-terminal routes
- **THEN** sale/POS feature code is not eagerly loaded.

#### Scenario: Admin tenant disables sale

- **WHEN** sale/POS is not enabled for an admin tenant
- **THEN** the route or navigation is hidden/disabled without breaking the rest of admin.

### Requirement: Proxy supports sub-route apps

Local and deployable proxy configuration SHALL support sub-routes for each app and backend API.

#### Scenario: One host routes to split apps

- **WHEN** a request targets `/public/**`, `/admin/**`, `/platform/**`, or `/api/v1/**`
- **THEN** proxy configuration routes the request to the owning app or backend API
- **AND** feature code uses relative API URLs.

### Requirement: Angular modern defaults

New and migrated code SHALL use modern Angular patterns by default.

#### Scenario: New component is created

- **WHEN** a component is generated or migrated
- **THEN** it uses standalone APIs and `ChangeDetectionStrategy.OnPush`
- **AND** it uses separate template/style files when non-trivial.

#### Scenario: New form is created

- **WHEN** a new form is created
- **THEN** it uses signal forms unless a local decision documents why not.

#### Scenario: Read-oriented HTTP state is added

- **WHEN** a read-oriented HTTP load fits Angular signal resource semantics
- **THEN** it uses `httpResource` or `resource`
- **AND** command/mutation flows remain explicit in stores/services.

### Requirement: SSR-ready surfaces

The split apps SHALL avoid patterns that block future SSR/hydration.

#### Scenario: Code needs browser APIs

- **WHEN** code needs `window` or `document`
- **THEN** access is guarded or isolated behind injectable services
- **AND** constructors/top-level code remain SSR-safe.

#### Scenario: Public portal renders public content

- **WHEN** public routes are implemented
- **THEN** SSR/SSG is prioritized before admin SSR.

### Requirement: UI and style conventions remain enforced

Features and shared UI SHALL use the established theme/style/component ownership model.

#### Scenario: Feature needs styling

- **WHEN** feature styles need colors, spacing, or component-local tokens
- **THEN** they use `--tch-*` and `--comp-*` tokens
- **AND** they do not hardcode brand colors.

#### Scenario: Shared UI component is created

- **WHEN** a component is placed in `libs/ui/components`
- **THEN** it is stateless and reusable
- **AND** it does not call HTTP directly.
