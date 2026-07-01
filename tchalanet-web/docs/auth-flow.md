# Auth flow — Tchalanet Web

Document technique de référence. Décrit le cycle complet d'authentification sur les **trois
applications Angular** : affichage public → connexion → redirection vers le portail privé →
opérations → restauration de session → déconnexion.

> Architecture **3 apps** (depuis le split). L'ancien doc décrivait une mono-app
> (`AppRuntimeStore`, `PrivateShellPage`, `PrivateShellService`) — ces classes n'existent plus
> côté privé. Chaque portail est désormais une app Nx/Angular autonome partageant la lib
> `@tch/core/auth`.

---

## 0. Topologie des applications

| App | `appId` | Servie sous | Rôle | Shell |
|---|---|---|---|---|
| **public-portal** | `public-portal` | `/` | Vitrine marketing + **redirection** vers les portails privés | `TchPublicShellComponent` |
| **admin-portal** | `admin-portal` | `/admin` | Espace TENANT_ADMIN | `App` + `PrivateShellLayoutComponent` |
| **platform-portal** | `platform-portal` | `/platform` | Espace SUPER_ADMIN | `App` + `PrivateShellLayoutComponent` |

Les bases d'URL inter-portails viennent de `TchRuntimeConfig.portalBaseUrls`
(`runtime-config.ts`) : `{ 'admin-portal': '/admin', 'platform-portal': '/platform' }`.
La redirection inter-app se fait par `location.assign()` (navigation navigateur complète),
pas par le `Router` Angular — chaque portail est un bundle séparé.

---

## 1. Acteurs et responsabilités (lib partagée `@tch/core/auth`)

| Classe / fichier | Rôle |
|---|---|
| `FirebaseAuthService` · `firebase/firebase-auth.service.ts` | Implémente `AuthClient` — wrappe le SDK Firebase Auth |
| `AUTH_CLIENT` (InjectionToken) · `auth-client.ts` | Point d'injection de `AuthClient` (substituable) |
| `AuthSessionService` · `auth-session.service.ts` | Session applicative — construit `UserSession` depuis le bootstrap |
| `AuthRedirectService` · `auth-redirect.service.ts` | Décide la route post-login et la redirection same-app / cross-app |
| `LoginPage` · `firebase/login.page.ts` | Formulaire de connexion + restauration de session (`ngOnInit`) |
| `authBearerInterceptor` · `auth-bearer.interceptor.ts` | Ajoute `Authorization: Bearer <jwt>` + retry 401 |
| `authGuard` · `auth.guard.ts` | Exige une session authentifiée (sinon `/login`) |
| `spaceDispatchGuard` · `auth.guard.ts` | Route d'entrée `/app` — dispatche selon le rôle / `entryRoute` |
| `roleGuard(role)` · `auth.guard.ts` | Protège `/app/platform`, `/app/admin`, `''` |
| `PrivateRuntimeInitializer` · `runtime/private-runtime-initializer.ts` | Applique theme/i18n/settings + peuple le store |
| `PrivateBootstrapService` · `runtime/private-bootstrap.service.ts` | `GET /runtime/private` |
| `PrivateBootstrapStore` · `runtime/private-bootstrap.store.ts` | Store signal du runtime privé |

Côté apps privées : `App` (`app.ts`) héberge `PrivateShellLayoutComponent` (nav, header, thème,
logout). Côté public : `TchPublicShellComponent` + `PublicRuntimeStore`/`PublicShellService`.

---

## 2. Affichage public (avant connexion)

```
public-portal  →  /  →  TchPublicShellComponent  →  publicRoutes (home, results, rules, …)
```

**Initialisation** : `TchPublicShellComponent` (constructeur) appelle `PublicRuntimeStore.init()`
→ bootstrap public (`GET /public/...`), sans token. `authBearerInterceptor` passe sans header
(token = `null`).

**État** : `AuthSessionService.session()` = `{ authenticated: false, roles: [] }`. Thème/i18n
publics appliqués. Le bouton « Connexion » du header public appelle :

