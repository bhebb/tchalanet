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
