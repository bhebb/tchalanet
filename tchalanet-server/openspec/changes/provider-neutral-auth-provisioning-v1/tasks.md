# Tasks: provider-neutral-auth-provisioning-v1

## 1. Inventory and guardrails

- [x] Compare the proposed plan with the existing backend identity implementation.
- [x] Confirm provider SDK architecture gates exist and business modules do not import provider SDKs.
- [x] Record the backend gap analysis in `inventory.md`.
- [x] Make the OpenAPI bearer scheme provider-neutral.
- [x] Remove provider-specific names from the public tenant identity admin response.
- [x] Run focused OpenAPI, identity admin, and architecture tests.

## 2. Provider-neutral provisioning

- [x] Define a provider-neutral provisioning port under `platform.identity.api`.
- [x] Adapt Firebase provisioning behind the provider-neutral port.
- [x] Route tenant user provisioning through the configured provider adapter.
- [x] Define behavior for providers that do not support managed provisioning.
- [x] Add provisioning adapter and compensation tests.

## 3. Identity contract cleanup

- [x] Replace transitional `KeycloakUserSub` names in provider-neutral public views and requests.
- [x] Replace provider-specific sync status naming outside provider diagnostics.
- [x] Add an explicit manual external identity link admin endpoint.
- [x] Standardize unknown mapping failures as `403 external_identity.not_linked`.

## 4. Runtime and validation

- [x] Decide whether canonical private runtime stays `/tenant/runtime/bootstrap` or aliases `/runtime/private`.
- [x] Decide whether POS runtime belongs in this change or the terminal security change.
- [x] Add local-jwt, Firebase emulator, Firebase live, and Keycloak authentication matrix tests.
- [ ] Run `./mvnw verify`.
  Blocked outside this change: `TenantConfigValidatorTest.communicationBlankCurrencyIsRejected`
  fails before core/features/app verification.
