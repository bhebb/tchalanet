# Change: Move Nx web workspace under `tchalanet-web` and rename main app to `tchalanet-portal`

## Status

Proposed

## Context

The repository is organized as a product-level monorepo with separate top-level areas for backend, web, mobile, infra, docs and OpenSpec. The backend already owns its Maven/Spring world under `tchalanet-server/`. The mobile application is now Flutter under `tchalanet-mobile/`, and the old Ionic/Capacitor mobile app has been removed from the Nx workspace.

The current root still contains Nx-related files (`nx.json`, `package.json`, `pnpm-lock.yaml`, `pnpm-workspace.yaml`, `tsconfig.base.json`, etc.). This makes the repository root look like a frontend Nx workspace even though the root is actually the product orchestration boundary.

The current candidate app name `tch-web` is also too generic. The web application is not merely “the web app”; it is the main Tchalanet portal: public landing/home pages, authentication, role-based dashboards, tenant admin, platform admin, cashier/operator surfaces, reporting and settings.

## Decision

Move all Nx/web workspace files into `tchalanet-web/` and name the main Angular application:

```text
tchalanet-web/apps/tchalanet-portal
```

The product root remains a top-level orchestration root. `tchalanet-web/` becomes the frontend Nx workspace root.

## Goals

- Make repository boundaries explicit.
- Avoid mixing frontend Nx workspace concerns with backend Maven, Flutter mobile, infra and product docs at the product root.
- Give the main web app a name that reflects its real responsibility: the multi-role Tchalanet portal.
- Keep room for future specialized web apps without renaming the current app.
- Update documentation and version source-of-truth references accordingly.

## Non-goals

- No rewrite of Angular architecture in this change.
- No split into multiple Angular applications yet.
- No migration of backend, mobile, infra or docs modules.
- No business feature implementation.

## Naming

### Workspace root

```text
tchalanet-web/
```

### Main application

```text
tchalanet-portal
```

### Why not `tch-web`?

`tch-web` only says “web”, not what the application does. It hides the fact that this app is the main multi-role portal.

### Why not `dashboard`?

The app is broader than dashboards. It includes public pages, auth, navigation shell, administration areas, cashier/operator surfaces and settings.

## Target structure

```text
tchalanet/
  .agents/
  .github/
  openspec/
  tchalanet-docs/
  tchalanet-edge-service/
  tchalanet-infra/
  tchalanet-mobile/
  tchalanet-server/
  tchalanet-web/
    nx.json
    package.json
    pnpm-lock.yaml
    pnpm-workspace.yaml
    tsconfig.base.json
    eslint.config.mjs
    vitest.workspace.ts
    apps/
      tchalanet-portal/
        src/
          app/
            app.config.ts
            app.routes.ts
            shell/
            layouts/
            pages/
    libs/
      core/
      shared/
      ui/
      auth/
      i18n/
      page-model/
      rendering-engine/
      features/
        public/
        tenant/
        tenant-admin/
        platform-admin/
        cashier/
        reporting/
```

## Documentation updates

Update `VERSIONS.md` so Web/Nx source of truth points to:

```text
tchalanet-web/package.json
tchalanet-web/pnpm-lock.yaml
```

and no longer to the product root `package.json` / `pnpm-lock.yaml`.

## Migration notes

- Move existing Nx files from repository root to `tchalanet-web/`.
- Move current `apps/` and `libs/` into `tchalanet-web/` if they are frontend-only.
- Rename the Angular app project from the current generic name to `tchalanet-portal`.
- Adjust Nx project names, paths, tsconfig references, ESLint references and CI commands.
- Keep backend, mobile, infra and docs commands independent from Nx.

## Risks

- CI scripts may still assume Nx commands run from repository root.
- Developer scripts may break if they reference root `package.json`.
- Path aliases may need adjustment after moving `tsconfig.base.json`.
- GitHub actions may need explicit `working-directory: tchalanet-web`.

## Rollout

1. Move Nx workspace into `tchalanet-web/`.
2. Rename app to `tchalanet-portal`.
3. Update docs and version source-of-truth.
4. Update CI and local scripts.
5. Validate web build/test/lint from `tchalanet-web/`.
6. Validate backend/mobile are unaffected.
