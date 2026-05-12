# LimitPolicy — Mappers & Implémentation

**Source:** `core.limitpolicy.infra.persistence.assignment.mapper`

## Vue d'ensemble

Les mappers assurent la conversion bidirectionnelle entre :

- **Domaine** : Modèles métier (`LimitAssignment`, `LimitScopeRef`)
- **Persistence** : Entités JPA (`LimitAssignmentJpaEntity`)

---

## 1. LimitAssignmentMapper

**Fichier:** `LimitAssignmentMapper.java`  
**Responsabilités:**

- Mapper entité JPA → domaine (`toDomain`)
- Mapper domaine → entité JPA (`toEntity`)

### Dépendances

```java
@Component
@RequiredArgsConstructor
public class LimitAssignmentMapper {
    private final LimitScopeMapper scopeMapper;
    // ...
}
```

`LimitAssignmentMapper` dépend de `LimitScopeMapper` pour convertir les scopes.

### Méthode : toDomain

**Signature:**

```java
public LimitAssignment toDomain(LimitAssignmentJpaEntity entity)
```

**Flux:**

1. Accepte une entité JPA
2. Retourne `null` si l'entité est nulle
3. Extrait les champs bruts (id, ruleKey, params, etc.)
4. Convertit `ScopeType + UUID` → `LimitScopeRef` via `scopeMapper.toDomain()`
5. Crée un record `LimitAssignment` avec l'ID typé `LimitAssignmentId.of(entity.getId())`

**Exemple de flux:**

```
LimitAssignmentJpaEntity {
  id: "123e4567-e89b-12d3-a456-426614174000"
  ruleKey: "MAX_STAKE_PER_TICKET"
  scopeType: OUTLET
  scopeId: "456e7890-e89b-12d3-a456-426614174111"
  enabled: true
  onBreach: BLOCK
  params: { "valueCents": 500000 }
  startsAt: "2026-05-09T00:00:00Z"
  endsAt: null
  deletedAt: null
}
    ↓
LimitAssignment {
  id: LimitAssignmentId("123e...")
  ruleKey: MAX_STAKE_PER_TICKET
  scope: LimitScopeRef.OutletScope(OutletId("456e..."))
  enabled: true
  onBreach: BLOCK
  params: { "valueCents": 500000 }
  startsAt: 2026-05-09T00:00:00Z
  endsAt: null
  deleted: false
}
```

### Méthode : toEntity

**Signature:**

```java
public LimitAssignmentJpaEntity toEntity(LimitAssignment assignment)
```

**Flux:**

1. Accepte un record domaine
2. Retourne `null` si le record est nul
3. Crée une nouvelle entité JPA vide
4. Extrait `assignment.id().value()` pour mettre à jour l'ID UUID
5. Convertit `LimitScopeRef` → `ScopeType + UUID` via `scopeMapper`
6. Mappe les autres champs (ruleKey, params, dates)
7. **N'écrit pas `deletedAt`** lors de la création/mise à jour (géré via soft delete ailleurs)

---

## 2. LimitScopeMapper

**Fichier:** `LimitScopeMapper.java`  
**Responsabilités:**

- Convertir sealed interface `LimitScopeRef` ↔ enum `ScopeType` + UUID
- Valider les IDs et les types

### Méthode : toType

**Signature:**

```java
public ScopeType toType(LimitScopeRef scope)
```

**Pattern Matching (Java 17+):**

```java
return switch (scope) {
    case LimitScopeRef.TenantScope ignored      -> ScopeType.TENANT;
    case LimitScopeRef.AgentScope ignored       -> ScopeType.AGENT;
    case LimitScopeRef.OutletScope ignored      -> ScopeType.OUTLET;
    case LimitScopeRef.DrawChannelScope ignored -> ScopeType.DRAW_CHANNEL;
};
```

**Exemple:**

```
LimitScopeRef.outlet(OutletId("456e..."))
    ↓
ScopeType.OUTLET
```

### Méthode : toId

**Signature:**

```java
public UUID toId(LimitScopeRef scope)
```

**Pattern Matching:**

```java
return switch (scope) {
    case LimitScopeRef.TenantScope tenant       -> tenant.tenantId().value();
    case LimitScopeRef.AgentScope agent         -> agent.userId().value();
    case LimitScopeRef.OutletScope outlet       -> outlet.outletId().value();
    case LimitScopeRef.DrawChannelScope channel -> channel.drawChannelId().value();
};
```

**Exemple:**

```
LimitScopeRef.outlet(OutletId("456e..."))
    ↓
UUID("456e...")
```

### Méthode : toDomain

**Signature:**

```java
public LimitScopeRef toDomain(ScopeType type, UUID id)
```

**Validations:**

```java
if (type == null) {
    throw new IllegalArgumentException("scopeType is required");
}

if (id == null) {
    throw new IllegalArgumentException("scopeId is required for " + type);
}
```

**Pattern Matching:**

```java
return switch (type) {
    case TENANT      -> LimitScopeRef.tenant(TenantId.of(id));
    case AGENT       -> LimitScopeRef.agent(UserId.of(id));
    case OUTLET      -> LimitScopeRef.outlet(OutletId.of(id));
    case DRAW_CHANNEL -> LimitScopeRef.drawChannel(DrawChannelId.of(id));
    default -> throw new IllegalArgumentException("Unsupported scopeType for LimitPolicy V0: " + type);
};
```

