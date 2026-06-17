# Tasks

## 1. Contract and implementation

- [x] Inventory provider-specific mobile session dependencies.
- [x] Extend the authenticated runtime model with user and tenant context.
- [x] Build mobile application sessions from authenticated runtime data.
- [x] Use `/runtime/private` as the canonical authenticated bootstrap endpoint.
- [x] Attach bearer credentials to `/runtime/private`.
- [x] Remove OIDC/Keycloak SDK dependencies from the core network interceptor.
- [x] Integrate the official Firebase Auth Flutter adapter.
- [x] Use the generated FlutterFire Android configuration for `tchalanet-39115`.
- [x] Implement a Material 3 operator login page adaptive for mobile and POS.
- [x] Align the mobile auth convention.

## 2. Tests and validation

- [x] Add focused auth repository tests for backend-derived sessions.
- [ ] Run Flutter analyze.
      Blocked outside this change by the existing `unnecessary_underscores` info in
      `test/core/network/request_id_interceptor_test.dart`.
- [x] Run focused Flutter auth/runtime/widget tests (`9` tests).
- [x] Run full Flutter tests (`73` tests).
- [x] Build the Android debug application against the real Firebase configuration
      (`FIREBASE_AUTH_EMULATOR=false`).
