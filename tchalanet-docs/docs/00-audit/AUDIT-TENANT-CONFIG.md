# Audit : Tenant Config

> Date : 2026-04-24  
> Scope : `core/tenantconfig/` · `core/tenantuser/` · `core/outlet/` · `core/pos/` · `core/tenantgame/` · `core/tenanttheme/` · `core/limitpolicy/` · `catalog/tenant/` · `catalog/drawchannel/` · `catalog/pricing/` · `features/tenantadmin/` · `features/platformadmin/`  
> Statut : 0 modification — rapport uniquement

---

## Ce qui existe pour chaque sous-domaine

---

### Tenant de base

#### TenantJpaEntity

**PRÉSENT** — table `tenant`

| Champ           | Type         | Notes                         |
| --------------- | ------------ | ----------------------------- |
| `id`            | UUID (PK)    | BaseEntity                    |
| `code`          | String(64)   | unique                        |
| `name`          | String       | —                             |
| `type`          | TenantType   | COMMERCIAL / PERSONAL         |
| `timezone`      | String(64)   | —                             |
| `currency`      | String(3)    | ISO 4217                      |
| `status`        | TenantStatus | ACTIVE / SUSPENDED / ARCHIVED |
| `addressId`     | UUID (FK)    | optionnel                     |
| `activeThemeId` | UUID (FK)    | optionnel                     |

#### TenantRegistryJpaEntity (catalog/tenant)

**PRÉSENT** — read-model sur même table `tenant`, avec `deletedAt`, `createdAt`, `updatedAt`.

#### Endpoints création/config tenant

**PRÉSENT** — `TenantAdminController` (core/tenantconfig/infra/web)

Commands câblées : `CreateTenantCommand`, `ActivateTenantCommand`, `DeactivateTenantCommand`, `SuspendTenantCommand`, `ArchiveTenantCommand`, `UpdateTenantIdentityCommand`

Queries : `GetTenantByIdQueryHandler`, `GetTenantByCodeQueryHandler`, `ListTenantsQueryHandler`

#### Branding/thème

**PRÉSENT** — `core/tenanttheme/` avec :

- `TenantTheme` domain model + `TenantThemeJpaEntity`
- Commands : `ApplyTenantThemeCommand`, `UpsertTenantThemePresetCommand`
- `TchContext` + `TenantJpaEntity.activeThemeId` : FK vers le thème actif

---

### Outlets

#### OutletEntity / champs

**PRÉSENT** (domain model `Outlet`) avec infra/persistence :

Champs : `id`, `tenantId`, `name`, `slug`, `dayClosed`, `salesBlocked`, `salesBlockReason`, `salesBlockedAt`, `timezone`, `businessDayCutoff`, `receiptPrintingEnabled`, `receiptHeaderMessage`, `receiptFooterMessage`, `requireOpeningFloat`, `addressId`

#### Endpoints CRUD outlets

**PRÉSENT** — `TenantAdminOutletsController` (`/admin/outlets`)

| Méthode | Path                         | Action          |
| ------- | ---------------------------- | --------------- |
| `GET`   | `/admin/outlets`             | Liste outlets   |
| `GET`   | `/admin/outlets/{id}`        | Détail outlet   |
| `POST`  | `/admin/outlets`             | Création outlet |
| `PATCH` | `/admin/outlets/{id}/config` | Config outlet   |

⚠️ Pas d'endpoint `DELETE /admin/outlets/{id}` — pas de suppression.
⚠️ Pas d'endpoint de fermeture/réouverture de journée via admin (existe via `CloseOutletDayCommandHandler` mais pas exposé ici).

---

### Terminals

#### TerminalJpaEntity / champs

**PRÉSENT** — `Terminal` domain model (core/pos/domain/model)

Champs : `id` (UUID), `tenantId`, `outletId`, `state` (TerminalState), `lastSeen`, `meta`, `version`, `registeredAt`, `unregisteredAt`, `lockedAt`, `lockedBy`, `lockReason`, `deletedAt`, `label`, `inventoryTag`

