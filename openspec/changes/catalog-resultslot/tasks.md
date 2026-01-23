# tasks.md

Change-id: catalog-resultslot

## Tâches (checklist)

o- [x] Créer l’interface API `ResultSlotCatalog` sous `catalog/resultslot/api` avec les méthodes :

- `List<ResultSlotView> listActive()`
- `Optional<ResultSlotView> findByKey(String slotKey)`
- `ResultSlotView requireByKey(String slotKey)`

- [x] Définir `ResultSlotView` (DTO) avec les champs : `id`, `slotKey`, `provider`, `timezone`, `drawTime`, `daysOfWeek`, `active`, `sourceCfg` (JsonNode), `projectionCfg` (JsonNode), `labelKey`.

- [x] Créer l’entité JPA interne `ResultSlotJpaEntity` (table `result_slot`) et le repository `ResultSlotJpaRepository`.

- [x] Implémenter `ResultSlotCatalogImpl` dans `catalog/resultslot/internal` : mapper entity → `ResultSlotView`, annoter `listActive()` et `findByKey()` avec `@Cacheable`.

- [x] Ajouter `ResultSlotCacheNames` (noms de cache) et appliquer `@CacheEvict` sur les handlers admin (create/update/delete) pour évincer `resultslot.active` et `resultslot.by_key`.

- [x] Ajouter tests unitaires pour `ResultSlotCatalogImpl` : listActive (filtrage), findByKey/requireByKey, comportement de cache/eviction.

- [x] Adapter le contrôleur admin pour retourner le wrapper standard `ApiResponse<T>` et exposer des DTOs immutables (`ResultSlotView`) pour create/update/get/list (endpoints admin sous `/platform/result-slots`).

- [x] Exposer les endpoints GET admin :

  - `GET /platform/result-slots/active` → liste active (encapsulée dans ApiResponse)
  - `GET /platform/result-slots/by-key/{slotKey}` → lookup by key (encapsulé dans ApiResponse)

- [x] Ajouter la spec canonique (si requis) sous `openspec/specs/resultslot/spec.md` (déjà créé si nécessaire) et valider :

  - `./node_modules/.bin/openspec validate catalog-resultslot --strict --no-interactive`

- [ ] Documenter l'API catalog dans `catalog/resultslot/README.md` ou `DOCUMENTATION.md`.

- [ ] Préparer la PR : inclure lien vers la spec OpenSpec, checklist, instructions de validation et migration SQL si nécessaire.
