# Change: Harden audit system

## Why

Le système d'audit Tchalanet doit être clarifié et durci avant d'étendre les logs applicatifs et Envers.

Décisions déjà prises :

- L'audit applicatif sert à tracer les **actions métier sensibles**.
- Envers sert à tracer l'**historique technique des lignes DB**.
- Les deux mécanismes sont complémentaires et ne doivent pas être mélangés.
- Pour l'audit applicatif :
  - success = audit après commit ;
  - error = audit immédiat en transaction indépendante ;
  - l'audit ne doit jamais casser le flux principal.
- Le chemin canonique doit passer par `CommandBus`, jamais par appel direct au handler.
- `entityId` doit être un identifiant stable de type `String`, pas forcément un UUID.
- La purge des logs applicatifs doit fonctionner et être testée.
- Le contexte doit venir de `TchContext` / `TchContextResolver`, pas de `RequestContextHolder`.

Problèmes observés :

- Deux chemins concurrents d'audit applicatif existent :
  - `LogAuditEventCommand`
  - `RecordAuditEventCommand`
- `AuditedForceCommandAspect` est mal placé :
  - il vit dans `common`,
  - il dépend de `core.audit`,
  - il intercepte globalement `CommandBus.send(..)`,
  - il ne sait pas si la commande réussit,
  - il risque la récursion.
- `AuditLogAspect` contient un risque critique : `return null` dans le `finally`.
- `AuditEventRepositoryAdapter.toEntity()` ne renseigne pas explicitement `occurredAt`.
- `RecordAuditEventCommandHandler` parse `entityId` en UUID, ce qui viole la règle `entityId = String stable`.
- `PurgeOldAuditEventsCommandHandler` utilise `Instant.now()` au lieu d'un `Clock`.
- `TenantEntityListener` lit `RequestContextHolder` au lieu du contexte canonique.
- Envers est présent mais doit être cadré :
  - `revinfo` enrichi ;
  - choix explicite des entités `@Audited` ;
  - migrations `_AUD` maintenues ;
  - validation Flyway + `ddl-auto=validate`.

## What

Ce change durcit l'audit en deux volets.

### 1. Audit applicatif

- Standardiser le flux :
  - `@AuditLog`
  - `AuditLogAspect`
  - `CommandBus.send(LogAuditEventCommand)`
  - `AuditLoggingCommandHandler`
  - `AuditEventFactory`
  - `AuditEventWriterPort`
  - `audit_event`
- Supprimer ou migrer le chemin concurrent `RecordAuditEventCommand`.
- Supprimer `AuditedForceCommandAspect`.
- Auditer les opérations `force=true` dans les handlers propriétaires.
- Définir une liste canonique des actions auditables.
- Vérifier chaque endpoint/use-case sensible.
- Corriger la purge :
  - `Clock`,
  - `retentionDays`,
  - suppression par `occurred_at`,
  - tests.
- Corriger le modèle de persistance audit :
  - `entityId` string,
  - details JSONB-safe,
  - index de recherche,
  - modèle tenant/platform explicite.

### 2. Envers

- Enrichir `revinfo` avec le contexte d'exécution.
- Faire lire le contexte via `TchContextResolver`.
- Décider explicitement quelles entités sont `@Audited`.
- Ajouter/valider les tables `_AUD`.
- S'assurer que les futures migrations Flyway gardent les tables `_AUD` synchronisées.

## Impact

- Audit applicatif plus fiable.
- Pas de log “success” si rollback.
- Possibilité de tracer les erreurs même si la transaction métier rollback.
- Actions sensibles couvertes par une liste canonique.
- Purge testée.
- Envers cadré et exploitable.
- Réduction des violations de couche (`common` ne dépend pas de `core.audit`).

## Non-goals

- Ne pas construire une UI complète de consultation d'audit.
- Ne pas remplacer Envers par l'audit applicatif.
- Ne pas introduire d'outbox audit dans ce change.
- Ne pas auditer toutes les entités par défaut sans décision explicite.
- Ne pas construire une solution SIEM/export externe.
