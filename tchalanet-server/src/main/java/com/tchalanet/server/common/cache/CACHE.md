# 🧠 Tchalanet — Architecture du Cache

_(Caffeine L1 + Redis L2 + TTL par Feature)_

> **Status**: NORMATIVE  
> **Scope**: Backend (tchalanet-server)  
> **Audience**: Backend developers, reviewers, ops  
> **Related**:
>
> - Ops cache controller (`/platform/cache`)
> - Cache policy & usage (conventions)

---

## 🎯 Objectifs

Le système de cache de Tchalanet vise les objectifs suivants :

- **Performance** : réduire la latence des chemins critiques (home publique, tirages, thèmes, tenant config).
- **Scalabilité** : permettre le partage de cache entre plusieurs instances backend.
- **Cohérence maîtrisée** : TTL métier géré au niveau Redis (L2), TTL technique plus court côté Caffeine (L1).
- **Extensibilité par domaine** : chaque module déclare explicitement ses caches et TTL.
- **Tolérance aux pannes** : Redis peut être désactivé ; l’application fonctionne avec Caffeine seul.

---

## 1. Principes fondamentaux

### MUST

- Le cache est une **optimisation d’infrastructure**, jamais une source de vérité.
- Toute donnée en cache **peut disparaître à tout moment**.
- Le backend **doit rester fonctionnel** sans Redis.

### MUST NOT

- Introduire une dépendance fonctionnelle à la présence du cache.
- Encoder des règles métier dépendantes du cache.

---

## 2. Architecture du Cache

Tchalanet utilise un cache à deux niveaux ("2-Tier Cache") :

```text
        ┌──────────────┐
        │  L1 Cache     │   Caffeine (in-memory, par instance)
        │  Ultra-rapide │   TTL technique court
        └──────────────┘
               │
Miss →         ▼
┌──────────────┐
│  L2 Cache     │   Redis (cache partagé)
│  TTL métier   │   Par cacheName
└──────────────┘
        │
Miss →  ▼
┌──────────────┐
│  Backend DB   │  Postgres  / Providers externes
└──────────────┘
```

### Priorité de lecture

1. **Caffeine (L1)**
2. **Redis (L2)**
3. **Backend**

Si Redis retourne une valeur, elle est ré-hydratée en L1.

---

## 3. CacheManager composite

### 3.1. CombinedCacheManager

Le CombinedCacheManager orchestre L1 + L2.

#### MUST

- Utiliser un seul CacheManager applicatif.
- Appliquer put / evict / clear sur L1 et L2.

#### Comportement

- **Redis activé** → `CombinedCacheManager(Caffeine, Redis)`
- **Redis désactivé** → `CaffeineCacheManager` seul

---

## 4. Utilisation applicative du cache

### 4.1. Usage prioritaire : @Cacheable

#### MUST

- Privilégier `@Cacheable` pour les lectures idempotentes.
- Utiliser un `cacheName` fonctionnel et stable.
- Construire les clés via SpEL.

#### MUST NOT

- Accéder manuellement au cache si `@Cacheable` suffit.
- Utiliser des clés Redis explicites dans les annotations.

---

## 5. Cache manuel (exception contrôlée)

Le cache manuel est autorisé uniquement quand `@Cacheable` est insuffisant.

### Cas autorisés

- Clés composites complexes
- Protection anti-stampede
- Payloads externes bruts (HTTP / JSON)
- Infra, batch, providers externes

### MUST

- Encapsuler le cache dans un helper dédié (`XxxCache`)
- Accéder via `CacheManager`
- Construire les clés via `CacheKeyBuilder`
- Tolérer l'absence du cache

### MUST NOT

- Manipuler le cache dans le domaine ou l'application
- Construire des clés Redis "à la main"

---

## 6. Nommage des caches

### Règle

Les `cacheName` sont fonctionnels, pas techniques.

### Format standard

```
<scope>.<domain>.<resource>[.<qualifier>]
```

### Exemples

- `platform.tenant.by_code`
- `catalog.drawresult.by_id`
- `catalog.drawresult.id.by_slot_occurred`
- `infra.uslottery.provider_raw`
- `public.draw.latest`

### MUST NOT

- Inclure tenant, env ou date dans le `cacheName`
- Utiliser le format Redis comme nom de cache

---

## 7. CacheName vs Cache Key

### CacheName

- Stable
- Déclaré dans les annotations
- Déclaré dans un `CacheSpecProvider`

### Cache Key

- Calculée au runtime
- Peut inclure tenant, date, hash
- Définie via :
  - SpEL (`@Cacheable`)
  - `CacheKeyBuilder` (cache manuel)

---

## 8. TTL et CacheSpecProvider

### MUST

- Chaque cache doit être déclaré via un `CacheSpecProvider`.
- Chaque cache doit fournir :
  - `ttlL1`
  - `ttlL2`
- La règle `ttlL1 ≤ ttlL2` est obligatoire.

### MUST NOT

- Définir des TTL dans le code métier.
- Créer un cache sans TTL déclaré.

---

## 9. Éviction du cache

### MUST

- Effectuer l'éviction après commit transactionnel.
- Utiliser :
  - `@CacheEvict(beforeInvocation = false)`
  - un hook `afterCommit`
  - un evictor infra déclenché après commit

### MUST NOT

- Évincer avant commit
- Évincer depuis le domaine
- Évincer directement depuis un controller métier

---

## 10. Spring Data REST (cas particulier)

Quand les écritures ne sont pas maîtrisées :

### MUST

- Utiliser les événements de persistance (`AfterCreate`, `AfterSave`, `AfterDelete`)
- Évincer uniquement dans la couche infra
- Limiter l'éviction au strict nécessaire

---

## 11. Ops & administration

### MUST

- Fournir un endpoint Ops pour :
  - lister les caches
  - vider un cache
  - vider tous les caches
- Restreindre l'accès aux rôles Ops / Super-admin
- Auditer chaque action Ops

### Rappel

L'Ops cache n'est pas un mécanisme fonctionnel.  
Il sert uniquement au support et à la récupération.

---

## 12. Résilience

### Panne Redis

- Caffeine continue à servir les requêtes
- Pas de partage inter-instances
- Expiration via TTL L1 uniquement

### Scale-out

- Redis assure la cohérence métier
- Caffeine réduit la charge réseau

---

## 📌 Conclusion

- Cache à deux niveaux, transparent pour le métier
- `@Cacheable` en priorité
- Cache manuel uniquement si nécessaire
- TTL déclarés par domaine
- Éviction après commit
- Ops comme filet de sécurité

Cette architecture est validée pour le MVP et le scale futur.
