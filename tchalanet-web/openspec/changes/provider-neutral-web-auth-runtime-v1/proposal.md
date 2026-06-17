# Change: Provider-neutral Web Auth and Runtime

## Status

Active.

## Why

The portal currently derives application session data from the backend runtime, but its core
session service and bearer interceptor still depend directly on Firebase. This couples shared auth
orchestration to one identity provider and makes a provider change affect guards and session logic.

## What changes

- Introduce a provider-neutral `AuthClient` contract for authentication state, login/logout, bearer
  tokens, and token expiry.
- Keep Firebase behind a composition-root adapter.
- Make the session service and bearer interceptor consume only the neutral contract.
- Use the canonical private runtime endpoint `/runtime/private`.
- Document that application identity, roles, and tenant context come from backend runtime data.

## Non-goals

- No second web identity-provider adapter in this change.
- No changes to backend authorization rules or cashier operational context.
- No broad removal of legacy Keycloak packages or shared runtime helpers.
