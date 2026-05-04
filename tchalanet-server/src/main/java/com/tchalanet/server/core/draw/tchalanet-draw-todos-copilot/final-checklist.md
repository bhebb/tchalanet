# Final checklist — terminer `core.draw`

## P0 global

- [ ] `Draw` domain nettoyé : pas de `source` inutilisé, pas de `drawChannelCode`.
- [ ] `cutoffAt < scheduledAt` vérifié à création et reschedule.
- [ ] Transitions normales strictes : pas de retour arrière dangereux.
- [ ] `applyResult` refuse un résultat déjà présent.
- [ ] `overrideResult` limité à draw `RESULTED`.
- [ ] `reschedule` limité à `SCHEDULED` MVP.
- [ ] `DrawJpaEntity` sans `DrawChannelEntity`.
- [ ] Aucun SQL `d.result_slot_id`.
- [ ] Apply result vérifie `draw_result.result_slot_id = draw_channel.result_slot_id`.
- [ ] `force` clarifié dans apply/correct/cancel/settle.
- [ ] Unique active draw via Flyway partial index.
- [ ] Summary sans N+1.
- [ ] Summary projection hors domain.
- [ ] Cache keys tenant-safe.
- [ ] Events après commit.
- [ ] Event listeners idempotents.
- [ ] Scheduler apply utilise `apply_results`, pas `fetch_results`.
- [ ] Properties alignées YAML.
- [ ] Endpoints legacy vague supprimés/deprecated.

## P1 global

- [ ] `DrawSummaryJdbcRepository` complet.
- [ ] `bulkOpen/bulkClose` utilisent `Clock` via `Instant now` si décidé.
- [ ] `findOpenable/findDueToClose` utilisent `Instant`/bornes plutôt que epoch.
- [ ] Cache eviction ciblée.
- [ ] Batch notifications branchées.
- [ ] Watchdog provisional isolé.
- [ ] Settlement marqué deferred ou aligné avec `core.sales`.

## Tests minimum

### Domain tests

- [ ] `scheduled` valide `cutoffAt < scheduledAt`.
- [ ] `open`: SCHEDULED -> OPEN.
- [ ] `close`: OPEN -> CLOSED.
- [ ] `applyResult`: CLOSED -> RESULTED.
- [ ] `applyResult` refuse draw déjà resulté.
- [ ] `settle`: RESULTED -> SETTLED.
- [ ] `cancel`: SCHEDULED/OPEN/CLOSED -> CANCELED.
- [ ] `cancel` refuse RESULTED/SETTLED si transition stricte.
- [ ] `overrideResult` refuse non RESULTED.
- [ ] locked draw refuse mutations.

### Persistence tests

- [ ] `findDrawIdBySlotId` join `draw_channel.result_slot_id`.
- [ ] `attachResultBySlot` update only CLOSED unlocked no-result draw.
- [ ] `attachResultBySlot` refuses result from wrong slot.
- [ ] `bulkInsert` idempotent with partial unique index.
- [ ] `listLatestWithResults` no N+1.
- [ ] `listNext` respects status and scheduledAt.

### Handler tests

- [ ] Apply handler clamps days/maxSlots.
- [ ] Apply handler publishes events after commit.
- [ ] Correct handler idempotency atomic.
- [ ] Cancel handler requires reason.
- [ ] Generate handler does not duplicate with force.

### Scheduler tests

- [ ] Apply scheduler checks `results.scheduler.active`.
- [ ] Apply scheduler checks `apply_results` window.
- [ ] Lifecycle scheduler isolates tenant errors.
- [ ] Gates disable jobs.

## Done definition

`core.draw` est prêt quand Copilot/Claude peut lancer les tests et qu'aucun item P0 n'est ouvert.
