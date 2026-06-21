# Draw Execution — Cycles et pipeline

> Lifecycle complet d'un draw tenant. Plusieurs cycles selon les cas opérationnels.  
> Domaine pivot : `core.draw` · Référence : `core/draw/DOMAIN_DRAW.md`

---

## Machine d'états complète

```
              ┌─────────────────────────────────────────────────────┐
              │                CYCLE NORMAL                         │
              └─────────────────────────────────────────────────────┘

SCHEDULED ──► OPEN ──► CLOSED ──► RESULTED ──► SETTLED ──► ARCHIVED
                                      │
                           (PROVISIONAL → CONFIRMED)
                           Watchdog 30min si bloqué PROVISIONAL

              ┌─────────────────────────────────────────────────────┐
              │              CYCLE ANNULATION                       │
              └─────────────────────────────────────────────────────┘

SCHEDULED ─┐
OPEN       ├──► CANCELED ──► ARCHIVED
CLOSED     ┘

              ┌─────────────────────────────────────────────────────┐
              │              CYCLE CORRECTION                       │
              └─────────────────────────────────────────────────────┘

RESULTED ──► CorrectAppliedDrawResult ──► RESULTED (nouveau drawResultId)
            (interdit si SETTLED)

              ┌─────────────────────────────────────────────────────┐
              │           CYCLE RÉSULTAT MANUEL                     │
              └─────────────────────────────────────────────────────┘

CLOSED ──► RecordManualDrawResultCommand / OverrideDrawResultCommand
        ──► RESULTED (resultSource: OPS)
```

---

## États

| État | Signification | Terminal |
|---|---|---|
| `SCHEDULED` | Généré, pas encore ouvert à la vente | Non |
| `OPEN` | Vente active | Non |
| `CLOSED` | Cutoff dépassé — plus de vente | Non |
| `RESULTED` | Résultat lié (PROVISIONAL ou CONFIRMED) | Non |
| `SETTLED` | Tickets traités (WON/LOST) | Oui |
| `CANCELED` | Annulé par ops — refund si tickets vendus | Oui |
| `ARCHIVED` | Archivé après SETTLED ou CANCELED | Oui |

---

## Cycle 1 — Normal (automatique)

### Phase Generate — J → J+7 (5h UTC quotidien)

```
Scheduler GenerateDrawsForRangeCommand (gate: DRAW_GENERATE)
  → Pour chaque draw_channel actif du tenant :
    UNIQUE(tenant_id, draw_channel_id, draw_date) — idempotent
  → Draw créé : SCHEDULED
  → systemGenerated = true
  → scheduledAt, cutoffAt calculés depuis draw_channel.draw_time + timezone
```

### Phase Open (scheduler, fenêtre configurable)

```
Scheduler OpenDueDrawsCommand (gate: DRAW_OPEN)
  → Draws SCHEDULED où sales_open_time <= now
  → SCHEDULED → OPEN
  → DrawOpenedEvent (pas publié — state transition interne)
```

> Fallback : si `draw_channel.sales_open_time` absent → `tch.draw.scheduler.open-today.default-sales-open-time`

### Phase Close (scheduler, fenêtre configurable)

```
Scheduler CloseDueDrawsCommand (gate: DRAW_CLOSE)
  → Draws OPEN où cutoffAt <= now
  → OPEN → CLOSED
  → DrawClosedEvent publié (AfterCommit)
     → core.sales : refuse nouvelles ventes
     → cache : invalidation
```

### Phase Fetch — ingestion résultat externe

```
Scheduler FetchExternalResultsWindowCommand (gate: RESULTS_EXTERNAL_FETCH)
  → core.drawresult : lit result_slot.source_cfg pour chaque slot actif
  → Appel provider externe (core.uslottery : NY/FL/GA/TX/TN)
  → Projection Haïti via core.haiti (lot1..lot4 depuis pick3+pick4)
  → draw_result créé : status PROVISIONAL ou CONFIRMED
  → DrawResultIngestedEvent publié → accélère apply
```

