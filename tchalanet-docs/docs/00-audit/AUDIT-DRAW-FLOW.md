# Audit : Draw Flow

> Date : 2026-04-24  
> Scope : `core/draw/` · `core/drawresult/` · `catalog/drawchannel/` · `features/ops/` · `features/publicdraw/` · schedulers  
> Statut : 0 modification — rapport uniquement

---

## Ce qui existe (PRÉSENT / PARTIEL / ABSENT)

### DrawJpaEntity — structure et états

**PRÉSENT**

Table `draw`, champs complets :

| Champ                    | Type       | Rôle                             |
| ------------------------ | ---------- | -------------------------------- |
| `draw_channel_id`        | UUID (FK)  | Canal de tirage                  |
| `draw_date`              | LocalDate  | Date du tirage                   |
| `scheduled_at`           | Instant    | Heure planifiée                  |
| `cutoff_at`              | Instant    | Cutoff vente                     |
| `opened_at`              | Instant    | Ouverture effective              |
| `closed_at`              | Instant    | Fermeture effective              |
| `resulted_at`            | Instant    | Date d'attribution des résultats |
| `settled_at`             | Instant    | Date de settlement               |
| `canceled_at`            | Instant    | Date d'annulation                |
| `cancel_reason`          | String     | Raison annulation                |
| `status`                 | DrawStatus | État courant                     |
| `draw_result_id`         | UUID       | FK vers `draw_result`            |
| `system_generated`       | boolean    | Généré automatiquement           |
| `locked`                 | boolean    | Verrou sur le tirage             |
| `result_source`          | DrawSource | API/MANUAL/IMPORT                |
| `result_override_reason` | String     | Raison override                  |
| `result_overridden_at`   | Instant    | Date d'override                  |

Contrainte unique : `(tenantId, draw_channel_id, draw_date)` — un seul tirage par canal par date par tenant.

### Machine à états des tirages

**PRÉSENT**

```
SCHEDULED → OPEN → CLOSED → RESULTED → SETTLED → ARCHIVED
              ↓       ↓        ↓
           CANCELED (depuis SCHEDULED, OPEN ou CLOSED)
```

Classes d'implémentation :

- `DrawStatus` enum : SCHEDULED, OPEN, CLOSED, RESULTED, SETTLED, ARCHIVED, CANCELED
- `DrawStatusTransition` : règles de transition présentes
- Handlers : `OpenDueDrawsCommandHandler`, `CloseDueDrawsCommandHandler`, `SettleDrawsCommandHandler`

### DrawResultJpaEntity

**PRÉSENT**

Table `draw_result`, champs complets :

| Champ             | Type             | Rôle                                   |
| ----------------- | ---------------- | -------------------------------------- |
| `result_slot_id`  | UUID (FK)        | Créneau horaire (Result Slot catalog)  |
| `occurred_at`     | Instant          | Heure réelle du tirage source          |
| `source_result`   | jsonb            | Données brutes source (Miami/NY/TX...) |
| `haiti_result`    | jsonb            | Résultat normalisé Haiti (pick3/pick4) |
| `raw_payload`     | jsonb            | Payload HTTP brut                      |
| `flags`           | jsonb            | Meta flags (USING_FALLBACK, etc.)      |
| `status`          | DrawResultStatus | État (PENDING, APPLIED, etc.)          |
| `quality`         | ResultQuality    | Qualité du résultat                    |
| `source`          | DrawSource       | API/MANUAL/IMPORT                      |
| `source_hash`     | String (64)      | Déduplication                          |
| `fetched_at`      | Instant          | Date de récupération                   |
| `override_reason` | String           | Raison override                        |

### DrawCalendarOpsController — ce qu'il fait exactement

**PRÉSENT** — path `/platform/ops/draws`

| Endpoint          | Action                                                                                                                        |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| `POST /generate`  | Génère les tirages pour une plage de dates (`GenerateDrawsForRangeCommand`) — avec `dryRun` et `force`                        |
| `POST /open-due`  | Ouvre tous les tirages dont le `scheduled_at` est passé (`OpenDueDrawsCommand`)                                               |
| `POST /close-due` | Ferme tous les tirages dont le `cutoff_at` est passé (`CloseDueDrawsCommand`)                                                 |
| `POST /apply`     | Applique les `draw_result` aux draws correspondants (`ApplyExternalResultsWindowCommand`) — association `draw.draw_result_id` |

