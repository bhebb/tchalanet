# Use cases, ports & adapters dans Tchalanet

Ce document explique les concepts clés de l’architecture applicative Tchalanet et comment migrer progressivement depuis l’ancien modèle `controller → service → repository` vers le modèle **domain/usecase/port/adapter**.

---

## 1. Use Case

### 1.1 Définition

Un **use case** représente une **action métier concrète** que l’application peut réaliser.

- 1 use case = 1 classe dans `*.domain.usecase`.
- Il orchestre :
  - le modèle métier (`*.domain.model`)
  - les ports (`*.domain.ports`)
  - éventuellement plusieurs domaines
- Il ne connaît rien de HTTP, de JPA, ni de la façon dont les données sont stockées.

Exemples de use cases pour Tchalanet :

- `CreateTicketUseCase`
- `VerifyTicketUseCase`
- `ConfigureTenantThemeUseCase`
- `UpdateUserPreferenceUseCase`
- `ScheduleDrawUseCase`

### 1.2 Exemple de squelette

```java
// ticket.domain.usecase
@UseCase
public class CreateTicketUseCase {

    private final TicketRepository ticketRepository;
    private final TenantContext tenantContext;

    public CreateTicketUseCase(TicketRepository ticketRepository,
                               TenantContext tenantContext) {
        this.ticketRepository = ticketRepository;
        this.tenantContext = tenantContext;
    }

    public Ticket handle(CreateTicketCommand command) {
        // TODO: logique métier (autonomie, limites, tirage, etc.)
        // 1. valider la requête
        // 2. construire le Ticket (domain.model)
        // 3. persister via TicketRepository (port)
        // 4. retourner le Ticket créé
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

Un use case :

- prend des **commandes** ou paramètres métiers (pas des DTO HTTP bruts),
- utilise les **ports** pour interagir avec la persistance ou les systèmes externes,
- retourne des objets métier (entités / value objects).

---

## 2. Ports & Adapters

### 2.1 Port

Un **port** est une interface définie côté **domaine** qui exprime ce dont le domaine a besoin (persistance, services externes, horloge, etc.).

Exemples :

```java
// ticket.domain.ports
public interface TicketRepository {
    Optional<Ticket> findById(TicketId id);

    Ticket save(Ticket ticket);
}

// common.domain.ports
public interface ClockPort {
    Instant now();
}
```

Caractéristiques :

- Aucun détail technique (pas d’annotations Spring ou JPA).
- N’est pas lié à une technologie (Postgres, Redis, HTTP, …).
- Permet de tester le domaine avec des doubles (mocks/fakes) sans vraie base.

### 2.2 Adapter

Un **adapter** est une **implémentation technique** d’un port.

Exemples :

```java
// ticket.infra.persistence
@Repository
class JpaTicketRepository implements TicketRepository {

    private final SpringDataTicketJpaRepository jpa;
    private final TicketEntityMapper mapper;

    @Override
    public Optional<Ticket> findById(TicketId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Ticket save(Ticket ticket) {
        var entity = mapper.toEntity(ticket);
        return mapper.toDomain(jpa.save(entity));
    }
}
```

```java
// common.infra.time
@Component
public class SystemClockAdapter implements ClockPort {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
```

Caractéristiques :

- Vivent dans `*.infra.*`.
- Dépendent de Spring, JPA, WebClient, JDBC, Redis, Meilisearch, Keycloak, etc.
- Implémentent les ports définis dans `*.domain.ports`.

---

## 3. Rôle des contrôleurs (`*.web`)

Les contrôleurs REST :

- vivent dans `<domaine>.web`,
- dépendent des **use cases**,
- transforment :
  - la requête HTTP → **commandes métier / paramètres**,
  - la réponse du use case → **DTO HTTP**.

Exemple :

```java
// ticket.web
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final CreateTicketUseCase createTicket;

    public TicketController(CreateTicketUseCase createTicket) {
        this.createTicket = createTicket;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@RequestBody CreateTicketRequest request) {
        var command = request.toCommand(); // mapping HTTP → domaine
        var ticket = createTicket.handle(command);
        return ResponseEntity.ok(TicketResponse.from(ticket)); // domaine → HTTP DTO
    }
}
```

---

## 4. Migration : ancien modèle → nouveau modèle

### 4.1 Ancien modèle typique

```java
// api
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService service;

