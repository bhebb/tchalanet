# Spec — keycloak-local-users

## Intent

Provide local/dev Keycloak users that cover real Tchalanet auth and POS scenarios.

## Placement rule

```text
base-realm.json
  no demo users

overlays/local.json
  demo/test users

overlays/prod.json
  no demo users
```

## Required users

```text
super_admin
admin
operator
cashier
cashier_blocked
cashier_no_terminal
cashier_offline_allowed
cashier_offline_denied
```

## Default password

```text
changeme
```

Only for local/dev.

## User definitions

```json
[
  {
    "username": "super_admin",
    "enabled": true,
    "email": "super_admin@local",
    "firstName": "Super",
    "lastName": "Admin",
    "emailVerified": true,
    "credentials": [{ "type": "password", "value": "changeme", "temporary": false }],
    "realmRoles": ["SUPER_ADMIN"],
    "groups": [],
    "attributes": {
      "locale": ["fr"],
      "tenant_code": ["tchalanet"],
      "plan": ["pro"],
      "featureSetId": ["base"]
    }
  },
  {
    "username": "admin",
    "enabled": true,
    "email": "admin@local",
    "firstName": "Tenant",
    "lastName": "Admin",
    "emailVerified": true,
    "credentials": [{ "type": "password", "value": "changeme", "temporary": false }],
    "realmRoles": ["TENANT_ADMIN"],
    "groups": ["/tenants/tchalanet/admins"],
    "attributes": {
      "locale": ["fr"],
      "tenant_code": ["tchalanet"],
      "plan": ["pro"],
      "featureSetId": ["base"]
    }
  },
  {
    "username": "operator",
    "enabled": true,
    "email": "operator@local",
    "firstName": "Operator",
    "lastName": "Tchalanet",
    "emailVerified": true,
    "credentials": [{ "type": "password", "value": "changeme", "temporary": false }],
    "realmRoles": ["OPERATOR"],
    "groups": ["/tenants/tchalanet/agents"],
    "attributes": {
      "locale": ["fr"],
      "tenant_code": ["tchalanet"],
      "plan": ["free"],
      "featureSetId": ["base"]
    }
  },
  {
    "username": "cashier",
    "enabled": true,
    "email": "cashier@local",
    "firstName": "Marie",
    "lastName": "Joseph",
    "emailVerified": true,
    "credentials": [{ "type": "password", "value": "changeme", "temporary": false }],
    "realmRoles": ["CASHIER"],
    "groups": ["/tenants/tchalanet/agents"],
    "attributes": {
      "locale": ["fr"],
      "tenant_code": ["tchalanet"],
      "plan": ["free"],
      "featureSetId": ["base"],
      "test_case": ["cashier_ok"]
    }
  },
  {
    "username": "cashier_blocked",
    "enabled": true,
    "email": "cashier_blocked@local",
    "firstName": "Jean",
    "lastName": "Bloque",
    "emailVerified": true,
    "credentials": [{ "type": "password", "value": "changeme", "temporary": false }],
    "realmRoles": ["CASHIER"],
    "groups": ["/tenants/tchalanet/agents"],
    "attributes": {
      "locale": ["fr"],
      "tenant_code": ["tchalanet"],
      "plan": ["free"],
      "featureSetId": ["base"],
      "test_case": ["cashier_blocked"]
    }
  },
  {
    "username": "cashier_no_terminal",
    "enabled": true,
    "email": "cashier_no_terminal@local",
    "firstName": "Paul",
    "lastName": "Sans Terminal",
    "emailVerified": true,
    "credentials": [{ "type": "password", "value": "changeme", "temporary": false }],
    "realmRoles": ["CASHIER"],
    "groups": ["/tenants/tchalanet/agents"],
    "attributes": {
      "locale": ["fr"],
      "tenant_code": ["tchalanet"],
      "plan": ["free"],
      "featureSetId": ["base"],
      "test_case": ["cashier_no_terminal"]
    }
  },
  {
    "username": "cashier_offline_allowed",
    "enabled": true,
    "email": "cashier_offline_allowed@local",
    "firstName": "Nadia",
    "lastName": "Offline",
    "emailVerified": true,
    "credentials": [{ "type": "password", "value": "changeme", "temporary": false }],
    "realmRoles": ["CASHIER"],
    "groups": ["/tenants/tchalanet/agents"],
    "attributes": {
      "locale": ["fr"],
      "tenant_code": ["tchalanet"],
      "plan": ["free"],
      "featureSetId": ["base"],
      "test_case": ["cashier_offline_allowed"]
    }
  },
  {
    "username": "cashier_offline_denied",
    "enabled": true,
    "email": "cashier_offline_denied@local",
    "firstName": "Marc",
    "lastName": "OfflineDenied",
    "emailVerified": true,
    "credentials": [{ "type": "password", "value": "changeme", "temporary": false }],
    "realmRoles": ["CASHIER"],
    "groups": ["/tenants/tchalanet/agents"],
    "attributes": {
      "locale": ["fr"],
      "tenant_code": ["tchalanet"],
      "plan": ["free"],
      "featureSetId": ["base"],
      "test_case": ["cashier_offline_denied"]
    }
  }
]
```

## Alignment with Tchalanet seed data

Keycloak users only provide login and broad role tokens. Tchalanet local seed data must create corresponding app users, memberships, statuses, terminal/outlet/session state, and offline policy state.

## Acceptance criteria

- All local users can login.
- Tokens include first/last names.
- Test users map to Tchalanet local seed scenarios.
- No local/demo users exist in prod overlay.