Tous les endpoints sont protégés par `BatchGate` (feature flag).

### DrawResultsOpsController — ce qu'il fait exactement

**PRÉSENT** — path `/platform/ops/draw-results`

| Endpoint         | Action                                                                                             |
| ---------------- | -------------------------------------------------------------------------------------------------- |
| `POST /fetch`    | Récupère les résultats depuis API externe dans `draw_result` (`FetchExternalResultsWindowCommand`) |
| `POST /refresh`  | Fetch + Apply en une seule opération (`RefreshExternalResultsWindowCommand`)                       |
| `POST /override` | Override manuel d'un résultat existant pour un slot/date (`OverrideDrawResultCommand`)             |
| `POST /manual`   | Enregistrement manuel d'un résultat (`RecordManualDrawResultCommand`)                              |

### Scheduler automatique

**PRÉSENT — FONCTIONNEL**

Trois schedulers dans `core/draw/infra/scheduler/` :

1. **`DrawLifeCycleTickScheduler`** : tick pour open/close automatique des tirages dus
2. **`DrawSettleScheduler`** : batch settle via Spring Batch (`DrawSettleJobConfig`)
3. **`ExternalResultsApplyTickScheduler`** : tick pour appliquer les résultats externes aux tirages

Dans `core/drawresult/infra/scheduler/` : 4. **`ExternalResultsFetchTickScheduler`** : tick qui interroge le `ResultSlotCatalog`, identifie les slots "due" (dans la fenêtre `minMinutesAfterDraw..maxMinutesAfterDraw`), déclenche le fetch. Rate-limité par cooldown par slot.

Configuration via `DrawResultsProperties` (YAML), protégée par `BatchGate`.

### Fetch externe résultats (API Miami/NY/TX/GA)

**PRÉSENT**

- `ExternalResultsFetchPort` : port d'extraction externe
- `ExternalDrawResultPort` : port de mapping résultat externe → Haiti format
- Adapters dans `core/uslottery/infra/adapter/` et `core/uslottery/infra/external/` : intégration API US Lottery
- `HaitiResultExtractors` : utilitaire de normalisation des résultats (pick3/pick4)
- `SourceResultBuilder` + `ExternalPickMapper` : builders dans drawresult
- `ResultSlotCatalog` (catalog) : catalogue des créneaux horaires (Miami=12h, NY=14h30, etc.)

---

## Ce qui manque pour v1

| Item                                                                                                                                                                                                               | Criticité | Estimation |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------- | ---------- |
| **Chain complète fetch → apply → settle → ticket results** : Le flux existe parcellaire mais le test end-to-end (fetch externe → apply sur draw → SettleDraws → RecordDrawTicketsResult → ledger) n'est pas validé | HAUTE     | **M**      |
| **Manual fallback UI** : Les endpoints `/manual` et `/override` existent mais il n'y a pas de workflow documenté pour le cas où l'API externe est Down (fallback manuel à la main OPS)                             | MOYENNE   | **S**      |
| **Securisation DrawAdminController** : `@PreAuthorize` commenté avec `//todo remove testing` — endpoint admin draws non protégé en prod                                                                            | CRITIQUE  | **S**      |
| **Endpoint tenant/draws** : Pas d'endpoint `GET /tenant/draws` pour permettre au caissier de voir les tirages disponibles (nécessaire pour la vente)                                                               | HAUTE     | **M**      |
| **DrawAdminController violations** : Retourne `ResponseEntity<>` partout au lieu de `ApiResponse<>`                                                                                                                | BASSE     | **S**      |
| **Tests draw lifecycle** : 0 tests unitaires sur le cycle de vie (open/close/settle)                                                                                                                               | HAUTE     | **L**      |

---

## Endpoints existants liés aux draws

