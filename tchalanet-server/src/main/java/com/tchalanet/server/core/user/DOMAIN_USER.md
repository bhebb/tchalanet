# DOMAIN_USER.md — Domaine User (core.user)

## 1) Scope

### Responsabilité principale

Gérer le **profil applicatif** d’un utilisateur (AppUser) et ses **préférences UI** (UserPreference), tout en restant compatible avec une identité externe (Keycloak).

### Ce que le domaine fait

- Upsert / synchronisation d’un utilisateur lors du login (bootstrap depuis JWT Keycloak).
- Lifecycle applicatif : create / approve / suspend / reactivate / delete.
- Mise à jour du profil applicatif (nom, email, locale, timezone, avatar…).
- Gestion des préférences utilisateur (theme_mode, density, locale) via ports + persistence.

### Ce que le domaine ne fait pas

- Authentification / login (Keycloak).
- Rôles & permissions (core.accesscontrol).
- Résolution du tenant à partir d’un code (fait par le filtre/contexte; pas un port tenant dans user).
- Logique UI/BFF : l’assemblage “me + preferences” est fait dans l’adapter web (mini BFF), pas dans le domaine.

---

## 2) Tables & Index

### Table `app_user`

- Clé primaire: `id` (UUID généré par Postgres)
- Identité externe: `keycloak_id` (UUID, unique, indexé)
- Champs profil: username, email, phone, first_name, last_name, display_name, avatar_url, locale, time_zone
- Multi-tenant: `tenant_id` (tenant principal optionnel), `tenant_code` (claim / contexte)
- Lifecycle: `status` (enum) + timestamps (last_login_at, approved_at, … si présents)
- Versioning/audit: `version`, `created_at`, `updated_at`, `deleted_at` (si utilisé)

Index recommandés:

- `ux_app_user_keycloak_id` UNIQUE WHERE keycloak_id IS NOT NULL
- `ux_app_user_email` UNIQUE WHERE email IS NOT NULL
- `ux_app_user_phone` UNIQUE WHERE phone IS NOT NULL
- `idx_app_user_tenant_code` (si utilisé par recherche)
- `idx_app_user_tenant_id` (si requêtes fréquentes)

> Décision: `app_user.id` n’est **pas** le keycloak_id. Keycloak reste dans `keycloak_id`.

### Table `user_preference`

- Clé primaire: `id` (UUID)
- FK: `user_id -> app_user(id)`
- Champs: theme_mode, density, locale
- Versioning/audit: `version`, `created_at`, `updated_at`, `deleted_at`

---

## 3) Modèle de domaine

### Aggregate Root

#### `AppUser`

- Champs métier (profil applicatif + status)
- Méthodes métier (exemples):
  - `approve(approvedBy, approvedAt)`
  - `suspend(reason?, performedBy, at)`
  - `reactivate(reason?, performedBy, at)`
  - `updateProfile(...)`
  - `touchLogin(at)`

> Pas de champs UI/projection dans l’aggregate. Le domaine ne renvoie pas de DTO web.

### Préférences

#### `UserPreference`

- Valeurs simples: theme_mode, density, locale
- Associé à `AppUser` par `userId`

### Enum `UserStatus` (V1)

État recommandé:

- `INVITED` (créé par admin, pas encore approuvé)
- `ACTIVE`
- `SUSPENDED`

> `DELETED` n’est pas un status : la suppression correspond à un delete DB (ou deleted_at si tu gardes un soft-delete technique, mais pas piloté par status).
> `ARCHIVED` = process ultérieur (batch / cleanup), hors cycle métier.

---

## 4) Commands (CQRS)

Emplacement:

- `core.user.application.command.model`
- handlers: `core.user.application.command.handler`

### Liste V1 (implémentée dans la nouvelle monture)

- `CreateUserCommand`  
  Crée un `AppUser` (souvent `INVITED`) et associe l’identité Keycloak si provisioning admin est utilisé (via core.external).
- `ApproveUserCommand`  
  Transition `INVITED -> ACTIVE`.
- `SuspendUserCommand`  
  Transition `ACTIVE -> SUSPENDED`.
- `ReactivateUserCommand`  
  Transition `SUSPENDED -> ACTIVE`.
- `DeleteUserCommand`  
  Suppression applicative (DB) + action Keycloak (disable / delete selon policy).
