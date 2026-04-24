---
name: core-module
description: Use when creating or modifying anything in the core/ layer — critical business domains, aggregates, value objects, domain services, command handlers, query handlers, output ports, JPA adapters, or anything touching sales, draws, money, limits, or audit.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Module Core — Tchalanet

## Rôle

Domaines métier **critiques** : ventes, tirages, argent, limites, audit.
C'est le cœur de l'application — pur, framework-free au niveau domaine.

## Structure interne complète

```
core/<domain>/
├─ domain/
│  ├─ model/          ← aggregates, entities, value objects (PURE, 0 framework)
│  ├─ exception/      ← exceptions métier du domaine
│  └─ service/        ← domain services (logique pure, pas d'IO)
├─ application/
│  ├─ command/
│  │  ├─ model/       ← XxxCommand (record + typed IDs)
│  │  └─ handler/     ← XxxCommandHandler (1 handler = 1 commande)
│  ├─ query/
│  │  ├─ model/       ← XxxQuery (record)
│  │  └─ handler/     ← XxxQueryHandler
│  └─ event/          ← application events (pas les domain events)
├─ port/
│  └─ out/            ← output ports (XxxReaderPort, XxxWriterPort, XxxRepositoryPort)
└─ infra/
   ├─ persistence/    ← JpaEntity, JpaRepository, JpaAdapter (UUID autorisé ICI)
   ├─ web/            ← controllers HTTP (thin, délèguent au bus)
   ├─ batch/          ← schedulers (orchestration uniquement)
   ├─ event/          ← listeners @TransactionalEventListener AFTER_COMMIT
   └─ cache/          ← adapters cache
```

## Règles de la couche domain/

```java
// ✅ Domain model = PURE Java, 0 annotation Spring, 0 JPA
public class Ticket {
    private final TicketId id;
    private TicketStatus status;

    public Ticket place(DrawId drawId, Amount amount) {
        // logique métier pure et déterministe
        if (this.status != TicketStatus.PENDING)
            throw new TicketAlreadyPlacedException(this.id);
        this.status = TicketStatus.PLACED;
        return this;
    }
}

// ✅ Domain service = logique pure cross-aggregate, pas d'IO
public class TicketDomainService {
    public Ticket validateAndPlace(Ticket ticket, DrawCalendar calendar) { ... }
}
```

## Règles de la couche application/

```java
// ✅ Command = record + typed IDs
public record SellTicketCommand(TicketId ticketId, TenantId tenantId, Amount amount) {}

// ✅ Handler = @TchTx + 1 commande = 1 handler
@Component
@RequiredArgsConstructor
public class SellTicketCommandHandler {

    private final TicketReaderPort ticketReaderPort;
    private final TicketWriterPort ticketWriterPort;
    private final DomainEventPublisher domainEventPublisher;

    @TchTx
    public TicketId handle(SellTicketCommand cmd) {
        var ticket = ticketReaderPort.findOrThrow(cmd.ticketId());
        ticket.place(...);
        ticketWriterPort.save(ticket);
        AfterCommit.run(() -> domainEventPublisher.publish(new TicketPlacedEvent(...)));
        return ticket.id();
    }
}
```

## Règles des ports (port/out/)

```java
// ✅ Output ports = interfaces uniquement, jamais d'implémentation ici
public interface TicketReaderPort {
    Optional<Ticket> findById(TicketId id);
    Ticket findOrThrow(TicketId id);
}

public interface TicketWriterPort {
    Ticket save(Ticket ticket);
    void delete(TicketId id);
}
```

## Règles de la couche infra/

```java
// ✅ JpaEntity : UUID autorisé ICI UNIQUEMENT
@Entity @Table(name = "tickets")
public class TicketJpaEntity extends BaseTenantEntity {
    @Id private UUID id;
    // ...
}

// ✅ JpaAdapter : fait la conversion UUID ↔ TypedId
@Component
public class TicketJpaAdapter implements TicketReaderPort, TicketWriterPort {
    public Ticket findOrThrow(TicketId id) {
        return repo.findById(id.value()) // UUID ici
            .map(mapper::toDomain)
            .orElseThrow(() -> new TicketNotFoundException(id));
    }
}

// ✅ Controller = thin (validation + mapping + bus)
@RestController
public class TicketController {
    @PostMapping("/tickets/{id}/sell")
    public ApiResponse<TicketId> sell(@PathVariable TicketId id, @RequestBody @Valid SellRequest req) {
        var cmd = mapper.toCommand(id, req);
        return ApiResponse.ok(commandBus.dispatch(cmd));
    }
}
```

## Ce qui est INTERDIT dans core/

```java
// ❌ core/ dépend de features/ ou catalog/
import com.tchalanet.server.features.*; // interdit
import com.tchalanet.server.catalog.*; // interdit

// ❌ Logique métier dans les controllers ou schedulers
// ❌ UUID brut dans domain/ ou application/
// ❌ @Autowired sur les champs (toujours constructor injection)
// ❌ @Data Lombok (préférer @Value, @Builder, constructeurs)
// ❌ JpaEntity exposée en dehors de infra/persistence/
```

## Checklist avant tout nouveau module core

- [ ] `domain/model/` = pur Java, 0 annotation Spring/JPA
- [ ] Commands et Queries = records avec typed IDs
- [ ] 1 CommandHandler = 1 commande, annoté `@TchTx`
- [ ] Output ports = interfaces dans `port/out/`
- [ ] UUID uniquement dans `infra/persistence/`
- [ ] JpaAdapter implémente les ports
- [ ] Controllers délèguent au bus, retournent `ApiResponse<T>`
- [ ] Aucune dépendance vers `features/` ou `catalog/`
