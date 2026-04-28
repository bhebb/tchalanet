## Context

### Règle Feature vs Core (non-négociable)

```
Feature = BFF orchestrant ≥ 2 cores pour un payload composite
CRUD mono-domaine = controller dans core/<bc>/infra/web/<scope>/
```

Les 4 slices à migrer sont des wrappers sans valeur autour du `CommandBus`/`QueryBus`. L'unique dépendance cross-domaine est `policies` (limitpolicy + autonomy) et `users` (user + tenantuser + accesscontrol), qui ne justifient pas un BFF car il n'y a pas de payload composite — c'est du séquençage de commandes déclenché par une intention unique.

### État actuel des packages cibles

| Slice             | Package core cible                 | Existe déjà ?                      |
| ----------------- | ---------------------------------- | ---------------------------------- |
| outlets           | `core.outlet.infra.web.admin`      | ✅ (avec `OutletReportController`) |
| terminals         | `core.pos.infra.web.admin`         | ❌ à créer                         |
| policies/limits   | `core.limitpolicy.infra.web.admin` | ❌ à créer                         |
| policies/autonomy | `core.autonomy.infra.web.admin`    | ❌ à créer                         |
| users             | `core.tenantuser.infra.web.admin`  | ✅ (stub partiel à enrichir)       |

### Violations à corriger lors de la migration

| Classe source                    | Violation                                                             |
| -------------------------------- | --------------------------------------------------------------------- |
| `TenantAdminTerminalsController` | Path `${tch.web.paths.tenant_admin}` au lieu de `/admin/terminal`     |
| `TenantAdminTerminalsController` | `registerDevice` retourne `UUID` brut (pas `ApiResponse<TerminalId>`) |
| `TenantAdminTerminalsController` | `sendHeartbeat` retourne `void` (pas `ApiResponse<Void>`)             |
| `TenantAdminTerminalsController` | `LockRequest.actorId` : `UUID` brut → `UserId`                        |
| `TenantAdminUsersController`     | `@RequestMapping("/admin}/users")` — `}` parasite                     |
| `TenantAdminUsersOrchestrator`   | `createUserAndAssign` séquence 3 cores inline sans handler            |
| Tous les controllers             | `hasAnyRole(...)` → `hasAnyAuthority(...)` pour cohérence projet      |

## Goals / Non-Goals

**Goals:**

- Les 4 controllers migrent dans leurs cores respectifs, dispatching direct via bus
- Les orchestrateurs mono-wrapper sont supprimés
- Les violations de typage, path et retour sont corrigées au passage
- L'overview policies (multi-domaine) reste dans `features/tenantadmin/`
- Build vert, aucun import cassé

**Non-Goals:**

- Modifier la logique métier des handlers core
- Créer des migrations Flyway (base recréée from scratch)
- Traiter `config/settings` et `config/i18n` (spec séparée)
- Implémenter `@AuditLog` sur les endpoints qui n'en ont pas encore (à ajouter mais hors du périmètre principal)

## Decisions

### D1 — Policies : split limitpolicy + autonomy, overview reste feature

L'orchestrateur `TenantAdminPoliciesOrchestrator.getPoliciesOverview()` agrège `core.limitpolicy` + `core.autonomy` → payload composite → feature légitime. Les endpoints CRUD limits et autonomy sont chacun mono-domaine.

**Décision** : créer `LimitPolicyAdminController` dans `core.limitpolicy.infra.web.admin` et `AutonomyAdminController` dans `core.autonomy.infra.web.admin`. L'overview policies est absorbé dans `TenantAdminConfigOverviewController` (déjà feature légitime) ou gardé dans un `TenantAdminPoliciesController` minimal sous `features/tenantadmin/`.

### D2 — Users : controller multi-dispatch direct dans core.tenantuser

La séquence `createUserAndAssign` (CreateUserCommand → AssignUserToTenantCommand → SetTenantUserRoleCommand) représente une **intention unique** (créer un tenant-user) déclinée en 3 commandes. Il n'y a pas de payload composite en sortie — c'est du séquençage transactionnel.

**Décision** : le controller `TenantUserAdminController` dispatch directement les 3 commands via `commandBus`. Pas d'orchestrateur intermédiaire. La logique de séquençage reste minimale (3 lignes `commandBus.send(...)`) et justifiée par la sémantique HTTP unique.

Si la séquence doit devenir atomique → créer une `CreateAndAssignTenantUserCommand` dans `core.tenantuser` gérant les 3 steps via une saga ou un aggregate handler (décision post-migration).

### D3 — `hasAnyRole` → `hasAnyAuthority`

Keycloak émet des authorities (pas des roles Spring Security au sens strict). Le projet utilise `hasAuthority(...)` dans les controllers core existants. Les controllers migrés doivent être alignés.

**Décision** : remplacer `@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")` par `@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")` dans tous les controllers migrés.

### D4 — DTOs feature vs modèles core

Les DTOs feature (`OutletResponse`, `TerminalResponse`, etc.) sont des modèles web qui n'appartiennent pas au domaine core. Ils migrent dans `core/<bc>/infra/web/admin/model/`.

**Exception** : les DTOs qui utilisent des types internes feature (`TenantUserDetails`, `PoliciesOverviewView`) sont soit remplacés par des types core existants, soit maintenus dans le package web admin du core cible.

## Risks / Trade-offs

- **[Risque] Imports cassés** : les classes des orchestrateurs et controllers features peuvent être importées dans des tests. → Mitigation : `grep -r "features.tenantadmin.<slice>"` avant suppression. **Vérification systématique** avant chaque `git rm`.
- **[Risque] Path terminals** : le fix `${tch.web.paths.tenant_admin}` → `/admin/terminals` change le path effectif si la propriété est customisée en env. → Vérifier `application.yaml` de tous les profils.
- **[Risque] Users multi-dispatch** : la séquence Create+Assign+SetRole n'est pas atomique si le 2e ou 3e command échoue. → Issue connue, à documenter. Dans la scope de ce change : conserver le comportement actuel (pas d'atomicité), créer un TODO pour la saga.
- **[Trade-off] Policies overview** : garder une mini-feature `TenantAdminPoliciesController` pour l'overview introduit un résidu dans features/. → Acceptable : 1 endpoint composite justifie légitimement une feature.

## Migration Plan

1. **Slice outlets** : créer `OutletAdminController` dans `core.outlet.infra.web.admin` ; déplacer DTOs ; supprimer feature slice ; vérifier imports ; tester.
2. **Slice terminals** : créer `core.pos.infra.web.admin` ; créer `TerminalAdminController` ; corriger violations (path, types, ApiResponse) ; supprimer feature slice.
3. **Slice policies** : créer `LimitPolicyAdminController` dans `core.limitpolicy.infra.web.admin` (limits) ; créer `AutonomyAdminController` dans `core.autonomy.infra.web.admin` (autonomy) ; réduire `features/tenantadmin/policies/` au seul `TenantAdminPoliciesController` (overview) ; supprimer les classes supprimées.
4. **Slice users** : enrichir `TenantUserAdminController` dans `core.tenantuser.infra.web.admin` ; fix path `}` ; remplacer `hasAnyRole` ; inliner les 3 commands create+assign+role ; déplacer DTOs et mapper ; supprimer feature slice.
5. **Docs** : mettre à jour `FEATURE_TENANT_ADMIN.md` et `ARCHITECTURE.md`.
6. **Build** : `./mvnw clean verify` → vert.
