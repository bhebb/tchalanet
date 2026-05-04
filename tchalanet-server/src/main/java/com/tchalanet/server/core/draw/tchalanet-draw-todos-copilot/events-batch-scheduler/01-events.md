# TODO — `core.draw` events

## Files principaux

- `DrawEventListener`
- `DrawResultAppliedEvent`
- `DrawResultCorrectedEvent`
- `DrawCancelledEvent`
- `DrawSettledEvent`
- `DomainEventPublisher`
- `ProcessedEventPort`
- `DrawCacheEvictor`

## Règles communes

- [ ] Tous les cross-domain effects doivent être `AFTER_COMMIT`.
- [ ] Les event classes appartiennent au bounded context producteur.
- [ ] Les listeners appartiennent au bounded context consommateur.
- [ ] Un listener de `core.draw` n'injecte pas d'infra evictor d'un autre domaine.
- [ ] Idempotency via `markProcessedIfAbsent` ou équivalent atomique.
- [ ] Pas de `alreadyProcessed` puis `markProcessed` non atomique.

## P0 — event publishing depuis handlers

- [ ] Construire l'event avant `AfterCommit.run(...)`.
- [ ] Ne pas faire de lookup dans le lambda `AfterCommit`.
- [ ] Utiliser `EventId.of(idGenerator.newUuid())`.
- [ ] Utiliser un `Instant eventTime = clock.instant()` stable.
- [ ] Publier après save DB.

Pattern :

```java
var event = new DrawCancelledEvent(...);
AfterCommit.run(() -> eventPublisher.publish(event));
```

## P0 — `DrawResultAppliedEvent`

C'est l'event principal après attach result à un draw tenant.

- [ ] Publié seulement après update draw `CLOSED -> RESULTED`.
- [ ] Contient `tenantId`.
- [ ] Contient `drawId`.
- [ ] Contient `drawChannelId`.
- [ ] Contient `resultSlotId`.
- [ ] Contient `drawResultId`.
- [ ] Contient `drawDate`.
- [ ] Contient `eventTime`.
- [ ] Pas de champs importants null.

## P0 — `DrawResultCorrectedEvent`

- [ ] Publié après correction tenant draw attachment.
- [ ] Contient previousDrawResultId + correctedDrawResultId.
- [ ] Contient reason.
- [ ] Ne signifie pas mutation du draw_result global.
- [ ] Consumers sales/payout/cache doivent être idempotents.

## P0 — `DrawCancelledEvent`

- [ ] Publié après cancel réussi.
- [ ] Contient reason.
- [ ] Ne pas publier si command no-op/duplicate.
- [ ] Guard sales avant publication.

## P1 — `DrawSettledEvent`

Deferred jusqu'à finalisation sales.

- [ ] Ne jamais publier avec `resultSlotId = null`.
- [ ] Publier seulement si transition réelle `RESULTED -> SETTLED`.
- [ ] Consumers sales/stats/cache idempotents.
- [ ] Vérifier relation avec future settlement sales/ledger.

## P0 — `DrawEventListener`

- [x] Utiliser `@TransactionalEventListener(phase = AFTER_COMMIT)`.
- [x] Utiliser `DrawCacheEvictor` local au domaine draw.
- [ ] Idempotency atomique `markProcessedIfAbsent`.
- [ ] Remplacer `evictAll()` plus tard par targeted eviction.
- [ ] Loguer event type, eventId, tenantId, drawId.
- [ ] Ne pas avaler silencieusement les exceptions critiques.

## P1 — split listeners

À terme séparer :

- [ ] `DrawCacheInvalidationListener`
- [ ] `DrawNotificationListener`
- [ ] `DrawStatsProjectionListener`
- [ ] `DrawSettlementListener` si applicable

## DrawResult events boundary

- [ ] `DrawResultIngestedEvent` = global provider ingestion seulement.
- [ ] Ne pas utiliser `DrawResultIngestedEvent` pour effects tenant draw.
- [ ] `DrawResultAppliedEvent` = event tenant important après attach à draw.

## Définition de terminé

- Tous les events métier sortent après commit.
- Les listeners sont idempotents.
- Aucun event important avec IDs null.
- Cache invalidation draw reste dans `core.draw`.
