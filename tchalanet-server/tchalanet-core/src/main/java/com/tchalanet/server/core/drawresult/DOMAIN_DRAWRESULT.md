# Domaine `core.drawresult` — Résultats de Tirages

> Ingestion, projection haïtienne et persistance des résultats de tirages externes. Scope global (pas de tenant_id). Source de vérité pour les résultats utilisés par `core.draw` et `core.sales`.

> Flow associé : `tchalanet-docs/docs/02-functional/flows/draw-execution.md`
> Ops API : `features/ops/FEATURE_OPS.md`

---

## 1. Séparation des responsabilités

| Module | Rôle |
|---|---|
| `core.uslottery` | Fetch + normalisation provider (NY, FL, GA…) |
| `core.drawresult` | Mapping, projection Haïti, persist, exposition |
| `core.draw` | Lifecycle métier des draws tenant |

Règle : `result_slot` décide QUOI fetcher — `uslottery` fetch COMMENT — `core.drawresult` décide COMMENT UTILISER.

---

## 2. Modèle — `DrawResult`

| Champ | Type | Sens |
|---|---|---|
| `id` | `DrawResultId` | — |
| `resultSlotId` | `ResultSlotId` | Slot source |
| `occurredAt` | `Instant` | Moment du tirage |
| `status` | `DrawResultStatus` | PROVISIONAL / CONFIRMED / OVERRIDDEN / ERROR |
| `source` | `DrawSource` | SYSTEM / AUTO / EXTERNAL / US_LOTTERY / NY_OPEN_DATA / FL_APIM / MANUAL / ADMIN_OVERRIDE |
| `quality` | `DrawResultQuality` | Qualité de la donnée source |
| `sourceHash` | `String` | Hash pour idempotence (change detection) |
| `fetchedAt` | `Instant` | Timestamp du fetch |
| `sourceResult` | `JsonNode` | Résultat brut du provider |
| `haitiResult` | `JsonNode` | Projection Haïti calculée |
| `rawPayload` | `String` | Payload brut (debug) |
| `overrideReason` | `String` | Raison de l'override si applicable |

### `DrawResultStatus`

| Valeur | Sens |
|---|---|
| `PROVISIONAL` | Résultat reçu, non encore confirmé — watchdog 30 min |
| `CONFIRMED` | Confirmé — terminal pour la settle |
| `OVERRIDDEN` | Remplacé par un résultat corrigé |
| `ERROR` | Erreur lors du fetch |

> Règle settle : `CONFIRMED` requis pour `RESULTED → SETTLED`. `PROVISIONAL` bloque le settlement.

---

## 3. Projection métier — `DrawResultProjection`

Projection calculée depuis `haitiResult` pour le calcul des gains :

- `lot1`, `lot2`, `lot3`, `lot4`
- `derivedPairs` — paires dérivées (Maryaj, Loto 4, etc.)
- `pick3`, `pick4` — quand disponibles depuis la source

---

## 4. API publique — `DrawResultReaderPort`

```java
DrawResult        getById(DrawResultId)
DrawResultView    findViewById(DrawResultId)
DrawResultView    findViewBySlotKeyAndOccurredAt(String slotKey, Instant)
DrawResultProjection findProjectionById(DrawResultId)
DrawResultProjection findProjectionBySlotKeyAndOccurredAt(String slotKey, Instant)
List<DrawResultView> findViewsByCriteria(DrawResultSearchCriteria)
```

### `DrawResultView` (read model)

`id, slotKey, occurredAt, status, source, quality, sourceHash, fetchedAt, sourceResult, haitiResult, rawPayload, overrideReason`

---

## 5. Commandes

| Commande | Sens |
|---|---|
| `FetchExternalResultsWindowCommand` | Fetch+upsert les résultats pour une fenêtre temps (global, non-tenant) |
| `RecordManualDrawResultCommand` | Saisie manuelle OPS (`DrawSource.MANUAL`) |
| `OverrideDrawResultCommand` | Override d'un résultat existant (`status → OVERRIDDEN`) |

---

## 6. Logique UPSERT (idempotence)

Clé d'upsert : `(result_slot_id, occurred_at)`

- Si `status == CONFIRMED` et `force = false` → conserver (terminal)
- Sinon → écraser avec le nouveau résultat
- `sourceHash` : si identique → skip (même donnée)
- `force=true` : bypasse la protection CONFIRMED, exige un `reason`

---

## 7. Pipeline runtime

```
fetch → uslottery → projection Haïti → upsert DrawResult
apply → core.draw attache le DrawResult → DrawResultAppliedEvent
settle → core.draw (requiert status=CONFIRMED) → DrawSettledEvent
```

---

## 8. Événements

| Événement | Déclencheur |
|---|---|
| `DrawResultIngestedEvent` | Après tout upsert (fetch, manual, override) |

Consommé par : `core.draw.DrawEventListener` → déclenche `ApplyDrawResultCommand`.

---

## 9. Scheduler

- Tick global unique (gate `RESULTS_EXTERNAL_FETCH`)
- Slot-driven : résout les `result_slot` actifs
- Cooldown configurable entre ticks
- Props : `maxSlotsPerTick=100`, `hardDaysBack=7`

---

## 10. OPS API (via `features.ops`)

```
GET /platform/ops/draw-results
GET /platform/ops/draw-results/{id}
GET /platform/ops/draw-results/by-slot
POST /platform/ops/draw-results/fetch
POST /platform/ops/draw-results/refresh
POST /platform/ops/draw-results/{id}/override
POST /platform/ops/draw-results/manual
```

---

## 11. Invariants

- Scope global — pas de `tenant_id` dans `draw_result`.
- Fetch par `result_slot_key`, jamais par `draw_channel_code`.
- Events publiés after-commit via `DrawResultIngestedEvent`.
- `force=true` uniquement sur `/platform/ops/**`, audité via `@AuditedForceCommand`.

---

## 12. Analyse V1 (2026-05-05) — Chemins critiques validés

- `FetchExternalResultsWindowCommand` : global, slot-driven, idempotent via sourceHash, compteurs `inserted/updated/skipped/errors`
- `RecordManualDrawResultCommand` : saisie tenant-scoped, `DrawSource.MANUAL`, upsert+audit
- `OverrideDrawResultCommand` : `status → OVERRIDDEN`, `DrawResultIngestedEvent`, eviction cache draw
- Architecture : scope global, upsert par `(result_slot_id, occurred_at)`, TypedIDs, events after-commit
