# Feature Ops

> BFF d'exploitation plateforme : déclencher des jobs, piloter les gates, orchestrer fetch/apply résultats.  
> Audience : `SUPER_ADMIN` uniquement — tous les endpoints sont sur `/platform/ops/**`.

---

## Rôle

`features.ops` orchestre des commandes core existantes sans porter d'invariants métier.  
Il agrège `common.batch` (registre, gates, exécutions), `core.draw` et `core.drawresult`.

**Ne contient pas** : `CommandHandler`, `VoidCommandHandler`, règles métier tenant, écritures tables core.

---

## Endpoints

### Draws — lifecycle manuel

```http
POST /platform/ops/draws/generate       ← GenerateDrawsForRangeCommand (gate: draw:lifecycle:generate)
POST /platform/ops/draws/open-today     ← OpenTodayDrawsCommand (gate: draw:lifecycle:open)
POST /platform/ops/draws/close-due      ← CloseDueDrawsCommand (gate: draw:lifecycle:close)
POST /platform/ops/draws/apply          ← ApplyExternalResultsWindowCommand (gate: results:external:apply)
```

Toutes auditées (`@AuditLog`). Toutes vérifient le gate avant exécution.

### Draw results — ingestion et correction

```http
POST /platform/ops/draw-results/fetch    ← FetchExternalResultsWindowCommand (gate: results:external:fetch)
POST /platform/ops/draw-results/refresh  ← fetch + apply (gate: results:external:refresh)
POST /platform/ops/draw-results/override ← OverrideDrawResultCommand (gate: results:external:override)
POST /platform/ops/draw-results/manual   ← RecordManualDrawResultCommand (gate: results:external:manual)

GET  /platform/ops/draw-results                  ← liste paginée
GET  /platform/ops/draw-results/{drawResultId}   ← détail
GET  /platform/ops/draw-results/by-slot          ← recherche par slot/date
```

### Batch jobs — registre et exécution

```http
GET  /platform/ops/batch/jobs                       ← liste des jobs enregistrés
GET  /platform/ops/batch/jobs/{jobKey}              ← métadonnées d'un job
POST /platform/ops/batch/jobs/{jobKey}:start        ← démarrage manuel
GET  /platform/ops/batch/executions?job_key=...     ← exécutions récentes
GET  /platform/ops/batch/executions/{executionId}   ← détail exécution
```

### Batch gates — activer/désactiver les schedulers

```http
GET  /platform/ops/batch/gates/{jobKey}             ← état du gate (+ tenant_id optionnel)
GET  /platform/ops/batch/gates:effective            ← état effectif multi-gates
PUT  /platform/ops/batch/gates/{jobKey}             ← activer/désactiver
```

---

## Job keys connus

| Job key | Description |
|---|---|
| `draw:lifecycle:generate` | Génération des draws J→J+7 |
| `draw:lifecycle:open` | Ouverture des draws à l'heure |
| `draw:lifecycle:close` | Fermeture des draws au cutoff |
| `draw:lifecycle:settle` | Settlement des tickets après tirage |
| `results:external:fetch` | Ingestion résultats providers |
| `results:external:apply` | Application résultats aux draws |
| `results:external:refresh` | Fetch + apply (orchestration BFF) |
| `results:external:manual` | Saisie manuelle résultat |
| `results:external:override` | Override résultat existant |

---

## Pattern refresh

`POST /platform/ops/draw-results/refresh` est une orchestration BFF pure :

```
1. Vérifie gate RESULTS_EXTERNAL_REFRESH
2. FetchExternalResultsWindowCommand → core.drawresult
3. ApplyExternalResultsWindowCommand → core.draw
4. Retourne réponse consolidée des deux opérations
```

Pas de command handler dédié dans `features.ops` — c'est une composition de commandes core.

---

## Écart documenté

`OpsBatchService` écrit les gates directement via `catalog.settings.internal.persistence.SettingRepository`.  
Écart temporaire et borné — uniquement pour piloter les paramètres techniques batch.  
Migration cible : exposer un contrat write dans `catalog.settings.api`.

---

## Références

- Cycles draw : `tchalanet-docs/docs/02-functional/flows/draw-execution.md`
- Draw domain : `core/draw/DOMAIN_DRAW.md`
- DrawResult domain : `core/drawresult/DOMAIN_DRAWRESULT.md`
