# DOMAIN_USER.md — Domaine User (`core.user`)

> **Type**: Functional Domain Specification  
> **Scope**: Core (`com.tchalanet.server.core.user`)  
> **Related**: `OpenSpec — Core Rules (80)`  
> **External reference (MkDocs)**:  
> `tchalanet-docs/docs/02-functional/domains/accesscontrol.md`  
> _(user / accesscontrol interplay)_

---

## 1. Scope

### 1.1 Responsabilité principale

Le domaine **User** gère le **profil applicatif** d’un utilisateur (`AppUser`) et ses
**préférences UI** (`UserPreference`), tout en restant compatible avec une identité
externe fournie par **Keycloak**.

---

### 1.2 Ce que le domaine fait

- Upsert / synchronisation d’un utilisateur lors du login (bootstrap depuis JWT Keycloak).
- Gestion du lifecycle applicatif utilisateur :
  - create
  - approve
  - suspend
  - reactivate
  - delete
- Mise à jour du profil applicatif :
  - nom, email, téléphone
  - locale, timezone
  - avatar
- Gestion des préférences utilisateur :
  - `theme_mode`
  - `density`
  - `locale`

---

### 1.3 Ce que le domaine ne fait PAS

- Authentification / login (Keycloak).
- Gestion des rôles et permissions (→ `core.accesscontrol`).
- Résolution du tenant à partir d’un code (déjà résolu par le contexte).
- Logique UI ou BFF complexe :
  - l’assemblage `me + preferences` est fait **dans l’adapter web**, pas dans le domaine.

---

## 2. Tables & Index

### 2.1 Table `app_user`

**Clé primaire**

- `id` UUID (généré par PostgreSQL)

**Identité externe**

- `keycloak_id` UUID (unique, nullable)

**Profil**

- `username`
- `email`
- `phone`
- `first_name`
- `last_name`
- `display_name`
- `avatar_url`
- `locale`
- `time_zone`

**Multi-tenant**

- `tenant_id` (tenant principal, optionnel)
- `tenant_code` (issu du contexte / claim)

**Lifecycle**

- `status` (enum `UserStatus`)
- `last_login_at`
- `approved_at`

**Audit / versioning**

- `version`
- `created_at`
- `updated_at`
- `deleted_at` (si soft-delete technique)

#### Index recommandés

- `ux_app_user_keycloak_id` UNIQUE WHERE `keycloak_id IS NOT NULL`
- `ux_app_user_email` UNIQUE WHERE `email IS NOT NULL`
- `ux_app_user_phone` UNIQUE WHERE `phone IS NOT NULL`
- `idx_app_user_tenant_code`
- `idx_app_user_tenant_id`

> **Décision**  
> `app_user.id` **n’est pas** le `keycloak_id`.  
> Keycloak reste une identité externe référencée.

---

### 2.2 Table `user_preference`

- `id` UUID (PK)
- `user_id` → FK vers `app_user(id)`
- `theme_mode`
- `density`
- `locale`
- `version`
- `created_at`
- `updated_at`
- `deleted_at`

---

## 3. Modèle de domaine

### 3.1 Aggregate Root — `AppUser`

Responsable du **lifecycle applicatif**.

#### Méthodes métier (exemples)

- `approve(approvedBy, approvedAt)`
- `suspend(reason?, performedBy, at)`
- `reactivate(reason?, performedBy, at)`
- `updateProfile(...)`
- `touchLogin(at)`

> L’aggregate **ne contient aucun DTO web**, aucune logique UI, aucun mapping.

---

### 3.2 Préférences utilisateur — `UserPreference`

- Valeurs simples (pas d’invariants complexes)
- Associées à `AppUser` par `userId`

Si un jour des règles métier apparaissent → bascule CQRS.

---

### 3.3 Enum `UserStatus` (V1)

États autorisés :

- `INVITED`
- `ACTIVE`
- `SUSPENDED`

Notes :

