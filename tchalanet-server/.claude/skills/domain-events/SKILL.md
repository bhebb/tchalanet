---
name: domain-events
description: >
  Déclencher pour toute création ou modification de domain event, listener,
  event publisher, ou tout code réagissant à un fait passé dans le domaine.
  Indispensable si la tâche implique : AfterCommit, DomainEventPublisher,
  @TransactionalEventListener, ProcessedEventPort, idempotence, ou cross-domain listeners.
---

# Domain Events — Tchalanet

## Règles absolues

| Règle                              | Détail                                              |
| ---------------------------------- | --------------------------------------------------- |
| Un DomainEvent = un **fait passé** | `TicketPlacedEvent` ✅ — `PlaceTicketEvent` ❌      |
| Publié **uniquement after-commit** | Via `AfterCommit.run(...)` + `DomainEventPublisher` |
| Listeners cross-domain             | `@TransactionalEventListener(phase = AFTER_COMMIT)` |
| Listeners = thin                   | log + idempotence + dispatch vers bus uniquement    |
| `catalog/`                         | **Jamais** de domain events                         |

## Pattern canonique de publication

```java
@TchTx
public Result handle(SellTicketCommand cmd) {
    // 1. logique métier pure
    var ticket = ticketDomainService.sell(...);

    // 2. persister via port
    ticketWriterPort.save(ticket);

    // 3. publier APRÈS commit uniquement
    AfterCommit.run(() ->
        domainEventPublisher.publish(new TicketPlacedEvent(ticket.id(), ticket.tenantId(), ...))
    );

    return Result.success(ticket.id());
}
```

## Pattern canonique d'un listener

```java
@Component
@RequiredArgsConstructor
public class LimitPolicyExposureProjector {

    private final ProcessedEventPort processedEvent;
    private final LimitPolicyWriterPort writerPort;

    private static final String HANDLER_KEY = "limitpolicy.exposure"; // domain.projection

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void on(TicketPlacedEvent event) {
        // idempotence obligatoire
        if (processedEvent.alreadyProcessed(HANDLER_KEY, event.eventId())) return;

        // logique de projection (thin)
        writerPort.updateExposure(event.ticketId(), event.amount());

        processedEvent.markProcessed(HANDLER_KEY, event.eventId());
    }
}
```

## HANDLER_KEY — convention

Format : `"<domain>.<projection>"` en minuscules.

Exemples : `"limitpolicy.exposure"`, `"audit.ticketplaced"`, `"settlement.pending"`

## Naming des events

| Contexte   | Pattern             | Exemple              |
| ---------- | ------------------- | -------------------- |
| Création   | `XxxCreatedEvent`   | `TicketCreatedEvent` |
| Annulation | `XxxCancelledEvent` | `DrawCancelledEvent` |
| Règlement  | `XxxSettledEvent`   | `PayoutSettledEvent` |
| Placement  | `XxxPlacedEvent`    | `TicketPlacedEvent`  |

Toujours au **passé**. Jamais de verbe à l'infinitif ou à l'impératif.

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

## Checklist avant tout nouvel event ou listener

- [ ] Nom de l'event au passé (`XxxCreatedEvent`, `XxxSettledEvent`)
- [ ] Publication via `AfterCommit.run(...)` uniquement
- [ ] Listener annoté `@TransactionalEventListener(phase = AFTER_COMMIT)`
- [ ] Idempotence via `ProcessedEventPort` avec un `HANDLER_KEY` unique
- [ ] Listener thin (log + idempotence + dispatch), pas de logique lourde
- [ ] Aucun event dans `catalog/`
