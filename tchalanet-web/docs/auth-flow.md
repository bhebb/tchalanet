# Auth flow — Tchalanet Web

Document technique de référence. Décrit le cycle complet :
affichage public → connexion superadmin → opérations → déconnexion → connexion admin tenant.

---

## Acteurs et responsabilités

| Classe / fichier | Rôle |
|---|---|
| `FirebaseAuthService` · `firebase-auth.service.ts` | Implémente `AuthClient` — wraps Firebase Auth SDK |
| `AUTH_CLIENT` (InjectionToken) | Point d'injection de `AuthClient` — permet de substituer Firebase |
| `AuthSessionService` · `auth-session.service.ts` | Session applicative Angular — construit `UserSession` depuis le bootstrap |
| `authBearerInterceptor` · `auth-bearer.interceptor.ts` | Injecte `Authorization: Bearer <token>` sur tous les appels API backend |
| `spaceDispatchGuard` · `auth.guard.ts` | Route d'entrée `/app` — dispatche vers l'espace selon le rôle |
| `roleGuard(role)` · `auth.guard.ts` | Protège `/app/platform`, `/app/admin`, `/app/cashier` |
| `PrivateBootstrapService` · `private-bootstrap.service.ts` | Appelle `GET /tenant/runtime/bootstrap` |
| `PrivateRuntimeInitializer` · `private-runtime-initializer.ts` | Applique theme, i18n, settings, et peuple `PrivateBootstrapStore` |
| `PrivateBootstrapStore` · `private-bootstrap.store.ts` | Store signal du runtime privé (space, user, tenantContext, readiness…) |
| `AppRuntimeStore` · `app-runtime.store.ts` | Orchestrateur bootstrap (scope public / private) |
| `PrivateShellService` · `private-shell.service.ts` | Détermine la sidenav et charge la page shell depuis le PageModel API |
| `PrivateShellPage` · `private-shell.page.ts` | Composant shell — layout, header, sidebar, router-outlet |
| `PublicBootstrapService` · `public-bootstrap.service.ts` | Appelle `GET /public/runtime/bootstrap` (sans auth) |
| `LoginPage` · `login/login.page.ts` | Formulaire de connexion |

---

## 1. Affichage public (avant toute connexion)

```
Navigateur → /  →  redirectTo: 'public'  →  /public
```

**Composant** : `TchPublicShellComponent` (chargé pour toutes les routes `/public/**`).

**Initialisation** :

```
AppRuntimeStore.initPublicRuntime()
  └─ PublicRuntimeInitializer.initialize(locale)
       └─ PublicBootstrapService.bootstrap(locale)
            └─ GET /public/runtime/bootstrap   (aucun token)
```

Le backend retourne le runtime public : thème par défaut, bundle i18n, feature flags, page model de la page d'accueil. Aucune session Firebase. L'intercepteur `authBearerInterceptor` passe sans ajouter de header (token = null).

**État après** :
- `AppRuntimeStore.bootstrapScope()` = `'public'`
- `AuthSessionService.sessionState()` = `{ authenticated: false, roles: [] }`
- Thème et i18n publics appliqués

---

## 2. Connexion — chemin commun (superadmin et admin tenant)

L'utilisateur navigue vers `/login`. La route est ouverte — aucun guard.

**`LoginPage.submit()`** :

```ts
// login/login.page.ts
const session = await this.authSession.login(email, password, remember);
await this.router.navigateByUrl('/app');
```

**`AuthSessionService.login()`** :

```ts
// auth-session.service.ts
await this.auth.login({ username, password, remember });   // ① Firebase
return this.refreshSession(true);                          // ② Bootstrap backend
```

### ① Firebase — `FirebaseAuthService.login()`

```ts
// firebase/firebase-auth.service.ts
await setPersistence(auth, remember ? browserLocalPersistence : browserSessionPersistence);
await signInWithEmailAndPassword(auth, username, password);
```

