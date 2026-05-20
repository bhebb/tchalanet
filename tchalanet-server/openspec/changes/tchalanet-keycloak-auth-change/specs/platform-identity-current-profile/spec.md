# Spec — platform-identity-current-profile

## Intent

Stabilize the current-user profile endpoints consumed by Web and Flutter mobile after authentication.

## Canonical surface

```http
GET   /tenant/me/profile
POST  /tenant/me/profile/bootstrap
PATCH /tenant/me/profile
```

## Controller cleanup

Current profile endpoints must not live under `/admin/profile`.

Target controller remains in:

```text
platform.identity.internal.web.me
```

Target path:

```text
/tenant/me/profile
```

## Bootstrap rule

Bootstrap uses Keycloak `sub` as the stable external identity key.

```text
sub -> KeycloakUserSub -> platform.identity app user
```

Do not use email or username as primary identity key.

## View profile response

Should include:

```text
user id
keycloak sub
username
email
firstName
lastName
displayName
isNew
tenant context
user preferences
effective UI context
```

## Update profile fields

Allowed in P0:

```text
firstName
lastName
phone
locale
timezone/preferences
```

Email should be read-only unless a dedicated Keycloak-synchronized email update flow exists.

Forbidden:

```text
sub
username
roles
permissions
tenant id
status
terminal/outlet/session
offline grants
```

## Security

- Requires authenticated user.
- Uses `TchRequestContext` for tenant/user.
- If current user missing and endpoint is not bootstrap, return appropriate problem response.

## Acceptance criteria

- Web and Mobile can call `GET /tenant/me/profile` after login.
- Bootstrap creates or resolves app user using `sub`.
- Patch profile updates only allowed fields.
- Endpoint path is tenant scope, not admin scope.
