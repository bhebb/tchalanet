# CATALOG_DRAWCHANNEL

> Référentiel tenant-scoped des canaux de vente pour les tirages.  
> Lie un canal tenant (`HT_NY_MID`) à un slot global (`NY_MID`).  
> Catalog pur — pas de lifecycle métier, pas d'événements.

---

## Rôle

`catalog.drawchannel` répond à : **quels canaux de vente ce tenant a-t-il configurés ?**

Il porte la configuration tenant-facing d'un canal (label, horaire, jeux actifs, fuseau horaire) et pointe vers un `result_slot` global quand le canal est piloté par un provider externe.

---

## Modèle — champs clés

| Champ | Type | Sémantique |
|---|---|---|
| `code` | `String` | Identifiant stable tenant, ex: `HT_NY_MID` |
| `label` | `String` | Label affiché vendeur, ex: `Haïti • New York Midi` |
| `timezone` | `ZoneId` | Fuseau horaire du tirage |
| `drawTime` | `LocalTime` | Heure du tirage dans `timezone` |
| `cutoffSec` | `Integer` | Secondes avant `drawTime` → calcul de `cutoffAt` |
| `daysOfWeek` | `List<DayOfWeek>` | Jours actifs (null = tous les jours) |
| `active` | `boolean` | Canal actif — seuls les actifs génèrent des draws |
| `sortOrder` | `int` | Ordre d'affichage |
| `resultSlotId` | `ResultSlotId?` | FK vers `catalog.resultslot` (null → résultat manuel obligatoire) |
| `defaultSource` | `DrawSource` | `AUTO` (provider) ou `OPS` (manuel) |

### Calcul du cutoff

```
cutoffAt = drawDate.atTime(drawTime, timezone).minus(cutoffSec seconds)
```

`core.draw` snapshot `cutoffAt` à la génération — modifier le channel n'affecte pas les draws déjà créés.

---

## API publique (`DrawChannelCatalog`)

| Méthode | Retour |
|---|---|
| `listAll(tenantId, activeOnly)` | `List<DrawChannelSummaryView>` |
| `findById(tenantId, id)` | `Optional<DrawChannelView>` |
| `findByCode(tenantId, code)` | `Optional<DrawChannelView>` |
| `listWithGames(tenantId, activeOnly)` | `List<ChannelGamesView>` |
| `listCalendar(tenantId, criteria)` | `List<DrawChannelCalendarRow>` |

Tous les appelants passent par `DrawChannelCatalog` — jamais par `internal.*`.

---

## Position dans le pipeline draw

```
catalog.drawchannel
    ↓ listAll(tenantId, activeOnly=true)
core.draw : GenerateDrawsForRangeCommand
    → Crée un Draw par (tenant, channel, date) active
    → snapshot cutoffAt depuis channel.cutoffSec + timezone
    ↓ résultat disponible dans catalog.resultslot
core.draw : ApplyExternalResultsWindowCommand
    → lie draw au draw_result via resultSlotId
```

---

## Invariants

- `UNIQUE(tenant_id, code)` — un code channel par tenant
- `resultSlotId` null → canal sans provider, résultat manuel obligatoire
- Changer `cutoffSec` ou `drawTime` n'affecte que les draws générés après la modification
- `active = false` → plus de nouveaux draws générés, draws existants non impactés

---

## Non-responsabilités

- Pas d'événements domaine
- Pas de transitions d'état draw
- Pas d'appels HTTP provider (rôle de `core.uslottery`)
- Pas de projection Haïti (rôle de `core.haiti`)
- Pas de settlement tickets

---

## Références

- Slot global : `catalog/resultslot/CATALOG_RESULTSLOT.md`
- Jeux du canal : `catalog/game/CATALOG_GAME.md`
- Pipeline draw : `tchalanet-docs/docs/02-functional/flows/draw-execution.md`
