# Checklist — Catalog or Core ? (Décision rapide)

Ce document est une checklist courte et normée pour décider si un module/fonctionnalité doit rester dans `catalog/<name>` ou être déplacée dans `core.<bounded_context>`.

---

## 1) Règle rapide (si une seule réponse "❌" critique → c'est Core)

Reste en `catalog/<name>` si toutes les conditions suivantes sont vraies :

- ✅ Read-only au runtime métier
- ✅ Logique minimale (filtrage `active`, `deletedAt`, ordering)
- ✅ Pas d'émission de DomainEvents métier
- ✅ Pas d'orchestration multi-domaines
- ✅ Écritures uniquement via admin/ops CRUD ou migrations/seeds
- ✅ Stable et réutilisé comme lookup/registry/config déclarative

Doit aller dans `core.<bc>` si l'une des conditions suivantes est vraie :

- ❌ Invariants métier / règles financières / settlement / validations critiques
- ❌ Workflow (approval, state machine, transitions)
- ❌ Besoin d'écrire en réaction à des DomainEvents métier (consommer + écrire)
- ❌ Doit publier des DomainEvents
- ❌ Dépend de plusieurs domaines (orchestration)

**Décision** : si une seule réponse « ❌ » critique → déplacer vers `core`.

---

## 2) Refacto structure : standardiser tous les catalogs

### 2.1 Structure canonique à obtenir

Pour chaque `catalog/<name>` :

```
catalog/<name>/
  api/
    <Name>Catalog.java          # interface / facade publique
    <Name>View.java             # records / projections used by API
  internal/
    <Name>CatalogImpl.java      # implementation (package-private when possible)
    cache/
    persistence/
      <Entity>.java
      <JpaRepository>.java
      <JpaAdapter>.java
    rest/                       # admin / ops controllers
  DOMAIN_<NAME>.md
```

### 2.2 Règles de dépendance

- Seul `catalog.<name>.api..` est importable depuis `core` et `features`.
- `catalog.<name>.internal..` **ne doit pas** être référencé hors du module (test ArchUnit).

**Action Copilot (optionnelle / à demander)**

- Déplacer toutes les interfaces/records consommés vers `api/`.
- Rendre les impls `internal` package-private quand possible.
- Remplacer tout import `catalog.<name>.internal..` par `catalog.<name>.api..` dans les modules consommateurs.

---

## 3) Typed IDs & UUID — règle stricte

### 3.1 Règle

- Hors persistence : utiliser des wrappers typés (ex : `TenantId`, `DrawId`, `ResultSlotId`).
- Persistence : stocker des `UUID` (entity fields, repositories, Flyway migrations).

**Action Copilot**

- Si un port/DAO retourne `UUID`, transformer la signature publique en wrapper (`Optional<DrawId>`).
- Ne pas exposer `.uuid()` / `.value()` dans les signatures publiques ; limiter à `internal.persistence`.

---

## 4) Context & tenant scoping

- Les catalogs tenant-scoped acceptent `TenantId` en paramètre ; ne jamais inventer `tenantId` dans le catalog.
- Tables tenant-scoped → `BaseTenantEntity` + RLS ; tables globales → `BaseEntity`.

**Action Copilot**

- Vérifier chaque entity : si tenant-scoped → extends `BaseTenantEntity`.
- Vérifier que les méthodes catalogue prennent `TenantId` si nécessaire.
- Vérifier que les requêtes repository filtrent correctement sur tenant (si demandé).

---

## 5) Cache conventions (catalog)

### 5.1 Nommage canonique

- `catalog:<name>:<resource>` (ex: `catalog:pricing:odds`, `catalog:resultslot:by_key`).

### 5.2 Keys

- Déterministes et stables.
- Évitez `SimpleKey.of(tenantId.uuid(), ...)` si possible ; préférez une string stable :
  - `"t=" + tenantId + "|g=" + gameCode + "|bt=" + betType + "|o=" + betOption`.

### 5.3 Invalidation

- Uniquement via admin/ops endpoints (REST/SDR).
- Centraliser dans `internal.cache.<Name>CacheEvictor` si possible.

**Action Copilot**

- Renommer les `cacheNames` pour suivre le pattern `catalog:<name>:*`.
- Créer/adapter les evict handlers existants vers le pattern canonique.
- Documenter TTL / keys / invalidation dans `DOMAIN_<NAME>.md`.

---

