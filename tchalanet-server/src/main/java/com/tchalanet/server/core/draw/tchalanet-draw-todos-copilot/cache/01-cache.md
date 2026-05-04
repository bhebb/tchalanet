# TODO — `core.draw` cache

## Files principaux

- `DrawCacheSpecProvider`
- `DrawCacheKeyBuilder`
- `DrawCacheEvictor`
- `DrawEventListener` cache invalidation

## P0 — cache specs

État actuel bon :

```java
CacheSpec.of(name, Duration.ofSeconds(10), Duration.ofSeconds(60))
```

- [x] Ordre L1 puis L2 correct.
- [x] Noms `core.draw.*` corrects.

À compléter :

- [ ] Ajouter cache distinct si besoin : `core.draw.next.search`.
- [ ] Ajouter cache distinct si besoin : `core.draw.latest_with_results.search`.
- [ ] Garder TTL court pour données draw/results.
- [ ] Ne pas cacher long les vues admin sensibles.

## P0 — cache key tenant-safe

Bug actuel : `summary(criteria, pageKey)` ne contient pas tenant.

Patch :

```java
public String summary(DrawSearchCriteria criteria, String pageKey) {
    return "tenant:%s:slot:%s:from:%s:to:%s:status:%s:page:%s"
        .formatted(
            criteria.tenantId().value(),
            criteria.resultSlotId() == null ? "all" : criteria.resultSlotId().value(),
            criteria.from(),
            criteria.to(),
            criteria.status() == null ? "all" : criteria.status(),
            pageKey);
}
```

TODO :

- [ ] Ajouter `tenantId` à toutes les clés.
- [ ] Utiliser `tenantId.value()`, pas `tenantId.toString()`.
- [ ] Inclure tous les filtres significatifs : status, channel, slot, dates, withResults, active, sort.
- [ ] Normaliser les listes avant key : sorted/uppercase/distinct.
- [ ] `pageKey` doit contenir page, size, sort.

## P1 — eviction

MVP : `evictAll()` acceptable.

Cible : targeted eviction.

Sur events :

- [ ] `DrawResultAppliedEvent` evict tenant summaries/latest/next/today.
- [ ] `DrawResultCorrectedEvent` evict tenant summaries/latest.
- [ ] `DrawCancelledEvent` evict tenant summaries/next/today.
- [ ] `DrawSettledEvent` evict tenant summaries/latest.
- [ ] Open/close events si publiés : evict next/today.

## P1 — after commit

- [ ] Eviction après commit seulement.
- [ ] Listener idempotent.
- [ ] Pas d'evictor d'un autre domaine.

## Définition de terminé

- Aucune clé cache cross-tenant.
- TTLs courts et cohérents.
- Eviction fonctionne après apply/correct/cancel/settle.
