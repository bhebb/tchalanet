# TODO — `core.draw` persistence critique

## Files principaux

- `DrawJpaEntity`
- `DrawMapper`
- `DrawJpaRepository`
- `DrawApplyJdbcRepository`
- `DrawApplyJdbcAdapter`
- `DrawLookupJdbcRepository`
- `DrawLookupPersistenceAdapter`
- `DrawLifecycleJpaAdapter`
- `DrawBatchQueryRepository`
- `SettleableDrawIdsJpaAdapter`
- Flyway migration draw indexes/constraints

## P0 — `DrawJpaEntity`

### 1. Entity découplée

État actuel bon :

- [x] `drawChannelId` UUID.
- [x] `drawResultId` UUID.
- [x] Pas de `@ManyToOne DrawChannelEntity`.
- [x] Timestamps en `Instant`.

À vérifier :

- [ ] Aucun getter `getDrawChannel()` restant dans `DrawJpaEntity`.
- [ ] Aucun champ `resultSlotId` dans `DrawJpaEntity`.
- [ ] Aucun champ provider/channelCode/external key dans `DrawJpaEntity`.

### 2. Unique constraint + soft delete

Problème : `@UniqueConstraint` classique ne correspond pas à `ON CONFLICT ... WHERE deleted_at IS NULL`.

- [ ] Retirer le `@UniqueConstraint` JPA si soft delete doit permettre recréation après delete.
- [ ] Créer un unique index partiel Flyway :

```sql
CREATE UNIQUE INDEX IF NOT EXISTS uq_draw_tenant_channel_date_active
ON draw (tenant_id, draw_channel_id, draw_date)
WHERE deleted_at IS NULL;
```

- [ ] Adapter `bulkInsert` pour cibler l'index partiel :

```sql
ON CONFLICT (tenant_id, draw_channel_id, draw_date)
WHERE deleted_at IS NULL DO NOTHING
```

- [ ] Vérifier que le nom d'index/constraint existe réellement en DB.

## P0 — `DrawLookupJdbcRepository`

### Bug critique

Ne pas utiliser :

```sql
d.result_slot_id = ?
```

Le draw ne stocke pas `result_slot_id`.

### Patch cible

```sql
select d.id
from draw d
join draw_channel dc on dc.id = d.draw_channel_id
where d.deleted_at is null
  and dc.deleted_at is null
  and d.tenant_id = ?
  and d.draw_date = ?
  and dc.result_slot_id = ?
limit 1
```

- [ ] Corriger `findDrawIdBySlotId(...)` avec le join `draw_channel`.
- [ ] Supprimer try/catch RuntimeException inutile ou laisser Spring wrapper.
- [ ] Ajouter null-checks en adapter, pas forcément en repository bas niveau.

## P0 — `DrawApplyJdbcRepository`

### 1. Vérifier que le result correspond au même slot

Ajouter `draw_result dr` dans le SQL pour empêcher un mauvais attach :

```sql
update draw d
set draw_result_id = ?,
    status = 'RESULTED',
    resulted_at = ?,
    result_source = 'AUTO',
    updated_at = ?
from draw_channel dc, draw_result dr
where dc.id = d.draw_channel_id
  and dr.id = ?
  and dr.result_slot_id = dc.result_slot_id
  and d.tenant_id = ?
  and d.draw_date = ?
  and dc.result_slot_id = ?
  and d.deleted_at is null
  and dc.deleted_at is null
  and d.locked = false
  and d.status = 'CLOSED'
  and d.draw_result_id is null
returning d.id, d.draw_channel_id
```

- [ ] Ajouter paramètre `drawResultId` une deuxième fois pour `dr.id`.
- [ ] Vérifier `dr.status` si nécessaire : exclure `REJECTED/INVALID` si ces statuts existent.
- [ ] Décider si `PROVISIONAL` est auto-applicable en MVP.

### 2. Clarifier `force`

Actuellement `force` est ignoré.

Option recommandée MVP :

- [ ] Retirer `force` du port apply automatique **ou** documenter : `force intentionally ignored by automatic apply`.
- [ ] Garder les corrections dans `CorrectAppliedDrawResultCommandHandler`.

Si `force=true` doit être supporté plus tard :

- [ ] Ajouter guard sales avant SQL.
- [ ] Ne pas remplacer un draw `SETTLED`.
- [ ] Audit obligatoire.

## P0 — `DrawLookupPersistenceAdapter`

### 1. Supprimer duplication

- [ ] Vérifier que la classe n'est pas dupliquée dans le projet.

### 2. Supprimer N+1

Ne pas faire :

```java
if (e.getDrawResultId() != null) {
    dr = drawResultReader.getById(DrawResultId.of(e.getDrawResultId()));
}
```

- [ ] Créer `DrawSummaryJdbcRepository` qui retourne directement les summaries enrichies.
- [ ] Une seule query doit joindre `draw`, `draw_channel`, `result_slot`, `draw_result` pour les read models.
- [ ] Ne pas appeler `drawResultReader.getById` dans une boucle.

### 3. Supprimer `e.getDrawChannel()`

