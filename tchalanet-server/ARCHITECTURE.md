# Tchalanet – Architecture Backend (Hexagonal + CQRS)

> Version : 2025-11  
> Ce document est la **source de vérité** concernant l’architecture du backend Tchalanet  
> (Spring Boot, PostgreSQL, RLS, Keycloak, Unleash, Meilisearch, Redis, Traefik).
>
> Tous les domaines doivent suivre les conventions décrites ici,  
> y compris les assistants AI (Copilot, ChatGPT, etc.).
>
> Pour les règles de style détaillées (naming, logging, tests), voir `CODE_STYLE.md`.

---

## 1. Principes fondamentaux

### 1.1 Hexagonal Architecture (Ports & Adapters)

Le backend suit strictement l’approche **Ports & Adapters** :

- **domain**

  - modèles métier, invariants, logique pure ;
  - aucune dépendance Spring, JPA, WebClient, Lombok, etc.

- **application**

  - implémentation des cas d’usage (use cases) ;
  - commands / queries (CQRS) ;
  - ports `in` (handlers exposés) et `out` (dépendances externes) ;
  - dépend de `domain`, **jamais** des adapters.

- **infra**
  - implémentations des ports (JPA, WebClient, Redis, Meilisearch, Envers, batch, web, config) ;
  - contient les annotations Spring / JPA / WebClient ;
  - dépend de `application`, _jamais l’inverse_.

### 1.2 Séparation CQRS

- **Commands** : changent l’état, transactionnelles, renvoient un ID / summary / void.
- **Queries** : lecture seule, pas d’effets de bord, renvoient des DTO optimisés pour la vue.

---

## 2. Organisation des packages

Le backend est organisé en deux top-level packages sous `com.tchalanet.server` :

- **`core`** : bounded contexts critiques (draw, sales, payout, limits, tenant, etc.) utilisant une architecture hexagonale stricte.
- **`features`** : vertical slices pour les fonctionnalités UI-oriented et non-critiques (dashboards, public pages, reports, news, etc.).

### 2.1 Core (Architecture Hexagonale)

Pour un bounded context `<bc>` dans `core` (ex. `draw`, `sales`) :

```text
com.tchalanet.server.core.<bc>
 ├── domain
 │     ├── model        // entités, agrégats, value objects
 │     ├── service      // services métier purs (optionnel)
 │     └── exception    // exceptions métier
 │
 ├── application
 │     ├── command
 │     │     ├── model     // *Command records
 │     │     └── handler   // implémentations *CommandHandler (pas d'interfaces)
 │     ├── query
 │     │     ├── model     // *Query records
 │     │     └── handler   // implémentations *Handler (pas d'interfaces)
 │     └── port
 │           ├── in        // interfaces génériques CommandHandler / QueryHandler / VoidCommandHandler
 │           └── out       // ports vers DB, services externes, etc.
 │
 └── infra
       ├── persistence     // entités JPA, repositories Spring, adapters persistence
       ├── external        // WebClient / HTTP / messagerie
       ├── web             // controllers REST, aspects, DTO HTTP
       ├── batch           // scheduled jobs, tasklets
       └── config          // config technique (AuditorAware, Jackson, WebClient, etc.)
```

**Règle importante :**

- Les **ports ne vont jamais dans `domain`**.  
  Ils vivent toujours sous `application.port.in` / `application.port.out`.
- Dans `core`, les handlers sont des **implémentations directes** des interfaces génériques (`CommandHandler`, `VoidCommandHandler`, `QueryHandler`), sans interfaces spécifiques par use case.

### 2.2 Features (Vertical Slices)

Pour une feature `<feature>` dans `features` (ex. `dashboard`, `reports`) :

```text
com.tchalanet.server.features.<feature>
 ├── controller(s)       // REST controllers, DTOs
 ├── service(s)          // orchestrators, business logic
 ├── repository          // optional, non-critical CRUD
 └── mapper              // MapStruct mappers
```

Les features composent les use cases de `core` et n'implémentent pas de logique métier critique.

