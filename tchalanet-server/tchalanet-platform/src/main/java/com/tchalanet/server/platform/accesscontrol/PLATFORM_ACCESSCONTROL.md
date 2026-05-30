
# Platform Capability `platform.accesscontrol`

## Role


`platform.accesscontrol` owns authorization policy:

- Gestion des rôles (création, mise à jour, hiérarchie, multi-tenant)
- Permissions (catalogue, affectation aux rôles)
- Affectation mono-rôle par utilisateur/tenant
- Résolution des permissions effectives
- Décision d’autorisation (API et annotations)

Il répond à : **cet acteur peut-il effectuer cette action dans ce contexte ?**

Ne valide pas l’état métier des ressources cibles (voir core).


## Public Surface

**API publique** :

- `platform/accesscontrol/api/AccessControlApi.java` — gestion des rôles, permissions, affectations, vérification des droits
- `@RequiresPermission` — annotation pour vérification déclarative
- `TchPermissionEvaluator` — intégration Spring Security

**Endpoints admin** (REST, internes) :

- `/admin/roles` — CRUD rôles (multi-tenant)
- `/admin/permissions` — catalogue des permissions
- `/admin/roles/{roleId}/permissions` — gestion des permissions d’un rôle

**Important** :
- Les consumers ne doivent jamais importer `platform.accesscontrol.internal.*`.


## Deny-Safe Evaluation

L’évaluation des permissions est **deny-by-default** si des faits de sécurité sont manquants ou ambigus.

Faits requis pour les checks tenant-scoped :
- acteur authentifié
- tenant effectif (contexte canonique)
- permission demandée
- faits de rôle/membership issus de l’état platform

Ne jamais faire confiance aux tenantId issus du payload HTTP comme source d’autorité.


## Ce que AccessControl ne fait pas

Ne valide pas :
- l’état métier (payout, ticket, terminal, etc.)
- les eligibility business (offline, limits, draw, etc.)

Ces checks relèvent des validateurs/handlers du domaine core.


## Intégration

- Contrôleurs HTTP :
	- `@PreAuthorize("hasPermission('permission:code')")` (Spring Security)
	- `@RequiresPermission` (annotation custom)
- Flows non-HTTP : appel direct à `AccessControlApi`
- Endpoints d’écriture (rôles, permissions) audités via `platform.audit`


## Persistence

- Tables rôles, permissions, affectations détenues par `platform.accesscontrol`
- Rows tenant-scoped compatibles RLS
- Les queries ne doivent jamais utiliser un tenantId client comme source d’isolation


## Guardrails

- Platform ne doit pas dépendre de core/features
- Aucune importation de packages métier core/features
- Deny systématique si faits manquants (actor, tenant, permission)
- Endpoints d’écriture audités
- Les invariants métier restent dans core

## Limitations connues

- La méthode `setTenantUserRole` n’est pas encore implémentée (UnsupportedOperationException)
- L’affectation mono-rôle par utilisateur/tenant est prévue, mais nécessite wiring avec platform.identity
