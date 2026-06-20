# Tasks

- [x] Inventory existing routes, placeholder page, reusable admin UI, and tenant create logic.
- [x] Add reusable admin detail layout component.
- [x] Add reusable Tch identity card component.
- [x] Add reusable provisioning health card component.
- [x] Replace `PlatformTenantProvisioningPage` with functional form, preview, submit, and result state.
- [x] Add i18n fallback keys for FR/EN/HT.
- [ ] Validate focused web build/checks.
  - `pnpm exec tsc -p apps/tch-portal/tsconfig.app.json --noEmit` passes.
  - `pnpm exec ngc -p apps/tch-portal/tsconfig.app.json` passes with pre-existing warnings outside this page.
  - `pnpm nx build tch-portal --configuration development` is blocked by an esbuild service deadlock.