- [ ] `DrawJpaEntity` n'a plus de relation JPA channel.
- [ ] Les infos channel doivent venir d'une projection SQL.

### 4. Supprimer hack `current=true`

Ne pas faire :

```java
mutable.set(0, new DrawSummaryView(... current=true ...));
```

- [ ] Supprimer ce hack.
- [ ] Définir `current` par une vraie règle SQL si le champ reste nécessaire.
- [ ] Sinon supprimer `current` du view model.

### 5. Dates criteria

- [ ] Pour critères `from/to` basés sur `drawDate`, filtrer sur `d.draw_date`, pas `scheduled_at UTC`.
- [ ] Pour `next`, filtrer sur `scheduled_at >= now`.
- [ ] Éviter `Instant.ofEpochSecond(Long.MAX_VALUE)`.

## P0 — `DrawLifecycleJpaAdapter` / `DrawJpaRepository`

### 1. Remplacer `nowEpoch`

Actuel : `to_timestamp(:nowEpoch)`.

Cible : passer des `Instant`/`Timestamp` ou des bornes calculées.

- [ ] `findDueToClose(now, limit)` avec `d.cutoff_at <= :now`.
- [ ] `findOpenable(from, to, now, limit)` avec :

```sql
d.scheduled_at >= :from
and d.scheduled_at <= :to
and d.cutoff_at > :now
```

- [ ] Calculer `from = now.minus(openLagHours, HOURS)`.
- [ ] Calculer `to = now.plus(openHorizonHours, HOURS)`.

### 2. `bulkOpen` / `bulkClose`

Actuel : `opened_at = now()`, `closed_at = now()` DB.

Cible optionnelle : passer `Instant now` depuis handler.

- [ ] Décider DB time vs application `Clock`.
- [ ] Si application time : changer signature `bulkOpen(ids, now)` / `bulkClose(ids, now)`.
- [ ] Garder guards SQL : status + locked + deleted.

## P1 — `DrawSummaryJdbcRepository`

Créer un repository dédié pour summaries :

```java
@Repository
@RequiredArgsConstructor
public class DrawSummaryJdbcRepository {
    private final JdbcTemplate jdbc;

    public DrawSummaryView getById(UUID drawId) { ... }
    public List<DrawSummaryView> findByCriteria(DrawSearchCriteria criteria, Pageable pageable) { ... }
    public List<DrawSummaryView> listNext(DrawSearchCriteria criteria, Pageable pageable) { ... }
    public List<DrawSummaryView> listLatestWithResults(DrawSearchCriteria criteria, Pageable pageable) { ... }
}
```

Query base :

```sql
select
  d.id as draw_id,
  d.tenant_id,
  d.draw_date,
  d.scheduled_at,
  d.cutoff_at,
  d.status,
  d.locked,
  dc.id as draw_channel_id,
  dc.code as channel_code,
  dc.name as channel_name,
  dc.timezone as channel_timezone,
  rs.id as result_slot_id,
  rs.slot_key as result_slot_key,
  dr.id as draw_result_id,
  dr.status as draw_result_status,
  dr.occurred_at as draw_result_occurred_at,
  dr.haiti_result as haiti_result
from draw d
join draw_channel dc on dc.id = d.draw_channel_id
left join result_slot rs on rs.id = dc.result_slot_id
left join draw_result dr on dr.id = d.draw_result_id
where d.deleted_at is null
  and dc.deleted_at is null
  and d.tenant_id = ?
```

- [ ] Ajouter count query si pagination complète nécessaire.
- [ ] Mapper JSON `haiti_result` proprement.
- [ ] Ne pas exposer raw provider payload dans summary.

## P1 — settlement persistence

`DrawBatchQueryRepository` est plus propre qu'avant.

- [ ] Garder tenant-scoped.
- [ ] Garder `status = 'RESULTED'`.
- [ ] Garder `draw_result_id is not null`.
- [ ] Supprimer ou clarifier `force` car status `SETTLED` ne sera pas sélectionné de toute façon.
- [ ] Utiliser `DrawId.of`, pas `nullableOf`.
- [ ] Valider `maxDraws > 0`.
- [ ] Garder settlement deferred jusqu'à alignement `core.sales`.

## Indexes recommandés

- [ ] `draw(tenant_id, draw_date)`.
- [ ] `draw(tenant_id, scheduled_at)`.
- [ ] `draw(status, scheduled_at)`.
- [ ] `draw(status, cutoff_at)`.
- [ ] `draw(draw_result_id)`.
- [ ] `draw(tenant_id, status, cutoff_at)`.
- [ ] `draw(tenant_id, status, scheduled_at)`.
- [ ] `draw_channel(id, result_slot_id)`.
- [ ] `draw_channel(tenant_id, result_slot_id)` si tenant-scoped.
- [ ] `draw_result(id, result_slot_id)`.

## Définition de terminé

- Aucun SQL `d.result_slot_id`.
- Aucun N+1 summary.
- Aucun `DrawChannelEntity` dans draw entity/domain.
- Apply result vérifie `draw_result.result_slot_id = draw_channel.result_slot_id`.
- Unique active draw par tenant/channel/date via index partiel Flyway.