**Fenêtres fetch (timezone NY) :** `12:00-14:00, 20:00-23:00`

### Phase Apply — liaison résultat → draw (gate: RESULTS_EXTERNAL_APPLY)

```
Scheduler ApplyExternalResultsWindowCommand
  → Draws CLOSED + draw_result disponible (PROVISIONAL ou CONFIRMED)
  → draw.drawResultId = drawResultId
  → CLOSED → RESULTED
  → DrawResultAppliedEvent publié (AfterCommit)
     → core.sales : RecordDrawTicketsResultCommandHandler
     → core.draw : Declenche settlement si result CONFIRMED
     → features.stats, cache
```

> ⚠ Apply autorisé dès PROVISIONAL. Settle nécessite CONFIRMED.

**Watchdog PROVISIONAL :** Si draw reste RESULTED avec résultat PROVISIONAL > 30 min → alerte ops (`DrawProvisionalWatchdogScheduler`, gate: `DRAW_WATCHDOG_PROVISIONAL`).

### Phase Settle — traitement des tickets (gate: DRAW_SETTLE)

```
Scheduler SettleDrawCommand
  Prérequis : draw_result.status = CONFIRMED  ← pas PROVISIONAL
  → draw.locked = true  (verrou anti-concurrence)
  → RESULTED → SETTLED
  → Pour chaque ticket du draw : WON ou LOST calculé
  → DrawSettledEvent publié (AfterCommit)
        → features.stats, notifications, cache
  → draw.locked = false
```

Note : `DrawSettledEvent` déclenche l'ouverture de claims de gain pour les tickets gagnants (domaine non documenté).

**Fenêtres settle (timezone NY) :** `12:00-15:00, 20:00-23:30`

---

## Cycle 2 — Annulation

```
Ops (ou admin) : CancelDrawCommand
  Autorisé depuis : SCHEDULED, OPEN, CLOSED
  Interdit depuis : RESULTED, SETTLED, ARCHIVED

  → CANCELED
  → DrawCancelledEvent publié
     → core.sales : refund tickets vendus (SOLD → VOID)
     → features.stats, notifications

  → Ops : ArchiveDrawCommand
  → CANCELED → ARCHIVED
```

Cas d'usage : erreur de configuration, fraude détectée, incident provider.

---

## Cycle 3 — Correction de résultat (après Apply, avant Settle)

```
Ops : CorrectAppliedDrawResultCommand
  { drawId, correctedDrawResultId, reason, idempotencyKey }

  Prérequis :
  - draw.status = RESULTED
  - draw non SETTLED (DrawSalesGuardPort.assertCanCorrectAppliedResult)
  - correctedDrawResultId ≠ previousDrawResultId

  → draw.drawResultId = correctedDrawResultId
  → DrawResultCorrectedEvent publié (AfterCommit)
     → core.drawresult : MarkDrawResultOverriddenCommand
       (previous DrawResult → OVERRIDDEN)
  → DrawResultAppliedEvent re-publié
     → Re-trigger settlement pipeline avec le nouveau résultat
```

> ⚠ Interdit si le draw est déjà SETTLED — le settlement est définitif.  
> ⚠ `DrawSalesGuardPort` actuellement NoOp (TODO: implémenter RealDrawSalesGuardAdapter).

---

## Cycle 4 — Résultat manuel (ops)

```
Pour draw CLOSED sans résultat provider disponible :

RecordManualDrawResultCommand  ← résultat saisi par ops
  → draw_result créé avec source MANUAL et status CONFIRMED directement
  → resultSource: OPS

OverrideDrawResultCommand  ← remplacement d'un résultat existant
  → draw_result existant → OVERRIDDEN
  → nouveau draw_result créé
  → DrawResultAppliedEvent re-publié
  → Re-trigger settlement
```

Gates ops sur `/platform/ops/draw-results/**` :
`RESULTS_MANUAL_RECORD` · `RESULTS_OVERRIDE` · `RESULTS_EXTERNAL_REFRESH`

