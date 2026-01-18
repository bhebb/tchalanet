# Backend Cache Conventions (Domain-level)

Dernière mise à jour: 2026-01-17

Ce document définit les conventions pour **ajouter/modifier** du cache dans un domaine backend.

---

## Principes

- L1 = Caffeine (technique, TTL court) ; L2 = Redis (métier, TTL piloté).
- Les clés de cache sont **déterministes** et stables (builder utilitaire si nécessaire).
- Les caches sont **déclarés par domaine** via un `CacheSpecProvider`.
- Les méthodes annotées `@Cacheable` utilisent `cacheNames` spécifiques au domaine.

## Ajout d’un cache (Checklist)

1. Définir le **nom** de cache (`cacheNames = "sales:ticketViews"`).
2. Définir la **clé** de cache (wrappers d’ID et paramètres métier).
3. Déclarer les **specs** (TTL L1/L2) via `CacheSpecProvider` du domaine.
4. Annoter la méthode (`@Cacheable(cacheNames = "sales:ticketViews", key = "...")`).
5. Ajouter **tests** (vérifier hit/miss, invalidation si applicable).
6. Documenter dans `DOMAIN.md` (si logique métier associée au cache).

## Modification d’un cache (Checklist)

1. Évaluer l’impact (TTL, keys, invalidation, memory footprint).
2. Mettre à jour `CacheSpecProvider` (TTL L1/L2).
3. Mettre à jour `@Cacheable` si la clé change.
4. Ajouter/mettre à jour les **tests**.
5. Mettre à jour la **doc** (ce fichier + `DOMAIN.md` si pertinent).

## Invalidation

- Utiliser `@CacheEvict` pour invalidations explicites.
- Préférer invalidations ciblées (key) plutôt que globales.
- Alignement évènements: publier un event (AfterCommit) si nécessaire pour invalider en L2.

## Conventions de nommage

- `core.<domain>.cache:<resource>` pour les caches métier.
- `features.<slice>.cache:<resource>` pour orchestration (rare).

## Liens techniques

- Spec/provider: `common.cache.*` (si existant)
- Annotations: `org.springframework.cache.annotation.*`
- AfterCommit events: `common.tx.AfterCommit`, `common.events.DomainEventPublisher`
