# Typed IDs (Wrappers) Policy

> **Status**: NORMATIVE  
> **Applies to**: tchalanet-server (`common/`, `core/`, `catalog/`, `features/`)  
> **Last reviewed**: 2026-01-20

---

## 1) Goal

Prevent UUID mixups, enforce domain boundaries, and make method signatures self-documenting.

**Typed IDs are mandatory everywhere outside persistence.**

---

## 2) Core rule (NON-NEGOTIABLE)

**Outside persistence, NEVER use raw `UUID`.  
Always use typed ID wrappers**:

`TenantId`, `TicketId`, `PayoutId`, `TerminalId`, …

This rule applies to:

- domain models
- commands / queries
- handlers / application services
- controllers (method parameters)
- domain & application events
- application-level DTOs

If you see a `UUID` in these layers, it is a violation.

---

## 3) Where `UUID` is allowed (CLOSED LIST)

Raw `UUID` is allowed **only** in the following places:

- JPA entities (`*JpaEntity`)
- Spring Data repositories / JDBC row mapping
- Flyway migrations / SQL
- low-level infra payloads (external API DTOs) **before mapping**
- low-level infra keys (Redis, cache) **if required**  
  (prefer `id.value()` explicitly)

**As soon as data enters the application or domain, convert to typed IDs immediately.**

---

## 4) Where `UUID` is forbidden

`UUID` is forbidden in:

- `core/**/domain/**`
- `core/**/application/**` (commands, queries, handlers)
- `features/**` orchestration services
- controller method parameters
- domain/application events
- application DTOs

Also forbidden:

- `Optional<UUID>`
- `List<UUID>`
- `Map<UUID, …>`

Outside persistence, these types must never appear.

---

## 5) Canonical wrapper shape (STANDARD)

All ID wrappers **MUST** follow this exact pattern.

```java
package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record NameId(UUID value) {

  public NameId {
    if (value == null) {
      throw new IllegalArgumentException("NameId.value is null");
    }
  }

  public static NameId of(UUID value) {
    return new NameId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static NameId nullableOf(UUID raw) {
    return raw == null ? null : new NameId(raw);
  }

  /** Parse from UUID string input (web/request). */
  public static NameId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("NameId string is required");
    }
    return new NameId(UUID.fromString(raw));
  }
}
```

### MUST / MUST NOT

- **MUST** be a record
- **MUST** contain exactly one field: `UUID value`
- **MUST** validate `value != null` in the compact constructor
- **MUST** provide:
  - `of(UUID)`
  - `nullableOf(UUID)`
  - `parse(String)`
- **MUST NOT** add duplicate accessors (`uuid()`, custom getters, etc.)
- **MAY** keep the default record `toString()` (not standardized)

---

## 6) ID creation policy (NO UUID.randomUUID() SPRAWL)

Rule

Direct calls to UUID.randomUUID() are forbidden outside a single generator implementation.

Creation of new aggregate/entity IDs MUST go through a shared generator,
then be wrapped with XxxId.of(...).

Generator interface (common)

common/types/id/IdGenerator.java

public interface IdGenerator {
UUID newUuid();
}

Default implementation (common)

common/types/id/UuidV4Generator.java

@Component
public class UuidV4Generator implements IdGenerator {

@Override
public UUID newUuid() {
return UUID.randomUUID();
}
}

Usage (application layer ONLY)
var ticketId = TicketId.of(idGenerator.newUuid());
var ticket = Ticket.create(ticketId, /_ ... _/);

7. Domain purity (STRICT)

Domain code MUST NOT:

call UUID.randomUUID()

inject Spring beans

depend on IdGenerator

Domain constructors/factories accept typed IDs as parameters.

Application layer is responsible for ID generation.

✅ Correct

Handler generates TicketId

Domain receives TicketId

❌ Forbidden

Domain calling UUID.randomUUID()

Domain injecting IdGenerator

8. Web layer conversions (Spring MVC)

Controllers MUST accept typed IDs directly in method signatures.

@PathVariable TicketId ticketId
@RequestParam TerminalId terminalId

Spring converters MUST delegate to XxxId.parse(...).

Example converter
@Component
public class StringToTicketIdConverter
implements Converter<String, TicketId> {

@Override
public TicketId convert(String source) {
return TicketId.parse(source);
}
}

Converters live in:

common.web.converter

9. Persistence mapping (MapStruct)

JPA entities store UUID.
Domain & application layers use typed IDs.

MapStruct mappers MUST provide explicit conversions.

default TicketId toTicketId(UUID id) {
return TicketId.nullableOf(id);
}

default UUID fromTicketId(TicketId id) {
return id == null ? null : id.value();
}

No implicit or magic conversions.

### 9.1 Central MapStruct helper (CommonIdMapper)

Le projet fournit un mapper central pour les conversions `UUID <-> XxxId`, utilisable par MapStruct via `uses = CommonIdMapper.class`.

- **Emplacement** : `com.tchalanet.server.common.mapper.CommonIdMapper`
- **Usage** : dans les `@Mapper`, ajouter `uses = { CommonIdMapper.class }`
- **Règle** : les mappings de typed IDs doivent être centralisés ici quand ils sont transverses (`TenantId`, `TicketId`, `PayoutId`, …)

**Exemple** :

```java
@Mapper(componentModel = "spring", uses = { CommonIdMapper.class })
public interface TicketJpaMapper {
  TicketJpaEntity toEntity(Ticket ticket);
  Ticket toDomain(TicketJpaEntity entity);
}
```

---

## 10) AI rules (ENFORCED)

An AI agent contributing to Tchalanet **MUST**:

- Never introduce `UUID` in command/query/event records
- Use typed IDs in controllers and handlers
- Parse IDs via `XxxId.parse` or Spring converters
- Use `IdGenerator` for ID creation
- Keep the domain free of Spring and UUID generation

**Violations are considered architectural errors.**

---

## 11) Touchpoints (reference)

- **Wrappers**: `com.tchalanet.server.common.types.id.*`
- **Converters**: `com.tchalanet.server.common.web.converter.*`
- **Mappers**: `/**/infra/persistence/*Mapper`
- **JPA entities**: `/**/infra/persistence/*JpaEntity`
