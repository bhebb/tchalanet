# Catalogues recommandés — core.game & core.drawresult

But :
- Forcer les autres modules à passer par des catalogues publics pour accéder aux métadonnées partagées (caching, stabilité, contrôle d'accès).
- Éviter que des modules consommateurs importent directement des ports/entités internes qui devraient rester encapsulés.

1) Principe

- Comme pour `core.resultslot`, créez une façade publique `Catalog` pour `game` et `drawresult` :
  - `core.game.api.GameCatalog` (expose `listActive`, `findByKey`, `findById`)
  - `core.drawresult.api.DrawResultCatalog` (expose `findById`, `findRefBySlotKeyAndDate`, `findByResultSlotIdAndOccurredAt`, `findByTenantAndDateRange`)

- Les ports internes / adapters restent dans `internal.port.out` / `internal.infra`.
- Ajoutez des règles ArchUnit (déjà en place) pour interdire l'importation des packages `internal` depuis d'autres modules.

2) API publique minimale (exemples)

- `GameCatalog` :
  - `List<GameView> listActive()`
  - `Optional<GameView> findByKey(String gameKey)`
  - `Optional<GameView> findById(GameId id)`

- `DrawResultCatalog` :
  - `Optional<DrawResult> findById(UUID id)`
  - `Optional<UUID> findRefBySlotKeyAndDate(String slotKey, LocalDate drawDate)`
  - `Optional<DrawResult> findByResultSlotIdAndOccurredAt(UUID slotId, Instant occurredAt)`
  - `List<DrawResult> findByTenantAndDateRange(TenantId tenantId, LocalDate from, LocalDate to)`

3) Cache & eviction

- Catalogues exposent des lectures cacheables (@Cacheable) :
  - `game.byKey::{key}` TTL long
  - `drawresult.id::{id}` TTL court (ex: 30–60s) pour UI
- Eviction :
  - Quand un writer modifie l'entité globale (ex: drawresult upsert/override), evict les caches publics correspondants.

4) ArchUnit & enforcement

- Vous avez ajouté des règles dans `ArchitectureTest` pour forbiddre l'accès direct aux packages internes — conservez ces règles et élargissez si nécessaire pour tous les modules.

5) Next steps

- Générer les stubs `GameCatalog` et `DrawResultCatalog` (si vous le voulez).
- Mettre en place listeners d'éviction sur writers (ex: DrawResultWriterAdapter) pour evict les caches publics.

---

Ce document est un guide; dites-moi si je dois générer les stubs Java pour ces catalogues maintenant (GameCatalog + DrawResultCatalog + ports + adapters skeletons).
