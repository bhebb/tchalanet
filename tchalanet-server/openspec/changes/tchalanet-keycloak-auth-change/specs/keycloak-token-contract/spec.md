# Spec — keycloak-token-contract

## Intent

Define the stable token contract consumed by Tchalanet API, Web, Swagger, and Mobile.

## Minimum token claims

```json
{
  "sub": "keycloak-user-id",
  "preferred_username": "cashier",
  "email": "cashier@local",
  "given_name": "Marie",
  "family_name": "Joseph",
  "name": "Marie Joseph",
  "locale": "fr",
  "roles": ["CASHIER"],
  "groups": ["agents"],
  "tch": {
    "tenant_code": "tchalanet",
    "plan": "free",
    "featureSetId": "base"
  }
}
```

## Optional Tchalanet-specific claims

Only if stable and supported by the provider:

```json
{
  "tch": {
    "tenant_id": "uuid-if-known",
    "app_user_id": "uuid-if-known",
    "keycloak_sub": "same-as-sub"
  }
}
```

## Required mappers

- `sub` / Keycloak subject must be present and treated as the stable identity key.
- `preferred_username`
- `email`
- `given_name`
- `family_name`
- `name`
- `locale`
- `roles`
- `groups`
- `tch` JSON claim from custom provider

## Explicit `sub` mapper

Even though OIDC normally includes `sub`, the realm must make subject expectations testable and visible.

Recommended extra diagnostic alias:

```text
tch.keycloak_sub or tch.keycloakSub
```

## Claims forbidden from token truth

Do not encode these as trusted token facts:

```text
fine-grained permissions
terminalId
outletId
salesSessionId
offline grant
sales limits
cashier exposure
terminal/outlet status
session status
```

These belong to Tchalanet runtime validation, operational context, or domain policies.

## API usage rule

- Backend uses `sub` as the stable Keycloak identity key.
- Backend must not use email or username as primary identity keys.
- Token roles are broad roles only.
- Fine permissions are evaluated by `platform.accesscontrol`.

## Acceptance criteria

- Access token includes all minimum claims.
- ID token includes claims needed by Web/Mobile UX.
- UserInfo returns profile claims.
- `sub` is present and stable.
- `given_name` and `family_name` are present for local users.