⚠️ `id` est typé `UUID` dans le domain model (et non `TerminalId`) — violation typed-ids.

#### Endpoints CRUD terminals

**PRÉSENT** — `TenantAdminTerminalsController` (`/api/v1/tenant-admin/terminals`)

| Méthode | Path                        | Action                 |
| ------- | --------------------------- | ---------------------- |
| `POST`  | `/terminals`                | Enregistrer terminal   |
| `POST`  | `/terminals/{id}/heartbeat` | Heartbeat              |
| `POST`  | `/terminals/{id}/lock`      | Verrouiller            |
| `POST`  | `/terminals/{id}/unlock`    | Déverrouiller          |
| `PUT`   | `/terminals/{id}/metadata`  | Mettre à jour metadata |

⚠️ Pas d'endpoint `DELETE` / unregister depuis l'admin.
⚠️ Pas d'endpoint `GET /terminals` ou `GET /terminals/{id}` dans le controller admin.

---

### Users / Vendeurs

#### TenantUserJpaEntity / champs

**PRÉSENT** — table `tenant_user`

Champs : `userId` (UUID), `roleId` (UUID), `status` (TenantUserStatus), `isOwner` (boolean), `outletId` (UUID), `terminalId` (UUID)

Contrainte unique : `(tenant_id, user_id)`.

#### VendorProfile ou équivalent

**ABSENT** comme entité dédiée — mais `TenantUserJpaEntity` porte `outletId` + `terminalId` (workplace assignment) qui remplissent ce rôle. Pas de VendorProfile séparé.

#### Provisioning Keycloak

**PARTIEL**

- L'orchestrateur `TenantAdminUsersOrchestrator.getBootstrap()` utilise `EnsureUserExistsForPrincipalCommand` pour créer l'utilisateur app à partir du `keycloakSub` (JWT sub).
- **Aucune création d'utilisateur Keycloak depuis le backend** : le backend ne provisionne pas dans Keycloak. L'utilisateur doit préexister dans Keycloak (créé manuellement ou via un flux externe).
- `TenantUserJpaEntity` ne stocke pas le `keycloakSub` directement — il est porté par `core/user` (`UserJpaEntity`).

#### Endpoints gestion users

**PARTIEL** — `TenantAdminUsersController`

| Méthode | Path                      | Statut                                 |
| ------- | ------------------------- | -------------------------------------- |
| `GET`   | `/admin}/users/bootstrap` | ✅ (bootstrap user courant)            |
| `GET`   | `/admin}/users`           | ✅ (list paginé)                       |
| —       | Create user               | ⚠️ Partiellement via orchestrator      |
| —       | Delete / unassign         | ⚠️ Via `UnassignUserFromTenantCommand` |

🔴 **Bug critique** : path mapping `"/admin}/users"` — le `}` dans le chemin est une **typo** qui brise le routing Spring. L'endpoint n'est probablement pas accessible.

---

### Tirages config

#### Config tirages par tenant

**PRÉSENT** via `core/tenantgame/`

- `TenantGameJpaEntity` : association tenant ↔ jeu (GameCode), avec policy JSON
- `EnableTenantGameCommand` / `DisableTenantGameCommand` : activation/désactivation par jeu
- `EnsureTenantGamesCommandHandler` : initialisation des jeux pour un nouveau tenant

**Pas de `DrawSchedule` entity dédiée** — la configuration des horaires est portée par `DrawChannelEntity` (catalog) qui définit `drawTime`, `cutoffSec`, `daysOfWeek`, `timezone` de manière globale.

#### DrawSchedule ou équivalent

**ABSENT** comme entité de configuration par tenant — la planification est globale (DrawChannel) et non surchargeable par tenant.

#### Horaires Miami/NY/TX/GA configurables

**PARTIEL**

- `DrawChannelEntity` (catalog) : `drawTime`, `timezone`, `daysOfWeek`, `cutoffSec` — configurable au niveau platform via `PlatformDrawChannelController`
- `ResultSlotCatalog` : catalogue des créneaux horaires par source
- **Pas de configuration horaire par tenant** : un tenant ne peut pas personaliser les horaires

