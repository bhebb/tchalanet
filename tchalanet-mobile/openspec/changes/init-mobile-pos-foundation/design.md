# Design — Init Mobile POS Foundation

## 1. Architecture intention

The mobile app is a POS-first Flutter application. It shares backend contracts and concepts with Web, but its runtime flow is different.

Mobile V1 prioritizes:

- auth/session;
- secure storage;
- protected POS route;
- terminal/outlet/session operational context;
- i18n merge;
- runtime settings;
- runtime theme;
- minimal POS dashboard.

Do not force Web PageModel into Mobile V1 unless a dynamic mobile screen requirement is explicitly accepted.

## 2. Suggested folder shape

```text
tchalanet-mobile/lib/
  app/
    app.dart
    router.dart
    bootstrap.dart
  core/
    api/
    auth/
    settings/
    i18n/
    theme/
    storage/
  pos/
    operational_context/
    dashboard/
    sync/
  shared/
    models/
    ui/
```

This is a folder suggestion, not a demand for many Dart packages.

## 3. Dependency policy

Use the dependencies already validated for mobile foundation unless a new need is documented.

| Package | Purpose | Why needed | Do not use for |
|---|---|---|---|
| flutter_riverpod | App state/providers | Session, settings, theme, i18n, operational context | Business invariants |
| go_router | Routing and route protection | Redirects/guards for auth and role access | Business workflow decisions |
| dio | HTTP client/interceptors | Bearer token, error handling, API base | Long-running offline queue by itself |
| flutter_secure_storage | Secure token storage | Mobile auth tokens | General app cache |

Do not add packages for simple formatting, tiny helpers, or one-off widgets unless documented.

## 4. Auth/session design

Auth state model:

```dart
class UserSession {
  final bool authenticated;
  final String? userId;
  final String? username;
  final String? displayName;
  final String? tenantId;
  final String? tenantCode;
  final List<UserRole> roles;
  final DateTime? tokenExpiresAt;
}
```

The UI consumes a session provider. Widgets do not parse tokens.

Secure storage owns token persistence. Logs must never print raw tokens.

## 5. Runtime settings design

Runtime settings provide simple V1 flags/config:

```dart
class RuntimeSettings {
  final Map<String, bool> featureFlags;
  final Map<String, Object?> values;
}
```

Sensitive server decisions remain on backend. Client flags only hide/show or guide UX.

## 6. i18n merge design

Same rule as Web:

```text
merged = deepMerge(localMobileTranslations, backendOverrides)
backend wins on duplicate keys
```

Local translations must be enough for app startup and basic error states.

## 7. Theme design

The mobile theme starts with Tchalanet default. Runtime theme can apply tenant/default values later.

Flutter theme should map Tchalanet concepts to Material theme primitives:

- primary/secondary/accent colors;
- surface/background;
- text styles;
- shape/radius;
- density/spacing where applicable.

## 8. Operational context design

Operational context is displayed early, but sensitive validation is backend-owned.

Client models:

```dart
class OperationalContextView {
  final String? terminalId;
  final String? terminalCode;
  final String? outletId;
  final String? outletName;
  final String? salesSessionId;
  final String status;
  final String source;
}
```

The POS dashboard should clearly show:

- missing terminal binding;
- missing outlet;
- no open session;
- ready state.

The backend remains responsible for validating sell/payout/offline operations.

## 9. Mobile UI policy

Build minimal POS UI primitives first:

- Notice/banner;
- Error panel;
- Loading;
- Empty state;
- Status badge;
- Operational context bar/card;
- Session status;
- Sync/offline placeholder.

Do not build ticket/receipt/payout components before the first real POS flow needs them.

## 10. Quality gates

Initial commands should exist:

```text
flutter analyze
flutter test
flutter format
```

Pre-commit hook can be added once baseline is clean. It should run fast checks only:

```text
- format staged Dart files
- analyze changed scope if practical
```

Do not run emulator tests in pre-commit.

## 11. Initial tests

Minimum tests:

- i18n merge: backend wins;
- settings defaults: missing flags fail safely;
- operational context view maps missing state correctly;
- auth guard redirect logic if testable without device auth.