```ts
// public-shell.component.ts
protected login(): void {
  void this.router.navigate(['/login']);   // LoginPage, dans public-portal
}
```

---

## 3. Connexion — `LoginPage` (partagée par les 3 apps)

`LoginPage` est montée sur `/login` dans chaque app (route ouverte, aucun guard).

```ts
// firebase/login.page.ts
async submit(): Promise<void> {
  this.loading.set(true);
  try {
    const session = await this.authSession.login(this.email, this.password, this.remember);
    if (!session.authenticated) { this.errorKey.set('auth.login.errors.accessDenied'); return; }
    await this.authRedirect.navigateAfterLogin(session);
  } catch {
    this.errorKey.set('auth.login.errors.invalidCredentials');
  } finally {
    this.loading.set(false);   // garanti — le bouton ne reste jamais bloqué
  }
}
```

### `AuthSessionService.login()`

```ts
// auth-session.service.ts  — chaque étape bornée par withTimeout (15 s)
await withTimeout(this.auth.login({ username, password, remember }), 15_000, 'auth.login.timeout');
await withTimeout(this.auth.getAccessToken(true), 15_000, 'auth.token.timeout');   // force refresh JWT
return withTimeout(this.refreshSession(true), 15_000, 'auth.session.timeout');     // bootstrap backend
```

- `getAccessToken(true)` force le refresh du JWT Firebase **avant** le bootstrap : les claims
  (rôles, tenant) sont à jour côté backend.
- L'enveloppe `withTimeout` sur `refreshSession(true)` garantit que le bouton se débloque même si
  le backend pend (corrige le symptôme « Authentification… » infini).

### ① Firebase — `FirebaseAuthService`

```ts
// firebase/firebase-auth.service.ts
async login(request: AuthLoginRequest): Promise<void> {
  await signInWithEmailAndPassword(this.auth, request.username, request.password);
}
```

La persistance est fixée **une seule fois** dans le constructeur, dans le contexte d'injection
Angular (corrige l'avertissement AngularFire « outside injection context ») :

```ts
constructor() {
  void this.auth.authStateReady().then(() =>
    runInInjectionContext(this.injector, () => setPersistence(this.auth, browserLocalPersistence)),
  );
}
```

> ⚠️ **Persistance toujours locale et assumée.** Outil métier : on reste connecté entre les
> fermetures d'onglet. Le flag `remember` et la case « se souvenir » ont été **retirés** (interface
> `AuthLoginRequest`, `LoginPage`, `AuthSessionService`) — la persistance permanente est désormais
> le comportement explicite, sans contrôle UI trompeur.

### ② Bootstrap backend — `refreshSession(force)`

```ts
// auth-session.service.ts
async refreshSession(force = false): Promise<UserSession> {
  if (!(await this.auth.isAuthenticated())) return this.setAnonymousSession();   // Firebase local
  if (!force && this.sessionState().authenticated) return this.sessionState();   // cache signal
  const bootstrap = await firstValueFrom(
    this.runtime.initialize().pipe(timeout({ first: 15_000 })),
  );
  // … construit UserSession (voir plus bas)
}
```

Chaîne d'appel :

```
refreshSession(true)
  └─ PrivateRuntimeInitializer.initialize()
       └─ PrivateBootstrapService.bootstrap()
            └─ GET /runtime/private        (Bearer <firebase-jwt> via authBearerInterceptor)
```

`/runtime/private` est en **scope PLATFORM** : le JWT ne porte pas de tenant, donc
`tenantContext` peut être `null` même pour un TENANT_ADMIN — c'est normal, le rôle est dérivé de
l'`AccessSnapshot` côté backend et de `space`.

**Réponse `RuntimeBootstrapResponse`** (champs clés) :

