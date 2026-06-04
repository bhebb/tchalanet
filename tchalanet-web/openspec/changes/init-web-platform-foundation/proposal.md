# Change: Init Web Platform Foundation

## Status

Proposed

## Context

The Angular/Nx web application needs a stable runtime foundation before implementing tenant admin, cashier web, public pages, and future superadmin screens.

The first web milestone is not a business page. It is a platform proof:

- shared contracts exist and are typed;
- Keycloak login/logout works;
- roles are extracted from the token;
- protected routes are enforced;
- dashboards per surface render the detected role/session;
- settings, i18n, theme, and PageModel are separate runtime capabilities;
- the public page can bootstrap from these runtime capabilities;
- the reusable UI core is minimal and born from actual flows, not an abstract component catalog.

## Goals

- Establish Angular/Nx technical baseline without over-splitting libraries.
- Add a dependency governance document so `package.json` does not become a dumping ground.
- Create frontend contracts for backend response conventions and runtime UI contracts.
- Configure Angular Material, NgRx minimal store/effects/devtools, and HTTP interceptors.
- Configure Keycloak authentication with login/logout test controls.
- Prove route protection for CASHIER, TENANT_ADMIN, and SUPER_ADMIN surfaces.
- Create empty dashboards that display the detected user, tenant, and roles.
- Configure runtime bootstrap with four separate capabilities: settings, i18n, theme, PageModel.
- Merge local frontend i18n with backend overrides at runtime; backend wins on duplicate keys.
- Create Tchalanet default theme and prepare Material-equivalent theme presets.
- Create minimal reusable UI components required for the first runtime proof.
- Create layout shells for public and private surfaces.
- Implement the first public home skeleton through runtime bootstrap.

## Design notes

- See `design.md` for V1 public/private runtime separation, guarded private layout routes, i18n initialization rules, and backend i18n surface override ordering.

## Non-goals

- No full design system before real tenant admin and POS flows.
- No cashier sale flow in this change.
- No full tenant admin CRUD implementation in this change.
- No full superadmin console in this change.
- No custom theme builder in V1.
- No Unleash integration in V1; settings acts as the temporary runtime feature-toggle mechanism.
- No large dependency additions without documented purpose.

## Critical decisions

### Bootstrap has four separate capabilities

Runtime bootstrap must treat these as independent:

1. settings
2. i18n
3. theme
4. PageModel

PageModel references i18n keys and layout/widget structure only. It does not carry full translations, full theme, or full settings.

### Keycloak before rich components

Auth must be proven before creating many UI components. Protected pages must show role/session data from Keycloak.

### Minimal NgRx

NgRx is used for app-level state only:

- auth/session;
- runtime settings;
- active language / i18n state;
- active theme;
- shell state;
- optional operational context for web cashier later.

Do not put all forms, paged lists, dashboards, and temporary modal state into NgRx prematurely.

### Lint/pre-commit

Configure lint/format commands early. Add pre-commit hooks only after the baseline is clean, unless the current workspace already passes quickly.
