## ADDED Requirements

### Requirement: Mise à jour de l'identité tenant

Le système SHALL permettre à un `TENANT_ADMIN` ou `SUPER_ADMIN` de modifier les champs d'identité
d'un tenant (name, timezone, currency) via `PUT /config/identity`.

Le handler `UpdateTenantIdentityCommandHandler` SHALL :

1. Charger le tenant via `TenantCatalog.findRegistryById(cmd.tenantId())` — lever `ProblemRest.notFound` si absent.
2. Reconstruire `TenantConfig.fromRegistryView(registry)`.
3. Appliquer les modifications selon les champs non-null de la command :
   - `cmd.name() != null` → `tenant.rename(cmd.name(), now)`
   - `cmd.timezone() != null` → `tenant.updateLocale(cmd.timezone(), currency, now)` (ou rename seul si currency null)
   - `cmd.currency() != null` → appliquer avec la timezone existante
4. Persister via `writer.update(tenant)`.
5. Publier `TenantIdentityUpdatedEvent` after-commit.

La command `UpdateTenantIdentityCommand` MUST contenir : `tenantId` (non-null), et au moins l'un
des champs `name`, `timezone`, `currency` non-null pour que la mise à jour soit significative.

#### Scenario: Mise à jour du nom seul

- **WHEN** `PUT /config/identity` avec `{"name": "Nouveau Nom"}`
- **THEN** le tenant est renommé, `timezone` et `currency` inchangés
- **AND** `TenantIdentityUpdatedEvent` publié after-commit

#### Scenario: Mise à jour de la locale seule

- **WHEN** `PUT /config/identity` avec `{"timezone": "America/Port-au-Prince", "currency": "HTG"}`
- **THEN** la locale est mise à jour via `updateLocale(zone, currency, now)`
- **AND** `TenantIdentityUpdatedEvent` publié after-commit

#### Scenario: Mise à jour complète

- **WHEN** `PUT /config/identity` avec name + timezone + currency
- **THEN** les trois champs sont mis à jour
- **AND** `TenantIdentityUpdatedEvent` publié after-commit

#### Scenario: Timezone invalide → 400

- **WHEN** `PUT /config/identity` avec `{"timezone": "INVALID_ZONE"}`
- **THEN** la requête échoue avec HTTP 400
- **AND** le message d'erreur indique la timezone invalide

#### Scenario: Tenant introuvable → 404

- **WHEN** `PUT /config/identity` pour un tenantId inexistant
- **THEN** la requête échoue avec HTTP 404

#### Scenario: GET /config/identity via QueryBus

- **WHEN** `GET /config/identity` est appelé
- **THEN** le controller dispatch `GetTenantByIdQuery` via `QueryBus`
- **AND** ne fait aucun appel direct à `TenantCatalog`

### Requirement: Controller thin pour l'identité tenant

`TenantAdminIdentityController` SHALL respecter la règle "controller thin" :

- Aucune inject directe de `TenantCatalog`
- Aucun parsing de `ZoneId` ou `Currency` dans le controller (délégué au handler ou à la désérialisation)
- Aucun guard `if (tenantId == null)` — le contexte `@CurrentContext` garantit la valeur
- Chaque méthode SHALL contenir ≤ 3 appels de bus (dispatch + query optionnel + return)

#### Scenario: PUT /config/identity — controller thin

- **WHEN** `PUT /config/identity` est traité
- **THEN** le controller dispatch `UpdateTenantIdentityCommand` via `CommandBus`
- **AND** query via `QueryBus.send(GetTenantByIdQuery)` pour construire la réponse
- **AND** aucune logique de validation dans le controller

### Requirement: Suppression de `deactivate`

Le endpoint `POST /platform/tenants/{id}/deactivate` SHALL être supprimé.
`DeactivateTenantCommand` et `DeactivateTenantCommandHandler` SHALL être supprimés.
`suspend` est la seule transition vers `SUSPENDED`.

Aucun statut `DEACTIVATED` ne SHALL être ajouté à `TenantStatus`.

#### Scenario: Endpoint deactivate supprimé

- **WHEN** `POST /platform/tenants/{id}/deactivate` est appelé
- **THEN** la réponse est HTTP 404 (endpoint inexistant)

### Requirement: IdGenerator dans les handlers lifecycle

Les handlers `ActivateTenantCommandHandler`, `SuspendTenantCommandHandler`, `ArchiveTenantCommandHandler`
SHALL utiliser `IdGenerator.newUuid()` pour générer les `EventId`.

`UUID.randomUUID()` direct ne SHALL PAS être utilisé dans ces handlers.

#### Scenario: EventId généré via IdGenerator

- **WHEN** un handler lifecycle traite une commande
- **THEN** l'`EventId` de l'event publié est généré via `idGenerator.newUuid()`

### Requirement: Mapper sans Instant.now() hardcodé

`TenantMapper.toEntity()` SHALL NOT contenir d'expressions `java(java.time.Instant.now())`
pour `createdAt` ou `updatedAt`.

`@CreatedDate` et `@LastModifiedDate` de Spring Data Auditing SHALL gérer l'initialisation.

#### Scenario: Création tenant — audit dates gérées par JPA Auditing

- **WHEN** un nouveau tenant est créé via `writer.create(tenant)`
- **THEN** `createdAt` est fixé par `@PrePersist` via Spring Data Auditing
- **AND** `updatedAt` est fixé par `@PreUpdate` via Spring Data Auditing
- **AND** le mapper ne surécrit pas ces valeurs

### Requirement: Logique address et activate dans CreateTenantCommandHandler

`CreateTenantCommandHandler` SHALL implémenter les champs `address` et `activate` de
`CreateTenantCommand` :

- Si `cmd.address() != null` → créer l'adresse via `AddressWriterPort` et passer l'`AddressId` au tenant
- Si `cmd.activate() == Boolean.TRUE` → appeler `tenant.activate(now)` après création en DRAFT

#### Scenario: Création avec address

- **WHEN** `CreateTenantCommand` inclut un `AddressInput` non-null
- **THEN** l'adresse est créée et l'`AddressId` est lié au tenant

#### Scenario: Création avec activate=true

- **WHEN** `CreateTenantCommand` inclut `activate=true`
- **THEN** le tenant est créé directement en statut `ACTIVE`

#### Scenario: Création sans address ni activate

- **WHEN** `CreateTenantCommand` sans `address` et sans `activate`
- **THEN** le tenant est créé en statut `DRAFT` sans adresse

### Requirement: Suppression de TenantCreatedEvent

`core/tenantconfig/domain/event/TenantCreatedEvent.java` SHALL être supprimé car il ne répond
à aucun consommateur et ne MAY PAS être confondu avec `TenantStatusChangedEvent(reason="tenant_created")`.

#### Scenario: Aucun import de TenantCreatedEvent

- **WHEN** le build est lancé
- **THEN** aucun fichier Java n'importe `TenantCreatedEvent`
