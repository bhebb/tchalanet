# Persistence — Architecture & Bonnes Pratiques (Tchalanet)

## Status

**NORMATIVE**

---

## 1) Rôle de la couche persistence

La persistence est **technique** :

- implémente les ports du domaine
- applique RLS, audit, versioning
- ne contient **aucune logique métier**

---

## 2) BaseEntity (toutes entités)

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Audited
public abstract class AuditableEntity { ... }

@MappedSuperclass
@Audited
public abstract class BaseEntity extends AuditableEntity {
  @Id
  private UUID id;

  @PrePersist
  void prePersistId() { if (id == null) id = UUID.randomUUID(); }
}

```

- `id` est UUID DB-generated (`gen_random_uuid()`)
- `version` = optimistic locking
- `deletedAt` = soft delete

📌 `hibernate.hbm2ddl.auto=validate` — Flyway est la source de vérité.

---

## 3) BaseTenantEntity (entités tenantées)

```java
@MappedSuperclass
@EntityListeners(TenantEntityListener.class)
public abstract class BaseTenantEntity extends BaseEntity {

  @Column(name="tenantId", nullable=false, updatable=false)
  private UUID tenantId;
}
```

- Toute table métier tenantée doit l'étendre
- `tenant_id` est injecté automatiquement

---

## 4) TenantEntityListener (safety tenant)

### @PrePersist

- si `tenantId` absent → injecte depuis `TchRequestContext`
- sinon → respecte la valeur existante (import/batch)

### @PreUpdate

- empêche toute modification cross-tenant

🎯 Protection applicative complémentaire au RLS (fail-fast).

---

## 5) Stratégie d'ID (alignée Typed IDs)

- `UUID` autorisé uniquement en persistence
- Domaine / application / web utilisent wrappers typés
- DB génère les UUID (`DEFAULT gen_random_uuid()`)

### MapStruct — helper commun

Utiliser `CommonIdMapper` :

```java
@Mapper(uses = CommonIdMapper.class)
public interface TicketMapper {
  // ...
}
```

---

## 6) JSON / JSONB

Colonnes `jsonb`

Converters JPA dédiés :

- `MapToJsonConverter`
- `ListToJsonConverter`

❌ Pas de manipulation String manuelle.

---

## 7) Pagination

- Spring `Page<T>` = interne persistence
- API expose uniquement `TchPage<T>`

**Helper canonique** :

```java
TchPage<Dto> page = TchPages.map(jpaPage, mapper::toDto);
```

---

## 8) Envers — Tables `_AUD` (OBLIGATOIRE)

- `@Audited` sur `Entity`
- Tables `_aud` générées automatiquement
- Listener enrichi avec tenant / user / requestId

Si une entité est historisée (Envers), alors :

- la table principale a une table `*_AUD`
- toute modification Flyway de la table principale doit être répercutée sur `*_AUD`

CI: `flyway migrate` + `ddl-auto=validate` doit échouer si `_AUD` n’est pas alignée.

## Touchpoints audit (références)

- Envers revision listener: `core.audit.infra.persistence.envers.TchRevisionListener`
- Spring Data auditor: `core.audit.infra.config.RequestContextAuditorAware`
- Audit métier: `core.audit` (AfterCommit → LogAuditEventCommand)

📌 Audit métier (actions business) = autre doc (`audit.md`).

---

## 9) Flyway

- Toutes les évolutions DB passent par Flyway
- RLS, fonctions, policies incluses
- Aucun schéma implicite Hibernate

---

## 10) Anti-patterns

- ❌ Entité tenantée sans `tenant_id`
- ❌ Relation JPA complexe cross-aggregate
- ❌ Filtrage tenant dans le code
- ❌ `UUID` utilisé hors persistence

---

## 11) Résumé

La persistence applique :

- RLS
- audit
- versioning
- mapping technique

**Le domaine reste pur.**
