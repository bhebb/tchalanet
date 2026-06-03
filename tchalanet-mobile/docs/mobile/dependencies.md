# Mobile Dependencies

Every package in `pubspec.yaml` must have an entry here.
Add a new row before adding a package; remove the row when removing the package.

## Direct dependencies

| Package | Version | Category | Purpose | Do not use for | Removal trigger |
|---|---|---|---|---|---|
| `flutter_riverpod` | `^3.x` | State management | App-wide state: auth session, settings, theme, i18n, operational context | Business invariants, domain validation | Replaced by a successor state solution adopted by the project |
| `go_router` | `^17.x` | Routing | Declarative routing, auth redirect guards, protected routes | Business workflow decisions | Flutter Navigator 2 built-in covers the use case |
| `dio` | `^5.x` | HTTP | API calls, bearer token interceptor, typed error mapping | Long-running offline queue or background sync by itself | Replaced by a project-wide HTTP abstraction decision |
| `flutter_secure_storage` | `^10.x` | Secure storage | Mobile auth token persistence (access + refresh tokens) | General app cache, non-sensitive data | OS-level Keychain/Keystore API wrapping is brought in-house |
| `flutter_appauth` | `^8.x` | Auth (OIDC) | Authorization Code + PKCE flow against Keycloak; opens system browser, handles redirect, exchanges code for tokens, silent refresh | Username/password forms, ROPC | Replaced by a project-wide auth SDK decision |

## Rules

- Do not add a package without a row in this table.
- Prefer Flutter SDK primitives when they cover the need.
- One package per concern — do not duplicate roles.
- Simple formatting, tiny helpers, or one-off widgets do not justify a new dependency.
