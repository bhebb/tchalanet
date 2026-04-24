---
name: backend-events
description: Use when writing domain events, event publishers, event listeners, or cross-domain side effects in tchalanet-server — enforces after-commit publication, thin listener pattern, idempotence via ProcessedEventPort, naming conventions, and prohibits events from catalog modules.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Events et effets de bord

> ⚠️ **Ce fichier est un résumé actionable pour l'IA.**
> Ne pas éditer ce fichier pour changer une règle — modifier la source canonique :
> 👉 `tchalanet-server/docs/conventions/event_model.md`

## Règles absolues

| Règle                              | Détail                                              |
| ---------------------------------- | --------------------------------------------------- |
| Un DomainEvent = un **fait passé** | `TicketPlacedEvent` ✅ — `PlaceTicketEvent` ❌      |
| Publié **uniquement after-commit** | Via `AfterCommit.run(...)` + `DomainEventPublisher` |
| Listeners cross-domain             | `@TransactionalEventListener(phase = AFTER_COMMIT)` |
| Listeners = thin                   | log + idempotence + dispatch vers bus uniquement    |
| `catalog/`                         | **Jamais** de domain events                         |

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
    // 1. logique métier pure
    var ticket = ticketDomainService.sell(...);

    // 2. persister via port
    ticketWriterPort.save(ticket);

    // 3. publier APRÈS commit uniquement
    AfterCommit.run(() ->
        domainEventPublisher.publish(new TicketPlacedEvent(
            EventId.generate(),
            Instant.now(),
            cmd.tenantId(),
            ticket.id()
        ))
    );

    return Result.success(ticket.id());
}
```

**Règle** : si la transaction rollback → aucun event publié.

---

## Consommation — listener pattern

```java
@Component
@RequiredArgsConstructor
public class LimitPolicyExposureProjector {

    private final ProcessedEventPort processedEvent;
    private final LimitPolicyWriterPort writerPort;

    private static final String HANDLER_KEY = "limitpolicy.exposure"; // domain.projection

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void on(TicketPlacedEvent event) {
        if (processedEvent.alreadyProcessed(HANDLER_KEY, event.eventId())) return;

        writerPort.updateExposure(event.ticketId(), event.amount()); // logique mince

        processedEvent.markProcessed(HANDLER_KEY, event.eventId());
    }
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

Format `HANDLER_KEY` : `"<domain>.<projection>"` ex : `"limitpolicy.exposure"`, `"stats.daily"`.

---

## Naming des events

| Contexte   | Pattern             | Exemple              |
| ---------- | ------------------- | -------------------- |
| Création   | `XxxCreatedEvent`   | `TicketCreatedEvent` |
| Annulation | `XxxCancelledEvent` | `DrawCancelledEvent` |
| Règlement  | `XxxSettledEvent`   | `PayoutSettledEvent` |
| Placement  | `XxxPlacedEvent`    | `TicketPlacedEvent`  |

Toujours au **passé**. Jamais de verbe à l'infinitif ou à l'impératif.

---

## Où vivent les events et listeners

| Artefact             | Localisation                                 |
| -------------------- | -------------------------------------------- |
| Classe event         | `core.<source_domain>.domain.event.*`        |
| Listener infra       | `core.<consumer_domain>.infra.event.*`       |
| Listener application | `core.<consumer_domain>.application.event.*` |
| Feature projection   | `features.<feature>.application.event.*`     |

---

## Ce qui est interdit

```java
// ❌ Publication avant commit
domainEventPublisher.publish(new TicketPlacedEvent(...)); // sans AfterCommit

// ❌ Listener avec logique métier lourde
@TransactionalEventListener
public void on(TicketPlacedEvent e) {
    complexBusinessLogic(); // ← déléguer au bus, pas ici
}

// ❌ Domain event dans catalog/
// catalog/ ne publie jamais d'events

// ❌ Listener non idempotent
// tout listener DOIT vérifier ProcessedEventPort
```

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
- [ ] `HANDLER_KEY` stable et unique défini comme constante (`"<domain>.<projection>"`)
- [ ] Event vit dans le domaine producteur
- [ ] Listener vit dans le domaine consommateur
- [ ] Aucun event dans `catalog/`
