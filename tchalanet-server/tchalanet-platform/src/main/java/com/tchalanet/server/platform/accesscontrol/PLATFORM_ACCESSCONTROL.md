
# Platform Capability `platform.accesscontrol`

## Role


`platform.accesscontrol` owns authorization policy:

- Catalogue des rôles système et des permissions (lecture pour tenant admins, écriture ops only en V1)
- Affectation de rôle(s) système à un utilisateur dans un tenant (`tenant_user_role`)
- Overrides de permission par utilisateur : GRANT / DENY (`user_permission_override`)
- Résolution des **permissions effectives** et décision d'autorisation (API + annotations)
- Bootstrap de la matrice rôle→permissions par défaut

Il répond à : **cet acteur peut-il effectuer cette action dans ce contexte ?**

Ne valide pas l'état métier des ressources cibles (voir core).


## Public Surface

**API publique** — `platform/accesscontrol/api/AccessControlApi.java` :

| Groupe | Méthodes |
|---|---|
| Checks | `checkPermissions` (utilisé par `TchPermissionEvaluator`) |
| Lectures catalogue | `listRoles`, `listPermissions`, `listRolePermissions`, `getEffectivePermissions` |
| Affectation de rôle | `assignRoleToUser`, `removeRoleFromUser` (opèrent sur `tenant_user_role`) |
| Overrides utilisateur | `grantUserPermission`, `denyUserPermission`, `removeUserPermissionOverride` |
| Bootstrap | `bootstrap` |
| Catalogue rôles (ops only) | `createRole`, `updateRole`, `grantPermission`, `revokePermission` |
| Déprécié | `setTenantUserRole` → utiliser `assignRoleToUser` |

**Constantes** — `platform/accesscontrol/api/PermissionKeys.java` : la source unique des codes
de permission (`user.role.assign`, `user.permission.manage`, `platform.ops.execute`,
`seller_terminal.manage`, `seller_terminal.block`, `seller_terminal.pin.reset`…).
Les `@PreAuthorize("hasPermission('…')")` doivent référencer ces clés, jamais des littéraux ad hoc.

**Intégration Spring Security** :
- `@RequiresPermission` — annotation déclarative
- `TchPermissionEvaluator` — `hasPermission('code')` dans `@PreAuthorize`

**Endpoints admin** (REST, `/api/v1/admin/access-control`, gate `TENANT_ADMIN`/`SUPER_ADMIN`) :

| Méthode | Route | Permission |
|---|---|---|
| GET | `/roles` | `role.read` |
| GET | `/permissions` | `permission.read` |
| GET | `/roles/{roleId}/permissions` | `role.read` |
| GET | `/users/{userId}/permissions/effective` | `user.read` |
| POST/DELETE | `/users/{userId}/roles/{roleCode}` | `user.role.assign` |
| PUT | `/users/{userId}/permissions/{permissionCode}/grant` | `user.permission.manage` |
| PUT | `/users/{userId}/permissions/{permissionCode}/deny` | `user.permission.manage` |
| DELETE | `/users/{userId}/permissions/{permissionCode}/override` | `user.permission.manage` |
| POST | `/bootstrap/{mode}` | `SUPER_ADMIN` + `platform.ops.execute` |

**Important** :
- Les consumers ne doivent jamais importer `platform.accesscontrol.internal.*`.


## Permissions effectives

```text
effective = permissions des rôles actifs (tenant_user_role)
            + overrides GRANT de l'utilisateur
            − overrides DENY de l'utilisateur
```

**DENY l'emporte toujours** sur les grants de rôle et les GRANT explicites
(`EffectivePermissionService`). Les permissions ne sont jamais copiées dans la ligne utilisateur :
elles se recalculent depuis rôle + overrides.


## Autorités et rôles applicatifs (invariant cross-module)

Firebase authentifie l'identité externe pour TOUS les acteurs (APP_USER et SELLER_TERMINAL).
Les rôles, autorités, permissions fines et overrides sont résolus depuis Tchalanet.
Aucun rôle n'est miroité vers Firebase.

**Les `SELLER_TERMINAL` n'ont pas de rôles** (`roleCodes = {}`). Leur autorisation repose sur :
- l'authority `ACTOR_SELLER_TERMINAL` (autorisation coarse-grained)
- les permissions directement accordées à l'entité (ex: `ticket.sell`)
- le statut (`ACTIVE`) et le flag `mustChangePin = false`

`platform.accesscontrol` gère les rôles et permissions des `APP_USER` uniquement.
La validation d'un `SELLER_TERMINAL` se fait via `core.sellerterminal`.


## Deny-Safe Evaluation

L'évaluation des permissions est **deny-by-default** si des faits de sécurité sont manquants ou ambigus.

Faits requis pour les checks tenant-scoped :
- acteur authentifié
- tenant effectif (contexte canonique)
- permission demandée
- faits de rôle/membership issus de l'état platform

Ne jamais faire confiance aux tenantId issus du payload HTTP comme source d'autorité.


## Ce que AccessControl ne fait pas

Ne valide pas :
- l'état métier (payout, ticket, sellerterminal, etc.)
- les eligibility business (offline, limits, draw, etc.)
- le **contexte opérationnel** — pour un `SELLER_TERMINAL`, le contexte est intrinsèque
  (acteur = contexte) ; pour un `APP_USER` en Admin POS, il faut une sélection explicite.
  Il n'existe pas de permission `operational-context.valid`.
  Voir : `docs/conventions/context/operational-context.md`.

Ces checks relèvent des validateurs/handlers du domaine core.


## Intégration

- Contrôleurs HTTP :
	- `@PreAuthorize("hasPermission('permission.code')")` (Spring Security)
	- `@RequiresPermission` (annotation custom)
- Flows non-HTTP : appel direct à `AccessControlApi`
- `platform.identity` (provisioning) appelle `assignRoleToUser` lors de la création d'un utilisateur
- Endpoints d'écriture (rôles, permissions, overrides) audités via `platform.audit`


## Persistence

- Tables détenues par `platform.accesscontrol` : `app_role`, `permission`, `app_role_permission`,
  `tenant_user_role`, `user_permission_override`
- Rows tenant-scoped compatibles RLS
- Les queries ne doivent jamais utiliser un tenantId client comme source d'isolation


## Guardrails

- Platform ne doit pas dépendre de core/features
- Aucune importation de packages métier core/features
- Deny systématique si faits manquants (actor, tenant, permission)
- Endpoints d'écriture audités
- Les invariants métier restent dans core
- Les codes de permission passent par `PermissionKeys`, jamais en dur

## Limitations connues

- Rôles custom par tenant : non supportés en V1 (roadmap 2027) — l'écriture du catalogue de rôles
  (`createRole`/`updateRole`/grant/revoke) est **ops only**, les tenant admins sont read-only.
- `setTenantUserRole` est conservé déprécié pour compat ; toute nouvelle intégration utilise
  `assignRoleToUser`.