- `browserLocalPersistence` → token JWT Firebase persisté en `localStorage` (survit à la fermeture de l'onglet).
- `browserSessionPersistence` → persisté en `sessionStorage` (effacé à la fermeture).
- Résultat : `auth.currentUser` est non null. Firebase gère le refresh silencieux du token (1h TTL).

### ② Bootstrap backend — `refreshSession(force=true)`

```ts
// auth-session.service.ts
const bootstrap = await firstValueFrom(this.runtime.initialize());
```

**Chaîne d'appel** :

```
AuthSessionService.refreshSession(force=true)
  └─ PrivateRuntimeInitializer.initialize()
       └─ PrivateBootstrapService.bootstrap()
            └─ GET /tenant/runtime/bootstrap
                 Header: Authorization: Bearer <firebase-jwt>   (authBearerInterceptor)
```

**`authBearerInterceptor`** (appliqué à tout appel vers le backend Tchalanet) :

```ts
// auth-bearer.interceptor.ts
const token = await auth.getAccessToken();          // Firebase getIdToken()
req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
// Sur 401 : retry une fois avec getAccessToken(forceRefresh=true)
```

**Réponse backend `RuntimeBootstrapResponse`** :

```json
{
  "space": "PLATFORM",              // ou "ADMIN" selon le rôle principal
  "user": { "userId", "roles": ["SUPER_ADMIN"], "defaultSpace": "PLATFORM", ... },
  "tenantContext": null,            // null pour SUPER_ADMIN, non-null pour TENANT_ADMIN
  "entitlements": { "roles": ["SUPER_ADMIN"], "permissions": [...] },
  "settings": { "locale", "timezone", "currency", "features": {} },
  "theme": { "presetCode", "tokens": {} },
  "i18n": { "locale", "messages": {} },
  "readiness": { "status": "READY", "checks": [] },
  "pageModelRef": { "route": "/app/platform", "endpoint": "..." },
  "entryRoute": null
}
```

**`PrivateRuntimeInitializer.applyBootstrap()`** applique immédiatement :
- `TranslateService.setTranslation()` → i18n injecté
- `ThemeStore.applyBootstrapTheme()` → tokens CSS appliqués
- `RuntimeSettingsStore.applyBootstrapSettings()` → locale, timezone, currency, features
- `PrivateBootstrapStore.setBootstrap()` → store signal populé

**`AuthSessionService`** construit `UserSession` depuis le bootstrap :

```ts
// auth-session.service.ts
const session: UserSession = {
  authenticated: true,
  userId:        bootstrap.user.userId,
  tenantId:      bootstrap.tenantContext?.tenantId,     // null → undefined pour SUPER_ADMIN
  tenantCode:    bootstrap.tenantContext?.tenantCode,
  roles:         normalizeRoles(bootstrap.entitlements.roles),
  entryRoute:    bootstrap.entryRoute ?? bootstrap.pageModelRef?.route,
  ...
};
```

**`normalizeRoles()`** :

```ts
'TENANT_OWNER' → 'TENANT_ADMIN'
'OPERATOR'     → 'CASHIER'
// Filtre strict : seuls 'CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN' passent
```

**État après login** :
- `AuthSessionService.sessionState()` = session avec rôle(s) normalisés
- Bootstrap stores tous peuplés
- `LoginPage` redirige vers `/app`

---

## 3. Dispatch vers l'espace — `spaceDispatchGuard`

```
/app  →  canActivate: [spaceDispatchGuard]
```

```ts
// auth.guard.ts
const session = await auth.refreshSession();   // no-force → retourne la session mise en cache

if (session.entryRoute) {
  return router.parseUrl(session.entryRoute);  // priorité absolue si le backend le précise
}
if (session.roles.includes('SUPER_ADMIN'))  → '/app/platform'
if (session.roles.includes('TENANT_ADMIN')) → '/app/admin'
if (session.roles.includes('CASHIER'))      → '/app/cashier'
// sinon → '/forbidden'
```

**Important** : `refreshSession()` sans `force=true` retourne la session en cache si `authenticated === true`. Le bootstrap n'est pas re-appelé ici. La session déjà construite en ② est réutilisée.

**Ordre de priorité des rôles** : `SUPER_ADMIN` est testé en premier. Un utilisateur avec les deux rôles va toujours sur `/app/platform`. L'`entryRoute` peut écraser ce comportement si le backend le définit.

---

## 4. Session superadmin — `/app/platform`

### Guard

```
/app/platform  →  canActivate: [roleGuard('SUPER_ADMIN')]
```

```ts
// auth.guard.ts
const session = await auth.refreshSession();   // cache hit
if (!auth.hasRole('SUPER_ADMIN')) → '/forbidden'
return true;
```

### Shell

**`PrivateShellPage` constructor** :

```ts
// private-shell.page.ts
this.runtime.initPrivateRuntime();
```

**`AppRuntimeStore.initPrivateRuntime()`** :

```ts
// Ici bootstrapScope est encore 'public' si c'est la première navigation privée.
// Il passe à 'private'. La session est déjà authentifiée (login ②).
// Il appelle privateInitializer.initialize() → GET /tenant/runtime/bootstrap (de nouveau).
// Ce second appel est redondant si login ② a déjà tout chargé, mais s'assure
// que le scope est bien marqué 'private' et que les stores sont synchronisés.
```

**`PrivateShellService.page$`** (après le fix `private-shell.service.ts`) :

```
router.events (NavigationEnd) → startWith(null) → map(url)
  distinctUntilChanged(urlToSpace)   // 'platform' vs 'other'
    switchMap(url)
      url.startsWith('/app/platform') → PageModelApi.getPlatformPage()
                                         └─ GET /platform/page   (avec Bearer)
    shareReplay(1)                   // cache valide tant que l'espace ne change pas
```

**`PrivateShellService.navigation()`** :

```ts
// Si le backend retourne navigationDrawer.sections non vides → utilise ces sections
// Sinon → fallback statique URL-driven :
//   url.startsWith('/app/platform') → PLATFORM_NAVIGATION   (private-navigation.model.ts)
//   url.startsWith('/app/admin')    → TENANT_ADMIN_NAVIGATION
```

**`PrivateShellPage`** render :

```html
<tch-sidebar-nav [sections]="shellSvc.navigation()" />
<router-outlet />                  <!-- pages platform chargées ici -->
```

### Opérations superadmin

Chaque appel API depuis les pages platform passe par `TchBackendClient` → `HttpClient` → `authBearerInterceptor` :

```
Page component
  └─ service métier (ex. PlatformTenantsApiService)
       └─ TchBackendClient.get('/platform/tenants')
            └─ HttpClient.get('/api/v1/platform/tenants')
                 ├─ authBearerInterceptor : ajoute Bearer <firebase-jwt>
                 └─ Backend vérifie JWT + rôle SUPER_ADMIN côté serveur
```

Sur expiration du token Firebase (1h) : l'intercepteur reçoit un 401, appelle `getAccessToken(forceRefresh=true)`, Firebase renouvelle silencieusement (tant que la session Firebase est valide), et rejoue la requête.

---

## 5. Déconnexion

```ts
// private-shell.page.ts
logout(): void {
  void this.auth.logout().then(() => this.router.navigate(['/login']));
}
```

**`AuthSessionService.logout()`** :

```ts
await this.auth.logout();       // FirebaseAuthService.logout() → signOut(auth)
this.setAnonymousSession();     // sessionState = { authenticated: false, roles: [] }
```

**`FirebaseAuthService.logout()`** :

```ts
await signOut(auth);   // révoque la session Firebase locale
                       // auth.currentUser = null
                       // localStorage/sessionStorage token supprimé
```

**État après logout** :
- `auth.currentUser` = null — `isAuthenticated()` retourne false
- `AuthSessionService.sessionState()` = `{ authenticated: false, roles: [] }`
- `PrivateBootstrapStore` : non réinitialisé (les signaux gardent leurs valeurs)
- `AppRuntimeStore.bootstrapScope()` = `'private'` (non réinitialisé)
- `PrivateShellService.page$` : observable actif, mais son dernier résultat sera écrasé dès le prochain `NavigationEnd` vers un espace différent (grâce au `switchMap` sur `urlToSpace`)
- Router navigue vers `/login` → `PrivateShellPage` détruit

---

## 6. Connexion admin tenant (même session navigateur, sans rechargement)

L'utilisateur est sur `/login`. La page se charge normalement.

**`LoginPage.submit()`** → même chemin qu'en §2.

### ① Firebase — nouvelle authentification

```ts
await signInWithEmailAndPassword(auth, adminEmail, adminPassword);
// auth.currentUser = nouvel utilisateur tenant admin
```

### ② Bootstrap backend

```
GET /tenant/runtime/bootstrap
  Header: Authorization: Bearer <nouveau-firebase-jwt>
```

Réponse pour un admin tenant :

```json
{
  "space": "ADMIN",
  "user": { "roles": ["TENANT_ADMIN"], "defaultSpace": "ADMIN", ... },
  "tenantContext": { "tenantId": "...", "tenantCode": "HAITILOTO", "tenantName": "..." },
  "entitlements": { "roles": ["TENANT_ADMIN"], "permissions": [...] },
  "entryRoute": null
}
```

`PrivateRuntimeInitializer.applyBootstrap()` réapplique :
- i18n du tenant
- thème du tenant (`ThemeStore.applyBootstrapTheme()` → tokens CSS écrasés)
- `PrivateBootstrapStore.setBootstrap()` → `space = 'ADMIN'`, `tenantContext` non null
- `AuthSessionService.sessionState()` = `{ authenticated: true, roles: ['TENANT_ADMIN'], tenantId: '...', tenantCode: 'HAITILOTO' }`

### Dispatch

```
/app → spaceDispatchGuard
  session.roles.includes('SUPER_ADMIN') → false
  session.roles.includes('TENANT_ADMIN') → true  →  '/app/admin'
```

### Guard admin

```
/app/admin → roleGuard('TENANT_ADMIN') → session.roles includes 'TENANT_ADMIN' → true
```

### Shell

**`PrivateShellPage`** est recréé (nouvelle instance, car la route `/app/admin` est distincte de `/app/platform`).

**`AppRuntimeStore.initPrivateRuntime()`** :

```ts
if (this.bootstrapScope() === 'private') return;  // déjà 'private' → no-op
// Pas de re-bootstrap ici. Mais la session est déjà correcte depuis login ②.
```

**`PrivateShellService.page$`** — premier `NavigationEnd` avec url `/app/admin` :

```
urlToSpace('/app/admin') = 'other'  ≠  urlToSpace('/app/platform') = 'platform'
  → distinctUntilChanged laisse passer
  → switchMap : url.startsWith('/app/admin') → PageModelApi.getTenantPage()
       └─ GET /admin/page   (avec nouveau Bearer du tenant admin)
  → shareReplay(1) met en cache la réponse admin
```

L'ancien cache platform est abandonné (le `switchMap` annule l'abonnement précédent).

