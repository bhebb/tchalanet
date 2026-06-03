# Convention — Auth mobile

**Scope** : `tchalanet-mobile/`
**Status** : normative

---

## 1. Flow retenu : Authorization Code + PKCE

Le mobile utilise exclusivement **Authorization Code + PKCE** (RFC 7636 / RFC 8252).

```
LoginPage
  → bouton "Se connecter"
  → AuthController.login()
  → AuthService (flutter_appauth)
  → ouvre navigateur système (Chrome Custom Tabs)
  → Keycloak login
  → redirect com.tchalanet.mobile:/oauth2redirect?code=…
  → AuthService échange le code contre access_token + refresh_token
  → AuthRepositoryImpl._buildSession() : décode JWT → UserSession
  → AuthController.state = AuthAuthenticated(session)
  → GoRouter redirect → /pos
```

**Interdit :**
- ROPC (Resource Owner Password Credentials) — déprécié OAuth 2.1, mot de passe transite dans l'app.
- Formulaire username/password dans l'app pour l'auth Keycloak.

---

## 2. Tokens — stockage

| Token | Clé secure storage | Durée de vie |
|---|---|---|
| `access_token` | `access_token` | Courte (Keycloak : ~5 min) |
| `refresh_token` | `refresh_token` | Longue (Keycloak : ~30 min ou session) |

**Règles :**
- Tous les tokens vivent dans `SecureTokenStorage` (Keychain iOS / Keystore Android).
- Aucun widget ne lit un token directement.
- Aucun token ne doit apparaître dans les logs (`UserSession.toString()` ne l'expose pas).
- `TokenStorage.clear()` supprime les deux tokens (appelé par `AuthController.logout()`).

---

## 3. UserSession — modèle de session UI

`UserSession` (`lib/features/auth/data/models/user_session.dart`) est le seul objet consommé par l'UI.

```dart
UserSession(
  authenticated: true,
  userId:        sub            // payload['sub']
  username:      preferred_username
  displayName:   name
  tenantId:      tch.tenant_id
  tenantCode:    tch.tenant_code
  roles:         roles[]        // claim racine Tchalanet
  tokenExpiresAt: exp
)
```

**Claims Keycloak (voir `keycloak-token-contract` spec) :**
- `roles` → claim racine injecté par le provider Tchalanet Keycloak (pas `realm_access.roles`).
- `tch` → bloc JSON custom : `tenant_code`, `tenant_id`, `plan`, `featureSetId`.

**Accès dans les widgets :**
```dart
final session = ref.watch(userSessionProvider); // Provider<UserSession>
```

Ne jamais accéder au token brut depuis un widget ou un ViewModel.

---

## 4. Route guards

```
GoRouter.redirect :
  AuthUnknown   → null (attendre la restauration)
  Unauthenticated + hors /login + hors /forbidden → /login
  Authenticated + sur /login → /pos
```

- `/pos` et toutes les routes POS sont protégées.
- `/login` et `/forbidden` sont publiques.
- La redirection est déclarative dans `app_router.dart` — aucun widget ne gère l'auth.

---

## 5. Restauration de session (cold start)

```
restoreSession()
  1. Lire access_token depuis secure storage
  2. Absent ou vide → null (→ login)
  3. Présent → décoder JWT, vérifier exp
  4. Expiré ou expire dans < 60 s → tenter refresh silencieux
     - Succès : stocker nouveaux tokens, retourner UserSession
     - Échec  : clear() + null (→ login)
  5. Valide → retourner UserSession
```

---

## 6. Logout

```dart
AuthController.logout()
  → AuthRepository.logout()
  → TokenStorage.clear()          // supprime access + refresh
  → state = AuthUnauthenticated()
  → GoRouter redirect → /login    // déclenché automatiquement
```

Le logout ne fait pas d'appel réseau à Keycloak en V1. Le refresh token est simplement supprimé côté client.

---

## 7. Configuration (dart-defines)

| Variable | Défaut (émulateur) | Description |
|---|---|---|
| `KC_BASE_URL` | `http://10.0.2.2:8082` | URL base Keycloak |
| `KC_REALM` | `tchalanet` | Realm |
| `KC_CLIENT_ID` | `tchalanet-mobile-pos` | Client PKCE public |
| `API_BASE_URL` | `http://10.0.2.2:8080/api/v1` | Backend API |

`kcRedirectUri` est une constante (pas un dart-define) : `com.tchalanet.mobile:/oauth2redirect`.

---

## 8. Android — prérequis

- `manifestPlaceholders["appAuthRedirectScheme"] = "com.tchalanet.mobile"` dans `build.gradle.kts`
- `android:networkSecurityConfig="@xml/network_security_config"` pour cleartext HTTP local (dev uniquement)
- `minSdk = 21` minimum (requis par `flutter_appauth`)
