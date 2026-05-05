# DrawResult & US Lottery — Architecture Finale

## 🎯 Objectif

Simplifier, clarifier et stabiliser le pipeline :

- ingestion des résultats externes
- projection Haïti
- persistance
- exposition aux autres domaines (draw, sales, ops)

---

## 🧱 Principe fondamental

result_slot → décide QUOI fetch  
uslottery → fetch COMMENT  
drawresult → décide COMMENT UTILISER

---

## 🧩 Séparation des responsabilités

- uslottery : Fetch + normalisation provider
- drawresult : Mapping + projection + persist
- draw : Lifecycle métier

---

## 🔥 Modèle unifié

### ❌ supprimé

ExternalResultOutput, US\_\* codes, mapping composite, external_draw_type

### ✅ nouveau modèle

DrawResult(
occurredAt,
status,
source,
quality,
sourceHash,
fetchedAt,
sourceResult,
haitiResult,
rawPayload,
overrideReason
)

---

## 🧠 Projection métier

DrawResultProjection :

- lot1
- lot2
- lot3
- lot4
- derivedPairs

---

## 📦 Read Model

DrawResultView :

- id
- slotKey
- occurredAt
- status
- source
- quality
- sourceHash
- fetchedAt
- sourceResult
- haitiResult
- rawPayload
- overrideReason

---

## 🔌 Ports

DrawResultReaderPort :

- getById
- findViewById
- findViewBySlotKeyAndOccurredAt
- findProjectionById
- findProjectionBySlotKeyAndOccurredAt
- findViewsByCriteria

---

## 🧮 Repository

Critères :

- slotKey
- status
- quality
- date range

---

## ⚠️ UPSERT fix

WHEN status IN (...) AND force = false → keep  
ELSE → overwrite

---

## 🔄 Pipeline

Fetch → uslottery → projection Haïti → persist  
Apply → draw_result attach → event

---

## ⚙️ Scheduler

- tick unique
- slot-driven
- cooldown

---

## 🧪 OPS API

GET:

- /platform/ops/draw-results
- /platform/ops/draw-results/{id}
- /platform/ops/draw-results/by-slot

POST:

- /fetch
- /refresh
- /override
- /manual

---

## 🏁 Résultat

✔ architecture simplifiée  
✔ découplage clair  
✔ pipeline stable  
✔ prêt pour draw/sales

---

## 13. Analysis V1 (2026-05-05) — Flow Validation

### Critical Path Validated

✅ **FetchExternalResultsWindowCommand flow**:

- Global, not tenant-scoped
- Resolve slots by key (NY_MID, FL_EVE, etc.)
- Fetch external results via ExternalResultFetcher
- Project to Haiti space via HaitiProjectionService
- Upsert by (result_slot_id, occurred_at) with idempotency via sourceHash
- Clamp daysBack (0-7), maxSlots (0-100) per config
- Return counters: inserted, updated, skipped, errors, dryRunMatched

✅ **Upsert Idempotency**:

- Key: (result_slot_id, occurred_at)
- Source hash for change detection
- Status CONFIRMED is terminal (blocks re-fetch unless force=true)
- Status PROVISIONAL/OVERRIDDEN: updatable
- force=true requires reason (audit)

✅ **Record Manual DrawResult flow**:

- Manual numbers input (tenant-scoped)
- Assemble DrawResult with DrawSource.MANUAL
- Upsert with audit trail
- Publish DrawResultIngestedEvent

✅ **Override flow**:

- Load existing DrawResult
- Update status to OVERRIDDEN
- Update numbers
- Publish DrawResultIngestedEvent
- Trigger cache eviction of related draws

### Architecture Compliance

- ✅ Global scope: No tenant_id filtering
- ✅ Upsert pattern: By result_slot_id + occurred_at
- ✅ Fetch by result_slot_key: Never by draw_channel_code
- ✅ Idempotency: sourceHash for duplicate detection
- ✅ Events: DrawResultIngestedEvent published after commit
- ✅ Typed IDs: EventId, ResultSlotId, DrawResultId

### Configuration

- Props limits: maxSlotsPerTick=100, hardDaysBack=7
- Props defaults: manualDaysBack=0, manualMaxSlots=50
- Scheduler: 5-min intervals, cooldown configurable
- Cache eviction: On result override (draw cache)