---

## 3. Commands & Queries

### 3.1 Commands

- Doivent être des **records Java**.
- Nom : `CreateDrawCommand`, `RecordAuditEventCommand`, `PayTicketCommand`, etc.
- Contiennent les paramètres métiers **déjà validés au niveau “shape”** (types de base).

Exemple :

```java
public record CreateDrawCommand(
    UUID tenantId,
    String channelCode,
    LocalDate scheduledDate
) {}
```

Handler (port d’entrée) :

```java
public interface CreateDrawCommandHandler extends CommandHandler<CreateDrawCommand, UUID> {
}
```

Implémentation :

```java
@UseCase
public class CreateDrawCommandHandler implements CommandHandler<CreateDrawCommand, UUID> {

    private final DrawWriterPort drawWriter;
    private final DrawChannelReaderPort drawChannelReader;

    @Override
    @TchTx
    public UUID handle(CreateDrawCommand command) {
        // 1. Charger le channel
        // 2. Vérifier les invariants métier
        // 3. Construire l’agrégat Draw
        // 4. Persister via DrawWriterPort
        // 5. Retourner l’ID
        return /* new draw id */;
    }
}
```

Pour les commands qui ne retournent rien (void), utiliser `VoidCommandHandler<C>` :

```java
public interface ArchiveDrawCommandHandler extends VoidCommandHandler<ArchiveDrawCommand> {
}
```

Implémentation :

```java
@UseCase
public class ArchiveDrawCommandHandler implements VoidCommandHandler<ArchiveDrawCommand> {

    @Override
    @TchTx
    public void handle(ArchiveDrawCommand command) {
        // logique métier
    }
}
```

### 3.2 Queries

- Doivent aussi être des **records Java**.
- **Aucun effet de bord**.
- Ne dépendent que de ports “reader”.

Exemple :

```java
public record ListUpcomingDrawsQuery(
    UUID tenantId,
    String channelCode,
    int limit
) {}
```

Handler :

```java
public interface ListUpcomingDrawsQueryHandler extends QueryHandler<ListUpcomingDrawsQuery, List<UpcomingDrawDto>> {
}
```

Implémentation :

```java
@UseCase
public class ListUpcomingDrawsHandler implements QueryHandler<ListUpcomingDrawsQuery, List<UpcomingDrawDto>> {

    @Override
    public List<UpcomingDrawDto> handle(ListUpcomingDrawsQuery query) {
        // logique de lecture
    }
}
```

### Mapping between layers (MapStruct)

Pour les mappings entre modèles de différentes couches (ex. `infra.web.model` ↔ `application.command.model` / `application.query.model`, ou `infra.persistence` ↔ `domain.model`) nous recommandons d'utiliser MapStruct :

- créer des interfaces `Mapper` dans `infra.web.mapper` ou `infra.persistence.mapper`.
- préférer MapStruct pour les mappings simples/structurés afin d'avoir du code généré, lisible et testable.
- config MapStruct : `componentModel = "spring"` pour injection auto des mappers.

Exemple :

```java
@Mapper(componentModel = "spring")
public interface DrawWebMapper {
  CreateDrawCommand toCreateDrawCommand(CreateDrawRequest request);
  DrawSummaryResponse toDrawSummaryResponse(Draw draw);
}
```

MapStruct facilite la séparation claire entre DTO HTTP et Commands/Queries et évite les conversions manuelles dispersées.

---

## 4. Ports : in & out

### 4.1 Ports d’entrée (`application.port.in`)

- Interfaces génériques exposées aux adapters (web, batch, messaging).
- Les handlers implémentent directement ces interfaces génériques sans interfaces spécifiques par use case.
- Correspondent aux handlers de commands / queries.

Exemples :

