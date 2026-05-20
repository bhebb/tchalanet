# Spec — web-auth-flow

## Intent

Implement a stable Web authentication flow using Keycloak Authorization Code + PKCE, then load and update the current Tchalanet profile.

## Client

```text
clientId: tchalanet-web
flow: authorization_code + PKCE
```

## Local config

```text
API_BASE_URL=http://api.tchalanet.lan/api/v1
KEYCLOAK_URL=http://auth.tchalanet.lan
KEYCLOAK_REALM=tchalanet-local
KEYCLOAK_CLIENT_ID=tchalanet-web
```

## Flow

```text
Web app starts
-> if no session, redirect to Keycloak
-> Keycloak callback returns code
-> Web exchanges code/token through OIDC library
-> Web calls GET /tenant/me/profile
-> if profile is absent or bootstrap required, call POST /tenant/me/profile/bootstrap
-> app shell/dashboard loads
```

## Profile update

```text
Profile screen
-> load current profile
-> edit allowed fields
-> PATCH /tenant/me/profile
-> refresh profile state
```

## Route guards

- Unauthenticated routes redirect to login.
- Authenticated shell loads current profile before showing protected pages.
- Admin routes require broad role or permissions from backend/UI context.

## Error handling

Handle:

```text
token absent
refresh failure
profile missing
bootstrap failure
403 permission denied
401 session expired
network unavailable
```

## Acceptance criteria

- User logs in from Web using Keycloak.
- Web calls API with Bearer token.
- Profile loads.
- Profile update works.
- Logout clears token/session and returns to public state.