```json
{
  "space": "ADMIN",
  "user": { "userId", "username", "roles": ["TENANT_ADMIN"], "mustChangePassword": false, ... },
  "tenantContext": { "tenantId", "tenantCode", "tenantName" },   // null en scope PLATFORM
  "entitlements": { "roles": [...], "permissions": [...] },
  "settings": { "locale", "timezone", "currency", "features": {} },
  "theme": { "presetCode", "mode", "tokens": {} },
  "i18n": { "locale", "messages": {} },
  "readiness": { "status": "READY", "checks": [] },
  "pageModelRef": { "route": "/app/admin", "endpoint": "..." },
  "entryRoute": null,
  "notices": null                  // ⚠ Java renvoie null (pas []) pour les listes vides
}
```

`PrivateRuntimeInitializer.applyBootstrap()` applique immédiatement i18n, thème
(`ThemeStore.applyBootstrapTheme`), settings (`RuntimeSettingsStore`) et peuple
`PrivateBootstrapStore`.

**Construction de `UserSession`** :

```ts
// auth-session.service.ts
const session: UserSession = {
  authenticated: true,
  userId:      bootstrap.user.userId ?? undefined,
  username:    bootstrap.user.username ?? bootstrap.user.email ?? undefined,
  tenantId:    bootstrap.tenantContext?.tenantId,
  tenantCode:  bootstrap.tenantContext?.tenantCode ?? undefined,
  roles:       normalizeRoles(
                 [...(bootstrap.user.roles ?? []), ...(bootstrap.entitlements.roles ?? [])],
                 bootstrap.space,
               ),
  entryRoute:  bootstrap.entryRoute ?? bootstrap.pageModelRef?.route ?? undefined,
  mustChangePassword:  bootstrap.user.mustChangePassword ?? false,
  mustCompleteProfile: bootstrap.user.mustCompleteProfile ?? false,
};
```

`normalizeRoles(roles, space)` :
- `ROLE_SUPER_ADMIN` / `PLATFORM_ADMIN` → `SUPER_ADMIN`
- `ROLE_TENANT_ADMIN` / `TENANT_OWNER` → `TENANT_ADMIN`
- `ROLE_CASHIER` / `OPERATOR` / `ACTOR_SELLER_TERMINAL` → `CASHIER`
- **Filet de sécurité par espace** : `space === 'ADMIN'` ajoute `TENANT_ADMIN`, `'PLATFORM'` ajoute
  `SUPER_ADMIN`, `'CASHIER'` ajoute `CASHIER`. Garantit un rôle même si `roles`/`entitlements` sont
  vides.

**Gestion d'erreur** : un 401/403 (accès refusé Tchalanet) → `setAnonymousSession()` (logout
implicite). Une erreur réseau / timeout / 5xx → conserve la dernière session connue (pas de
déconnexion sur un hoquet serveur).

---

## 4. Redirection post-login — `AuthRedirectService.navigateAfterLogin()`

C'est le cœur de la **redirection** (notamment depuis public-portal).

```ts
// auth-redirect.service.ts
async navigateAfterLogin(session: UserSession): Promise<void> {
  const route      = this.postLoginRoute(session);    // entryRoute | rôle → /app/platform | /app/admin
  const targetApp  = this.targetApp(session, route);  // 'platform-portal' | 'admin-portal' | null
  const currentApp = this.runtimeConfig.config().appId;

  if (currentApp === targetApp) {
    await this.router.navigateByUrl(route);                                  // même app → Router
    return;
  }
  if (currentApp === 'public-portal' && targetApp) {
    this.assignBrowserUrl(`${this.portalBaseUrl(targetApp)}${route}`);       // public → portail privé
    return;
  }
  await this.router.navigateByUrl('/forbidden');                             // incohérence
}
```

| Contexte | Comportement |
|---|---|
| Login depuis **platform-portal**, rôle SUPER_ADMIN | `router.navigateByUrl('/app/platform')` (même app) |
| Login depuis **admin-portal**, rôle TENANT_ADMIN | `router.navigateByUrl('/app/admin')` (même app) |
| Login depuis **public-portal**, SUPER_ADMIN | `location.assign('/platform/app/platform')` (cross-app) |
| Login depuis **public-portal**, TENANT_ADMIN | `location.assign('/admin/app/admin')` (cross-app) |
| App courante ≠ app cible (ex. TENANT_ADMIN sur platform-portal) | `/forbidden` |