---

## Cycle 5 — Reprogrammation

```
Ops : RescheduleDrawCommand
  { drawId, scheduledAt, cutoffAt, reason, force }

  Autorisé depuis : SCHEDULED (ou OPEN avec force=true)
  → Mise à jour scheduledAt + cutoffAt
  → Audité si force=true
```

Cas d'usage : décalage provider, heure d'été, incident réseau.

---

## DrawResultStatus — états du résultat global

| État | Signification | Settlement autorisé |
|---|---|---|
| `PROVISIONAL` | Résultat reçu, pas encore confirmé | Non |
| `CONFIRMED` | Résultat validé et définitif | Oui |
| `OVERRIDDEN` | Remplacé par un résultat corrigé | Non |
| `ERROR` | Erreur d'ingestion provider | Non |

---

## Scheduler windows (timezone NY — configurable)

| Opération | Fenêtres par défaut |
|---|---|
| Open draws | Continu (configurable par `draw_channel.sales_open_time`) |
| Close draws | Continu (déclenchée par `cutoffAt`) |
| Fetch results | `12:00-14:00`, `20:00-23:00` |
| Apply results | `12:00-14:30`, `20:00-23:30` |
| Settle draws | `12:00-15:00`, `20:00-23:30` |
| Watchdog PROVISIONAL | Toutes les N minutes (configurable) |

Désactiver une gate = suspendre cette phase sans arrêter les autres.

---

## Règle force=true

`force=true` est exposé uniquement sur `/platform/ops/**`.  
Il bypasse les contrôles non critiques (unicité, vérifications de statut).  
Il **ne bypasse jamais** : règle override post-SETTLED, RLS, authentification.  
Il **exige** un `reason` non vide. Il est **toujours audité** (`@AuditedForceCommand`).

---

## Events canoniques

| Event | Producteur | Consommateurs |
|---|---|---|
| `DrawClosedEvent` | `core.draw` | `core.sales` (refuse vente), cache |
| `DrawResultAppliedEvent` | `core.draw` | `core.sales` (settle tickets), stats, cache |
| `DrawResultCorrectedEvent` | `core.draw` | `core.drawresult` (mark OVERRIDDEN) |
| `DrawSettledEvent` | `core.draw` | stats, notifications, cache |
| `DrawCancelledEvent` | `core.draw` | `core.sales` (refund), stats, notifications |
| `DrawResultIngestedEvent` | `core.drawresult` | `core.draw` (accélère apply — optionnel) |

Tous publiés via `AfterCommit.run(...)`. Consommés en `@TransactionalEventListener(AFTER_COMMIT)`.  
Idempotence garantie par `processed_event` ou contraintes métier.

---

## Domaines impliqués

| Domaine | Rôle |
|---|---|
| `core.draw` | Lifecycle tenant + risk + settlement |
| `core.drawresult` | Ingestion résultats externes |
| `core.uslottery` | Clients HTTP providers NY/FL/GA/TX/TN |
| `core.haiti` | Projection lot1..lot4 depuis pick3+pick4 |
| `catalog.resultslot` | Créneaux providers avec `source_cfg` + `projection_cfg` |
| `catalog.drawchannel` | Canaux de vente tenant (timezone, cutoff_sec) |
| `catalog.game` | Définition des jeux disponibles par canal |
| `core.sales` | Tickets vendus sur le draw |
| `features.ops` | Orchestration manuelle + gates |

---

## Références

- Domaine draw : `core/draw/DOMAIN_DRAW.md`
- Domaine drawresult : `core/drawresult/DOMAIN_DRAWRESULT.md`
- Settlement tickets : [settlement](./settlement.md)
- Vente ticket : [sell-ticket](./sell-ticket.md)
- API ops : `POST /platform/ops/draws/{generate|open-due|close-due|apply}` · `POST /platform/ops/draw-results/{fetch|refresh|override|manual}`