**`PrivateShellService.navigation()`** :

```ts
space = 'admin'   →   TENANT_ADMIN_NAVIGATION   (ou sections du backend si non vides)
```

La sidenav affichée est celle du tenant admin.

---

## 7. Invariants de sécurité

| Invariant | Où c'est appliqué |
|---|---|
| Aucune route privée n'est accessible sans Firebase token valide | `authBearerInterceptor` + `roleGuard` |
| Le rôle est vérifié côté serveur à chaque requête | Backend valide le JWT + rôle sur chaque endpoint |
| Le `roleGuard` vérifie le rôle côté client avant de rendre la page | `auth.guard.ts:roleGuard` |
| Un TENANT_ADMIN ne peut pas atteindre `/app/platform` | `roleGuard('SUPER_ADMIN')` bloque → `/forbidden` |
| Un SUPER_ADMIN ne peut pas atteindre `/app/admin` directement sans le rôle TENANT_ADMIN | `roleGuard('TENANT_ADMIN')` bloque → `/forbidden` |
| Le token Firebase expiré (1h) est renouvelé silencieusement | `authBearerInterceptor` retry sur 401 avec `forceRefresh=true` |
| La session est effacée côté client au logout | `signOut(auth)` + `setAnonymousSession()` |
| La sidenav après un changement de session affiche toujours la nav du bon espace | `PrivateShellService.page$` re-fetch via `switchMap` sur `urlToSpace` |

