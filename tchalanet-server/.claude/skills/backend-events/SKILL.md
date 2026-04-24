---
name: backend-events
description: >
  Use when writing domain events, event publishers, event listeners, or cross-domain side effects in tchalanet-server — enforces after-commit publication, thin listener pattern, idempotence via ProcessedEventPort, and prohibits events from catalog modules.
---

# Events et effets de bord

> ⚠️ **Ce fichier est un résumé actionable pour l'IA.**
> Ne pas éditer ce fichier pour changer une règle — modifier la source canonique :
> 👉 `tchalanet-server/docs/conventions/event_model.md`

## Principes fondamentaux

- Un `DomainEvent` représente un **fait passé** (nommé au passé : `XxxCreatedEvent`, `XxxCancelledEvent`)
- Les events découplent les domaines et déclenchent des effets secondaires (stats, notifications, cache, payout)
- Tous les events cross-domain : publiés **after-commit**, consommés **after-commit**, idempotents

---

## Packages canoniques

```
common.event
├─ DomainEvent                        ← contrat minimal (eventId, occurredAt, tenantId)
├─ DomainEventPublisher               ← port de publication
└─ infra.spring
   ├─ SpringDomainEventPublisher      ← adapter Spring (ApplicationEventPublisher)
   └─ LoggingDomainEventListener      ← observabilité (profil dev/stg)

core.<domain>.domain.event.*          ← classes d'events (chez le producteur)
core.<consumer>.infra.event.*         ← listeners (chez le consommateur)
```

---

## Publication — pattern canonique (AFTER_COMMIT)

```java
@TchTx
public Result handle(SellTicketCommand cmd) {
  var ticket = ...; // mutations métier dans la transaction

  AfterCommit.run(() ->
    domainEventPublisher.publish(new TicketPlacedEvent(
        EventId.generate(),
        Instant.now(),
        cmd.tenantId(),
        ticket.id()
    ))
  );

  return result;
}
```

**Règle** : si la transaction rollback → aucun event publié.

---

## Consommation — listener pattern

```java
// Listener cross-domain : TOUJOURS AFTER_COMMIT
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onTicketPlaced(TicketPlacedEvent event) {
  log.info("Processing event {}", event.eventId());
  // → idempotence check
  // → map vers modèle local
  // → dispatch vers CommandBus si nécessaire
}
```

**Un listener doit être thin** :

- Logger l'event
- Vérifier idempotence (`ProcessedEventPort`)
- Mapper vers le modèle local
- Dispatcher vers bus

**Un listener ne doit pas** :

- Contenir de logique métier complexe
- Faire des calculs financiers
- Modifier plusieurs aggregates directement dans la même transaction

---

## Idempotence des consommateurs (OBLIGATOIRE)

```java
// Pattern check → apply → mark
private static final String HANDLER_KEY = "limitpolicy.exposure"; // stable, unique

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onTicketPlaced(TicketPlacedEvent event) {
  var eventId = event.eventId().value();

  if (processedEvent.alreadyProcessed(HANDLER_KEY, eventId)) return; // silencieux

  projector.applyTicketPlaced(event);

  processedEvent.markProcessed(HANDLER_KEY, eventId);
}
```

Format `HANDLER_KEY` : `"<domain>.<projection>"` ex: `"limitpolicy.exposure"`, `"stats.daily"`.

---

## Où vivent les events et listeners

| Artefact             | Localisation                                 |
| -------------------- | -------------------------------------------- |
| Classe event         | `core.<source_domain>.domain.event.*`        |
| Listener infra       | `core.<consumer_domain>.infra.event.*`       |
| Listener application | `core.<consumer_domain>.application.event.*` |
| Feature projection   | `features.<feature>.application.event.*`     |

---

## Règles catalog (rappel)

- `catalog/` **n'émet jamais** de domain events métier
- Les changements catalog (CRUD admin) peuvent déclencher des _application events_ de cache/refresh, jamais de domain events

---

## Quand publier

✅ Publier quand :

- Un état métier change et est validé
- D'autres domaines doivent réagir
- Des effets secondaires existent (stats, payout, notifications, cache)

❌ Ne pas publier quand :

- La logique est interne à l'aggregate
- L'information n'a aucun consommateur
- C'est un détail technique sans valeur métier

---

## Checklist nouvel event

- [ ] Nommé au passé : `XxxYyyEvent`
- [ ] Implémente `DomainEvent` (eventId, occurredAt, tenantId)
- [ ] Publié via `AfterCommit.run(() -> domainEventPublisher.publish(...))`
- [ ] Listener annoté `@TransactionalEventListener(phase = AFTER_COMMIT)`
- [ ] Listener implémente idempotence via `ProcessedEventPort`
- [ ] `HANDLER_KEY` stable et unique défini comme constante
- [ ] Event vit dans le domaine producteur
- [ ] Listener vit dans le domaine consommateur
