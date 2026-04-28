## Why

`core.tenantconfig` et `features.tenantadmin.config` présentent plusieurs anomalies détectées à
l'audit (`audit-core-tenantconfig-2026-04-27.md`) : un handler vide (`UpdateTenantIdentityCommandHandler`),
un controller fat qui parse lui-même ZoneId/Currency, un duplicat fonctionnel entre `deactivate` et
`suspend` (résolus en supprimant deactivate), un mapper qui écrase les dates gérées par `@CreatedDate`,
et du code mort (`TenantCreatedEvent`). Ces corrections sont bloquantes pour la livraison de
`features/tenantadmin/config/identity` et empêchent le build de valider correctement.

## What Changes

### P0-1 — Implémenter `UpdateTenantIdentityCommandHandler`

- Handler vide → implémenter la logique complète :
  1. Charger via `TenantCatalog.findRegistryById` → `ProblemRest.notFound` si absent
  2. Reconstruire `TenantConfig.fromRegistryView(registry)`
  3. Appliquer selon champs non-null de la command :
     - `cmd.name() != null` → `tenant.rename(cmd.name(), now)`
     - `cmd.timezone() != null` → `ZoneId.of(cmd.timezone().toString())` (lever `ProblemRest.badRequest` si invalide)
     - `cmd.currency() != null` → `Currency.getInstance(cmd.currency().getCurrencyCode())`
     - si timezone + currency → `tenant.updateLocale(zone, currency, now)`
  4. `writer.update(tenant)`
  5. `AfterCommit.run(() -> publisher.publish(TenantIdentityUpdatedEvent(...)))`
- Retourne `void` (`VoidCommandHandler`) — le controller re-query après commit

### P0-2 — Alléger `TenantAdminIdentityController`

- Supprimer injection directe de `TenantCatalog` → remplacer par `QueryBus.send(GetTenantByIdQuery)`
- Supprimer guard `if (tenantId == null)` (garanti par contexte `@CurrentContext`)
- Supprimer parsing `ZoneId`/`Currency` dans le controller → déléguer au handler
- `PUT /config/identity` : dispatch `UpdateTenantIdentityCommand` puis query pour réponse
- `GET /config/identity` : `QueryBus.send(GetTenantByIdQuery(tenantId))` → mapper vers `TenantIdentityView`

### P0-3 — Supprimer `deactivate` (fusion avec `suspend`)

- **DÉCISION** : `deactivate` et `suspend` sont fonctionnellement identiques → supprimer `deactivate`
- Supprimer `DeactivateTenantCommand.java`
- Supprimer `DeactivateTenantCommandHandler.java`
- Supprimer `POST /platform/tenants/{id}/deactivate` de `TenantAdminController`
- Aucun `TenantStatus.DEACTIVATED` ajouté — le statut `SUSPENDED` couvre ce cas

### P1-1 — Injecter `IdGenerator` dans les 4 handlers lifecycle

- `ActivateTenantCommandHandler`, `SuspendTenantCommandHandler`, `ArchiveTenantCommandHandler`
  (DeactivateTenantCommandHandler supprimé en P0-3)
- Remplacer `EventId.of(UUID.randomUUID())` par `EventId.of(idGenerator.newUuid())`

### P1-2 — Implémenter `address` et `activate` dans `CreateTenantCommandHandler`

- Champ `address` de `CreateTenantCommand` ignoré → implémenter la création d'adresse si non-null
  (via un port `AddressWriterPort` ou en passant `AddressInput` au writer)
- Champ `activate` ignoré → si `true`, appeler `tenant.activate(now)` après création DRAFT
- L'implémentation reste dans le handler, pas dans la command

### P1-3 — Corriger `TenantMapper.toEntity()`

- Supprimer `@Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")`
- Supprimer `@Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")`
- Laisser `@CreatedDate` / `@LastModifiedDate` JPA Auditing gérer l'initialisation

### P1-4 — Créer `TenantIdentityUpdatedEvent`

- Nouveau domain event dans `core/tenantconfig/domain/event/`
- Déclenché après commit par `UpdateTenantIdentityCommandHandler`
- Consommateurs attendus : aucun pour l'instant (placeholder pour invalidation cache futur)

### P1-5 — Supprimer `TenantCreatedEvent` (code mort)

- Supprimer `core/tenantconfig/domain/event/TenantCreatedEvent.java`
- Vérifier qu'aucun import ne le référence avant suppression

## Capabilities

### New Capabilities

- `tenant-identity-update`: Handler `UpdateTenantIdentityCommandHandler` opérationnel + endpoint
  `PUT /config/identity` fonctionnel avec validation ZoneId/Currency dans le handler
- `tenant-identity-updated-event`: Nouveau domain event `TenantIdentityUpdatedEvent` dans
  `core.tenantconfig.domain.event`

### Modified Capabilities

_(aucune — corrections d'implémentation sans changement de contrat API public)_

## Impact

**Java sources** :

- `core/tenantconfig/application/command/handler/UpdateTenantIdentityCommandHandler.java` — à implémenter
- `core/tenantconfig/application/command/handler/ActivateTenantCommandHandler.java` — IdGenerator
- `core/tenantconfig/application/command/handler/SuspendTenantCommandHandler.java` — IdGenerator
- `core/tenantconfig/application/command/handler/ArchiveTenantCommandHandler.java` — IdGenerator
- `core/tenantconfig/application/command/handler/CreateTenantCommandHandler.java` — address + activate
- `core/tenantconfig/infra/persistence/mapper/TenantMapper.java` — supprimer expressions Instant.now()
- `core/tenantconfig/infra/web/TenantAdminController.java` — supprimer endpoint `/deactivate`
- `core/tenantconfig/domain/event/TenantIdentityUpdatedEvent.java` — NOUVEAU
- `core/tenantconfig/domain/event/TenantCreatedEvent.java` — SUPPRIMER
- `core/tenantconfig/application/command/model/DeactivateTenantCommand.java` — SUPPRIMER
- `core/tenantconfig/application/command/handler/DeactivateTenantCommandHandler.java` — SUPPRIMER
- `features/tenantadmin/config/identity/TenantAdminIdentityController.java` — alléger

**Tests** : nouveaux tests unitaires `UpdateTenantIdentityCommandHandlerTest` avec scénarios :
name seul, locale seule, les deux, timezone invalide → 400, tenant not found → 404

**SQL / Flyway** : aucune migration requise

**API** : `POST /platform/tenants/{id}/deactivate` supprimé — **BREAKING** pour les clients qui
utilisent cet endpoint (mais non déployé en production — safe)