```java
// Dans application.port.in
public interface CommandHandler<C, R> {
    R handle(C command);
}

public interface VoidCommandHandler<C> {
    void handle(C command);
}

public interface QueryHandler<Q, R> {
    R handle(Q query);
}

// Implémentations directes dans application.command.handler / application.query.handler
@UseCase
public class CreateDrawCommandHandler implements CommandHandler<CreateDrawCommand, UUID> { ... }

@UseCase
public class ArchiveDrawCommandHandler implements VoidCommandHandler<ArchiveDrawCommand> { ... }

@UseCase
public class ListDrawsHandler implements QueryHandler<ListDrawsQuery, List<DrawSummary>> { ... }
```

Les controllers REST, aspects, jobs batch **injectent directement les classes concrètes des handlers**.

### 4.2 Ports de sortie (`application.port.out`)

- Interfaces décrivant les besoins d’un use case vis-à-vis de l’extérieur :
  - persistance,
  - services externes,
  - caches, etc.
- Ils sont implémentés exclusivement dans `infra.*`.

Exemples :

```java
public interface DrawReaderPort {
    Optional<Draw> findById(UUID tenantId, UUID drawId);
}

public interface DrawWriterPort {
    Draw save(Draw draw);
}
```

---

## 5. Domaine (domain)

### 5.1 Règles

- Les classes du domaine résident dans `domain.*`.
- Elles **ne dépendent pas** :
  - de Spring (`@Service`, `@Component`, etc.),
  - de JPA (`@Entity`, `@Table`, etc.),
  - de WebClient / HTTP,
  - des adapters.

### 5.2 Rich Domain Model

Les entités doivent encapsuler la logique métier et les invariants.

Exemple :

```java
public final class Draw {

    private final UUID id;
    private final UUID tenantId;
    private final String channelCode;
    private final LocalDate scheduledDate;
    private final LocalDateTime cutoffAt;
    private final DrawStatus status;

    public Draw close(LocalDateTime now) {
        if (status != DrawStatus.SCHEDULED) {
            throw new DrawAlreadyClosedException(id);
        }
        if (now.isBefore(cutoffAt)) {
            throw new DrawTooEarlyToCloseException(id);
        }
        return new Draw(id, tenantId, channelCode, scheduledDate, cutoffAt, DrawStatus.CLOSED);
    }
}
```

Les exceptions associées vivent dans `domain.exception`.

---

## 6. Infra (adapters)

### 6.1 Persistence

- `infra.persistence` contient :
  - entités JPA (`@Entity`),
  - repositories Spring Data,
  - adapters qui implémentent les ports `*ReaderPort` / `*WriterPort`.

Exemple d’adapter :

```java
@Component
@RequiredArgsConstructor
public class DrawPersistenceAdapter implements DrawReaderPort, DrawWriterPort {

    private final DrawSpringRepository jpa;

    @Override
    public Optional<Draw> findById(UUID tenantId, UUID drawId) {
        return jpa.findByIdAndTenantId(drawId, tenantId).map(this::toDomain);
    }

    @Override
    public Draw save(Draw draw) {
        var entity = toEntity(draw);
        var saved = jpa.save(entity);
        return toDomain(saved);
    }
}
```

### 6.2 Web

- `infra.web` :
  - `@RestController` minces,
  - mapping HTTP ↔ Commands / Queries ↔ DTOs.

Les controllers :

- ne contiennent **aucune règle métier**,
- ne parlent **jamais** aux repositories ou WebClient directement,
- appellent uniquement les handlers via ports `in`.

### 6.3 External

- `infra.external` :
  - clients `WebClient` pour API externes,
  - mapping JSON ↔ DTO.

Les détails HTTP **ne fuient pas** vers `application` ou `domain`.

---

## 7. Multi-tenancy & RLS

### 7.1 Colonne tenant & audit

Toutes les tables multi-tenant incluent :

```sql
tenant_id    uuid        NOT NULL,
version      bigint      NOT NULL DEFAULT 0,
created_at   timestamptz NOT NULL DEFAULT now(),
created_by   uuid,
updated_at   timestamptz NOT NULL DEFAULT now(),
updated_by   uuid,
deleted_at   timestamptz
```

