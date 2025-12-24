# Domaine POS - Gestion des Terminaux (POS Devices)

## Vue d'ensemble

Le domaine `core.pos` gère les terminaux de point de vente (POS devices) pour le système Tchalanet. Il fournit des fonctionnalités de base pour l'enregistrement, la surveillance et la gestion des terminaux connectés.

## Aggregate Terminal

### Champs
- `id`: UUID (identifiant unique)
- `tenantId`: UUID (tenant propriétaire)
- `outletId`: UUID (point de vente associé)
- `state`: TerminalState (ACTIVE | INACTIVE | BLOCKED)
- `lastSeen`: Instant (dernier heartbeat)
- `meta`: String (métadonnées JSON)
- `version`: long (version pour optimistic locking)
- `registeredAt`: Instant (date d'enregistrement)
- `unregisteredAt`: Instant (date de désenregistrement)
- `lockedAt`: Instant (date de verrouillage)
- `lockedBy`: UUID (utilisateur qui a verrouillé)
- `lockReason`: String (raison du verrouillage)
- `deletedAt`: Instant (soft delete)

### Comportements
- `register(Instant now)`: Active le terminal et définit lastSeen
- `heartbeat(Instant now, String metaDelta)`: Met à jour lastSeen et fusionne les métadonnées
- `lock(UUID by, String reason, Instant now)`: Verrouille le terminal (retourne nouvelle instance)
- `unlock(UUID by, Instant now)`: Déverrouille le terminal (retourne nouvelle instance)
- `unregister(UUID by, Instant now)`: Désenregistre le terminal (soft delete, retourne nouvelle instance)
- `mergeMetadata(Map<String, Object> patch, Instant now)`: Fusionne les métadonnées (retourne nouvelle instance)

## Ports

### TerminalReaderPort (out)
- `Optional<Terminal> findById(UUID tenantId, UUID terminalId)`
- `List<Terminal> listByOutlet(UUID tenantId, UUID outletId, PageRequest pageRequest)`

### TerminalWriterPort (out)
- `Terminal save(Terminal terminal)`

## Commandes

### RegisterPosDeviceCommand
- Champs: tenantId, outletId, deviceId (optionnel), label, capabilities (Map)
- Retour: UUID (deviceId)

### SendPosHeartbeatCommand
- Champs: tenantId, deviceId, lastSeenAt, status, batteryPercent, appVersion, extras (Map)
- Retour: void

### LockTerminalCommand
- Champs: tenantId, terminalId, actorId, reason
- Retour: Terminal

### UnlockTerminalCommand
- Champs: tenantId, terminalId, actorId
- Retour: Terminal

### UnregisterTerminalCommand
- Champs: tenantId, terminalId, actorId, reason
- Retour: Terminal

### UpdateTerminalMetadataCommand
- Champs: tenantId, terminalId, actorId, metadataPatch (Map), heartbeatAlso (boolean)
- Retour: Terminal

## Gestionnaires de Commandes

- `RegisterPosDeviceCommandHandler`: Crée et enregistre un nouveau terminal
- `SendPosHeartbeatCommandHandler`: Met à jour le heartbeat et les métadonnées
- `LockPosDeviceCommandHandler`: Verrouille un terminal
- `UnlockPosDeviceCommandHandler`: Déverrouille un terminal
- `UnregisterPosDeviceCommandHandler`: Désenregistre un terminal (soft delete)
- `UpdatePosDeviceMetadataCommandHandler`: Met à jour les métadonnées

## Requêtes

### GetPosDeviceByIdQuery
- Champs: tenantId, deviceId
- Retour: Optional<Terminal>

### GetPosDeviceStatusQuery
- Champs: tenantId, deviceId
- Retour: Map<String, Object> (state, lastSeen)

### ListPosDevicesByLocationQuery
- Champs: tenantId, outletId
- Retour: List<Terminal>

## Gestionnaires de Requêtes

- `GetPosDeviceByIdQueryHandler`
- `GetPosDeviceStatusQueryHandler`
- `ListPosDevicesByLocationQueryHandler`

## Contrôleur Web

`TerminalController` avec endpoints :
- `POST /api/v1/tenants/{tenantId}/terminals` - Enregistrer un terminal
- `POST /api/v1/tenants/{tenantId}/terminals/{id}/heartbeat` - Envoyer heartbeat
- `POST /api/v1/tenants/{tenantId}/terminals/{id}/lock` - Verrouiller un terminal
- `POST /api/v1/tenants/{tenantId}/terminals/{id}/unlock` - Déverrouiller un terminal
- `PUT /api/v1/tenants/{tenantId}/terminals/{id}/metadata` - Mettre à jour les métadonnées
- `DELETE /api/v1/tenants/{tenantId}/terminals/{id}` - Désenregistrer un terminal
- `GET /api/v1/tenants/{tenantId}/terminals/{id}` - Obtenir un terminal
- `GET /api/v1/tenants/{tenantId}/terminals/{id}/status` - Obtenir le statut
- `GET /api/v1/tenants/{tenantId}/terminals/outlets/{outletId}` - Lister par outlet

## Infrastructure

### Entité JPA
`TerminalJpaEntity` extends `BaseTenantEntity` avec champs correspondants.

### Repository
`TerminalJpaRepository` avec méthodes de requête tenant-aware.

### Mapper
`TerminalMapper` pour conversion domaine/entité.

### Adaptateurs
- `JpaTerminalReaderAdapter`
- `JpaTerminalWriterAdapter`

## Migrations

- `V3__core_terminal.sql`: Création table terminal
- `V14__core_payout.sql`: Création table payout
- `V13__rls_policies.sql`: Politiques RLS (inclut terminal et payout)
- `V15__rls_payout.sql`: RLS spécifiques pour payout

## Sécurité

- RLS multi-tenant appliquées sur les tables terminal et payout
- Soft delete respecté dans les requêtes
- Scoping par tenantId sur toutes les opérations

## État V1

Le domaine est stabilisé pour V1 avec les fonctionnalités essentielles :
- Enregistrement et désenregistrement des terminaux
- Surveillance via heartbeat
- Gestion des états (verrouillage/déverrouillage)
- Mise à jour des métadonnées
- Requêtes de statut et listage

Les fonctionnalités avancées (comme les paramètres de configuration ou les notifications) sont reportées à des versions ultérieures.
