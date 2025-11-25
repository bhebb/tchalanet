# Persistence — Tchalanet

## 1. Vision

La couche persistence est _technique_ et implémente les ports du domaine.  
Elle repose sur :

- `BaseEntity` : colonnes techniques
- `BaseTenantEntity` : entités tenantées
- Envers : historique automatique
- JSONB + converters
- Flyway comme source de vérité

---

## 2. BaseEntity

```java
@MappedSuperclass
@Audited
public abstract class BaseEntity {

  @Id @GeneratedValue
  private UUID id;

  @Version
  private long version;

  @Column(nullable=false, updatable=false)
  private Instant createdAt;

  @Column(nullable=false)
  private Instant updatedAt;

  private UUID createdBy;
  private UUID updatedBy;

  private Instant deletedAt;

  @PrePersist void prePersist() {
    createdAt = updatedAt = Instant.now();
  }

  @PreUpdate void preUpdate() {
    updatedAt = Instant.now();
  }
}
```

---

## 3. BaseTenantEntity

```java
@MappedSuperclass
public abstract class BaseTenantEntity extends BaseEntity {

  @Column(name="tenant_id", nullable=false, updatable=false)
  private UUID tenantId;
}
```

---

## 4. Converters JSONB

```java
@Converter
public class MapToJsonConverter implements AttributeConverter<Map<String,Object>, String> {
  ...
}
```

---

## 5. Repositories

### Port (domaine)

```java
public interface ThemeRepository {
   Optional<Theme> findById(UUID id);
   Theme save(Theme t);
}
```

### Adapter JPA

```java
@Repository
public class ThemeJpaRepositoryAdapter implements ThemeRepository {

   private final SpringThemeJpaRepo repo;
   private final ThemeMapper mapper;

   @Override
   public Optional<Theme> findById(UUID id) {
      return repo.findById(id).map(mapper::toDomain);
   }
}
```

### Spring JPA Repo

```java
public interface SpringThemeJpaRepo
   extends JpaRepository<ThemeJpaEntity, UUID> {}
```

---

## 6. Envers

- `revinfo` contient : rev, timestamp, tenant, user
- `_aud` générées automatiquement
- Ecouteur : `TchRevisionListener`

---

## 7. Rôle de Flyway

- Toute création/modification de schéma passe par Flyway
- `hbm2ddl=validate` assure la cohérence
- Migration versionnée = source de vérité

---

## 8. Bonnes pratiques

✔ Jamais accéder à une entité hors du tenant  
✔ Préférer les Converters JSONB  
✔ Eviter les relations complexes (favoriser IDs)  
✔ Tester avec Testcontainers pour vérifier RLS

# Persistence — architecture et bonnes pratiques

Ce document décrit la couche persistence du projet Tchalanet : stratégie d'IDs, entités de base, conversion JSON, version technique, migrations Flyway et bonnes pratiques pour travailler avec JPA/Hibernate.

## Vue d'ensemble

- Toutes les entités JPA partagent une super-classe `BaseEntity` (localisée dans `com.tchalanet.server.common.infra.persistence`) qui contient les colonnes techniques : `id` (UUID), `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted_at`, `version` (optimistic-lock).
- Les entités liées à un tenant étendent `BaseTenantEntity` (ajoute `tenant_id`). Certaines tables restent globales (ex : `plan`) et ne présentent pas `tenant_id`.
- Les migrations Flyway se trouvent dans `src/main/resources/db/migration`. Les scripts V2, V3, V4... construisent les tables et les fonctions RLS.

## Stratégie d'identifiants (UUID)

- La base est conçue pour utiliser `uuid` (`gen_random_uuid()` au niveau DB) pour toutes les PK.
- Côté JPA, la pratique actuelle est d'utiliser `UUID` comme type de champ : soit Hibernate gère la génération, soit la DB génère une valeur par défaut. Il faut garder une seule stratégie — pour ce projet nous recommandons :
  - DB génère par défaut (`DEFAULT gen_random_uuid()` dans les migrations) et JPA laisse l'ID à null avant persist ; Hibernate récupère la valeur après INSERT.
  - Alternative (optionnelle) : générer l'UUID côté application via `UUID.randomUUID()` avant persist. Choisir l'une ou l'autre et l'appliquer partout.

## BaseEntity / BaseTenantEntity

- `BaseEntity` (fichier : `common/infra/persistence/BaseEntity.java`) contient :
  - `UUID id`
  - `Instant createdAt`, `Instant updatedAt`
  - `UUID createdBy`, `UUID updatedBy` (alimentés par `AuditorAware<UUID>`)
  - `Instant deletedAt` (soft delete)
  - `long version` (annoté `@Version` pour optimistic locking)
  - `@PrePersist` / `@PreUpdate` pour remplir timestamps si manquants
- `BaseTenantEntity` ajoute `UUID tenantId` et est annoté avec `@EntityListeners(TenantEntityListener.class)` pour prefixer automatiquement `tenantId` lors du `prePersist`.

## JSON / JSONB columns

- Pour stocker maps et listes JSONB, on fournit des converters JPA :
  - `MapToJsonConverter` pour `Map<String,Object>`
  - `ListToJsonConverter` pour `List<String>` (ex: `app_user.roles`)
- Les champs JPA doivent préciser `columnDefinition = "jsonb"` et `@Convert(converter = ...)`.

## Version technique (optimistic locking)

- `BaseEntity.version` (type `long`) est présent pour toutes les entités héritant de `BaseEntity`.
- Les migrations ont été alignées pour ajouter `version bigint NOT NULL DEFAULT 0` sur les tables tenant-scoped et globales qui héritent logiquement de `BaseEntity`.
- En pratique : use cases qui veulent vérifier l'état doivent passer l'`expectedVersion` au endpoint (ou utiliser `@Version` via JPA flush / exception `OptimisticLockingFailureException`).

## Envers (historique d'entité)

- Hibernate Envers est activé via `spring.jpa.properties.*` (cf `application.yaml` : `org.hibernate.envers.*`) et `@Audited` sur les classes de base.
- Le listener de revision (`TchRevisionListener`) est configuré pour enrichir la table de révision avec des métadonnées (tenant, user, requestId). Important : Envers instancie son listener via Hibernate, donc le listener doit lire le contexte (ex. `RequestContextHolder`) de façon autonome (lecture statique), pas via injection constructor Spring.

## Migrations et Flyway

- Les scripts Flyway sont dans `src/main/resources/db/migration`.
- Ordre important : V2 (helpers RLS), V3 (tenant & plan), ... V8 (politiques RLS), V9 (envers) et scripts de seed (V10+).
- Si tu recrées la base localement, exécute Flyway (ou la commande d'init) et vérifie que `pgcrypto` / `pgcrypto` extension est activée (pour `gen_random_uuid()`).

## Recommandations pratiques

- Toujours définir explicitement `tenantId` via le `RequestUserContextFilter` ; laisser `TenantEntityListener` remplir les entités si la valeur est manquante.
- Utiliser `AuditEventFactory` + Envers pour l’historique ; préférer un audit `afterCommit` si tu veux un `audit_event` business distinct.
- Pour les colonnes JSONB, préférer les converters fournis (MapToJsonConverter, ListToJsonConverter) plutôt que manipuler manuellement les String.
- Garder `hibernate.hbm2ddl.auto=validate` en dev/CI et appliquer Flyway pour la création/synchronisation du schéma.