    @PostMapping
    public TicketDto create(@RequestBody TicketDto dto) {
        return service.create(dto);
    }
}

// services
@Service
public class TicketService {

    @Autowired
    private TicketRepository repository;

    public TicketDto create(TicketDto dto) {
        // logique métier mélangée + mapping + JPA
        var entity = new TicketEntity(...);
        repository.save(entity);
        return dtoFromEntity(entity);
    }
}
```

Problèmes :

- Service fait tout : validation, mapping, logique métier, persistance.
- Couplage fort aux entités JPA / DTO.

### 4.2 Étapes de migration pour une fonctionnalité

On migre **use case par use case**, pas tout d’un coup.

#### Étape 1 — Identifier le use case

Prendre une méthode de service claire, par ex. `TicketService.create()` → devient `CreateTicketUseCase`.

#### Étape 2 — Créer les éléments de domaine

1. Déplacer/introduire les **entités métier** dans `ticket.domain.model` :
   - `Ticket`, `TicketId`, `TicketStatus`, etc.
2. Définir le **port de persistance** dans `ticket.domain.ports` :
   ```java
   public interface TicketRepository {
       Ticket save(Ticket ticket);
   }
   ```

#### Étape 3 — Créer le use case

```java
// ticket.domain.usecase
@UseCase
public class CreateTicketUseCase {

    private final TicketRepository ticketRepository;

    public CreateTicketUseCase(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Ticket handle(CreateTicketCommand cmd) {
        // TODO: validation métier, règles de limite, autonomie, tirage, etc.
        var ticket = new Ticket(/* ... à partir de cmd ... */);
        return ticketRepository.save(ticket);
    }
}
```

> `CreateTicketCommand` est un **type métier** (pas un DTO HTTP), par ex. dans `ticket.domain.model` ou `ticket.domain.usecase`.

#### Étape 4 — Implémenter l’adapter de persistance

```java
// ticket.infra.persistence
@Repository
class JpaTicketRepository implements TicketRepository {

    private final SpringDataTicketJpaRepository jpa;
    private final TicketEntityMapper mapper;

    @Override
    public Ticket save(Ticket ticket) {
        var entity = mapper.toEntity(ticket);
        return mapper.toDomain(jpa.save(entity));
    }
}
```

#### Étape 5 — Adapter le contrôleur

Le contrôleur appelle maintenant le **use case**, plus le service :

```java
// ticket.web
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final CreateTicketUseCase createTicket;

    @PostMapping
    public ResponseEntity<TicketResponse> create(@RequestBody CreateTicketRequest req) {
        var command = req.toCommand();      // HTTP → domaine
        var ticket = createTicket.handle(command);
        return ResponseEntity.ok(TicketResponse.from(ticket));  // domaine → HTTP
    }
}
```

#### Étape 6 — Déprécier l’ancien service

- `TicketService` peut temporairement appeler `CreateTicketUseCase` et être annoté `@Deprecated`, le temps de migrer tous les appels.
- Quand tous les contrôleurs utilisent le use case, on supprime le service.

---

## 5. Règles pratiques

- **1 use case = 1 classe**, avec un verbe clair (`CreateXxx`, `UpdateXxx`, `VerifyXxx`, `ScheduleXxx`, …).
- Les contrôleurs **ne parlent qu’aux use cases**, jamais aux repositories.
- Les use cases **ne parlent qu’aux ports**, jamais directement à des implémentations techniques.
- Les adapters (JPA, HTTP, Redis, Meili, Keycloak…) vivent dans `*.infra.*` et implémentent les ports.
- Lors de la migration, on peut garder des services/DTO/entités “ponts” annotés `@Deprecated` le temps de tout déplacer.

Ce document sert de référence lors des refactorings : toute nouvelle fonctionnalité doit être pensée en termes de **domaine → use case → ports → adapters → web**, et non `controller → service → repository` générique.
