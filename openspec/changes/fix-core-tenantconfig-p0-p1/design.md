## Context

`core.tenantconfig` est le bounded context qui gère le registre et le cycle de vie des tenants.
L'audit du 27/04/2026 a révélé 6 anomalies bloquantes :

1. `UpdateTenantIdentityCommandHandler` existe en tant que fichier mais est **vide** — le handler
   n'est jamais invoqué → `PUT /config/identity` ne persiste rien.
2. `TenantAdminIdentityController` est **fat** : il parse ZoneId/Currency, appelle directement
   `TenantCatalog` (deux fois), et passe des valeurs pré-parsées à la command — violant la règle
   "controller thin".
3. `DeactivateTenantCommandHandler` appelle `tenant.suspend(now)` avec un commentaire
   `// use suspend for now` — les deux handlers font la même chose. C'est une dette confirmée.
4. Les handlers lifecycle (`Activate`, `Suspend`, `Archive`) utilisent `UUID.randomUUID()` au
   lieu d'`IdGenerator` — incohérence avec `CreateTenantCommandHandler`.
5. `TenantMapper.toEntity()` contient des expressions `java(java.time.Instant.now())` qui
   écrasent l'initialisation par Spring Data Auditing (`@CreatedDate` / `@LastModifiedDate`).
6. `TenantCreatedEvent` est un record non utilisé nulle part — code mort.

## Goals / Non-Goals

**Goals:**

- `PUT /config/identity` persiste vraiment les changements (name, timezone, currency)
- `TenantAdminIdentityController` devient thin (≤ 3 lignes par méthode)
- `deactivate` supprimé — `suspend` est l'unique transition vers `SUSPENDED`
- `IdGenerator` utilisé uniformément dans tous les handlers lifecycle
- `TenantMapper.toEntity()` ne pollue plus l'audit `createdAt`/`updatedAt`
- Code mort (`TenantCreatedEvent`, `DeactivateTenantCommand`) supprimé

**Non-Goals:**

- Modifier le schéma de base de données (aucune migration Flyway)
- Changer le contrat API des endpoints existants (sauf suppression de `/deactivate`)
- Implémentation des corrections i18n/settings (couvertes par `fix-tenantadmin-config-catalog-encapsulation`)

## Decisions

### D1 — `UpdateTenantIdentityCommandHandler` retourne `void`

**Décision** : le handler est `VoidCommandHandler<UpdateTenantIdentityCommand>`.
Le controller re-query via `QueryBus.send(GetTenantByIdQuery)` après commit pour construire la réponse.

**Pourquoi** : évite de casser la séparation CQS. Le handler écrit, la query lit. Le controller
fait `commandBus.send(cmd)` puis `queryBus.send(query)` — deux lignes claires.

**Alternative rejetée** : `CommandHandler<UpdateTenantIdentityCommand, TenantConfigView>` qui
retourne une vue. Rejeté car ça force le handler à connaître la représentation HTTP.

### D2 — Validation ZoneId/Currency dans le handler, pas le controller

**Décision** : `UpdateTenantIdentityCommandHandler` tente `ZoneId.of(cmd.timezone().toString())`
et `Currency.getInstance(cmd.currency().getCurrencyCode())`. Si invalide → lever
`ProblemRest.badRequest(...)`.

**Pourquoi** : la règle du projet est "controller thin, validations métier dans le handler". Le
controller ne connaît pas si une timezone est valide — c'est logique métier.

**Note** : `UpdateTenantIdentityCommand` contient déjà des types `ZoneId` et `Currency` —
la validation ZoneId/Currency est donc déjà faite implicitement lors de la construction de la
command. Le controller peut parser les strings en ZoneId/Currency, et si ça lève → 400 via
l'exception handler global.

### D3 — Suppression totale de `deactivate`

**Décision** : supprimer `DeactivateTenantCommand`, `DeactivateTenantCommandHandler`,
et `POST /platform/tenants/{id}/deactivate`.

