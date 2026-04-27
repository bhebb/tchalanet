---
name: common-module
description: Use when creating anything in the common/ layer or when a cross-cutting technical class is needed — event bus, TchContext, ApiResponse, TchPage, shared typed IDs, IdGenerator, error handling, cache infrastructure, or transverse utilities.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Module Common — Tchalanet

## Rôle

Infrastructure **technique transversale** uniquement.
Ne contient aucune logique métier — jamais.

## Ce que contient common/

```
common/
├─ bus/           ← CommandBus, QueryBus, DomainEventPublisher
├─ context/       ← TchContext, TchContextFilter, RlsAwareDataSource
├─ errors/        ← exceptions base, ProblemDetail handlers, error codes
├─ cache/         ← infrastructure cache (configuration Caffeine/Redis)
├─ types/
│  ├─ id/         ← typed IDs (TenantId, TicketId, PayoutId, TerminalId, ...)
│  └─ money/      ← Amount, Currency wrappers
├─ web/
│  ├─ response/   ← ApiResponse<T>, TchPage<T>
│  └─ converter/  ← StringToXxxIdConverter (Spring MVC)
└─ util/          ← helpers techniques purs (date, string, etc.)
```

## Ce qui est INTERDIT dans common/

```java
// ❌ Logique métier
// ❌ Référence à catalog/, core/, features/
import com.tchalanet.server.core.*;    // interdit
import com.tchalanet.server.catalog.*; // interdit
import com.tchalanet.server.features.*; // interdit

// ❌ Domain events métier (les events dans common/ sont techniques uniquement)
// ❌ Règles business, invariants, exceptions métier spécifiques
```

## ApiResponse<T> — usage obligatoire

```java
// ✅ Tous les controllers JSON retournent ApiResponse<T>
ApiResponse.ok(result)           // 200
ApiResponse.created(newId)       // 201
ApiResponse.noContent()          // 204

// ✅ Collections paginées
ApiResponse.ok(TchPage.of(springPage, mapper::toResponse))

// ✅ Erreurs — ProblemDetail géré automatiquement
// Ne jamais wrapper une erreur dans ApiResponse
```

## TchContext — contrat

```java
// Accessible en lecture dans l'infra et les schedulers
TchContext.current()             // contexte courant (ou throws si absent)
TchContext.currentOrEmpty()      // Optional<TchContext>
TchContext.set(ctx)              // schedulers uniquement
TchContext.clear()               // toujours dans le finally des schedulers

// Résolu par TchContextFilter pour les requêtes HTTP
// Ne jamais construire manuellement dans domain/ ou application/
```

## IdGenerator — contrat

```java
// Injecté dans l'application layer uniquement
@Component
public class SomeCommandHandler {
    private final IdGenerator idGenerator;

    public void handle(CreateXxxCommand cmd) {
        var id = XxxId.of(idGenerator.newUuid()); // ✅
    }
}

// ❌ Jamais UUID.randomUUID() dans domain/ ou application/
```

## CommonIdMapper — MapStruct

Toujours inclure dans les mappers qui manipulent des typed IDs transverses :

```java
@Mapper(componentModel = "spring", uses = { CommonIdMapper.class })
public interface TicketMapper { ... }
```

## Typed IDs dans common/types/id/

Chaque wrapper suit le pattern standard :

```java
public record TenantId(UUID value) {
    public TenantId { Objects.requireNonNull(value, "TenantId ne peut pas être null"); }
    public static TenantId of(UUID v)       { return new TenantId(v); }
    public static TenantId nullableOf(UUID v) { return v == null ? null : new TenantId(v); }
    public static TenantId parse(String s)  { return new TenantId(UUID.fromString(s)); }
}
```

## Checklist avant d'ajouter dans common/

- [ ] La classe est-elle vraiment **technique** et transversale ?
- [ ] Elle n'a aucune référence vers `catalog/`, `core/`, `features/`
- [ ] Si c'est un typed ID → suivre le pattern record standard
- [ ] Si c'est un converter MVC → nommer `StringToXxxIdConverter`
- [ ] Si c'est de la logique métier → aller dans `core/` ou `catalog/`
