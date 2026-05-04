# TODO — `core.draw` query handlers et summaries

## Files principaux

- `GetDrawByIdQueryHandler`
- `ListDrawsHandler`
- `ListNextDrawsHandler`
- `ListLatestDrawsWithResultsHandler`
- `GetDrawResultsQueryHandler`
- `DrawSearchCriteria`
- `DrawSummaryView` / `DrawSummary`
- `DrawSummaryReaderPort`
- future `DrawSummaryJdbcRepository`

## P0 — query handlers à garder

Garder dans `core.draw` :

- [x] `GetDrawByIdQueryHandler`
- [x] `ListDrawsHandler`
- [x] `ListNextDrawsHandler`
- [x] `ListLatestDrawsWithResultsHandler`

Ajouter validations communes :

- [ ] `Objects.requireNonNull(query, "query is required")`.
- [ ] `Objects.requireNonNull(query.pageable(), "pageable is required")` si pageable.
- [ ] Valider IDs obligatoires.
- [ ] Valider `lookaheadHours > 0`.
- [ ] Valider `limitPerChannel > 0`.
- [ ] Borne max : `lookaheadHours <= 168` par exemple.
- [ ] Borne max : `limitPerChannel <= 20` par exemple.

## P0 — supprimer/déplacer `GetDrawResultsQueryHandler`

Si `DrawSummary` contient déjà le résultat :

- [ ] Supprimer `GetDrawResultsQueryHandler` de `core.draw`.
- [ ] Supprimer endpoint `/draws/{id}/result` si redondant.
- [ ] Garder détails techniques résultat côté `ops` ou `core.drawresult`.

Si endpoint conservé :

- [ ] Ne jamais retourner un faux `DrawResultView` avec `PROVISIONAL` quand aucun résultat n'existe.
- [ ] Retourner 404, `Optional`, ou wrapper `available=false`.

## P0 — déplacer summary hors domain

- [ ] Ne pas utiliser `core.draw.domain.model.DrawSummary`.
- [ ] Déplacer vers : `core.draw.application.query.projection.DrawSummaryView` ou `core.draw.application.view.DrawSummary`.
- [ ] Les controllers retournent DTO web, pas domain object.

## P0 — `DrawSearchCriteria`

- [ ] Normaliser `resultSlotKeys` : trim, uppercase, distinct.
- [ ] Clarifier liste vide : tous les slots ou erreur.
- [ ] Normaliser `channelCode` si encore utilisé côté query read model.
- [ ] Borner dates : ne pas accepter range illimitée par défaut pour admin list.
- [ ] Différencier critères `drawDate` vs `scheduledAt`.

## P0 — summary read model cible

`DrawSummary` / `DrawSummaryView` doit contenir assez pour admin/public without extra endpoint :

- [ ] `drawId`
- [ ] `tenantId` si nécessaire côté admin interne seulement
- [ ] `drawDate`
- [ ] `scheduledAt`
- [ ] `cutoffAt`
- [ ] `status`
- [ ] `locked`
- [ ] `drawChannelId`
- [ ] `drawChannelCode`
- [ ] `drawChannelLabel`
- [ ] `resultSlotId`
- [ ] `resultSlotKey`
- [ ] `resultSlotLabel` si disponible
- [ ] `drawResultSummary` léger

`DrawResultSummary` léger :

- [ ] `id`
- [ ] `status`
- [ ] `occurredAt`
- [ ] `haitiResult` ou champs projetés utiles
- [ ] pas de raw payload provider
- [ ] pas de debug/source_payload complet

## P0 — éviter N+1

- [ ] `ListDraws` ne doit pas appeler `DrawResultReaderPort.getById` par draw.
- [ ] `ListLatestDrawsWithResults` doit être une query SQL optimisée.
- [ ] `GetDrawById` peut utiliser une query summary join unique.

## P1 — `listNext`

Définition : prochains draws ouverts/à venir par channel/slot.

- [ ] Filtrer `scheduled_at >= now`.
- [ ] Exclure `CANCELED`, `ARCHIVED`, `SETTLED`.
- [ ] Optionnel : inclure `SCHEDULED` et `OPEN` seulement.
- [ ] Si `resultSlotId` fourni : filter via `draw_channel.result_slot_id`.
- [ ] `limitPerChannel` doit être appliqué par channel si requis via window function.

Exemple SQL pattern :

```sql
select * from (
  select d.*, row_number() over(partition by d.draw_channel_id order by d.scheduled_at asc) rn
  from draw d
  join draw_channel dc on dc.id = d.draw_channel_id
  where d.tenant_id = ?
    and d.deleted_at is null
    and d.status in ('SCHEDULED', 'OPEN')
    and d.scheduled_at >= ?
) x
where rn <= ?
order by scheduled_at asc
```

## P1 — `listLatestWithResults`

- [ ] Filtrer `d.draw_result_id is not null`.
- [ ] Filtrer `d.status in ('RESULTED', 'SETTLED')` si business.
- [ ] Join `draw_result` pour résultat léger.
- [ ] Trier `scheduled_at desc` ou `draw_date desc, scheduled_at desc`.
- [ ] Filtrer via `result_slot_keys` si fourni.

## Définition de terminé

- Draw pages peuvent afficher résultats directement depuis summaries.
- Pas d'endpoint résultat séparé sauf ops/debug.
- Pas de N+1.
- Summary projection hors domaine.
