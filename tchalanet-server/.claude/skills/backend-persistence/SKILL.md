---
name: backend-persistence
description: Use when writing Flyway migrations, JPA entities, repositories, soft-delete logic, or any database schema change in tchalanet-server — enforces ddl-auto=validate, UUID scope restrictions, standard column conventions, soft-delete patterns, JpaAdapter pattern, and Flyway naming conventions.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Persistance et Flyway

> ⚠️ **Ce fichier est un résumé actionable pour l'IA.**
> Ne pas éditer ce fichier pour changer une règle — modifier les sources canoniques :
> 👉 `tchalanet-server/docs/conventions/persistence/rls.md`
> 👉 `tchalanet-server/docs/conventions/typed_ids.md`

## Règles fondamentales

| Règle                          | Détail                                                    |
| ------------------------------ | --------------------------------------------------------- |
| `ddl-auto=validate` uniquement | Flyway gère **tout** le DDL                               |
| UUID                           | Autorisé uniquement dans `*JpaEntity` et `*JpaRepository` |
| Spring Data REST               | Interdit (`@RepositoryRestResource` banni)                |
| Filtres tenant                 | Interdits dans les repositories (RLS fait le travail)     |

---

## Flyway — conventions

### Nommage des migrations

```
V###__short_snake_case_name.sql
```

Exemples : `V040__rls_policies.sql`, `V041__add_outlet_deleted_at.sql`

```sql
-- ❌ Jamais modifier une migration existante en production
-- ✅ Toujours créer une nouvelle migration pour corriger
```

### Toute modification de schéma = nouvelle migration

Jamais de modification manuelle en base ou de `ALTER TABLE` hors migration.

---

## Colonnes standard obligatoires

```sql
-- Toutes les tables
version     BIGINT NOT NULL DEFAULT 0         -- lock optimiste Hibernate

-- Tables avec audit
created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()

-- Soft delete
deleted_at  TIMESTAMPTZ NULL                  -- NULL = actif

-- Tables tenantées
tenant_id   UUID NOT NULL                     -- géré par RLS

-- Foreign keys
<ref>_id    UUID NOT NULL / NULL              -- ex: draw_id, outlet_id

-- Conventions SQL
snake_case  -- tables et colonnes toujours en snake_case
```

---

## JPA Entities

```java
// Entity tenantée (hérite BaseTenantEntity)
@Entity
@Table(name = "tickets")
public class TicketJpaEntity extends BaseTenantEntity {
    @Id
    private UUID id;

    private UUID drawId;      // FK snake_case → draw_id en DB

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    @Version
    private Long version;     // lock optimiste

    private Instant deletedAt; // soft delete
}

// Entity non-tenantée (hérite BaseEntity)
@Entity
@Table(name = "draw_calendars")
public class DrawCalendarJpaEntity extends BaseEntity {
    @Id
    private UUID id;
    // ...
}
```

---

## Soft delete

- Colonne : `deleted_at TIMESTAMPTZ NULL`
- Ligne active : `deleted_at IS NULL`
- Ligne supprimée : `deleted_at IS NOT NULL`
- Existence checks : méthode `...AndDeletedAtIsNull`
- Commands : gérer la logique "resurrect / recreate" si nécessaire

```java
// ✅ Toujours filtrer les supprimés côté Java si RLS ne le fait pas
Optional<TicketJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
findAllByStatusAndDeletedAtIsNull(TicketStatus status);

// ❌ Jamais supprimer physiquement sauf cas admin explicite
```

---

## JPA Repository — règles

```java
public interface TicketJpaRepository extends JpaRepository<TicketJpaEntity, UUID> {
    Optional<TicketJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
    List<TicketJpaEntity> findAllByDrawIdAndDeletedAtIsNull(UUID drawId);
    // Pas de findByTenantId — RLS filtre automatiquement
    // ❌ Pas de @Query JPQL/SQL sauf si absolument nécessaire
    // ❌ Pas de @RepositoryRestResource
}
```

---

## JpaAdapter — pattern

```java
// ✅ L'adapter fait UUID ↔ TypedId + mapping domain
@Component
@RequiredArgsConstructor
public class TicketJpaAdapter implements TicketReaderPort, TicketWriterPort {

    private final TicketJpaRepository repo;
    private final TicketPersistenceMapper mapper;

    @Override
    public Optional<Ticket> findById(TicketId id) {
        return repo.findByIdAndDeletedAtIsNull(id.value()) // UUID ici
                   .map(mapper::toDomain);
    }

    @Override
    public Ticket save(Ticket ticket) {
        var entity = mapper.toEntity(ticket);
        return mapper.toDomain(repo.save(entity));
    }
}
```

---

## Mapping Entity ↔ Domain

```java
@Mapper(componentModel = "spring", uses = { CommonIdMapper.class })
public interface TicketPersistenceMapper {
    TicketJpaEntity toEntity(Ticket domain);
    Ticket toDomain(TicketJpaEntity entity);
}
```

- MapStruct + `CommonIdMapper` pour tous les typed IDs
- Pas de mapping en dehors de `infra/persistence/mapper/`
- `JpaEntity` ne doit **jamais** fuir en dehors de `infra/persistence/`

---

## Cache

- Read side : `@Cacheable`
- Write side : `@CacheEvict` (après chaque mutation)
- Nommage catalog : `catalog:<name>:active`, `catalog:<name>:by_id`, `catalog:<name>:by_key`

---

## Checklist nouvelle table

- [ ] Migration Flyway `V###__short_snake_case.sql`
- [ ] `ddl-auto=validate` — ne jamais changer
- [ ] Colonnes standard présentes : `version`, `created_at`, `updated_at`
- [ ] Si table tenantée : `tenant_id` + policy RLS dans la migration
- [ ] Si soft delete : colonne `deleted_at`
- [ ] Entity étend `BaseTenantEntity` (tenantée) ou `BaseEntity` (non-tenantée)
- [ ] Aucun `UUID.randomUUID()` dans les entities (géré par handler + IdGenerator)
- [ ] JpaAdapter implémente les ports de `core/port/out/`
- [ ] Mapper dans `infra/persistence/mapper/`
- [ ] Aucun filtre `tenant_id` dans les repositories
- [ ] Jamais modifier une migration existante committée
