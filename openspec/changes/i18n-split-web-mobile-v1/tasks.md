# Tasks

- [x] Document the cross-project OpenSpec change.
- [x] Update web and mobile i18n conventions for multi-file bundles.
- [x] Implement web multi-bundle loading and tests.
- [x] Implement mobile multi-bundle loading and tests.
- [x] Split existing web and mobile local bundles into locale folders.
- [x] Add i18n inventory/reference tooling.
- [x] Run focused validation and record residual risks.

## Validation notes

- `openspec validate i18n-split-web-mobile-v1 --strict` passes.
- `pnpm i18n:inventory -- --check` passes with zero referenced missing keys.
- `pnpm nx test shared-config` passes.
- `/Users/bhebb/fvm/versions/3.44.0/bin/flutter test test/core/i18n/i18n_contract_test.dart` passes.
- `pnpm nx test tch-portal` is blocked by an existing `apps/tch-portal/src/app/core/auth/auth.guard.spec.ts` import error for `../../shared/types`.
- `pnpm nx build tch-portal --configuration=development` hit an esbuild deadlock twice after Angular warnings, with no i18n TypeScript error reported.
