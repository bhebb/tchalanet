# Eventing Model — Conventions (MVP)

> **Statut** : NORMATIVE  
> **Périmètre** : tchalanet-server  
> **Audience** : développeurs backend, reviewers  
> **Mode** : MVP (in-process, Spring events, after-commit)

Ce document définit :

- quand publier un event,
- où vivent les events et listeners,
- comment consommer les events de manière sûre (after-commit, idempotence),
  sans outbox pour le MVP.

---

## 1. Principes fondamentaux

- Un **DomainEvent** représente un fait métier déjà arrivé.
- Les events servent à :
  - découpler les domaines ;
  - déclencher des effets secondaires (stats, notifications, cache, payout…) ;
  - éviter les écritures cross-domain synchrones.
- Tous les events **cross-domain** doivent :
  - être publiés **after-commit** ;
  - être consommés **after-commit** ;
  - être **idempotents** côté consumer.

---

## 2. Packages canoniques

### 2.1 Primitives transverses (`common`)

Arborescence canonique :

```text
common.event
├─ DomainEvent
├─ DomainEventPublisher
└─ infra.spring
   ├─ SpringDomainEventPublisher
   └─ LoggingDomainEventListener   (profil dev/stg)
```

- `DomainEvent` : contrat minimal (eventId, occurredAt, tenantId).
- `DomainEventPublisher` : port de publication.
- `SpringDomainEventPublisher` : adapter Spring (`ApplicationEventPublisher`).
- `LoggingDomainEventListener` : utilitaire d’observabilité (profil dev/stg).

### 2.2 Où vivent les events (les classes `*Event`)

Règle canonique :

- Un event vit dans le **domaine source** qui produit le fait métier.
- Emplacement recommandé :

```
core.<bounded_context>.domain.event.*
```

**Exemples**

- `core.sales.domain.event.TicketPlacedEvent`
- `core.draw.domain.event.DrawResultAppliedEvent`
- `core.session.domain.event.SessionClosedEvent`

🚫 Interdit :

- `catalog.*.domain.event` (le catalogue est _side‑effect free_).

### 2.3 Où vivent les listeners

Règle canonique :

- Un listener vit dans le **domaine consommateur**.

Deux conventions autorisées (choisir l’une et l’appliquer) :

- Option recommandée (réaction infra) :
  - `core.<consumer>.infra.event.*`
- Option acceptable (application-level) :
  - `core.<consumer>.application.event.*`

Cas particulier pour les projections applicatives :

- `features.stats.aggregates.application.event.*`

---

## 3. Quand publier un `DomainEvent`

### 3.1 Publier un event quand

- un changement d’état métier est validé ;
- d’autres domaines doivent réagir ;
- il y a des effets secondaires (stats, payout, notifications, cache).

**Exemples** : Ticket placé / annulé / résulté ; Résultat de tirage appliqué ; Session ouverte / fermée ; Paiement créé / confirmé.

### 3.2 Ne PAS publier d’event quand

- la logique est strictement interne à l’aggregate ;
- l’information n’a aucun consommateur ;
- c’est un détail technique sans valeur métier.

---

## 4. Publication : règle `AFTER_COMMIT`

### 4.1 Règle absolue

Un `DomainEvent` doit toujours être publié **après le commit** de la transaction métier.

Pattern canonique (dans un command handler annoté `@TchTx`) :

```java
@TchTx
public Result handle(Command c) {
  var res = ...; // écritures métier

  AfterCommit.run(() ->
      domainEventPublisher.publish(
          new TicketPlacedEvent(...)
      )
  );

  return res;
}
```

➡️ Si la transaction rollback, aucun event n’est publié.

---

## 5. Consommation : listeners

### 5.1 `AFTER_COMMIT` obligatoire côté listener

Tous les listeners cross-domain doivent utiliser :

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
```

Cela protège contre :

- une publication accidentelle avant commit ;
- la mise à jour de projections basées sur des données rollbackées.

### 5.2 Listener « thin » (règle pratique)

Un listener doit :

- logger l’event ;
- appliquer une logique d’idempotence ;
- mapper l’event vers le modèle local ;
- dispatcher vers un `CommandBus` / `QueryBus` si nécessaire.

Un listener ne doit pas :

- contenir de logique métier complexe ;
- faire de calculs financiers critiques ;
- modifier plusieurs aggregates directement dans la même transaction.

---

## 6. Idempotence (obligatoire)

Même en MVP in-process, un event peut être publié deux fois, rejoué ou relancé après erreur — il faut donc gérer l’idempotence.

### 6.1 Stratégies acceptées

- Idempotence métier (contraintes uniques, upserts) ;
- Event log : table `processed_event` (PK = `event_id`) pour marquer un event traité.

**Exemples**

- `features.stats` : table `stats_event_log` ;
- `sales/payout` : contrainte unique `(tenantId, drawId, drawResultId)`.

---

## 7. Nombre de listeners (règle pratique)

Recommandation :

- 1 listener « router » par domaine consommateur ;
- 2–3 listeners maximum si séparation claire par concern (stats / notifications / cache).

**Objectifs** : lisibilité, centralisation idempotence/logs, facilité de désactivation / feature-flag.

**Exemples** : `SalesDomainEventsListener`, `StatsAggregatesEventListener`.

---

## 8. Règles spécifiques au catalogue (`catalog`)

- `catalog/` est _read-only_ côté core ;
- `catalog/` n’émet **jamais** de `DomainEvent` métier.

Les changements du catalogue (CRUD admin) peuvent déclencher des _application events_ (invalidate cache, refresh projections) mais **jamais** des domain events métier.

---

## 9. Résumé exécutif

- `DomainEvent` = fait métier déjà arrivé ;
- Publication = `after-commit` uniquement ;
- Listener = `after-commit` + idempotent ;
- Event vit chez le producer ;
- Listener vit chez le consumer ;
- Catalog = pas d’events ;
- MVP = Spring events in-process (pas d’outbox).

---

## 10. Évolution future (hors MVP)

Voies d’évolution plausibles :

- Outbox pattern ;
- Consumer batch / retry backoff ;
- Dead-letter queue ;
- Event versioning.
