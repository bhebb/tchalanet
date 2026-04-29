# Draw Execution — Flow cross-domaines

> Pipeline complet de l'exécution d'un tirage : de la planification à la résolution finale, en passant par l'ouverture/fermeture de la vente, l'ingestion du résultat externe et le settlement des tickets.

---

## Vue d'ensemble (6 phases)

```
T-N jours       T-...           T-cutoff       T (tirage)      T+5min        T+...
   │               │                │              │              │             │
   ▼               ▼                ▼              ▼              ▼             ▼
┌─────────┐  ┌─────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌──────────┐
│Generate │→ │  Open   │→ │    Close    │→ │   Fetch     │→ │   Apply     │→ │  Settle  │
│         │  │ (vente) │  │  (cutoff)   │  │ (provider)  │  │  (résultat) │  │ (tickets)│
└─────────┘  └─────────┘  └─────────────┘  └─────────────┘  └─────────────┘  └──────────┘
SCHEDULED      OPEN           CLOSED        draw_result       RESULTED         SETTLED
```

| Phase        | Quand                                  | Domaine pivot              | Action                                                       |
| ------------ | -------------------------------------- | -------------------------- | ------------------------------------------------------------ |
| **Generate** | J → J+7 (5h UTC daily)                 | `core.draw`                | Crée les `Draw` tenant à partir des `draw_channel`           |
| **Open**     | À l'heure de vente                     | `core.draw`                | `SCHEDULED → OPEN`                                           |
| **Close**    | Au cutoff (avant tirage)               | `core.draw`                | `OPEN → CLOSED`                                              |
| **Fetch**    | Après tirage provider                  | `core.drawresult`          | Lit `result_slot.source_cfg`, fetch provider, projette Haïti |
| **Apply**    | Après tirage (résultat externe arrivé) | `core.draw`                | `CLOSED → RESULTED` (lie au `draw_result`)                   |
| **Settle**   | Après apply (résultat FINAL)           | `core.draw` (Spring Batch) | `RESULTED → SETTLED` (tickets WON/LOST)                      |

---

## Domaines impliqués

| Domaine               | Type    | Rôle                                                                        |
| --------------------- | ------- | --------------------------------------------------------------------------- |
| `catalog.game`        | global  | Définition des jeux (HT_BOLET, HT_LOTO3...)                                 |
| `catalog.resultslot`  | global  | Créneaux providers (NY_MID, FL_EVE...) avec `source_cfg` + `projection_cfg` |
| `catalog.drawchannel` | tenant  | Canal de vente tenant pointant vers un slot                                 |
| `core.uslottery`      | global  | Clients HTTP providers (NY/FL/GA/TX/TN)                                     |
| `core.haiti`          | global  | Projection lot1..lot4 depuis pick3+pick4                                    |
| `core.drawresult`     | global  | Ingestion + persistance `draw_result` global                                |
| `core.draw`           | tenant  | Lifecycle tenant + risk + settlement                                        |
| `features.publicdraw` | feature | Exposition publique des résultats                                           |
| `features.ops`        | feature | Orchestration manuelle                                                      |

---

## Vocabulaire métier

| Terme             | Sens court                                       | Source of truth                             |
| ----------------- | ------------------------------------------------ | ------------------------------------------- |
| **Game**          | Type de jeu (HT_BOLET, HT_LOTO3, etc.)           | `catalog/game/DOMAIN_GAME.md`               |
| **Slot**          | Créneau global de tirage provider                | `catalog/resultslot/DOMAIN_RESULTSLOT.md`   |
| **Channel**       | Canal de vente tenant pointant vers un slot      | `catalog/drawchannel/DOMAIN_DRAWCHANNEL.md` |
| **Draw**          | Instance de vente tenant pour un (channel, date) | `core/draw/DOMAIN_DRAW.md`                  |
| **Draw Result**   | Résultat externe global ingéré + projeté Haïti   | `core/drawresult/DOMAIN_DRAWRESULT.md`      |
| **Draw Exposure** | Compteur risk par scope sur un draw              | `core/draw/DOMAIN_DRAW.md`                  |

---

## Decision points

### PROVISIONAL vs FINAL

