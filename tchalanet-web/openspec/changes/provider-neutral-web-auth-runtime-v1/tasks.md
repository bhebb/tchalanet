# Tasks

## 1. Inventory and contract

- [x] Inventory provider-specific dependencies in core auth and runtime startup.
- [x] Define a provider-neutral web auth client contract.

## 2. Implementation

- [x] Make Firebase implement the auth client contract.
- [x] Make session orchestration consume the neutral auth client.
- [x] Replace the Firebase-specific bearer interceptor with a neutral interceptor.
- [x] Bind the configured provider adapter at the composition root.
- [x] Encapsulate Firebase SDK composition behind a provider-adapter provider function.
- [x] Point private bootstrap at `/runtime/private`.
- [x] Align the auth convention with backend-derived session identity.

## 3. Tests and validation

- [x] Add focused session, bearer-interceptor, and private-bootstrap service tests.
- [x] Run TypeScript compilation and ESLint on changed portal files.
- [x] Run `tch-portal` tests (`22` files, `92` tests).
- [ ] Run `tch-portal` lint.
      Blocked outside this change: two existing public Tchala templates use forbidden `autofocus`.
- [ ] Run `tch-portal` build.
      Blocked by a reproducible esbuild service deadlock during application bundle generation;
      the test target and both TypeScript configs compile successfully.