`postLoginRoute` : `session.entryRoute` prioritaire, sinon `/app/platform` (SUPER_ADMIN) ou
`/app/admin` (TENANT_ADMIN), sinon `/forbidden`. `targetApp` déduit le portail depuis le préfixe
de route (`/app/platform` → platform-portal, `/app/admin|/app/profile|/app/seller-terminal` →
admin-portal, `/app/account` → selon rôle) puis fallback rôle.

---

## 5. Restauration de session (retour d'un utilisateur déjà connecté)

À l'ouverture de `/login`, `LoginPage.ngOnInit()` tente une redirection automatique si Firebase a
encore une session persistée (localStorage), **sans appeler le backend** (pour ne pas déclencher
`bootstrapStore.setLoading()` qui figerait le formulaire) :

```ts
// firebase/login.page.ts
private async redirectRestoredSession(): Promise<void> {
  // Détection de boucle guard-bounce
  const lastAttempt = Number(sessionStorage.getItem(LoginPage.RESTORE_TS_KEY) ?? 0);
  if (Date.now() - lastAttempt < LoginPage.RESTORE_WINDOW_MS) {   // 20 s
    sessionStorage.removeItem(LoginPage.RESTORE_TS_KEY);
    return;                                                       // on vient de rebondir → stop
  }
  if (!(await withTimeout(this.authClient.isAuthenticated(), 2_000))) return;  // Firebase only
  const route = this.localAuthenticatedEntryRoute();              // /app/admin | /app/platform | null
  if (!route) return;                                             // public-portal → null → reste sur le form
  sessionStorage.setItem(LoginPage.RESTORE_TS_KEY, String(Date.now()));
  await this.router.navigateByUrl(route);
}
```

Points clés :
- `localAuthenticatedEntryRoute()` est **hardcodée par app** (`admin-portal → /app/admin`,
  `platform-portal → /app/platform`, public-portal → `null`). Sur public-portal, aucune
  auto-redirection : la vitrine reste accessible.