- **Apply** dès `draw_result.status IN (PROVISIONAL, FINAL)`.
- **Settle** uniquement si `draw_result.status = FINAL`.
- La projection Haïti lit d'abord `result_slot.projection_cfg`, puis utilise le fallback global documenté.

### Override après SETTLED

- **Refusé** purement et simplement (MVP). Cas correction provider post-settlement → traité manuellement par les ops.

### Force=true

- Bypasse les contrôles non critiques uniquement.
- `reason` obligatoire.
- Audité systématiquement.
- Exposé uniquement sur `/platform/ops/**`.

---

## Events canoniques

| Event                     | Producer          | Consumers                                   |
| ------------------------- | ----------------- | ------------------------------------------- |
| `DrawClosedEvent`         | `core.draw`       | `sales` (refuse vente), `cache`             |
| `DrawResultIngestedEvent` | `core.drawresult` | `core.draw` (accélère apply, optionnel)     |
| `DrawResultAppliedEvent`  | `core.draw`       | `sales` (settle), `stats`, `cache`          |
| `DrawSettledEvent`        | `core.draw`       | `stats`, `notifications`, `cache`, `payout` |
| `DrawCanceledEvent`       | `core.draw`       | `sales` (refund), `stats`, `notifications`  |

> Tous publiés via `AfterCommit.run(...)`. Tous consommés en `@TransactionalEventListener(AFTER_COMMIT)`. Idempotence garantie par `processed_event` ou contraintes métier.

---

## Cross-apps (Web / Mobile)

### Web (UI)

- Pages :
  - `/public/results` — historique des résultats publics
  - `/public/draws/today` — draws ouverts du jour
- Components :
  - `PublicDrawList`, `LatestResultsPanel`, `DrawCountdown`
- i18n namespaces : `draw.*`, `publicdraw.*`, `result.*`

### Mobile (POS)

- Écrans :
  - "Ventes en cours" (draws OPEN du tenant)
  - "Consulter dernier résultat"
  - "Settlement en cours" (draws RESULTED non settled)
- Offline/Sync : cache court (5 min) sur les draws OPEN.

### API publique (cross-apps)

- `GET /api/v1/public/draws` — liste des draws OPEN (avec countdown)
- `GET /api/v1/public/results/latest` — derniers résultats par channel
- `GET /api/v1/public/results` — historique paginé
- Notes : rate-limit, noindex pour public.

### API tenant (POS / admin)

- `GET /api/v1/tenant/draws` — draws du tenant
- `GET /api/v1/tenant/draws/{id}` — détails d'un draw

### API ops (super-admin)

- `POST /platform/ops/draws/generate|open-due|close-due|apply` — orchestration manuelle
- `POST /platform/ops/draw-results/fetch|refresh|override|manual` — gestion résultats

Gates ops distinctes : fetch, apply, refresh, manual et override peuvent être coupés séparément.

---

## Source of truth backend

> Cette page est une **vue fonctionnelle cross-apps**. La source de vérité technique vit près du code dans `tchalanet-server`.

- Backend `core.draw` : `99-links/_ref/server/core/draw/DOMAIN_DRAW.md`
- Backend `core.drawresult` : `99-links/_ref/server/core/drawresult/DOMAIN_DRAWRESULT.md`
- Backend `core.uslottery` : `99-links/_ref/server/core/uslottery/DOMAIN_USLOTTERY.md`
- Backend `core.haiti` : `99-links/_ref/server/core/haiti/DOMAIN_HAITI.md`
- Backend `catalog.resultslot` : `99-links/_ref/server/catalog/resultslot/DOMAIN_RESULTSLOT.md`
- Backend `catalog.drawchannel` : `99-links/_ref/server/catalog/drawchannel/DOMAIN_DRAWCHANNEL.md`

> En cas d'incohérence entre cette page et les `DOMAIN_*.md` backend, **les docs backend font foi**.

---

## Liens

- Conventions backend : `tchalanet-server/docs/conventions/`
  - `event_model.md`, `idempotency.md`, `timezone.md`, `cache.md`, `inter_domain_calls.md`
- Architecture backend : `tchalanet-server/docs/ARCHITECTURE.md`
- Functional domains : `docs/02-functional/domains/draw.md`, `drawresult.md`
