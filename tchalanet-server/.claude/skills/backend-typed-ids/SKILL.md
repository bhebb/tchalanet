---
name: backend-typed-ids
description: Use when writing or reviewing Java code in tchalanet-server that involves IDs, UUIDs, domain models, commands, queries, handlers, controllers, or mappers — enforces typed ID wrappers (TicketId, TenantId, etc.) and bans raw UUID outside the persistence layer.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Typed IDs — Wrappers obligatoires

> ⚠️ **Ce fichier est un résumé actionable pour l'IA.**
> Ne pas éditer ce fichier pour changer une règle — modifier la source canonique :
> 👉 `tchalanet-server/docs/conventions/typed_ids.md`

## Règle fondamentale (NON-NÉGOCIABLE)

**UUID brut interdit en dehors de la persistance.**  
Utiliser des wrappers typés partout ailleurs : `TenantId`, `TicketId`, `PayoutId`, `TerminalId`, `OutletId`, ...

---

## Pattern canonique du wrapper

```java
package com.tchalanet.server.common.types.id;

public record TicketId(UUID value) {

  public TicketId {
    if (value == null) throw new IllegalArgumentException("TicketId.value is null");
  }

  public static TicketId of(UUID value)         { return new TicketId(value); }
  public static TicketId nullableOf(UUID raw)   { return raw == null ? null : new TicketId(raw); }
  public static TicketId parse(String raw)      { return new TicketId(UUID.fromString(raw)); }
}
```

**MUST** :

- Être un `record`
- Contenir exactement un champ `UUID value`
- Valider `value != null` dans le compact constructor
- Fournir `of(UUID)`, `nullableOf(UUID)`, `parse(String)`

**MUST NOT** : ajouter des accesseurs redondants (`uuid()`, getters custom).

---

## Où UUID est autorisé (LISTE FERMÉE)

- `*JpaEntity` — entités JPA uniquement
- `*JpaRepository` — repositories Spring Data
- Migrations Flyway / SQL
- DTOs d'API externes **avant** mapping
- Clés Redis (si nécessaire, préférer `id.value()` explicitement)

---

## Où UUID est interdit

- `core/**/domain/**`
- `core/**/application/**` (commands, queries, handlers)
- `features/**` (orchestration, controllers)
- Domain/application events
- Application DTOs et read models

Également interdit : `Optional<UUID>`, `List<UUID>`, `Map<UUID, …>` hors persistence.

---

## Génération d'IDs

```java
// Injecter IdGenerator dans l'application layer (handlers uniquement)
// Ne JAMAIS appeler UUID.randomUUID() dans le domaine

var ticketId = TicketId.of(idGenerator.newUuid());
var ticket = Ticket.create(ticketId, ...);
```

Le domaine **reçoit** les typed IDs en paramètre — il ne les génère jamais.

---

## Conversions Spring MVC

```java
// Controllers : typed IDs directement dans les signatures
@GetMapping("/{id}")
public ApiResponse<TicketResponse> get(@PathVariable TicketId id) { ... }

// Converter (common.web.converter)
@Component
public class StringToTicketIdConverter implements Converter<String, TicketId> {
  public TicketId convert(String source) { return TicketId.parse(source); }
}
```

---

## Mapping MapStruct

```java
// Toujours via CommonIdMapper (com.tchalanet.server.common.mapper.CommonIdMapper)
@Mapper(componentModel = "spring", uses = { CommonIdMapper.class })
public interface TicketJpaMapper {
  TicketJpaEntity toEntity(Ticket ticket);
  Ticket toDomain(TicketJpaEntity entity);
}

// Helpers dans CommonIdMapper
default TicketId toTicketId(UUID id)   { return TicketId.nullableOf(id); }
default UUID fromTicketId(TicketId id) { return id == null ? null : id.value(); }
```

Pas de conversions implicites ou magiques.

---

## Checklist rapide

- [ ] Aucun `UUID` dans les signatures des commands/queries
- [ ] Aucun `UUID` dans les paramètres des controllers
- [ ] `IdGenerator` injecté dans les handlers (pas dans le domaine)
- [ ] `StringToXxxIdConverter` créé pour chaque nouveau typed ID web-facing
- [ ] `CommonIdMapper` utilisé dans les mappers MapStruct
- [ ] Domaine reçoit des typed IDs, ne les génère pas