- `tenant_id` est **obligatoire** et géré par :
  - RequestContext + hooks JPA (`TenantEntityListener`),
  - règles RLS.

### 7.2 RLS

Les migrations doivent définir des policies RLS typiques :

```sql
ALTER TABLE ticket ENABLE ROW LEVEL SECURITY;

CREATE POLICY ticket_tenant_isolation
  ON ticket
  USING (tenant_id = current_setting('app.tenant')::uuid);
```

Les adapters ne doivent jamais ignorer `tenantId`.

---

## 8. Exemples de domaines (BC)

### 8.1 AccessControl

- Rôle : calcul des permissions d’un user par tenant.
- Ports :
  - in : `CheckUserPermissionsUseCase`, `GetEffectivePermissionsUseCase`.
  - out : `TenantUserDirectoryPort`, `PermissionCatalogPort`.

### 8.2 Audit

- Rôle : traçabilité des actions + révisions Envers.
- Ports :
  - in : `RecordAuditEventCommandHandler`, `ListRecentAuditEventsQueryHandler`, etc.
  - out : `AuditEventReaderPort`, `AuditEventWriterPort`, `RevisionReaderPort`.

### 8.3 Draw / Ticket / Session / TenantConfig / PageModel

- Suivent le même pattern : `domain` riche, `application` en CQRS, `infra` en adapters.

---

## 9. Règles transversales

- Pas de “service layer” anémique.
- Pas de logique métier dans les controllers / adapters.
- Logging :
  - utiliser SLF4J,
  - messages structurés,
  - pas de `System.out.println`.
- Gestion d’erreurs HTTP via `@ControllerAdvice` global :
  - 400 : validations,
  - 403 : accès interdit,
  - 404 : ressources non trouvées,
  - 500 : erreurs inattendues.

### Audit et aspects

- Pour l'audit applicatif, prioriser l'utilisation de l'annotation métier `@AuditLog` (aspect) sur les méthodes de controller / use case où nécessaire. L'aspect doit rester léger : il doit construire un command minimal (ou utiliser `AuditEventFactory`) et déléguer au port d'application d'audit.
- L'audit doit être isolé : le handler d'audit doit écrire dans une transaction REQUIRES_NEW et ne doit jamais faire échouer le flux métier en cas d'erreur d'écriture d'audit (log et swallow).

### Records vs classes

- Préférence pour les **records Java** pour les DTOs immutables (Commands / Queries / simple DTOs) : clarté, immutabilité et concision.
- Lorsque la structure nécessite des comportements ou des annotations (JPA entities, etc.), utiliser des classes ; dans ce cas, l'utilisation de Lombok (`@Getter`, `@Value`, `@Builder`) est autorisée pour réduire le boilerplate.

Cette convention facilite la compréhension : records = données immuables, classes (+ Lombok) = entités/mutations.

---

## 10. Tests

- **Tests domaine** :

  - pas de Spring,
  - instanciation directe des entités / services.

- **Tests application** :

  - use cases testés avec des faux ports / in-memory ports.

- **Tests adapters** :
  - persistence : Testcontainers Postgres,
  - external HTTP : MockWebServer / mocks WebClient,
  - web : `@WebMvcTest` pour les controllers.

---

## 11. Références externes

Ces références sont la base conceptuelle de cette architecture :

- Hexagonal Architecture — Alistair Cockburn  
  https://alistair.cockburn.us/hexagonal-architecture/

- Clean Architecture — Robert C. Martin  
  https://www.oreilly.com/library/view/clean-architecture/9780134494272/

- CQRS Documents — Greg Young  
  https://cqrs.files.wordpress.com/2010/11/cqrs_documents.pdf

- Domain-Driven Design — Eric Evans  
  https://domainlanguage.com/

---

## 12. Priorité du document

En cas de conflit entre ce document et un autre fichier de documentation :

- **ARCHITECTURE.md prévaut** pour :
  - structure des packages,
  - localisation des ports,
  - principes Hexagonal/CQRS,
  - séparation domain / application / infra.