---

### Limites et cotes

#### LimitDefinitionJpaEntity / champs

**PRÉSENT** — table `limit_definition`

| Champ       | Type           | Notes                                     |
| ----------- | -------------- | ----------------------------------------- |
| `ruleKey`   | RuleKey (enum) | Type de règle                             |
| `enabled`   | boolean        | Actif                                     |
| `onBreach`  | BreachOutcome  | BLOCK / WARN                              |
| `params`    | jsonb          | Paramètres de la règle (ex: maxStake=500) |
| `appliesTo` | jsonb          | Scope d'application                       |

#### LimitAssignmentJpaEntity / champs

**PRÉSENT** — table `limit_assignment`

| Champ               | Type       | Notes                              |
| ------------------- | ---------- | ---------------------------------- |
| `limitDefinitionId` | UUID (FK)  | Référence à la définition          |
| `targetType`        | TargetType | TENANT / OUTLET / TERMINAL / AGENT |
| `targetId`          | UUID       | ID cible                           |
| `enabled`           | boolean    | Actif                              |
| `startsAt`          | Instant    | Début validité                     |
| `endsAt`            | Instant    | Fin validité                       |

#### Cotes (odds) — PricingOddsEntity dans catalog

**PRÉSENT** — table `pricing_odds`

| Champ       | Type             | Notes                         |
| ----------- | ---------------- | ----------------------------- |
| `gameCode`  | String(32)       | Code jeu                      |
| `betType`   | BetType          | STRAIGHT / BOX / COMBO / etc. |
| `betOption` | Short            | Pour PATTERN bets (nullable)  |
| `odds`      | BigDecimal(12,4) | Cote                          |
| `active`    | boolean          | Actif                         |

Scoped par tenant via `BaseTenantEntity`.

⚠️ **Pas de cotes par tenant distinctes des cotes plateforme** : `PricingOddsEntity` étend `BaseTenantEntity` donc est per-tenant, mais le mécanisme de résolution (fallback tenant → plateforme) n'est pas évident dans le code audit.

#### Endpoints config limites

**PRÉSENT** — `TenantAdminPoliciesLimitsController` (`/admin/policies/limits`)

Via `features/tenantadmin/policies/web/` :

- `TenantAdminPoliciesLimitsController` : endpoints CRUD limites
- `TenantAdminPoliciesController` : vue d'ensemble des policies
- `TenantAdminPoliciesAutonomyController` : gestion de l'autonomie (approval rules)

---

## Ce qui manque pour débloquer Sales et Draw

| Gap                                                                                                                                                                                                            | Déblocage       | Estimation |
| -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------- | ---------- |
| **`GET /tenant/draws` manquant** : Le caissier doit pouvoir lister les tirages OPEN pour vendre. Dépend de Draw domain mais l'endpoint n'existe pas.                                                           | Sales / Draw    | **M**      |
| **Keycloak provisioning** : Créer un user vendeur depuis l'UI admin nécessite de le créer dans Keycloak manuellement. Pas d'API de provisioning backend. Bloque l'onboarding vendeur sans accès Keycloak admin | Sales (vendeur) | **L**      |
| **Bug routing `/admin}/users`** : Les endpoints de gestion users sont inaccessibles à cause de la typo dans le path. Bloque la gestion des vendeurs                                                            | Sales (vendeur) | **S**      |
| **DrawSchedule per tenant** : Pas de possibilité pour un tenant de désactiver un tirage spécifique. `TenantGame` gère les jeux, pas les tirages                                                                | Draw            | **M**      |
| **Endpoint terminal GET manquant** : Le caissier n'a pas d'endpoint pour résoudre son terminal (nécessaire pour SellTicketCommand.terminalId)                                                                  | Sales           | **S**      |
| **Odds fallback mécanisme** : Si un tenant n'a pas ses propres `PricingOdds`, le mécanisme de fallback vers les cotes globales n'est pas documenté/implémenté                                                  | Sales           | **M**      |

---

## Violations conventions

