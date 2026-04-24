---
name: backend-persistence
description: >
  Use when writing Flyway migrations, JPA entities, repositories, soft-delete logic, or any database schema change in tchalanet-server — enforces ddl-auto=validate, UUID scope restrictions, soft-delete patterns, and Flyway naming conventions.
---

# Persistance et Flyway

> ⚠️ **Ce fichier est un résumé actionable pour l'IA.**
> Ne pas éditer ce fichier pour changer une règle — modifier les sources canoniques :
> 👉 `tchalanet-server/docs/conventions/persistence/rls.md`
> 👉 `tchalanet-server/docs/conventions/typed_ids.md`

## Règles fondamentales

- `ddl-auto=validate` **uniquement** — Flyway gère tout le DDL
- ❌ Jamais `ddl-auto=update` ou `ddl-auto=create`
- ❌ `@RepositoryRestResource` interdit — pas de Spring Data REST exposé
- UUID autorisé **uniquement** dans `*JpaEntity` et `*JpaRepository`

---

## Flyway — conventions

### Nommage des migrations

```
V###__short_snake_case_name.sql
```

Exemples : `V040__rls_policies.sql`, `V041__add_outlet_deleted_at.sql`

### Toute modification de schéma = nouvelle migration

Jamais de modification manuelle en base ou de `ALTER TABLE` hors migration.

---

## JPA Entities

```java
// Entity tenantée (hérite BaseTenantEntity)
@Entity
@Table(name = "ticket")
public class TicketJpaEntity extends BaseTenantEntity {
  @Id
  private UUID id;                    // UUID autorisé ici
  private UUID tenantId;              // requis pour RLS
  private String code;
  private Instant deletedAt;          // soft delete
  // ...
}

// Entity non-tenantée (hérite BaseEntity)
@Entity
@Table(name = "result_slot")
public class ResultSlotJpaEntity extends BaseEntity {
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
// Repository read-side (RLS gère le tenant, on filtre seulement le soft-delete)
Optional<TicketJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
```

---

## Repositories

```java
// Spring Data : toujours internal à la couche persistence
public interface TicketJpaRepository extends JpaRepository<TicketJpaEntity, UUID> {
  Optional<TicketJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
  // Pas de findByTenantId — RLS filtre automatiquement
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
- Cache = détail d'implémentation (jamais exposé via les APIs)

---

## Checklist nouvelle table

- [ ] Migration Flyway `V###__<name>.sql`
- [ ] Table tenantée → `tenant_id UUID NOT NULL REFERENCES tenant(id)` + policy RLS
- [ ] `deleted_at TIMESTAMPTZ NULL` si soft delete
- [ ] `created_at`, `updated_at` TIMESTAMPTZ (audit)
- [ ] `version INTEGER` si lock optimiste
- [ ] Entity étend `BaseTenantEntity` ou `BaseEntity`
- [ ] Aucun `UUID.randomUUID()` dans les entities (géré par handler + IdGenerator)
- [ ] Mapper dans `infra/persistence/mapper/`
