# CATALOG_RESULTSLOT

> Référentiel global des créneaux de résultats (slots) de tirage.  
> Un slot = un créneau provider (NY Midday, FL Evening, etc.) avec sa configuration d'ingestion et de projection.  
> Catalog global — pas de tenant-scope, pas d'événements.

---

## Rôle

`catalog.resultslot` répond à : **quels créneaux de résultats sont disponibles sur la plateforme ?**

Un slot est le point de rattachement entre un provider externe (NY Lottery, FL Lottery…) et le système Tchalanet. Il porte la configuration technique d'ingestion (`sourceCfg`) et la configuration de projection Haïti (`projectionCfg`).

---

## Modèle — `ResultSlotView`

| Champ | Type | Sémantique |
|---|---|---|
| `id` | `ResultSlotId` | Identifiant |
| `slotKey` | `String` | Clé stable, ex: `NY_MID`, `FL_EVE` |
| `provider` | `String` | Nom du provider, ex: `NY`, `FL` |
| `timezone` | `ZoneId` | Fuseau horaire du tirage |
| `drawTime` | `LocalTime` | Heure du tirage dans `timezone` |
| `daysOfWeek` | `String` | Jours actifs (CSV ou expression) |
| `active` | `boolean` | Slot actif |
| `sourceCfg` | `JsonNode` | Config technique d'ingestion provider (URL, clés API, format) |
| `projectionCfg` | `JsonNode` | Config de projection Haïti (lot1..lot4 depuis pick3+pick4) |
| `labelKey` | `String` | Clé i18n, ex: `slot.ny_mid.label` |

`sourceCfg` et `projectionCfg` sont des blobs JSON opaques pour `catalog.resultslot` — interprétés par `core.uslottery` et `core.haiti`.

---

## API publique — `ResultSlotCatalog`

```java
List<ResultSlotView> listActive()
Optional<ResultSlotView> findByKey(String slotKey)
ResultSlotView requireByKey(String slotKey)   // throws si absent
Optional<ResultSlotView> findById(ResultSlotId id)
ResultSlotStatsView stats()
```

---

## Calendrier et overrides — `ResultSlotCalendarCatalog`

`ResultSlotCalendarCatalog` gère les exceptions au calendrier habituel d'un slot (jours fériés, fermetures ponctuelles, reprises).

```java
List<ResultSlotCalendarOverrideView> listBySlot(ResultSlotId resultSlotId)
  // Retourne les overrides spécifiques ET récurrents
```

### `ResultSlotCalendarOverrideView`

| Champ | Type | Sémantique |
|---|---|---|
| `id` | `ResultSlotCalendarOverrideId` | — |
| `resultSlotId` | `ResultSlotId` | Slot concerné |
| `slotLocalDate` | `LocalDate?` | Date spécifique — **XOR** avec `recurringMd` |
| `recurringMd` | `String?` | Date récurrente annuelle format `MM-dd`, ex: `01-01` — **XOR** avec `slotLocalDate` |
| `available` | `boolean` | `false` = fermé ce jour, `true` = ouvert malgré calendrier |
| `reasonCode` | `String` | Code raison (ex: `HOLIDAY`, `SPECIAL_EVENT`) |
| `reasonLabel` | `String` | Libellé libre |

**Invariant XOR** : exactement l'un de `slotLocalDate` ou `recurringMd` est non-null.  
`recurringMd` = override récurrent chaque année (ex: `01-01` = Jour de l'An).  
`slotLocalDate` = exception ponctuelle.

---

## Invariants

- `slotKey` unique et stable — ne doit pas changer après la création
- `sourceCfg` et `projectionCfg` : format défini par le provider cible, pas validé ici
- Slot inactif (`active=false`) : les draws liés à ce slot ne génèrent plus de résultats automatiques

---

## Position dans le pipeline

```
catalog.resultslot
    ↓ findByKey(slotKey)
core.drawresult : FetchExternalResultsWindowCommand
    → lit sourceCfg pour appeler le provider
    → lit projectionCfg pour projection Haïti (core.haiti)
    → crée draw_result

catalog.drawchannel
    → resultSlotId pointe vers ce slot
    → core.draw lie le draw au draw_result via resultSlotId
```

---

## Cache

- `listActive()` : TTL long (~24h) — les slots changent rarement
- Invalidation manuelle via `ResultSlotAdminService` à chaque write

---

## Références

- Draw channel : `catalog/drawchannel/CATALOG_DRAWCHANNEL.md`
- Pipeline résultats : `tchalanet-docs/docs/02-functional/flows/draw-execution.md`
- Domaine drawresult : `core/drawresult/DOMAIN_DRAWRESULT.md`
