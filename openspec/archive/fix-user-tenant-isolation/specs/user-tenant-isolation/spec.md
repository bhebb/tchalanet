## ADDED Requirements

### Requirement: findByTenantId n'utilise pas de filtre SQL applicatif

L'isolation tenant de `AppUserPersistenceAdapter.findByTenantId()` SHALL être assurée exclusivement par le RLS PostgreSQL via `app.current_tenant`, conformément à la règle absolue du projet (❌ jamais `WHERE tenant_id = ?` côté Java). Le paramètre `TenantId` de l'interface est informatif et ne génère PAS de prédicat SQL supplémentaire.

#### Scenario: TENANT_ADMIN — lecture isolée par RLS

- **WHEN** un TENANT_ADMIN appelle l'endpoint de liste des users avec son tenant positionné dans `TchContext`
- **THEN** `RlsAwareDataSource` pose `set_config('app.current_tenant', '<tenant-uuid>')` et la policy RLS filtre automatiquement les résultats au seul tenant courant

#### Scenario: Aucun WHERE tenant_id dans le code Java

- **WHEN** le code de `AppUserPersistenceAdapter` est inspecté
- **THEN** aucune clause `WHERE tenant_id = ?` ou prédicat Criteria `tenantId = ...` n'est ajoutée dans `findByTenantId`, `findAllActiveUsersByTenant` ou `searchByCriteria`

### Requirement: SUPER_ADMIN exécute findByTenantId dans le contexte tenant cible

Lorsqu'un SUPER_ADMIN demande les users d'un tenant spécifique, le call site SHALL positionner `TchContext` avec le `tenantUuid` du tenant cible avant l'appel à l'adapter. Cela garantit que la policy RLS filtre sur ce tenant, y compris quand `allow_platform_cross_tenant_select()` est actif.

#### Scenario: Liste des users d'un tenant par un SUPER_ADMIN

- **WHEN** un SUPER_ADMIN appelle l'endpoint `/admin/users` avec un `tenantId` cible
- **THEN** `TchContext` contient le `tenantUuid` du tenant cible avant la requête de liste
- **THEN** seuls les users de ce tenant sont retournés (pas de fuite cross-tenant)

#### Scenario: Absence de contexte tenant — aucune donnée exposée

- **WHEN** `findByTenantId` est appelée sans `TchContext` positionné (`app.current_tenant = ''`)
- **THEN** la policy RLS retourne 0 lignes (current_tenant() IS NULL → prédicat false)

### Requirement: Tests d'intégration prouvent l'isolation RLS

Des tests d'intégration Testcontainers exécutant de vraies requêtes PostgreSQL SHALL vérifier que l'isolation RLS entre tenants est effective. Ces tests se connectent avec le rôle DB `app_user` et positionnent `app.current_tenant` via `set_config`.

#### Scenario: Isolation entre deux tenants en base réelle

- **WHEN** la base contient des users pour tenant-A et tenant-B
- **AND** `set_config('app.current_tenant', '<tenant-A-uuid>')` est positionné
- **THEN** la requête retourne uniquement les users du tenant-A, aucun du tenant-B

#### Scenario: RLS bloque les requêtes sans contexte tenant

- **WHEN** `set_config('app.current_tenant', '')` est positionné (aucun tenant)
- **THEN** la requête retourne 0 lignes (même si des users existent en base)
