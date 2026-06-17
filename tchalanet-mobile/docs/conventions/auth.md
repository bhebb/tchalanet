# Convention — Auth mobile

**Scope** : `tchalanet-mobile/`
**Status** : normative

---

## 1. Flow retenu : Firebase Auth opérateur

Le mobile utilise le SDK officiel FlutterFire Firebase Auth. L'opérateur se connecte avec son
email et son mot de passe. Le terminal POS n'est jamais utilisé comme identité opérateur.

```
LoginPage
  → bouton "Se connecter"
  → AuthController.login()
  → FirebaseAuthTokenClient
  → Firebase Auth email/password
  → Firebase ID token
  → AuthRepositoryImpl appelle GET /runtime/private
  → le runtime backend → UserSession
  → AuthController.state = AuthAuthenticated(session)
  → GoRouter redirect → /pos
```

Le binding terminal/appareil est exécuté après authentification et reste séparé de l'identité.

---

## 2. Tokens — stockage

| Token | Clé secure storage | Durée de vie |
|---|---|---|
| `access_token` | `access_token` | Firebase ID token, courte |

**Règles :**
- Tous les tokens vivent dans `SecureTokenStorage` (Keychain iOS / Keystore Android).
- Aucun widget ne lit un token directement.
- Aucun token ne doit apparaître dans les logs (`UserSession.toString()` ne l'expose pas).
- `TokenStorage.clear()` supprime le token mis en cache (appelé par `AuthController.logout()`).
- Firebase Auth conserve et rafraîchit sa propre session via le SDK officiel.

---

## 3. UserSession — modèle de session UI

`UserSession` (`lib/features/auth/data/models/user_session.dart`) est le seul objet consommé par
l'UI. L'identité applicative, le tenant et les rôles viennent de `GET /runtime/private`; le token
fournisseur sert uniquement au bearer, au refresh et à l'expiration technique.

```dart
UserSession(
  authenticated: true,
  userId:        runtime.user.userId
  username:      runtime.user.username
  displayName:   runtime.user.displayName
  tenantId:      runtime.tenantContext.tenantId
  tenantCode:    runtime.tenantContext.tenantCode
  roles:         runtime.entitlements.roles
  tokenExpiresAt: token exp // technique uniquement
)
```

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

Le logout appelle `FirebaseAuth.signOut()`, puis efface le token mis en cache et le contexte
opérationnel.

---

## 7. Configuration Firebase

La configuration publique de l'application Android est générée par FlutterFire dans
`lib/firebase_options.dart` et `android/app/google-services.json`. Le compte de service Firebase
Admin du backend ne doit jamais être embarqué dans le mobile.

| Variable dart-define | Défaut (émulateur) | Description |
|---|---|---|
| `FIREBASE_AUTH_EMULATOR` | `true` | Active l'émulateur local |
| `FIREBASE_AUTH_EMULATOR_HOST` | `10.0.2.2` | Hôte émulateur Android |
| `FIREBASE_AUTH_EMULATOR_PORT` | `9099` | Port émulateur |
| `API_BASE_URL` | `https://api.localtest.me:8443/api/v1` | Backend API |

---

## 8. Android — prérequis

- `android:networkSecurityConfig="@xml/network_security_config"` pour cleartext HTTP local (dev uniquement)
