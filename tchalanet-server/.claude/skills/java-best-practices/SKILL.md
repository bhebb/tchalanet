---
name: java-best-practices
description: Use when reviewing or writing Java code in tchalanet-server — covers records, sealed classes, pattern matching, Optional, streams, concurrency, immutability, clean code, and modern Java 25 best practices.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Java Best Practices — Tchalanet

> Java 25 — utiliser les features modernes du langage

## Records — utiliser partout où c'est immutable

```java
// ✅ Commands, Queries, DTOs, Value Objects = records
public record SellTicketCommand(TicketId ticketId, TenantId tenantId, Amount amount) {}
public record MoneyAmount(BigDecimal value, Currency currency) {
    public MoneyAmount {
        Objects.requireNonNull(value);
        if (value.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Amount cannot be negative");
    }
}

// ❌ Pas de class mutable pour des données immutables
```

## Sealed classes — modéliser les variants finis

```java
// ✅ Pour les états et résultats
public sealed interface PayoutResult
    permits PayoutResult.Success, PayoutResult.Failed, PayoutResult.Pending {

    record Success(PayoutId id, Amount amount) implements PayoutResult {}
    record Failed(String reason) implements PayoutResult {}
    record Pending(PayoutId id) implements PayoutResult {}
}

// Utilisation avec pattern matching
switch (result) {
    case PayoutResult.Success s -> processSuccess(s.amount());
    case PayoutResult.Failed f  -> handleFailure(f.reason());
    case PayoutResult.Pending p -> scheduleRetry(p.id());
}
```

## Pattern matching — préférer à instanceof classique

```java
// ✅ Pattern matching Java 21+
if (obj instanceof TicketJpaEntity entity) {
    return entity.getStatus();
}

// ✅ Switch expressions
String label = switch (status) {
    case PENDING  -> "En attente";
    case PLACED   -> "Placé";
    case CANCELLED -> "Annulé";
};
```

## Optional — usage correct

```java
// ✅ Pour les valeurs potentiellement absentes
Optional<Ticket> findById(TicketId id);

// ✅ Chaîner proprement
return ticketRepo.findById(id)
    .map(mapper::toDomain)
    .orElseThrow(() -> new TicketNotFoundException(id));

// ❌ Ne jamais retourner null — toujours Optional ou exception
// ❌ Ne jamais faire optional.get() sans vérification
// ❌ Ne jamais Optional comme paramètre de méthode
```

## Streams — bonnes pratiques

```java
// ✅ Préférer les streams aux boucles pour les transformations
var activeTickets = tickets.stream()
    .filter(t -> t.status() == TicketStatus.PLACED)
    .map(mapper::toResponse)
    .toList();  // Java 16+ — immuable

// ❌ Éviter les streams pour les side-effects (préférer forEach classique)
// ❌ Pas de streams imbriqués complexes — extraire en méthodes
```

## Gestion des nulls

```java
// ✅ Objects.requireNonNull dans les constructeurs critiques
public TicketId(UUID value) {
    this.value = Objects.requireNonNull(value, "TicketId value must not be null");
}

// ✅ @NonNull/@Nullable de Lombok pour documenter l'intent
// ❌ Jamais retourner null depuis une méthode publique
```

## Immutabilité — préférer par défaut

```java
// ✅ Collections immuables
List.of(a, b, c)
Map.of(k1, v1, k2, v2)
Set.copyOf(existing)

// ✅ Champs final partout possible
private final TicketId id;
private final TenantId tenantId;
```

## Clean code — règles simples

```java
// Méthodes courtes : max ~20 lignes, une seule responsabilité
// Noms explicites : findActiveTicketsByDraw() pas getTickets()
// Pas de commentaires qui expliquent le QUOI — le code doit être lisible
// Commentaires uniquement pour expliquer le POURQUOI (décision non-évidente)

// ✅ Early return pour réduire l'imbrication
public void process(Ticket ticket) {
    if (ticket == null) throw new IllegalArgumentException("...");
    if (ticket.isCancelled()) return;
    // logique principale
}
```

## Checklist review Java

- [ ] Données immuables → `record` plutôt que class
- [ ] Variants finis → `sealed interface`
- [ ] Pas de `null` retourné — `Optional` ou exception
- [ ] Collections immuables (`List.of`, `Set.copyOf`)
- [ ] Pattern matching au lieu de `instanceof` + cast
- [ ] Méthodes < 20 lignes, une responsabilité
- [ ] `Objects.requireNonNull` dans les constructeurs critiques
