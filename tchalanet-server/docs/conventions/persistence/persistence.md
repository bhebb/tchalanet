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

## 4.1) Updates JPA sensibles (NORMATIVE)

Les entités sensibles (`ticket`, `draw`, `draw_result`, `payout`, `terminal`,
`outlet`, `sales_session`, `ledger_entry`, offline sync, etc.) ne doivent pas être
mises à jour via un rebuild détaché `mapper.toEntity(domain)` suivi de `save`.

Règle canonique :

- création : mapping vers une nouvelle entité autorisé ;
- update JPA : charger l'entité managée puis muter les champs autorisés ;
- alternative : SQL explicite avec garde tenant/status/version/idempotency ;
- interdit : transplanter `tenantId`, `version`, `createdAt`, `createdBy` pour faire
  passer un `merge`.

Voir la norme dédiée :
`docs/conventions/persistence/sensitive_jpa_updates.md`.

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

### 9.1) Stratégie pré-go-live (NORMATIVE)

Tant que le projet **n'est pas en production** :

- ❌ **Ne pas créer de nouveau fichier `V*.sql`** pour faire évoluer le schéma
- ✅ **Modifier directement la migration d'origine** (le `CREATE TABLE`, l'index, le trigger, la policy RLS, le seed)
- ✅ Tout `ALTER TABLE` récent (patch) doit être **absorbé** dans le `CREATE TABLE` du fichier original puis le patch supprimé

**Pourquoi :** la DB est recréée à chaque déploiement de dev/stage. Multiplier les `V*` patches fragmente la lecture du schéma sans bénéfice et nous ferait arriver en prod avec 100+ migrations.

**Cible structure (pré-go-live)** :

```
V001            extensions + RLS helpers
V100-V107       schéma core, audit, technical, indexes, triggers, RLS, permissions, batch
V108            vues read-model (fichier dédié)
V200-V209       seeds
```

**Mapping des évolutions vers le fichier d'origine** :

| Type de changement                               | Va dans               |
| ------------------------------------------------ | --------------------- |
| Nouvelle colonne, nouveau type, contrainte CHECK | `V100` (CREATE TABLE) |
| Colonne sur table `*_aud`                        | `V101`                |
| Nouvel index                                     | `V103`                |
| Nouveau trigger                                  | `V104`                |
| Nouvelle policy RLS                              | `V105`                |
| Nouvelle vue / modification vue                  | `V108`                |
| Donnée seed                                      | `V20x` correspondant  |

### 9.2) Règle agent : confirmation avant création d'un fichier `V*.sql`

**Avant de créer tout nouveau fichier de migration**, l'agent doit :

1. **Vérifier d'abord** si l'évolution peut s'absorber dans une migration existante (cas pré-go-live)
2. **Confirmer explicitement avec l'utilisateur** avant de créer un nouveau `V*.sql`
3. Mentionner si on est en pré-go-live ou post-go-live pour décider de la stratégie

Cette règle bascule **après go-live** : à ce moment, plus jamais de modification rétroactive d'une migration déjà appliquée en production. Uniquement de nouvelles migrations forward-only.

### 9.3) Vues read-model et tables (NORMATIVE)

Les vues SQL dépendent des tables qu'elles agrègent. **À chaque modification d'une table**, l'agent doit systématiquement vérifier et mettre à jour les vues qui s'en servent.

**Fichier dédié** : `V108__create_read_views.sql` (pré-go-live).

**Vues actuelles** :

- `v_ticket_summary` — dépend de `ticket`, `terminal`, `outlet`, `draw`, `draw_channel`, `result_slot`, `draw_result`
- `v_ticket_print` — dépend de `ticket`, `ticket_line`, et tables liées
- `v_draw_summary` — dépend de `draw`, `draw_channel`, `result_slot`, `draw_result`

**Checklist PR** lorsqu'on touche une table : lister explicitement les vues impactées et les mettre à jour dans la même PR.

**Validation** : `flyway clean && flyway migrate` doit passer ; `ddl-auto=validate` doit passer.

### 9.4) Synchronisation tables ↔ entités ↔ audit (NORMATIVE)

Toute modification de table SQL doit être propagée à l'ensemble de la stack pour éviter toute désynchro :

1. **Entité JPA** correspondante (`@Entity` + `@Column`, types, contraintes)
2. **Mapper / projection** (DTO, `XxxView`, `XxxSummaryView`)
3. **Table d'audit `_AUD`** si l'entité est `@Audited` (mêmes colonnes, mêmes types)
4. **Vue read-model** dans V108 si elle référence la colonne
5. **Seeds** (V200-V209) si la colonne est NOT NULL ou impacte la donnée initiale

**Principe :** la cohérence est validée par `ddl-auto=validate` au démarrage Spring + tests d'archi (`FlywayAuditAlignmentArchTest`).

**Tolérance temporaire :** les ajustements entités/projections peuvent suivre dans une PR adjacente quand la refonte est volumineuse, **mais ils doivent absolument être faits** — jamais laissés en dette. Une désynchro `audit_event` / `_aud` / vue lue produit des bugs silencieux à l'exécution.

**Checklist obligatoire à chaque PR qui touche un `CREATE TABLE`** :

- [ ] Entité JPA mise à jour
- [ ] Mappers / projections (`XxxView`, DTOs) mis à jour
- [ ] Table `_aud` mise à jour si `@Audited`
- [ ] Vue read-model V108 vérifiée
- [ ] Seeds (V200-V209) cohérents
- [ ] `./mvnw verify` passe (`ddl-auto=validate` + tests d'archi inclus)

---

## 10) Anti-patterns

- ❌ Entité tenantée sans `tenant_id`
- ❌ Relation JPA complexe cross-aggregate
- ❌ Filtrage tenant dans le code
- ❌ `UUID` utilisé hors persistence
- ❌ Update d'une entité sensible via `mapper.toEntity(domain)` + `save`
- ❌ Création silencieuse quand un update avec id existant attendu ne trouve pas la ligne

---

## 11) Résumé

La persistence applique :

- RLS
- audit
- versioning
- mapping technique

**Le domaine reste pur.**