| Fichier                          | Violation                                                                                                                                  | Sévérité               |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------- |
| `TenantAdminUsersController`     | `@RequestMapping("/admin}/users")` — typo `}` dans le path → **endpoint inaccessible**                                                     | 🔴 BUG CRITIQUE        |
| `Terminal` (domain model)        | `id` typé `UUID` au lieu de `TerminalId`                                                                                                   | 🔴 VIOLATION typed-ids |
| `LimitPolicyRuntimeService`      | `@Autowired(required = false)` ×2 — field injection interdit                                                                               | 🔴 VIOLATION           |
| `TenantAdminTerminalsController` | Retourne `UUID` directement au lieu de `ApiResponse<UUID>` sur `POST /terminals`                                                           | ⚠️ VIOLATION           |
| `TenantAdminTerminalsController` | `void` retourné sur heartbeat au lieu de `ApiResponse<Void>`                                                                               | ⚠️ VIOLATION           |
| `TenantGameAdminController`      | `@PreAuthorize("hasPermission('tenantgame.read')")` — syntaxe de permission incohérente avec le reste (qui utilise `hasRole()`/`@Secured`) | ⚠️ MINEURE             |

---

## Tests existants

| Fichier                            | Type | Ce qu'il teste                  |
| ---------------------------------- | ---- | ------------------------------- |
| `TenantBootstrapLookupTest`        | Unit | Lookup bootstrap tenant         |
| `TenantCodeToContextInfoCacheTest` | Unit | Cache résolution code → context |

**Couverture globale** : ~5% — seulement le bootstrap et le cache. Aucun test CRUD outlets/terminals/users/limits.

---

## Dépendances entre sous-domaines

```
Tenant Config fournit à Sales :
  ├── TenantId (résolution tenant dans chaque request)
  ├── Terminal.outletId (terminal associé à l'outlet)
  ├── TenantUserJpaEntity.outletId + .terminalId (vendeur → terminal)
  ├── PricingOddsEntity (odds par jeu/betType pour TicketLine.oddsSnapshot)
  ├── LimitDefinition + LimitAssignment (règles de limite évaluées au moment de la vente)
  └── TenantGame (jeux actifs pour validation de GameCode dans SellTicketCommand)

Tenant Config fournit à Draw Flow :
  ├── DrawChannelEntity (horaires, timezone, cutoff, daysOfWeek)
  ├── TenantGame (activation/désactivation des jeux = canaux de tirage actifs)
  └── ResultSlotCatalog (mapping slotKey → timezone+drawTime pour fetch externe)

Dépendances internes à Tenant Config :
  ├── TenantConfig ← Outlet (outlet.tenantId FK)
  ├── Outlet ← Terminal (terminal.outletId FK)
  ├── Terminal ← TenantUser (tenantUser.terminalId FK)
  └── LimitAssignment ← (Tenant | Outlet | Terminal) selon targetType
```

**Ordre d'implémentation recommandé pour débloquer Sales + Draw :**

```
1. Corriger bug path TenantAdminUsersController  [S]
2. Exposer GET /terminals (résolution terminal caissier) [S]
3. Créer endpoint GET /tenant/draws [M]
4. Valider mécanisme odds fallback (tenant → platform) [M]
5. Documenter / implémenter Keycloak provisioning vendeur [L]
6. DrawSchedule per tenant (activer/désactiver tirages par tenant) [M]
```

---

## Estimation gaps : S / M / L par item manquant

| Gap                                                          | Taille |
| ------------------------------------------------------------ | ------ |
| Fix bug typo `"/admin}/users"` → `"/admin/users"`            | **S**  |
| Exposer `GET /admin/terminals` + `GET /admin/terminals/{id}` | **S**  |
| Endpoint `GET /tenant/draws` (tirages dispo pour vente)      | **M**  |
| Valider + documenter odds fallback tenant → platform         | **M**  |
| DrawSchedule per-tenant (activer/désactiver tirages)         | **M**  |
| Keycloak user provisioning (création user depuis admin)      | **L**  |
| Tests CRUD outlets / terminals / users / limits              | **L**  |
