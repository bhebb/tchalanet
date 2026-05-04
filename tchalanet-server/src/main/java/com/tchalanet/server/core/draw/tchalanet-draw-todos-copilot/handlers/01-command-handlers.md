# TODO — `core.draw` command handlers

## Règles communes

- [ ] Tous les handlers write ont `@UseCase`.
- [ ] Tous les handlers write ont `@TchTx`.
- [ ] `Objects.requireNonNull(command, ...)`.
- [ ] Validation des champs obligatoires.
- [ ] Pas de `UUID.randomUUID()` direct ; utiliser `IdGenerator`.
- [ ] Pas de `Instant.now()` direct ; utiliser `Clock`.
- [ ] Construire event avant `AfterCommit.run(...)`.
- [ ] `AfterCommit.run(() -> eventPublisher.publish(event));`.
- [ ] Ne pas faire de lookup externe dans le lambda `AfterCommit`.
- [ ] `force=true` implique reason + audit + guard.

## P0 — `GenerateDrawsForRangeCommandHandler`

- [x] Handler globalement OK.
- [ ] Clarifier `force` : ne pas l'utiliser pour générer des doublons.
- [ ] Toujours charger `existingKeys`, même si `force=true`, sauf future commande dédiée `RegenerateDrawsForRangeCommand`.
- [ ] Supprimer `defaultSource` si ancien modèle provider-driven.
- [ ] Vérifier `cutoffSec` null-safe.
- [ ] Vérifier `cutoffAt < scheduledAt`.
- [ ] Vérifier `bulkInsert` idempotent.
- [ ] Logs/métriques : candidates, existing, created, conflicts, dryRun.

Patch recommandé :

```java
var existingKeys = drawLifecyclePort.findExistingKeys(
    command.tenantId(),
    command.from(),
    command.to()
);
```

## P0 — `OpenDueDrawsCommandHandler`

- [x] Pattern OK.
- [ ] Supprimer variable inutilisée `allIds`.
- [ ] Valider `batchSize > 0`.
- [ ] Valider `lookaheadHours >= 0`.
- [ ] Valider `lagHours >= 0`.
- [ ] `bulkOpen(empty)` doit retourner 0.
- [ ] SQL doit garder guards : `status='SCHEDULED'`, `locked=false`, `deleted_at is null`.
- [ ] Ne pas ouvrir si `cutoff_at <= now`.

## P0 — `CloseDueDrawsCommandHandler`

- [x] Pattern OK.
- [ ] Valider `batchSize > 0`.
- [ ] `bulkClose(empty)` doit retourner 0.
- [ ] SQL doit garder guards : `status='OPEN'`, `locked=false`, `cutoff_at <= now`.
- [ ] Logs/métriques : due, locked, closed, dryRun.

## P0 — `ApplyExternalResultsWindowCommandHandler`

- [x] Bonne base.
- [ ] Utiliser `clampDaysBack(...)` si la méthode existe.
- [ ] Ajouter `hard_days_back` dans properties ou supprimer la méthode.
- [ ] Valider `max_slots_per_tick > 0`.
- [ ] Distinguer `slotNotFound` et `slotInactive`.
- [ ] Précharger les result slots avant la boucle date/slot.
- [ ] Déterminer `occurredAt` de façon déterministe depuis `(date, slot.drawTime, slot.timezone)`.
- [ ] Utiliser `DrawApplyPort.attachResultBySlot(...)` uniquement slot-driven.
- [ ] Accumuler les events puis publier une fois after commit.
- [ ] `force=false` scheduler.
- [ ] `dryRun=false` scheduler.
- [ ] Si `force=true` manuel : audit + reason + sales guard.

Vérifier que le port apply :

- [ ] ne remplace pas un draw déjà RESULTED en auto.
- [ ] ne touche pas locked.
- [ ] ne touche pas OPEN/SCHEDULED/CANCELED/SETTLED.
- [ ] ne joint pas provider/channelCode.

## P0 — `CorrectAppliedDrawResultCommandHandler`

- [ ] Remplacer `alreadyProcessed + markProcessed` par `markProcessedIfAbsent`.
- [ ] Valider `drawId`, `correctedDrawResultId`, `reason`, `idempotencyKey`.
- [ ] Vérifier que le draw a déjà un résultat.
- [ ] Vérifier que corrected result != previous result.
- [ ] Guard sales : ne pas corriger si payout/settlement incompatible sauf force auditée.
- [ ] Ne pas muter le `draw_result` global ici.
- [ ] La correction est seulement : tenant draw attachment `draw.draw_result_id`.
- [ ] Résoudre toute donnée nécessaire avant `AfterCommit`.
- [ ] Construire `DrawResultCorrectedEvent` avant `AfterCommit`.

## P0 — `CancelDrawCommandHandler`

- [x] Bon pattern.
- [ ] Valider `command != null`.
- [ ] Valider `drawId`.
- [ ] Valider `reason` non blank.
- [ ] Utiliser un seul `Instant now`.
- [ ] Guard sales `assertCanCancel(draw.id(), force)`.
- [ ] `draw.cancel(reason, now)` doit refuser RESULTED/SETTLED si transition stricte.
- [ ] Construire event avant `AfterCommit`.

## P0 — `OverrideDrawCommandHandler`

- [ ] Supprimer le handler si c'est un no-op.
- [ ] Ne pas garder une commande vague qui fait `save(draw)` sans mutation.
- [ ] Remplacer par commandes explicites si besoin :
  - `CorrectAppliedDrawResultCommand`
  - `ForceCancelDrawCommand`
  - `LockDrawCommand`
  - `UnlockDrawCommand`
  - `ArchiveDrawCommand`

## P1 — `SettleDrawCommandHandler`

Statut : à garder minimal/deferred jusqu'à refonte `core.sales`.

- [ ] Clarifier si settlement exige `DrawResultStatus.CONFIRMED` ou accepte `PROVISIONAL` MVP.
- [ ] Si status draw != RESULTED, retourner no-op batch ou conflict manuel.
- [ ] Refuser si `drawResultId == null`.
- [ ] Ne jamais publier event avec `resultSlotId = null`.
- [ ] Settlement doit être aligné avec tickets/payout/ledger.
- [ ] Si déjà SETTLED : no-op batch, conflict manuel.
- [ ] Ajouter guard sales ou orchestrer depuis sales settlement.

## P1 — Lock / Unlock / Archive / Reschedule

- [ ] Chaque handler charge aggregate via `DrawLookupPort.getById`.
- [ ] Appelle méthode domaine.
- [ ] Sauve via `DrawLifecyclePort.save`.
- [ ] Event after commit si l'action impacte cache/UI.
- [ ] Reason obligatoire pour archive/reschedule si admin action.
- [ ] Audit pour actions sensibles.

## Définition de terminé

- Aucun handler write ne fait de mutation sans `@TchTx`.
- Aucun event publié avant commit.
- Aucun handler vague/legacy/no-op.
- `force` est rare, audité, et guardé.