- `DELETED` n’est **pas** un status (suppression DB).
- `ARCHIVED` = futur batch / cleanup (hors métier).

---

## 4. Commands (CQRS)

**Emplacement**

- `core.user.application.command.model`
- `core.user.application.command.handler`

### Liste V1

- `CreateUserCommand`
- `ApproveUserCommand`
- `SuspendUserCommand`
- `ReactivateUserCommand`
- `DeleteUserCommand`
- `EnsureUserExistsForPrincipalCommand`
- `UpdateUserProfileCommand`

#### Règles

- 1 command = 1 intention
- Pas de `UpdateUserStatusCommand` générique
- `ClockPort` obligatoire dans les handlers
- Commands idempotentes si invoquées par bootstrap

---

## 5. Queries (CQRS)

**Emplacement**

- `core.user.application.query.model`
- `core.user.application.query.handler`

### Liste V1

- `GetCurrentUserQuery(keycloakId)` → `UserDetails`
- `GetUserDetailsQuery(userId)` → `UserDetails`
- `GetUserQuery(userId)` → `UserRow`
- `ListAllUsersQuery` / `PagedListAllUsersQuery`
- `ListTenantUsersQuery` / `PagedListTenantUsersQuery`

> Les queries retournent des **read models**, pas l’aggregate.

---

## 6. Ports (Hexagonal)

**Emplacement**

- `core.user.application.port.out`

### Ports utilisateur

- `UserReaderPort`
- `UserWriterPort`

### Ports préférences

- `UserPreferenceReaderPort`
- `UserPreferenceWriterPort`

---

### Intégration Keycloak

- Via `core.external.ports.KeycloakUserProvisioningPort`
- Appelé par certains handlers selon la policy :
  - approve
  - suspend
  - reactivate
  - delete
  - update profile

Le domaine **ne dépend pas** de `accesscontrol` repositories.

---

## 7. Infrastructure

### 7.1 Persistence

**Emplacement**

- `core.user.infra.persistence`

Contenu :

- `AppUserJpaEntity`
- `UserPreferenceJpaEntity`
- `JpaAppUserRepository`
- `JpaUserPreferenceRepository`
- `AppUserPersistenceAdapter`

---

### 7.2 Web / Mini-BFF profile

**Emplacement**

- `core.user.infra.web.api`

- `ProfileController`
  - endpoint `/me`
  - endpoint bootstrap
  - agrégation `UserDetails + UserPreference`
  - aucune logique métier

> Spring Data REST autorisé **uniquement** pour `user_preference`
> et **self-only**.

---

## 8. Events & Consumers

### V1

Aucun event métier obligatoire.

### Events possibles (V2+)

- `UserApprovedEvent`
- `UserSuspendedEvent`
- `UserReactivatedEvent`
- `UserDeletedEvent`
- `UserBootstrappedEvent` (technique)

Consumers potentiels :

- notifications
- reporting
- provisioning POS

---

## 9. Failure modes & fallback

- JWT `sub` non UUID → rejet explicite ou mapping contrôlé
- Keycloak indisponible :
  - actions admin → fail fast + log
- Violations d’unicité (email / phone / keycloak_id) :
  - erreurs explicites
  - tests obligatoires

---

## 10. Tests — Definition of Done

### Domain

- transitions autorisées :
  - `INVITED → ACTIVE`
  - `ACTIVE → SUSPENDED`
  - `SUSPENDED → ACTIVE`
- transitions interdites testées

### Application

- happy paths des handlers
- idempotence bootstrap
- gestion des erreurs Keycloak

### Integration

- contraintes uniques
- paging queries

---

## 11. Mini-checklist finale

- [ ] Aucun DTO web dans `domain/` ou `application/`
- [ ] Aucune logique métier dans les controllers
- [ ] Pas de command générique de status
- [ ] `ClockPort` utilisé partout
- [ ] `keycloak_id` unique, `app_user.id` DB-generated
- [ ] Préférences simples (CQRS si invariants)
- [ ] Aucun accès direct à `accesscontrol` repos
