# Change: Provider-Neutral Authentication and Provisioning V1

## Why

The backend already supports Firebase, Firebase Emulator, Keycloak, and local JWT identity
verification behind `platform.identity`, but provider-specific names and provisioning dependencies
still leak through parts of the public contract and internal orchestration.

Tchalanet must remain the source of truth for AppUser, tenant membership, roles, permissions,
operational context, RLS, and audit regardless of the configured authentication provider.

## What

- Inventory the existing provider-neutral identity implementation and remaining provider leaks.
- Keep provider SDKs confined to `platform.identity.internal` adapters.
- Make public OpenAPI and identity admin contracts provider-neutral.
- Introduce a provider-neutral provisioning port and route admin provisioning through it.
- Align runtime endpoints and tests with provider-neutral identity resolution.

## Impact

- Backend only for this change.
- Public identity admin response fields may be renamed from Keycloak-specific names.
- Provider-specific diagnostics may remain provider-specific under internal/platform ops endpoints.

## Non-goals

- Web auth adapter refactor.
- Flutter/POS credential implementation.
- Removing all transitional `KeycloakUserSub` internal types in one step.
- Adding a Clerk adapter.

