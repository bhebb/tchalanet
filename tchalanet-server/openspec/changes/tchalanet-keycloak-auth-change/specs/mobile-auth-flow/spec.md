# Spec — mobile-auth-flow

## Intent

Implement Flutter mobile authentication using Keycloak Authorization Code + PKCE, secure token storage, profile load/update, and POS dashboard bootstrap.

## Client

```text
clientId: tchalanet-mobile-pos
flow: authorization_code + PKCE
redirect: com.tchalanet.mobile:/oauth2redirect
```

## Flutter package baseline

```text
flutter_riverpod
go_router
dio
flutter_secure_storage
```

## Suggested structure

```text
lib/
  app/
    app.dart
    router.dart
    bootstrap.dart
  core/
    auth/
    config/
    http/
    storage/
    errors/
  features/
    auth/
    profile/
    pos/
    offline/
```

## Flow

```text
StartupScreen
-> read secure storage
-> try refresh token
-> if invalid, show LoginScreen
-> login via Keycloak PKCE
-> store tokens securely
-> call GET /tenant/me/profile
-> bootstrap if needed
-> load POS dashboard/bootstrap
-> route to PosHome
```

## Dio integration

- Bearer token interceptor.
- Refresh-on-401 behavior if refresh token is valid.
- Logout on refresh failure.

## Profile

- View profile via `/tenant/me/profile`.
- Update allowed fields via `PATCH /tenant/me/profile`.

## POS bootstrap

P0 screen data:

```text
cashier display name
tenant/outlet/terminal/session summary if available
offline status placeholder
```

## Acceptance criteria

- Mobile login works in emulator/local profile.
- Tokens are stored securely.
- Profile loads after login.
- Profile update works.
- Logout clears secure storage.
- POS home renders minimal identity/context data.
