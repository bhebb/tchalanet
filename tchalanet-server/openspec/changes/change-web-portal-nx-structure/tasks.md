# Tasks: Web portal Nx structure

## 1. Move Nx workspace

- [ ] Create/confirm `tchalanet-web/` as the frontend workspace root.
- [ ] Move root Nx files into `tchalanet-web/`:
  - [ ] `nx.json`
  - [ ] `package.json`
  - [ ] `pnpm-lock.yaml`
  - [ ] `pnpm-workspace.yaml`
  - [ ] `tsconfig.base.json`
  - [ ] `eslint.config.mjs`
  - [ ] `vitest.workspace.ts`
  - [ ] Nx helper scripts, if frontend-specific
- [ ] Move frontend-only `apps/` into `tchalanet-web/apps/`.
- [ ] Move frontend-only `libs/` into `tchalanet-web/libs/`.
- [ ] Keep product-level folders at root:
  - [ ] `tchalanet-server/`
  - [ ] `tchalanet-mobile/`
  - [ ] `tchalanet-infra/`
  - [ ] `tchalanet-edge-service/`
  - [ ] `tchalanet-docs/`
  - [ ] `openspec/`
  - [ ] `.agents/`
  - [ ] `.github/`

## 2. Rename main Angular application

- [ ] Rename the main Angular/Nx project to `tchalanet-portal`.
- [ ] Ensure app path is `tchalanet-web/apps/tchalanet-portal`.
- [ ] Update `project.json` / `workspace` project name references.
- [ ] Update imports, path aliases and test configs affected by the rename.
- [ ] Update README and developer commands to use `tchalanet-portal`.

## 3. Establish frontend library boundaries

- [ ] Keep shared frontend code under `tchalanet-web/libs/`.
- [ ] Use a first-pass library structure:
  - [ ] `core/`
  - [ ] `shared/`
  - [ ] `ui/`
  - [ ] `auth/`
  - [ ] `i18n/`
  - [ ] `page-model/`
  - [ ] `rendering-engine/`
  - [ ] `features/public/`
  - [ ] `features/tenant/`
  - [ ] `features/tenant-admin/`
  - [ ] `features/platform-admin/`
  - [ ] `features/cashier/`
  - [ ] `features/reporting/`
- [ ] Do not create future apps until there is a real need.

## 4. Update documentation

- [ ] Update `VERSIONS.md` Web/Nx source of truth:
  - [ ] from root `package.json` / `pnpm-lock.yaml`
  - [ ] to `tchalanet-web/package.json` / `tchalanet-web/pnpm-lock.yaml`
- [ ] Update root `README.md` to describe the product-level repo structure.
- [ ] Add or update `tchalanet-web/README.md` with frontend commands.
- [ ] Update agent instructions if they mention root Nx files.

## 5. Update CI and scripts

- [ ] Update GitHub Actions web jobs with `working-directory: tchalanet-web`.
- [ ] Update any root Makefile/Nx aliases.
- [ ] Ensure backend Maven jobs still run from `tchalanet-server/` or the intended root command.
- [ ] Ensure Flutter jobs still run from `tchalanet-mobile/`.

## 6. Validation

- [ ] From `tchalanet-web/`, run install.
- [ ] From `tchalanet-web/`, run lint.
- [ ] From `tchalanet-web/`, run tests.
- [ ] From `tchalanet-web/`, build `tchalanet-portal`.
- [ ] Verify no frontend command assumes product root as Nx workspace root.
- [ ] Verify backend and mobile builds are unaffected.