---

## 8. Ce qui ne se réinitialise pas entre deux sessions (même navigateur, sans rechargement)

Ces états sont intentionnellement conservés ou sans impact sécurité :

| État | Comportement | Impact |
|---|---|---|
| `AppRuntimeStore.bootstrapScope` | Reste `'private'` → `initPrivateRuntime()` devient no-op | Neutre : le bootstrap a été fait en ② via `refreshSession(true)` |
| `PrivateBootstrapStore` signaux | Écrasés par `setBootstrap()` lors du nouveau login | Correct — nouvelles valeurs tenant admin |
| `ThemeStore` | Écrasé par `applyBootstrapTheme()` lors du nouveau login | Correct — thème tenant admin |
| `PrivateShellService.page$` cache | Invalidé au premier `NavigationEnd` vers un espace différent | Correct depuis le fix `switchMap` |

---

## 9. Séquence complète (résumé)

```
Public
  GET /public/runtime/bootstrap  (no auth)

Login superadmin
  Firebase: signInWithEmailAndPassword
  GET /tenant/runtime/bootstrap  Bearer=<sa-token>
    → roles: [SUPER_ADMIN], tenantContext: null

/app → spaceDispatchGuard → /app/platform
  roleGuard('SUPER_ADMIN') → ok
  PrivateShellPage init
    GET /tenant/runtime/bootstrap  (AppRuntimeStore, redondant mais idempotent)
    page$ : GET /platform/page  Bearer=<sa-token>
    navigation → PLATFORM_NAVIGATION

Opérations superadmin
  GET|POST /platform/**  Bearer=<sa-token>  (auto via intercepteur)
  401 → retry avec token rafraîchi si expiré

Logout
  Firebase: signOut
  sessionState → { authenticated: false, roles: [] }
  navigate → /login

Login admin tenant
  Firebase: signInWithEmailAndPassword
  GET /tenant/runtime/bootstrap  Bearer=<admin-token>
    → roles: [TENANT_ADMIN], tenantContext: { tenantCode: 'HAITILOTO' }

/app → spaceDispatchGuard → /app/admin
  roleGuard('TENANT_ADMIN') → ok
  PrivateShellPage init (nouvelle instance)
    page$ : NavigationEnd '/app/admin' → urlToSpace change → switchMap
      GET /admin/page  Bearer=<admin-token>
    navigation → TENANT_ADMIN_NAVIGATION

Opérations admin tenant
  GET|POST /admin/**  Bearer=<admin-token>  (auto via intercepteur)
```

---

## 10. Points de vigilance

**`entryRoute` via `pageModelRef?.route`** (`auth-session.service.ts:49`) : si le backend retourne un `pageModelRef.route` débutant par `/app/platform/` pour un utilisateur sans rôle `SUPER_ADMIN`, `spaceDispatchGuard` tente d'y envoyer l'utilisateur. Le `roleGuard` bloque, mais l'UX produit un écran `/forbidden`. Vérifier côté backend que `pageModelRef.route` correspond toujours à l'espace du `space` retourné.

**Double rôle** (`SUPER_ADMIN` + `TENANT_ADMIN`) : `spaceDispatchGuard` dispatche toujours vers `/app/platform`. Si un utilisateur doit agir comme admin tenant, il doit passer par `Support tenant` dans la sidenav superadmin.

**Pas de rechargement de page entre sessions** : `AppRuntimeStore.bootstrapScope` reste `'private'`. Inoffensif si `login()` → `refreshSession(true)` applique bien le bootstrap. Si un utilisateur arrive sur `/app` directement sans passer par `login()` (ex. lien externe), le `spaceDispatchGuard` appelle `refreshSession()` qui re-bootstrap normalement.