- **Détection de boucle** : `RESTORE_WINDOW_MS = 20 s` doit dépasser le timeout du bootstrap
  (`AUTH_OPERATION_TIMEOUT_MS = 15 s`). Si le guard rejette la session et renvoie sur `/login`, le
  rebond (jusqu'à 15 s) tombe dans la fenêtre de 20 s → la redirection n'est pas rejouée. Une
  fenêtre trop courte (l'ancien 5 s) rouvrirait une boucle infinie sur backend lent.

---

## 6. Guards (`auth.guard.ts`)

```ts
// authGuard — exige une session
const session = await auth.refreshSession();
return session.authenticated ? true : router.parseUrl('/login');
```

```ts
// spaceDispatchGuard — route d'entrée /app
const session = await auth.refreshSession();
if (!session.authenticated)             return router.parseUrl('/login');
if (session.entryRoute)                 return router.parseUrl(session.entryRoute);  // priorité backend
if (roles.includes('SUPER_ADMIN'))      return router.parseUrl('/app/platform');
if (roles.includes('TENANT_ADMIN'))     return router.parseUrl('/app/admin');
if (roles.includes('CASHIER'))          return router.parseUrl('/app/cashier');
return router.parseUrl('/forbidden');
```

```ts
// roleGuard(requiredRole) — protège /app/admin, /app/platform, ''
const session = await auth.refreshSession();
if (!session.authenticated) return router.parseUrl('/login');
if (!auth.hasRole(requiredRole) && !isSupportTenantAdminAccess(...)) return router.parseUrl('/forbidden');
// Forçage activation (mot de passe / profil) — compare les DEUX formes d'URL :
const onActivation = state.url.startsWith('/account/activation')
                  || state.url.startsWith('/app/account/activation');
if (session.entryRoute === '/app/account/activation' && !onActivation)
  return router.parseUrl('/app/account/activation');
return true;
```

**Cache** : `refreshSession()` (sans `force`) renvoie la session en cache si déjà authentifiée —
**pas de second appel** `/runtime/private`. Le backend n'est rappelé qu'au premier chargement /
F5 (signal `sessionState` réinitialisé à `{ authenticated: false }`).

**`isSupportTenantAdminAccess`** : un SUPER_ADMIN avec le mode « support tenant » actif peut passer
un `roleGuard('TENANT_ADMIN')`.

---

## 7. Shell privé (admin-portal / platform-portal)

```ts
// app.ts (admin-portal et platform-portal, symétriques)
protected readonly showShell = computed(() => isPrivateShellRoute(this.currentUrl()));
// isPrivateShellRoute : false pour /login, /forgot-password, /forbidden
```

```html
<!-- app.html -->
@if (showShell()) {
  <tch-private-shell-layout [brand]="brand" [sections]="sections" [userName]="userName()"
    [darkMode]="darkMode()" (themeToggled)="toggleTheme()" (logoutRequested)="logout()">
    <router-outlet />
  </tch-private-shell-layout>
} @else {
  <main><router-outlet /></main>
}
```

Les sections de nav sont statiques par app (`TENANT_ADMIN_NAVIGATION`, `PLATFORM_NAVIGATION`).
Le thème/clair-sombre est piloté par `ThemeStore` (toggle dans le header).

### Dashboards

| App | Page | Appel |
|---|---|---|
| admin-portal | `AdminDashboardPage` | `PageModelApi.getTenantPage()` → `GET /tenant/dashboard` (logicalId résolu serveur via rôle) |
| platform-portal | `PlatformDashboardPage` | `PageModelApi.getPlatformPage(logicalId)` → `GET /platform/dashboard?logicalId=…` |

> ⚠ Le traitement de la réponse PageModel (`withSectionNotices`) doit null-guarder `notices` et
> `dynamic.errors` (Java renvoie `null` pour les listes vides) — sinon `TypeError` masqué par
> `catchError` → page en erreur silencieuse. Voir mémoire `java-null-empty-lists-web-guard`.

---

## 8. Routes par app (privées)

```
admin-portal                          platform-portal
  login                                 login
  forgot-password                       forgot-password
  account/activation  [roleGuard TA]    account/activation  [authGuard]
  profile             [authGuard]       profile             [authGuard]
  seller-terminal/activation [authGuard]
  app                 [spaceDispatch]   app                 [spaceDispatch]
  app/admin           [roleGuard TA]    app/platform        [roleGuard SA]
  ''                  [roleGuard TA]    ''                  [roleGuard SA]
  **  → ''                              **  → ''
```

(TA = `TENANT_ADMIN`, SA = `SUPER_ADMIN`.)

---

## 9. Interceptor & rafraîchissement de token

```ts
// auth-bearer.interceptor.ts — uniquement sur les URLs API Tchalanet
const token = await auth.getAccessToken();
req = withBearerToken(req, token);
// Sur 401 (et pas déjà rejoué) → getAccessToken(true) (force refresh Firebase) → rejoue 1 fois
```

Le token Firebase (TTL ~1 h) est renouvelé silencieusement par le SDK ; sur un 401 l'intercepteur
force un refresh et rejoue la requête une seule fois.

---

## 10. Déconnexion

```ts
// app.ts
protected async logout(): Promise<void> {
  await this.auth.logout();                  // signOut(firebase) + setAnonymousSession()
  await this.router.navigateByUrl('/login');
}
```

Après logout : `auth.currentUser = null`, `session() = { authenticated: false, roles: [] }`,
token localStorage supprimé. `PrivateBootstrapStore` n'est pas explicitement reset mais sera
écrasé au prochain login. La redirection reste **intra-app** (`/login` du portail courant).

---

## 11. Invariants de sécurité

| Invariant | Application |
|---|---|
| Aucune route privée sans JWT Firebase valide | `authBearerInterceptor` + guards |
| Rôle vérifié **côté serveur** à chaque requête | Backend valide JWT + scope/rôle par endpoint |
| Rôle vérifié côté client avant rendu | `roleGuard` / `spaceDispatchGuard` |
| TENANT_ADMIN ne peut pas atteindre platform-portal | `roleGuard('SUPER_ADMIN')` → `/forbidden` |
| SUPER_ADMIN ne peut pas atteindre admin-portal sans support actif | `roleGuard('TENANT_ADMIN')` + `isSupportTenantAdminAccess` |
| Token expiré renouvelé silencieusement | `authBearerInterceptor` retry 401 `forceRefresh=true` |
| Session effacée au logout | `signOut` + `setAnonymousSession` |
| Redirection inter-portail explicite | `AuthRedirectService` (`location.assign` cross-app) |

---

## 12. Points de vigilance

- **Persistance toujours locale (assumée)** : `FirebaseAuthService` fixe `browserLocalPersistence`
  une fois pour toutes. Le flag `remember` a été retiré (cf. §3) ; si un jour la persistance de
  session (`browserSessionPersistence`) redevient un besoin, réintroduire un paramètre explicite
  honoré dans `FirebaseAuthService.login()`, pas un flag mort.
- **Détection de boucle login** : `RESTORE_WINDOW_MS (20 s) > AUTH_OPERATION_TIMEOUT_MS (15 s)`.
  Toute réduction de la fenêtre sous le timeout du bootstrap rouvre la boucle
  `/login → /app/* → /login` sur backend lent.
- **Activation** : le `roleGuard` compare l'URL d'activation sous ses **deux** formes
  (`/account/activation` ET `/app/account/activation`) car `app/account/activation` `redirectTo`
  la forme canonique. Comparer une seule forme rouvre une boucle pour les comptes en activation.
- **Asymétrie `account/activation`** : admin-portal le protège par `roleGuard('TENANT_ADMIN')`,
  platform-portal par `authGuard`. Intentionnel (l'activation platform ne dépend pas d'un rôle
  tenant) mais à garder en tête.
- **`entryRoute` cross-espace** : si le backend renvoie un `pageModelRef.route` d'un autre espace
  que `space`, `spaceDispatchGuard` y envoie l'utilisateur et le `roleGuard` le rejette en
  `/forbidden`. Garder `pageModelRef.route` cohérent avec `space` côté backend.
- **Double rôle** (SUPER_ADMIN + TENANT_ADMIN) : `spaceDispatchGuard` privilégie toujours
  `/app/platform`. Pour agir en tenant admin, passer par le mode « Support tenant ».
- **Duplication `withTimeout`** : une implémentation dans `login.page.ts` (2 s, restauration) et
  une dans `auth-session.service.ts` (15 s, opérations). Candidates à factoriser dans un util
  partagé `@tch/core/auth`.

---

## 13. Séquence complète (résumé)

```
Public (public-portal)
  GET /public/...                          (no auth)
  clic « Connexion » → router /login

Login (n'importe quelle app)
  auth.login → signInWithEmailAndPassword  (persistance locale)
  getAccessToken(true)                     (JWT frais)
  refreshSession(true) → GET /runtime/private  Bearer=<jwt>
    → space + roles + tenantContext

navigateAfterLogin(session)
  même app        → router.navigateByUrl(/app/admin | /app/platform)
  public → portail → location.assign(/admin/app/admin | /platform/app/platform)

/app/* (portail privé)
  roleGuard → refreshSession() (cache) → hasRole ? render : /forbidden
  Dashboard → GET /tenant/dashboard | /platform/dashboard?logicalId=…
  Opérations → GET|POST /admin/** | /platform/**  Bearer auto (retry 401)

Retour ultérieur sur /login (session Firebase persistée)
  ngOnInit → redirectRestoredSession → /app/* (avec détection de boucle 20 s)

Logout
  signOut + setAnonymousSession → router /login (intra-app)
```
