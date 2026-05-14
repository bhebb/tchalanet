# Spec: Web frontend workspace

## ADDED Requirements

### Requirement: Dedicated web workspace root

The frontend Nx workspace SHALL live under `tchalanet-web/`.

#### Scenario: Nx config is located in the web workspace

- **GIVEN** the product repository root
- **WHEN** a developer looks for Nx workspace configuration
- **THEN** `nx.json`, `package.json`, `pnpm-lock.yaml`, `pnpm-workspace.yaml`, `tsconfig.base.json`, `eslint.config.mjs` and `vitest.workspace.ts` SHALL be under `tchalanet-web/`
- **AND** the product root SHALL NOT be treated as the Nx workspace root.

#### Scenario: Web commands run from the web workspace

- **GIVEN** a developer or CI job wants to run frontend commands
- **WHEN** it runs pnpm or Nx commands
- **THEN** the command SHALL run from `tchalanet-web/` or use `working-directory: tchalanet-web`.

### Requirement: Main app is named Tchalanet Portal

The main Angular application SHALL be named `tchalanet-portal`.

#### Scenario: Main application path

- **GIVEN** the frontend workspace
- **WHEN** the main Angular application is located
- **THEN** it SHALL be under `tchalanet-web/apps/tchalanet-portal`.

#### Scenario: App name reflects responsibility

- **GIVEN** the app includes public pages, dashboards, role navigation, cashier/operator pages, tenant admin and platform admin surfaces
- **WHEN** the app is named
- **THEN** it SHALL use `tchalanet-portal`
- **AND** it SHALL NOT use generic names such as `tch-web`.

### Requirement: Product root remains product-level orchestration

The product root SHALL remain a product-level orchestration boundary, not a frontend workspace boundary.

#### Scenario: Root folder responsibilities

- **GIVEN** the product repository root
- **WHEN** a developer inspects top-level folders
- **THEN** backend code SHALL live under `tchalanet-server/`
- **AND** web code SHALL live under `tchalanet-web/`
- **AND** mobile code SHALL live under `tchalanet-mobile/`
- **AND** infra code SHALL live under `tchalanet-infra/`
- **AND** OpenSpec changes SHALL live under `openspec/`.

### Requirement: Web version source of truth

Web/Nx version references SHALL point to the web workspace files.

#### Scenario: Versions document references web files

- **GIVEN** `VERSIONS.md`
- **WHEN** it describes Web/Nx source-of-truth files
- **THEN** it SHALL reference `tchalanet-web/package.json`
- **AND** it SHALL reference `tchalanet-web/pnpm-lock.yaml`
- **AND** it SHALL NOT reference root `package.json` or root `pnpm-lock.yaml` as the Web/Nx source of truth.

### Requirement: Future web apps are not created prematurely

The workspace SHALL start with a single main app unless a separate runtime/deployment boundary is justified.

#### Scenario: Need for another app is evaluated

- **GIVEN** a new web surface is proposed
- **WHEN** it can reasonably live inside the multi-role portal
- **THEN** it SHALL be implemented inside `tchalanet-portal`
- **AND** a new app SHALL NOT be created.

#### Scenario: Separate app is justified

- **GIVEN** a web surface requires a separate deployment, runtime shell, access model or specialized device UX
- **WHEN** the decision is documented
- **THEN** a new app MAY be created under `tchalanet-web/apps/` with an explicit name such as `tchalanet-kiosk` or `tchalanet-backoffice`.
