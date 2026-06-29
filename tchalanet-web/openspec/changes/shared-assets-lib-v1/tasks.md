# Tasks

- [x] Create `libs/shared-assets` with a minimal TypeScript API and public asset tree.
- [x] Move shared static assets out of `apps/tch-portal/public/assets`.
- [x] Configure portal apps to copy shared assets into `/assets/**`.
- [x] Update shared asset path references and docs.
- [ ] Validate focused Nx builds/lint. OpenSpec, `shared-assets:lint`, `shared-config:test`, widget contract, and app `tsc --noEmit` pass; Angular app builds remain blocked by the known esbuild deadlock.