**Pourquoi** : `suspend` et `deactivate` ont exactement la même sémantique dans l'état actuel.
Maintenir deux handlers identiques est une source de confusion. La décision est de **ne pas**
ajouter `TenantStatus.DEACTIVATED` — `SUSPENDED` couvre tous les cas d'usage actuels.

**BREAKING** : le endpoint `POST /platform/tenants/{id}/deactivate` disparaît. Acceptable car
l'application n'est pas encore déployée en production.

### D4 — `TenantMapper.toEntity()` : dates gérées par Spring Data Auditing

**Décision** : supprimer les deux expressions `java(java.time.Instant.now())` dans `toEntity()`.
`@CreatedDate` et `@LastModifiedDate` sur `TenantJpaEntity` gèrent l'initialisation.

**Pourquoi** : le mapper s'exécute avant le `persist()`. Spring Data Auditing intercepte
`@PrePersist` et `@PreUpdate`. Si le mapper écrase les champs, l'auditing JPA est contourné
et `updatedAt` est fixé à l'heure de mapping, pas de persistance.

### D5 — `TenantIdentityUpdatedEvent` comme domaine event distinct

**Décision** : créer `TenantIdentityUpdatedEvent(eventId, occurredAt, tenantId, changedFields)`
au lieu de réutiliser `TenantStatusChangedEvent` avec `reason="identity_updated"`.

**Pourquoi** : un changement d'identité (name, timezone, currency) n'est pas un changement de
statut. Réutiliser `TenantStatusChangedEvent` avec un reason string est un code smell. Les
consommateurs futurs (cache, search index) ont besoin de distinguer les deux types d'événement.

### D6 — `CreateTenantCommandHandler` : `address` via port dédié

**Décision** : si `cmd.address() != null`, le handler crée l'adresse via `AddressWriterPort`
(déjà présent dans le projet) avant de créer le tenant. L'`AddressId` résultant est passé à
`TenantConfig.createDraft(...)`.

**Pourquoi** : l'entité `address` vit dans `core.address`. Le handler `tenantconfig` peut appeler
le port `AddressWriterPort` car les dépendances `core → core` via port sont autorisées.

## Risks / Trade-offs

- **[Risk] Parsing ZoneId dans le controller** → si on délègue la validation au handler mais que
  le controller passe déjà un `ZoneId` typé, la validation est implicite à la construction.
  `UpdateTenantIdentityCommand` utilise `ZoneId timezone` et `Currency currency` en types Java
  — donc la validation se fait lors de la désérialisation JSON. → Mitigation : le `@JsonDeserialize`
  custom ou le handler global d'exceptions couvre `DateTimeException` et `IllegalArgumentException`
  → réponse 400 automatique.

- **[Risk] `CreateTenantCommand.activate` ignoré** → si le flag `activate=true` n'est pas
  implémenté dans le handler, les tenants créés restent always en DRAFT. → Mitigation : tâche
  explicite dans le task list.

- **[Risk] Suppression de `deactivate` brise des tests existants** → vérifier les tests avant
  suppression.

## Migration Plan

1. Implémenter `UpdateTenantIdentityCommandHandler` (D1, D2)
2. Créer `TenantIdentityUpdatedEvent` (D5)
3. Alléger `TenantAdminIdentityController` (D2)
4. Injecter `IdGenerator` dans les 3 handlers lifecycle (D4 P1-1)
5. Implémenter `address` + `activate` dans `CreateTenantCommandHandler` (D6)
6. Corriger `TenantMapper.toEntity()` (D4)
7. Supprimer `DeactivateTenantCommand`, handler, et endpoint (D3)
8. Supprimer `TenantCreatedEvent`
9. Build + tests

Rollback : aucun changement de schéma → rollback = revert Git.

## Open Questions

- _(aucune — toutes les décisions ont été tranchées)_
