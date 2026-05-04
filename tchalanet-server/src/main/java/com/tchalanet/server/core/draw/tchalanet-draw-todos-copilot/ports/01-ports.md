# TODO — `core.draw` ports

## Files principaux

- `DrawLifecyclePort`
- `DrawApplyPort`
- `DrawLookupPort`
- `DrawSummaryReaderPort`
- `DrawSalesGuardPort`
- `FindSettleableDrawIdsPort`
- `DrawResultReaderPort` usage

## Règles communes

- [ ] Ports application définissent les besoins de `core.draw`.
- [ ] Les adapters implémentent les détails JPA/JDBC/cross-domain.
- [ ] Typed IDs dans les ports.
- [ ] Pas de UUID brut dans l'application sauf adapter/repo.
- [ ] Ports write ne retournent pas d'entities JPA.
- [ ] Read projections hors domain model.

## P0 — `DrawLifecyclePort`

Doit couvrir :

- [ ] `bulkInsert(List<NewDrawRow>)`.
- [ ] `findExistingKeys(TenantId, LocalDate from, LocalDate to)`.
- [ ] `findOpenable(Instant now, int limit, int lookaheadHours, int lagHours)` ou bornes Instant.
- [ ] `bulkOpen(List<DrawId>)` ou `bulkOpen(List<DrawId>, Instant now)`.
- [ ] `findDueToClose(Instant now, int limit)`.
- [ ] `bulkClose(List<DrawId>)` ou `bulkClose(List<DrawId>, Instant now)`.
- [ ] `save(Draw)`.

À corriger :

- [ ] Si `bulkOpen/bulkClose` utilisent DB `now()`, décider si acceptable.
- [ ] Si target `Clock`, modifier signatures pour passer `Instant now`.
- [ ] Supprimer tout `defaultSource` obsolète dans `NewDrawRow`.

## P0 — `DrawApplyPort`

Signature actuelle OK :

```java
ApplyResult attachResultBySlot(
    TenantId tenantId,
    LocalDate drawDate,
    ResultSlotId resultSlotId,
    DrawResultId drawResultId,
    Instant now,
    boolean force
);
```

À faire :

- [ ] Décider si `force` reste dans le port.
- [ ] Si auto apply seulement : retirer `force`.
- [ ] Si `force` reste : documenter et guarder.
- [ ] `ApplyResult` doit distinguer au moins updated vs none.
- [ ] Optionnel : enrichir outcomes : locked, already_linked, not_closed, no_match.

## P0 — `DrawLookupPort`

Doit couvrir aggregate lookup, pas summaries lourdes.

- [ ] `Optional<Draw> findById(DrawId)`.
- [ ] `Draw getById(DrawId)`.
- [ ] `Optional<DrawId> findDrawIdBySlotId(TenantId, LocalDate, ResultSlotId)`.
- [ ] `existsSettledDrawForResult(DrawResultId)` si utilisé par drawresult guard.
- [ ] Éviter d'y mettre des queries de dashboard trop riches.

À nettoyer :

- [ ] Déplacer `findByCriteria`/summary vers `DrawSummaryReaderPort` si encore dans lookup.
- [ ] Déplacer watchdog query vers port/repo dédié si souhaité.

## P0 — `DrawSummaryReaderPort`

Port dédié read model :

- [ ] `DrawSummaryView getById(DrawId)`.
- [ ] `TchPage<DrawSummaryView> findByCriteria(DrawSearchCriteria, Pageable)`.
- [ ] `TchPage<DrawSummaryView> listNext(DrawSearchCriteria, Pageable)`.
- [ ] `TchPage<DrawSummaryView> listLatestWithResults(DrawSearchCriteria, Pageable)`.

À respecter :

- [ ] Pas de domain `Draw`.
- [ ] Pas de JPA entity.
- [ ] Projection SQL optimisée.
- [ ] Pas de N+1.

## P0 — `DrawSalesGuardPort`

Doit être clair et minimal.

Méthodes possibles :

```java
void assertCanCancel(DrawId drawId, boolean force);
void assertCanCorrectAppliedResult(DrawId drawId, DrawResultId correctedResultId, boolean force);
void assertCanSettle(DrawId drawId, boolean force);
```

À faire :

- [ ] Le port est dans `core.draw.application.port.out`.
- [ ] L'implémentation peut vivre côté adapter qui interroge sales via API/port.
- [ ] Pas de SQL sales direct dans core.draw infra si possible.
- [ ] Si force bypass, audit obligatoire au controller/handler.

## P1 — `FindSettleableDrawIdsPort`

Statut deferred.

- [ ] Retirer provider/channelCode des critères.
- [ ] Criteria : tenantId, from, to, maxDraws, force si justifié.
- [ ] Utiliser `DrawId.of`, pas nullable.
- [ ] Garder settlement aligné plus tard avec sales.

## Cross-domain port drawresult

Usage d'un port drawresult n'est pas strictement interdit, mais éviter :

- [ ] Ne pas appeler `DrawResultReaderPort.getById` en boucle.
- [ ] Pour summaries, préférer projection SQL ou batch lookup.
- [ ] Pour details/debug, garder côté ops/drawresult.

## Définition de terminé

- Ports séparés write aggregate vs read summaries.
- Aucun port ne force des dépendances JPA/catalog dans le domaine.
- Les adapters portent le détail SQL/JPA.
