# Inventory

## Provider-specific dependencies before this change

- `AuthSessionService` injected AngularFire `Auth`, observed Firebase users, configured Firebase
  persistence, performed Firebase login/logout, and decoded a Firebase JWT.
- `firebaseAuthInterceptor` injected `FirebaseAuthService` directly.
- `app.config.ts` selected Firebase directly but did not expose a neutral adapter binding.
- `PrivateBootstrapService` used the legacy `/tenant/runtime/bootstrap` route.

## Target ownership

- Core auth owns provider-neutral session orchestration and bearer attachment.
- The Firebase directory owns Firebase SDK integration.
- The composition root selects the configured provider adapter through one provider function; it
  does not import Firebase SDK primitives.
- Backend runtime remains authoritative for application user, roles, tenant, and entitlements.