- `EnsureUserExistsForPrincipalCommand`  
  Upsert idempotent depuis le JWT Keycloak (bootstrap).
- `UpdateUserProfileCommand`  
  Mise à jour du profil applicatif (et éventuellement synchro Keycloak via port core.external, si appliqué).

Règles:

- 1 command = 1 intention
- Pas de `UpdateUserStatusCommand` générique
- Utiliser `ClockPort` dans les handlers (pas de `Instant.now()`)

---

## 5) Queries (CQRS)

Emplacement:

- `core.user.application.query.model`
- handlers: `core.user.application.query.handler`

### Liste V1 (alignée sur la monture)

- `GetCurrentUserQuery(keycloakId)` -> `UserDetails` (read model)
- `GetUserDetailsQuery(userId)` -> `UserDetails`
- `GetUserQuery(userId)` -> `UserRow` (ou domaine minimal)
- `ListAllUsersQuery` / `PagedListAllUsersQuery` -> `Page<UserRow>`
- `ListTenantUsersQuery` / `PagedListTenantUsersQuery` -> `Page<UserRow>`

> Les queries retournent des read models, pas l’aggregate complet si possible.

---

## 6) Ports (hexagonal)

Emplacement:

- `core.user.application.port.out`

### Ports user

- `UserReaderPort`
  - findById, findByKeycloakId, list/paging...
- `UserWriterPort`
  - save/update...

### Ports preferences

- `UserPreferenceReaderPort`
- `UserPreferenceWriterPort`

> Le domaine `user` ne dépend pas de `accesscontrol` repos. Les liens tenant/roles sont gérés ailleurs.

### Intégration Keycloak (dépendance externe)

- Via `core.external.ports.KeycloakUserProvisioningPort`
- Appelé par certains handlers user (Create/Approve/Suspend/Reactivate/Delete/UpdateProfile) selon policy.

---

## 7) Infra (persistence + web)

### Persistence

Emplacement: `core.user.infra.persistence`

- `AppUserJpaEntity`, `UserPreferenceJpaEntity`
- `JpaAppUserRepository`, `JpaUserPreferenceRepository`
- `UserMapper` (Option B: mapping manuel)
- `AppUserPersistenceAdapter` implémente ports out

### Web / mini BFF profile

- `core.user.infra.web.api.ProfileController`
  - Expose un endpoint “me” / “bootstrap”
  - Peut agréger `UserDetails + UserPreference` dans une réponse (mini BFF)
  - Ne contient aucune logique métier (uniquement assemblage + mapping)

> Spring Data REST peut être utilisé pour `user_preference` si et seulement si l’accès est “self-only”.

---

## 8) Events & consumers

V1:

- Pas d’obligation d’events métier pour user.
- Si nécessaire plus tard:
  - `UserApprovedEvent`
  - `UserSuspendedEvent`
  - `UserReactivatedEvent`
  - `UserDeletedEvent`
  - `UserBootstrappedEvent` (technique)

Consumers potentiels (features):

- stats / reporting
- notifications
- provisioning POS (si besoin)

---

## 9) Failure modes & fallback

- JWT Keycloak subject non-UUID: le handler bootstrap doit accepter le cas (ou refuser proprement).
- Keycloak indisponible:
  - sur actions admin (approve/suspend/reactivate/delete): fail fast + log + retry manuel
- Conflits unique (email/phone/keycloak_id):
  - errors explicites + tests

---

## 10) Tests (Definition of Done)

### Domain unit tests

- transitions autorisées:
  - INVITED -> ACTIVE
  - ACTIVE -> SUSPENDED
  - SUSPENDED -> ACTIVE
- transitions interdites (ex: approve sur ACTIVE)

### Application tests

- handlers happy path
- idempotence bootstrap (`EnsureUserExistsForPrincipal` appelé 2 fois)
- keycloak failure handling (si utilisé)

### Integration tests

- repo adapter + contraintes uniques
- paging queries

---

## 11) Mini-checklist

- [ ] Pas de DTO web dans core.user.domain/application
- [ ] Pas de logique métier dans controllers
- [ ] Pas de UpdateStatus générique
- [ ] ClockPort dans handlers
- [ ] keycloak_id unique, app_user.id généré DB
- [ ] Preferences CRUD sans invariants (sinon CQRS)
- [ ] Aucun accès direct aux repos accesscontrol depuis user
