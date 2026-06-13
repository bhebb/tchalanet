
# Platform Capability `platform.identity` — User & Tenant Identity

Guide opératoire des providers, variables et tests :
`docs/conventions/identity-providers.md`.

## Rôle

Gérer les profils utilisateurs dans le contexte tenant, mapper une identité externe vérifiée vers
un utilisateur applicatif, et exposer le contexte utilisateur aux autres modules.

**Ce module fait** :
- Mapping provider-neutral d'un token déjà vérifié vers `ExternalAuthenticatedUser`
- Résolution et bootstrap d'un `UserId` depuis l'identité externe
- Lecture profil utilisateur courant et lookup par UUID
- Exposition des infos utilisateur (`CurrentUserView`, `AppUserView`, `UserProfileView`)
- Résolution de la surface client (`ClientSurface`) depuis les rôles
- **Appartenance tenant** (`tenant_user` / membership) — membership-only
- **Orchestration du provisioning** d'un utilisateur tenant (app_user + membership + sync KC),
  en déléguant l'affectation de rôle à `platform.accesscontrol`

**Ce module ne fait pas** :
- Autorisation métier (→ `platform.accesscontrol`)
- Gestion des rôles/permissions, affectation effective, overrides (→ `platform.accesscontrol`)
- Configuration tenant (→ `platform.tenantconfig`)
- Contexte opérationnel (terminal/outlet/session) — reste hors identity

La vérification externe est exposée par `IdentityProviderApi`. Le chemin Keycloak actif mappe le
JWT déjà vérifié sans seconde vérification. Le bootstrap résout ensuite l'utilisateur via
`app_user_external_identity`. Les sujets Keycloak existants sont backfillés avec l'issuer sentinelle
`legacy:keycloak`, puis liés à l'issuer réel lors de leur première authentification vérifiée.
`app_user` ne contient aucun identifiant fournisseur. Pour Keycloak, l'identifiant utilisateur est
stocké dans `app_user_external_identity.external_subject` avec `provider=KEYCLOAK`. Les noms
`KeycloakUserSub` encore exposés dans quelques APIs sont transitoires et ne représentent plus une
colonne de `app_user`.

Le provider actif est sélectionné par `tch.identity.provider`. En mode `firebase`, le decoder
valide la signature via le JWKS Google officiel, l'expiration, l'issuer
`https://securetoken.google.com/{projectId}`, l'audience `{projectId}` et le subject. Les claims
Firebase ne créent aucune autorité Spring; rôles et permissions restent résolus par Tchalanet.
Le contrôle Firebase Admin de révocation et d'utilisateur désactivé est configurable avec
`tch.identity.firebase.revocation-check-mode`: `off`, `sensitive-only` (défaut V0) ou `always`.
Lorsqu'il est requis, le contrôle échoue fermé et compare `auth_time` au timestamp de révocation
Firebase. Les credentials viennent de `FIREBASE_CREDENTIALS_PATH` ou des Application Default
Credentials.

Le bootstrap d'identité externe est contrôlé par `tch.security.user-bootstrap.mode`:
`deny` (défaut), `invite-only`, `admin-preprovisioned` ou `controlled-auto`. Les modes de liaison
par email exigent un email externe vérifié; `admin-preprovisioned` accepte aussi une identité
Firebase Phone correspondant exactement au numéro E.164 local. `controlled-auto` exige une
allowlist et crée seulement un `AppUser` `PENDING_APPROVAL`; il ne crée ni membership, ni rôle, ni
permission. En production, `controlled-auto` fait échouer le démarrage sans approbation explicite
et allowlist.

En mode `admin-preprovisioned`, l'admin crée préalablement un `AppUser` `ACTIVE`. À la première
connexion, l'email Firebase vérifié ou un numéro E.164 provenant d'une authentification Firebase
Phone (`firebase.sign_in_provider=phone`) permet de retrouver cet utilisateur et de créer le lien
durable vers le subject Firebase; l'accès est alors immédiat. Les connexions suivantes utilisent ce
lien durable et ne dépendent plus de l'email ou du téléphone. Un utilisateur non `ACTIVE` n'est
jamais lié dans ce mode. Le téléphone `app_user.phone` est unique parmi les utilisateurs actifs.

