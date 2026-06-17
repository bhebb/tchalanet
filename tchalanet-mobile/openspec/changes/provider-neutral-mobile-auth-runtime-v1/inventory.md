# Inventory

## Before

- `AuthRepositoryImpl` decoded Keycloak-specific `tch`, `roles`, `preferred_username`, and `name`
  claims to build `UserSession`.
- `RuntimeService` called the legacy `/tenant/runtime/bootstrap`.
- `AuthInterceptor` attached tokens only to `/tenant/*`, so the canonical `/runtime/private`
  endpoint would be anonymous.
- `AuthInterceptor` directly imported `flutter_appauth` and Keycloak endpoint configuration.

## Target ownership

- FlutterFire Firebase Auth owns provider authentication and token refresh.
- Backend private runtime owns application user, tenant context, roles, and permissions.
- `UserSession` remains the provider-neutral view model consumed by UI and routing.
