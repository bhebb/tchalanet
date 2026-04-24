---
name: persistence-flyway
description: >
  Déclencher pour toute modification de schéma, création de migration, JPA entity,
  repository, ou adapter de persistance. Indispensable si la tâche concerne :
  Flyway, ddl-auto, soft delete, lock optimiste, JPA, Hibernate, BaseTenantEntity,
  BaseEntity, ou tout accès base de données PostgreSQL.
---

# Persistance & Flyway — Tchalanet

## Règles fondamentales

| Règle                          | Détail                                                    |
| ------------------------------ | --------------------------------------------------------- |
| `ddl-auto=validate` uniquement | Flyway gère **tout** le DDL                               |
| UUID                           | Autorisé uniquement dans `*JpaEntity` et `*JpaRepository` |
| Spring Data REST               | Interdit (`@RepositoryRestResource` banni)                |
| Filtres tenant                 | Interdits dans les repositories (RLS fait le travail)     |

## Migrations Flyway — conventions

```sql
-- Nommage obligatoire
V001__create_tickets_table.sql
V002__add_tenant_id_to_tickets.sql
V003__create_draws_table.sql

-- Format : V###__short_snake_case_name.sql
-- ❌ Jamais modifier une migration existante en production
-- ✅ Toujours créer une nouvelle migration pour corriger
```

## Colonnes standard obligatoires

```sql
-- Toutes les tables
version     BIGINT NOT NULL DEFAULT 0        -- lock optimiste Hibernate

-- Tables avec audit
created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()

-- Soft delete
deleted_at  TIMESTAMPTZ NULL                 -- NULL = actif

-- Tables tenantées
tenant_id   UUID NOT NULL                    -- géré par RLS

-- Foreign keys
<ref>_id    UUID NOT NULL / NULL             -- ex: draw_id, outlet_id

-- Conventions SQL
snake_case  -- tables et colonnes toujours en snake_case
```

## JPA Entity — patterns

```java
// ✅ Table tenantée
@Entity
@Table(name = "tickets")
public class TicketJpaEntity extends BaseTenantEntity {
    @Id
    private UUID id;

    private UUID drawId;      // FK snake_case → draw_id en DB

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    @Version
    private Long version;     // lock optimiste (hérité de BaseEntity si présent)
}

// ✅ Table non-tenantée (catalogues globaux, tenant, result_slot)
@Entity
@Table(name = "draw_calendars")
public class DrawCalendarJpaEntity extends BaseEntity {
    @Id
    private UUID id;
    // ...
}
```

## Soft delete — requêtes

```java
// ✅ Toujours filtrer les supprimés côté Java si RLS ne le fait pas
findByIdAndDeletedAtIsNull(UUID id)
findAllByStatusAndDeletedAtIsNull(TicketStatus status)

// ❌ Jamais supprimer physiquement sauf cas admin explicite
```

## JPA Repository — règles

```java
// ✅ Interface Spring Data JPA dans infra/persistence/
public interface TicketJpaRepository extends JpaRepository<TicketJpaEntity, UUID> {
    Optional<TicketJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
    List<TicketJpaEntity> findAllByDrawIdAndDeletedAtIsNull(UUID drawId);
}

// ❌ Pas de @Query JPQL/SQL sauf si absolument nécessaire
// ❌ Pas de filtres tenant (WHERE tenant_id = ?) — RLS s'en charge
// ❌ Pas de @RepositoryRestResource
```

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

## Checklist avant toute migration Flyway

- [ ] Nommage `V###__short_snake_case.sql`
- [ ] `ddl-auto=validate` — ne jamais changer
- [ ] Colonnes standard présentes : `version`, `created_at`, `updated_at`
- [ ] Si table tenantée : `tenant_id` + policy RLS dans la migration
- [ ] Si soft delete : colonne `deleted_at`
- [ ] Jamais modifier une migration existante committée
- [ ] JpaEntity étend `BaseTenantEntity` (tenantée) ou `BaseEntity` (non-tenantée)
- [ ] JpaAdapter implémente les ports de `core/port/out/`
- [ ] Aucun filtre `tenant_id` dans les repositories
