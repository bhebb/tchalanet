# Tchalanet – Backend Code Style & Patterns

> Ce document complète `ARCHITECTURE.md`.  
> Il est focalisé sur les **conventions de code** (naming, patterns, logs, tests).  
> Tous les contributeurs (humains et AI) doivent le suivre.

---

## 1. Scope

S’applique à tout le backend :

- Architecture Hexagonale + CQRS.
- Commands / Queries / Handlers.
- Domain model riche (non anémique).
- Persistence, multi-tenancy, intégrations externes.
- Web layer, logging, error handling, tests.

---

## 2. Naming & Packages

### 2.1 Commands & Queries

- Commands : `CreateDrawCommand`, `RecordAuditEventCommand`, `PayTicketCommand`.
- Queries : `ListUpcomingDrawsQuery`, `GetTicketDetailsQuery`.

### 2.2 Handlers

- Interfaces :

  - `CreateDrawCommandHandler`,
  - `ListUpcomingDrawsQueryHandler`.

- Implémentations :
  - `CreateDrawUseCase`,
  - `ListUpcomingDrawsUseCase`.

### 2.3 Ports

- `application.port.in` :

  - `CreateDrawCommandHandler`,
  - `ListUpcomingDrawsQueryHandler`.

- `application.port.out` :
  - `DrawReaderPort`, `DrawWriterPort`,
  - `TicketReaderPort`, `TicketWriterPort`,
  - `ExternalDrawResultPort`.

**Rappel :** ne pas mettre les ports sous `domain`.

---

## 3. Commands & Queries

### 3.1 Commands

- Doivent être des **records Java**.
- **Ne** contiennent **aucune** logique métier.
- Représentent l’intention métier : “créer un tirage”, “payer un ticket”, etc.

### 3.2 Queries

- Records Java.
- Lecture seule.
- Ne dépendent pas de ports “writer”.

---

## 4. Domain Model

- Doit être regroupé dans `domain.model`.
- Pas d’annotations Spring/JPA.
- Les entités/agrégats doivent :
  - exposer des méthodes métier (`close`, `pay`, `cancel`, …),
  - protéger leurs invariants.

Exemple :

```java
public final class Ticket {

    public Ticket pay(BigDecimal amount) {
        if (status != TicketStatus.CREATED) {
            throw new TicketCannotBePaidException(id);
        }
        if (amount.compareTo(total) < 0) {
            throw new TicketInsufficientAmountException(id);
        }
        return new Ticket(id, tenantId, TicketStatus.PAID, total, paidAt);
    }
}
```

---

## 5. Persistence & Multi-Tenancy

- Tous les adapters persistence implémentent des ports `*ReaderPort` et `*WriterPort`.
- `tenant_id` est toujours présent pour les domaines multi-tenant.
- RLS est responsable de la séparation des données par tenant.
- Ne jamais injecter de repository JPA directement dans :
  - `domain`,
  - `application`.

---

## 6. External APIs

- `RestTemplate` est **interdit**.
- Utiliser exclusivement `WebClient`.
- Mapping JSON ↔ DTO fait dans `infra.external`.
- Les ports externes encapsulent les appels HTTP :

```java
public interface ExternalDrawResultPort {
    Optional<ExternalDrawResult> fetchResult(DrawExternalQuery query);
}
```

---

## 7. Web Layer (Controllers)

- Controllers dans `infra.web`.
- Responsabilités :

  - transformer HTTP → Commands/Queries,
  - appeler les handlers,
  - mapper le résultat en DTO HTTP.

- Interdictions :
  - pas de logique métier,
  - pas d’accès direct aux repositories,
  - pas d’accès direct à WebClient.

---

## 8. Logging

- Utiliser SLF4J (via Lombok `@Slf4j` ou injection).
- Messages structurés, incluant les champs importants :

```java
log.warn("External provider failure: provider={} channel={} date={} message={}",
    provider, query.channelCode(), query.drawDate(), e.getMessage());
```

- Interdits :
  - `System.out.println`,
  - `e.printStackTrace()`.

---

## 9. Error Handling

- Utiliser un `@ControllerAdvice` global pour mapper les exceptions métier vers HTTP :

  - 400 : erreurs de validation / format,
  - 403 : interdiction d’accès,
  - 404 : non trouvé,
  - 409 : conflits métier,
  - 500 : erreurs inattendues.

- Les exceptions domaine doivent être dans `domain.exception`.

---

## 10. Tests

- Tests **domaine** :

  - sans Spring,
  - instanciation directe des entités/agrégats.

- Tests **application** :

  - use cases testés avec des ports in-memory ou mocks.

- Tests **infra** :
  - persistence : Testcontainers Postgres,
  - external HTTP : MockWebServer / mocks WebClient,
  - web : `@WebMvcTest`.

---

## 11. Raccourci pour Copilot / AI

Lorsque tu génères du code backend Tchalanet, **toujours** :

1. Respecter l’architecture décrite dans `ARCHITECTURE.md`.
2. Placer les ports sous `application.port.in` / `application.port.out`.
3. Utiliser des `record` pour Commands / Queries.
4. Garder le domaine pur (pas d’annotations, pas de Spring).
5. Garder les controllers minces.
6. Utiliser `WebClient` pour le HTTP sortant.
7. Logger avec SLF4J.
8. Respecter `tenantId` pour tout ce qui est multi-tenant.

Pour plus de contexte conceptuel, voir les références :

- Hexagonal Architecture — Alistair Cockburn
- Clean Architecture — Robert C. Martin
- CQRS — Greg Young
- DDD — Eric Evans