## 6) REST/SDR dans catalog

### Autorisé

- CRUD admin simple ; sécurité isolée ; handlers d'invalidation cache.

### Interdit

- Endpoints métier (vente, payout, settlement) ; workflows ; endpoints publics runtime.

**Action Copilot**

- Si un controller catalog effectue de la logique métier, migrer la responsabilité vers `core` ou `features`.

---

## 7) Events : règle "catalog n’émet pas"

- `catalog` ne publie pas de `DomainEvent` métier.
- Les updates catalog peuvent provoquer des _application events_ (invalidate cache, refresh projections) mais pas des domain events.

**Action Copilot**

- Supprimer / déplacer tout `catalog.*.domain.event` vers le producteur métier approprié : `core.<source>.domain.event` ou `features.<slice>.application.event`.

---

## 8) Alignement module par module (script d'action)

Pour chaque catalog existant (ex : `pricing`, `resultslot`, `drawresult`, `game`, `theme`, `settings`) :

1. Classifier : reste `catalog` ou migrate to `core` (voir section 1).
2. API : créer/valider `<Name>Catalog` et `<Name>View` dans `api/`.
3. Internal :
   - `CatalogImpl` dans `internal/` ;
   - JPA entities/repos dans `internal.persistence` ;
   - cache/evict dans `internal.cache` ;
   - REST admin dans `internal.rest`.
4. Wrappers : signatures API → wrappers (pas UUID directement).
5. Cache : renommer `cacheNames` → `catalog:<name>:<resource>`.
6. Doc : créer/mettre à jour `DOMAIN_<NAME>.md` (TTL / keys / invalidation).
7. ArchUnit : ajouter `forbidInternal("catalog.<name>", classes);`.

> Copilot peut exécuter ces étapes automatiquement sur demande. Demande explicite requise pour lancer les modifications.

---

## 9) Patterns "Copilot ready" (snippets)

### 9.1 Interface catalog (api)

```java
package com.tchalanet.server.catalog.pricing.api;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;

public interface PricingCatalog {
  BigDecimal oddsFor(TenantId tenantId, String gameCode, BetType betType, Short betOption);
}
```

### 9.2 Impl catalog (internal) + cache name canonique

```java
@Cacheable(
  cacheNames = "catalog:pricing:odds",
  key = "'t=' + #tenantId + '|g=' + #gameCode + '|bt=' + #betType + '|o=' + #betOption"
)
```

### 9.3 Port persistence (internal.persistence)

- Repo Spring Data avec `UUID` au niveau persistence.
- `entity` extends `BaseTenantEntity` si tenant-scoped.

### 9.4 Evict handler (internal.cache)

```java
@CacheEvict(cacheNames="catalog:pricing:odds", allEntries=true)
public void evictPricingOdds() { ... }
```

---

## 10) Tests à ajouter / maintenir

### 10.1 ArchUnit

- Interdire imports `catalog.<name>.internal..` depuis l'extérieur.
- Ajouter chaque nouveau catalog à `forbidInternal("catalog.<name>", classes);`.

### 10.2 Unit tests (catalog)

- tests sur : selection `active/deletedAt`, `not found` behavior, key building, tenant scoping.

### 10.3 Integration tests (si RLS)

- Testcontainers + tenant context (si tables tenant-scoped).

---

## 11) Critères d’acceptation (DoD refacto catalog)

- Aucun import externe de `catalog.<name>.internal..` (ArchUnit green).
- `catalog:<name>:<resource>` utilisé partout pour caches.
- Signatures publiques : wrappers (pas UUID).
- Catalog ne publie aucun DomainEvent.
- Pas de workflow ni logique métier critique dans catalog.
- `DOMAIN_<NAME>.md` présent et à jour.

---

## 12) Si impossible à aligner → migration vers `core`

**Signaux**

- Logique métier / règles financières ;
- Events métier requis ;
- Orchestration multi-domaines ;
- Writes runtime depuis core.

**Action**

- Déplacer le module vers `core.<bc>` et appliquer :
  - handlers `@UseCase` + `@TchTx` ;
  - events after-commit ;
  - ports out/in selon l’architecture hexagonale.

---

### Lancement des actions

Si tu veux que j’exécute les actions listées (déplacements, renommages de cache, modifications ArchUnit, etc.), réponds `GO REFACTOR` et je lancerai un plan par étapes et les modifications automatiques (avec validations / compilations après chaque étape).