Les providers `local-jwt` et `local-perf` utilisent des JWT HS256 signés avec un secret local d'au
moins 32 caractères. Ils traversent le même mapping d'identité externe, le même contrôle de statut
`AppUser`, le même contexte, les mêmes permissions et la même RLS. `local-perf` ne constitue pas un
bypass d'autorisation; il sert uniquement à distinguer les fixtures de charge. Les deux providers
font échouer le démarrage avec un profil `prod` ou `production`.

Les rôles présents dans un token externe ou local sont uniquement des hints de pré-routage Spring
Security. Après résolution de l'`AppUser` et du tenant, `TchContextFilter` remplace les rôles et
permissions du contexte par ceux chargés depuis les tables d'access control. Un override tenant
annoncé par un faux hint `SUPER_ADMIN` est refusé si la base ne confirme pas ce rôle.

---

## Enums

### `UserStatus` : `PENDING_APPROVAL` · `ACTIVE` · `SUSPENDED` · `INVITED`
### `TenantUserStatus` : `INVITED` · `PENDING_APPROVAL` · `ACTIVE` · `SUSPENDED`
### `AutonomyLevel` : `NONE` · `PARTIAL` · `FULL`

### `ClientSurface`

| Valeur | Sens |
|---|---|
| `MOBILE_POS` | Application mobile POS (vendeur terrain) |
| `CASHIER_WEB` | Interface cashier web |
| `TENANT_ADMIN_WEB` | Interface admin tenant |
| `PLATFORM_ADMIN_WEB` | Interface super-admin |

`ClientSurfacePolicy.preferredSurface(roles)` détermine la surface principale selon les rôles.

---

## API — `IdentityApi`

```java
CurrentUserView       getCurrentUser(GetCurrentUserRequest)
BootstrapUserResult   bootstrapCurrentUser(BootstrapCurrentUserRequest)
  // isNew=true si premier bootstrap — idempotent
UserProfileView       getUserProfile(GetUserProfileRequest)
Optional<AppUserView> findAppUser(UUID keycloakSub)
long                  countTenantUsers()
```

**`CurrentUserView`** : id, keycloakSub, username, email, firstName, lastName, displayName, tenantId, tenantCode, tenantTimeZone, tenantCurrency, themeMode, density, locale, timeZone, currency

**`AppUserView`** : id, keycloakSub, username, email, phone, firstName, lastName, displayName, status (`UserStatus`)

---

## Provisioning utilisateur Firebase

`POST /admin/identity/users` (TENANT_ADMIN / SUPER_ADMIN) crée d'abord l'identité Firebase,
puis l'utilisateur applicatif et son lien durable dans `app_user_external_identity`.
Si la transaction Tchalanet échoue après la création Firebase, le compte Firebase nouvellement
créé est supprimé en compensation.

Firebase ne reçoit ni rôle, ni permission, ni tenant opérationnel. Le rôle, le tenant effectif,
les permissions et le contexte RLS sont toujours résolus depuis Tchalanet.

En `local-ide`, le bootstrap Firebase crée et lie de façon idempotente `super_admin`, `admin`
et `cashier`, avec des UID égaux aux UUID déterministes des `app_user` seedés.

## Structure interne (convention « API adapter »)

`internal/model/` (records domaine) · `internal/service/` (services fins) ·
`internal/firebase/` (verification, provisioning + bootstrap Firebase) ·
`internal/adapter/IdentityApiAdapter` (impl de l'API publique, délègue uniquement) ·
`internal/persistence` · `internal/web/{me,admin,ops}`.

**Séparation des responsabilités** (réorg `identity-user-provisioning-reorg`) :

- `TenantMembershipService` est **membership-only** : il ne fait pas `setRole`, n'affecte ni
  ne calcule de permission, et n'injecte pas `AccessControlApi`.
- `TenantUserProvisioningService` **orchestre** la création d'un utilisateur tenant : app_user +
  membership (identity), puis `AccessControlApi.assignRoleToUser(...)` (accesscontrol).
  Aucune permission n'est copiée dans la ligne utilisateur — elles dérivent du rôle + overrides.
- `TenantUserAdministrationService` compose la vue admin (profil + membership + rôles/permissions
  effectives via accesscontrol + statut invitation/sync) — sorti du controller, qui reste mince.

## Intégration

- Consommé par `TchContextFilter` pour bootstrapper le contexte de requête
- `platform.accesscontrol` utilise `IdentityApi` pour la résolution d'actor
- RLS actif sur toutes les tables utilisateur

## Règles

- `core` ne doit pas écouter les events de ce module directement
- `bootstrapCurrentUser` est idempotent — crée l'utilisateur si absent