**Exemple:**

```
ScopeType.OUTLET + UUID("456e...")
    ↓
LimitScopeRef.outlet(OutletId("456e..."))
```

---

## 3. Structure de packages

### Arborescence logique

```
core.limitpolicy
├── domain
│   └── model
│       ├── LimitAssignment
│       └── LimitScopeRef (sealed interface)
│
├── infra.persistence.assignment
│   ├── LimitAssignmentJpaEntity
│   ├── LimitAssignmentJpaRepository
│   ├── mapper
│   │   ├── LimitAssignmentMapper
│   │   └── LimitScopeMapper
│   └── adapter
│       └── LimitAssignmentRepositoryAdapter
│
└── ...
```

### Package Maven complet

```
com.tchalanet.server.core.limitpolicy.infra.persistence.assignment.mapper
```

**Composants:**

- `LimitAssignmentMapper.java`
- `LimitScopeMapper.java`

---

## 4. Flux d'intégration

### Load (Lecture)

```
LimitAssignmentRepositoryAdapter
    ↓
LimitAssignmentJpaRepository.findAll(...)
    ↓ (JPA)
LimitAssignmentJpaEntity[]
    ↓
LimitAssignmentMapper.toDomain(entity)
    ↓
LimitAssignment (domaine typé)
```

### Save (Écriture)

```
UpsertLimitAssignmentCommand
    ↓
UpsertLimitAssignmentCommandHandler
    ↓
LimitAssignment (domaine)
    ↓
LimitAssignmentMapper.toEntity(domain)
    ↓
LimitAssignmentJpaEntity
    ↓ (JPA)
LimitAssignmentJpaRepository.save(entity)
```

---

## 5. Règles de conversion

### ID mappings

| Scope        | Domaine                  | JPA                             |
| ------------ | ------------------------ | ------------------------------- |
| TENANT       | `TenantId.of(uuid)`      | `ScopeType.TENANT + uuid`       |
| AGENT        | `UserId.of(uuid)`        | `ScopeType.AGENT + uuid`        |
| OUTLET       | `OutletId.of(uuid)`      | `ScopeType.OUTLET + uuid`       |
| DRAW_CHANNEL | `DrawChannelId.of(uuid)` | `ScopeType.DRAW_CHANNEL + uuid` |

### Null handling

| Cas                      | Comportement                     |
| ------------------------ | -------------------------------- |
| `entity == null`         | Retourne `null`                  |
| `assignment == null`     | Retourne `null`                  |
| `type == null`           | Lance `IllegalArgumentException` |
| `id == null`             | Lance `IllegalArgumentException` |
| `ScopeType` non supporté | Lance `IllegalArgumentException` |

### Boolean flags

| Champ JPA           | Domaine     | Notes                 |
| ------------------- | ----------- | --------------------- |
| `enabled`           | `enabled()` | Booléen simple        |
| `deletedAt != null` | `deleted()` | Soft delete → boolean |

---

## 6. Sealed interface pattern

`LimitScopeRef` est une **sealed interface** avec 4 implémentations records :

```java
public sealed interface LimitScopeRef
    permits TenantScope, OutletScope, AgentScope, DrawChannelScope {

    record TenantScope(TenantId tenantId) implements LimitScopeRef {}
    record OutletScope(OutletId outletId) implements LimitScopeRef {}
    record AgentScope(UserId userId) implements LimitScopeRef {}
    record DrawChannelScope(DrawChannelId drawChannelId) implements LimitScopeRef {}
}
```

**Avantages:**

- Exhaustivenss : le compilateur vérifie tous les cas dans `switch`
- Type-safety : chaque scope ne peut contenir que son ID typé
- Pattern matching : `case LimitScopeRef.OutletScope outlet -> ...`

---

## 7. Tests recommandés

### LimitAssignmentMapper

- ✅ `toDomain` avec entité complète
- ✅ `toDomain` avec entité nule
- ✅ `toEntity` avec assignment complet
- ✅ `toEntity` avec assignment nul
- ✅ Roundtrip : `entity → domain → entity`

### LimitScopeMapper

- ✅ `toType` pour chaque scope
- ✅ `toId` pour chaque scope
- ✅ `toDomain` avec type et id valides
- ✅ `toDomain` avec type nul → exception
- ✅ `toDomain` avec id nul → exception
- ✅ `toDomain` avec type non supporté → exception

---

## 8. Contraintes & non-negotiables

- ✅ **Pas de logique métier** dans les mappers
- ✅ **Pattern matching exhaustif** (Java 17+)
- ✅ **Typed IDs** (`LimitAssignmentId`, `TenantId`, etc.)
- ✅ **Null safety** (validation explicite)
- ✅ **Separation of concerns** (mappers = conversion only)
- ✅ **Component injection** via `@RequiredArgsConstructor`
- ✅ **Stateless components** (`@Component` pas `@Service`)

---

## 9. Évolutions futures

- **Audit columns** : ajouter `created_by`, `updated_by` s'il faut tracer qui a configuré la règle
- **Encrypted params** : si les paramètres contiennent des secrets
- **Mapper factory** : si plusieurs variantes de conversion sont nécessaires
- **Validation chain** : ajouter des validations métier avant la conversion domaine