| Méthode | Path                                        | Statut                                 | Rôles           |
| ------- | ------------------------------------------- | -------------------------------------- | --------------- |
| `GET`   | `/admin/draws`                              | ✅ PRÉSENT                             | ⚠️ NON SÉCURISÉ |
| `POST`  | `/admin/draws`                              | ✅ PRÉSENT                             | ⚠️ NON SÉCURISÉ |
| `PUT`   | `/admin/draws/{id}`                         | ✅ PRÉSENT                             | ⚠️ NON SÉCURISÉ |
| `POST`  | `/platform/ops/draws/generate`              | ✅ PRÉSENT                             | BatchGate       |
| `POST`  | `/platform/ops/draws/open-due`              | ✅ PRÉSENT                             | BatchGate       |
| `POST`  | `/platform/ops/draws/close-due`             | ✅ PRÉSENT                             | BatchGate       |
| `POST`  | `/platform/ops/draws/apply`                 | ✅ PRÉSENT                             | BatchGate       |
| `POST`  | `/platform/ops/draw-results/fetch`          | ✅ PRÉSENT                             | BatchGate       |
| `POST`  | `/platform/ops/draw-results/refresh`        | ✅ PRÉSENT                             | BatchGate       |
| `POST`  | `/platform/ops/draw-results/override`       | ✅ PRÉSENT                             | BatchGate       |
| `POST`  | `/platform/ops/draw-results/manual`         | ✅ PRÉSENT                             | BatchGate       |
| `GET`   | `/public/draw-results/...`                  | ✅ PRÉSENT                             | Public          |
| `GET`   | `/public/draw-results/{slotKey}`            | ✅ PRÉSENT                             | Public          |
| `GET`   | `/tenant/draws` (liste tirages pour vendre) | ❌ ABSENT                              | —               |
| `GET`   | `/tenant/draws/{id}/result`                 | ⚠️ PARTIEL (via DrawResultsController) | —               |

---

## Violations conventions

| Fichier                                              | Violation                                                                                    | Sévérité                                    |
| ---------------------------------------------------- | -------------------------------------------------------------------------------------------- | ------------------------------------------- |
| `DrawAdminController`                                | `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` commenté — `//todo remove testing`            | 🔴 CRITIQUE — sécurité                      |
| `DrawAdminController`                                | `ResponseEntity<>` partout au lieu de `ApiResponse<>` + `@ResponseStatus`                    | 🔴 VIOLATION                                |
| `DrawAdminController`                                | `TenantId.of(UUID)` via `contextResolver.currentOrNull()` au lieu de typed `@CurrentContext` | ⚠️ VIOLATION context usage                  |
| `DrawMapper`                                         | `@Autowired protected DrawChannelMapper drawChannelMapper` — field injection interdit        | 🔴 VIOLATION — constructor injection requis |
| `DrawCalendarOpsController.apply()`                  | Reçoit `DrawResultsOpsController.WindowRequest` — couplage direct entre deux controllers     | ⚠️ VIOLATION encapsulation                  |
| `DrawResultsController` (core/drawresult/infra/web/) | À vérifier le niveau de sécurisation                                                         | —                                           |

---

## Tests existants

| Fichier   | Type | Ce qu'il teste                               |
| --------- | ---- | -------------------------------------------- |
| _(aucun)_ | —    | Aucun test dans les packages draw/drawresult |

**Couverture globale** : 0% sur le domaine draw. Aucun test du cycle de vie, des schedulers, du fetch externe, du settlement.

---

## Dépendances vers autres domaines

```
Draw Flow dépend de :
  ├── CATALOG/drawchannel → DrawChannelEntity (planning + cutoff)
  ├── CATALOG/resultslot  → ResultSlotCatalog (créneaux horaires pour fetch externe)
  ├── CORE/uslottery      → ExternalDrawResultPort (intégration API Miami/NY/TX/GA)
  ├── CORE/tenantconfig   → TenantId (scope par tenant)
  └── CORE/sales          → RecordDrawTicketsResultCommandHandler (settle → résultats tickets)

Draw Flow est dépendé par :
  └── CORE/sales          → DrawResultViewPort (lecture du résultat pour calcul gagnants)
```

**Blocants v1** :

- `DrawAdminController` non sécurisé (à activer `@PreAuthorize` avant prod)
- Endpoint `GET /tenant/draws` manquant (bloque la vente côté UI)
- 0 tests

---

## Estimation gaps : S / M / L par item manquant

| Gap                                                                 | Taille |
| ------------------------------------------------------------------- | ------ |
| Activer `@PreAuthorize` sur `DrawAdminController`                   | **S**  |
| Créer endpoint `GET /tenant/draws` (tirages disponibles pour vente) | **M**  |
| Valider chain fetch → apply → settle end-to-end                     | **M**  |
| Documenter procedure fallback manuel (OPS runbook)                  | **S**  |
| Corriger violations `ResponseEntity` + `@Autowired` dans draw       | **S**  |
| Tests lifecycle draw + scheduler + settlement                       | **L**  |
