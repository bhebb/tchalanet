# Sell Ticket — Flow cross-domaines

> Cycle complet de vente d'un ticket : du POST initial jusqu'aux events post-commit consommés par limites, session, ledger et stats. Inclut la branche `PENDING_APPROVAL` et la sous-séquence `approve` / `reject`.

---

## Vue d'ensemble (3 étapes)

```
Cashier (POS)         Backend                       Listeners (AFTER_COMMIT)
     │                   │                                 │
     ▼                   ▼                                 ▼
┌──────────┐    ┌──────────────────┐         ┌─────────────────────────────┐
│  POST    │ →  │ TicketSalePolicy │ → save  │ exposure / ledger / session │
│ /tenant/ │    │  + Factory + Tx  │         │ totals / stats              │
│ tickets  │    │  + emit event    │         │                             │
└──────────┘    └──────────────────┘         └─────────────────────────────┘
                       │
                       └─→ Si limit BLOCK + autonomy.requireApprovalOnBlock :
                            statut PENDING_APPROVAL, 202 Accepted, notice APPROVAL_REQUIRED
                            → POST /{id}/approve ou /{id}/reject ensuite
```

| Phase                   | Domaine pivot | Action                                                                               |
| ----------------------- | ------------- | ------------------------------------------------------------------------------------ |
| **Prepare**             | `core.sales`  | `TicketSalePolicy.prepareSale` : session + cutoff + normalize + limits + odds        |
| **Persist**             | `core.sales`  | Factory `Ticket.sell()` ou `Ticket.pendingApproval()` + `TicketWritterPort.save`     |
| **Publish AfterCommit** | `core.sales`  | `TicketPlacedEvent` (sauf branche REJECTED — pas d'event)                            |
| **Approve (option)**    | `core.sales`  | `ApproveTicketSaleCommand` re-valide cutoff + session ; transition SOLD ; émet event |
| **Reject (option)**     | `core.sales`  | `RejectTicketSaleCommand` ; pas d'event publié                                       |

---

## Domaines impliqués

| Domaine            | Type    | Rôle                                                                       |
| ------------------ | ------- | -------------------------------------------------------------------------- |
| `core.sales`       | tenant  | Pivot : commande, factory, persistence, events                             |
| `core.session`     | tenant  | `SalesSessionReaderPort.findOpenByTerminal` (session ouverte requise)      |
| `core.outlet`      | tenant  | `OutletLookupPort.isSalesBlocked(outletId)` (refus si bloqué)              |
| `core.draw`        | tenant  | `GetDrawQuery` + `Draw.cutoffAt()` (refus si cutoff dépassé)               |
| `catalog.pricing`  | tenant  | `PricingCatalog.oddsFor(tenant, gameCode, betType, betOption)` (snapshot)  |
| `core.limitpolicy` | tenant  | `EvaluateLimitPolicyQuery` + `ApplyTicketExposureCommand` (post-event)     |
| `core.autonomy`    | tenant  | `ResolveAutonomyPolicyService.resolve(...)` (override approval sur BLOCK)  |
| `core.ledger`      | tenant  | `RecordLedgerFromSalesPort.recordTicketSale` (consume `TicketPlacedEvent`) |
| `features.stats`   | feature | `StatsAggregatesEventListener`, `StatsDailyUpdater` (consume event)        |

---

## Vocabulaire métier

| Terme               | Sens court                                                    | Source of truth                                             |
| ------------------- | ------------------------------------------------------------- | ----------------------------------------------------------- |
| **Ticket**          | Agrégat tenant : SOLD / PENDING_APPROVAL / VOID / REJECTED    | `core/sales/DOMAIN_SALES.md`                                |
| **TicketLine**      | Mise individuelle (gameCode + betType + selection + stake)    | `core/sales/DOMAIN_SALES.md`                                |
| **ticketCode**      | Code interne `TCK-YYMMDD-HHMMSS-XXXXXX-C` (per-tenant unique) | `core.sales.infra.generator.TimeBasedTicketNumberGenerator` |
| **publicCode**      | Code public Crockford 12 chars (globalement unique)           | `core.sales.infra.generator.CrockfordPublicCodeGenerator`   |
| **Session POS**     | Session ouverte obligatoire pour vendre                       | `core/session/DOMAIN_SESSION.md`                            |
| **Limit policy**    | Évaluation de seuils (stake / cumul / payout potentiel)       | `core/limitpolicy/DOMAIN_LIMITPOLICY.md`                    |
| **Autonomy policy** | Décide si BLOCK doit basculer en PENDING_APPROVAL             | `core/autonomy/DOMAIN_AUTONOMY.md`                          |

---

## Pipeline détaillé

```
POST /tenant/tickets   (SellTicketRequest)
   ⚠ tenantId, sessionId, cashierId du body sont silencieusement écrasés/ignorés
   │
   ▼
TicketController.sell()  [@Secured CASHIER, ADMIN, SUPER_ADMIN]
   │
   ▼  TicketWebMapper.toSellCommand(req)  ← contexte écrase tenantId + cashierId
SellTicketCommand → CommandBus
   │
   ▼
SellTicketCommandHandler  [@TchTx]
   │
   ├── TicketSalePolicy.prepareSale(cmd)
   │     ├── validateSession(tenantId, terminalId)
   │     │     ├── SalesSessionReaderPort.findOpenByTerminal()    → SecurityException si absent
   │     │     └── OutletLookupPort.isSalesBlocked(outletId)    → SecurityException si bloqué
   │     ├── DrawCutoffRule.requireBeforeCutoff(drawId)
   │     │     └── QueryBus.send(GetDrawQuery) → ProblemRest.conflict si now > cutoff
   │     ├── TicketLinePreparationService.normalize(lines)      ← BetSelectionNormalizer
   │     ├── TicketLinePreparationService.mergeDuplicates(lines)
   │     ├── evaluateLimitsAndAutonomy()
   │     │     ├── QueryBus.send(EvaluateLimitPolicyQuery)      ← LimitContext OperationType.SALE
   │     │     │      ⚠ ZoneId.systemDefault() utilisé
   │     │     ├── ResolveAutonomyPolicyService.resolve(...)
   │     │     └── BLOCK + !autonomy.requireApprovalOnBlock
   │     │           → throw ProblemRest.limitBlocked(...) 422
   │     └── TicketLinePreparationService.toTicketLines(tenantId, merged)
   │           └── PricingCatalog.oddsFor(tenant, gameCode, betType, betOption)
   │                 └── potentialPayout = stake × odds (HALF_UP, scale 2)
   │
   ├── Si prepared.limits.outcome == BLOCK :
   │     ├── TicketSaleFactory.newPendingApprovalTicket(...)
   │     │     ├── TicketNumberGeneratorPort.generate()  → "TCK-YYMMDD-HHMMSS-XXXXXX-C"
   │     │     └── TicketPublicCodeGeneratorPort.generate()  → 12 chars Base32
   │     ├── TicketWritterPort.save(ticket)
   │     ├── ApiResponseContext.addNotice(APPROVAL_REQUIRED, approvalRequestId aléatoire)
   │     │     ⚠ approvalRequestId = UUID.randomUUID() (TODO domaine approval)
   │     └── return 202 ACCEPTED + ApiResponse.pending(notice, ticket)
   │
   └── Sinon :
         ├── TicketSaleFactory.newSoldTicket(...)
         ├── TicketWritterPort.save(ticket)
         ├── Vérifie "single gameCode per ticket" (MVP)
         │      ⚠ Vérification APRÈS save → tx rollback nécessaire si mixed
         ├── Construit TicketPlacedEvent (lines en cents)
         ├── AfterCommit.run(() -> publisher.publish(event))
         ├── outcome = WARN ? SUCCESS_WITH_WARNINGS : SUCCESS
         └── return 201 CREATED + ApiResponse.created(ticket)
   │
   ▼  AFTER COMMIT
   ├── core.limitpolicy.LimitPolicyEventsListener
   │     └── CommandBus.send(ApplyTicketExposureCommand) → projette exposure
   ├── core.session.SalesSessionTotalsProjectionListener
   │     └── met à jour les totaux de session
   ├── features.stats.StatsAggregatesEventListener
   │     └── agrégats temps réel
   └── features.stats.StatsDailyUpdater
         └── compteurs journaliers
   │
   ▼  DURANT TX  (anomalie : @EventListener synchrone, devrait être AFTER_COMMIT)
core.sales.SalesLedgerListener
   └── RecordLedgerFromSalesPort.recordTicketSale(tenantId, ticketId, stakeCents, occurredAt)
         ⚠ Exception loggée mais silencieusement ignorée
```

---

## Décisions / branches

### Outcome SELL

| Outcome                 | HTTP | Statut ticket    | Event publié               |
| ----------------------- | ---- | ---------------- | -------------------------- |
| `SUCCESS`               | 201  | SOLD             | `TicketPlacedEvent`        |
| `SUCCESS_WITH_WARNINGS` | 201  | SOLD + notices   | `TicketPlacedEvent`        |
| `PENDING_APPROVAL`      | 202  | PENDING_APPROVAL | (aucun ; émis à l'approve) |

### Outcome APPROVE / REJECT

`POST /tenant/tickets/{id}/approve` (ROLE_ADMIN/SUPER_ADMIN) :

- Re-valide cutoff (`DrawCutoffRule`) → 409 si dépassé.
- Re-valide session (`SalesSessionReaderPort.findById(sessionId)`) → 409 si absente.
- `Ticket.approve(now)` : transition `PENDING_APPROVAL → SOLD`.
- Publie `TicketPlacedEvent` (mais avec `outletId/agentId/drawChannelId` potentiellement null si la session n'est pas re-trouvée — voir audit).

`POST /tenant/tickets/{id}/reject` (ROLE_ADMIN/SUPER_ADMIN) :

- `Ticket.reject(now)` : `PENDING_APPROVAL → REJECTED`.
- **Aucun event publié**.

### Cutoff dépassé

- Réponse 409 Conflict : `Draw cutoff time has passed`.
- Le ticket n'est pas sauvegardé (le check est fait en amont du save).

### Outlet bloqué

- Réponse 500 (SecurityException brute, pas mappée en `ProblemRest.forbidden`).
- Anomalie : devrait retourner 403 ou 422 avec `ProblemRest`.

---

## Events canoniques

| Event                         | Producer                                                      | Mode          | Consumers                                                                                          |
| ----------------------------- | ------------------------------------------------------------- | ------------- | -------------------------------------------------------------------------------------------------- |
| `TicketPlacedEvent`           | `SellTicketCommandHandler`, `ApproveTicketSaleCommandHandler` | `AfterCommit` | `core.limitpolicy`, `core.session`, `features.stats` (×2), `core.sales.SalesLedgerListener` (sync) |
| `TicketCancelledEvent`        | `CancelSaleCommandHandler`                                    | `AfterCommit` | `core.limitpolicy`, `core.session`, `features.stats`                                               |
| `TicketResultedEvent`         | `RecordDrawTicketsResultCommandHandler`                       | `AfterCommit` | `features.stats`                                                                                   |
| `TicketResultOverriddenEvent` | `OverrideTicketResultCommandHandler`                          | `AfterCommit` | (aucun listener confirmé)                                                                          |
| `TicketPaidEvent`             | `core.payout` (cross-domain)                                  | `AfterCommit` | `features.stats`                                                                                   |

> Tous publiés via `AfterCommit.run(...)` sauf `SalesLedgerListener` qui consomme `TicketPlacedEvent` en `@EventListener` synchrone (anomalie connue).

---

## Cross-apps

### Mobile (POS)

- Écran "Vente" : sélection `Draw`, lignes (`gameCode`, `betType`, `betOption`, `selection`, `stake`).
- Affiche les notices `LIMIT_WARN` au cashier sans bloquer ; bloque sur `APPROVAL_REQUIRED` (statut 202) avec demande de validation manager.
- Imprime ticket via `GET /tenant/tickets/{id}/print.escpos` (ESC/POS) ou `.pdf` (Capacitor) — ⚠ pas de `@Secured` actuellement.

### Web (admin)

- `/admin/tickets` — liste paginée + filtres (`terminalId`, `outletId`, `agentId`, `drawId`, `status`, `from`, `to`).
- Approve/Reject : `POST /tenant/tickets/{id}/approve|reject?approvedBy=&reason=`.
- Override result : `PATCH /tenant/tickets/{id}/result/override` (rôle ADMIN/SUPER_ADMIN + permission `ticket.result.override`).

### API tenant (POS / admin)

- `POST /tenant/tickets` — sell
- `POST /tenant/tickets/{id}/approve|reject` — workflow d'approbation
- `PATCH /tenant/tickets/{id}/cancel` — annulation (⚠ pas de `@Secured`)
- `PATCH /tenant/tickets/{id}/result/override` — override admin
- `GET /tenant/tickets` — liste filtrée
- `GET /tenant/tickets/{id}` — détails
- `GET /tenant/tickets/{id}/print[.escpos|.pdf]` — impression (⚠ pas de `@Secured`)

---

## Source of truth backend

> Cette page est une **vue fonctionnelle cross-apps**. La source de vérité technique vit près du code.

- Backend `core.sales` : `99-links/_ref/server/core/sales/DOMAIN_SALES.md`
- Backend `core.draw` : `99-links/_ref/server/core/draw/DOMAIN_DRAW.md`
- Backend `core.session` : `99-links/_ref/server/core/session/DOMAIN_SESSION.md`
- Backend `core.outlet` : `99-links/_ref/server/core/outlet/DOMAIN_OUTLET.md`
- Backend `core.limitpolicy` : `99-links/_ref/server/core/limitpolicy/DOMAIN_LIMITPOLICY.md`
- Backend `core.autonomy` : `99-links/_ref/server/core/autonomy/DOMAIN_AUTONOMY.md`
- Backend `core.ledger` : `99-links/_ref/server/core/ledger/DOMAIN_LEDGER.md`
- Backend `catalog.pricing` : `99-links/_ref/server/catalog/pricing/CATALOG_PRICING.md`
- Audit : `99-links/_ref/server/docs/audit/2026-04-26-sales-pipeline-audit.md`

> En cas d'incohérence entre cette page et les `DOMAIN_*.md` backend, **les docs backend font foi**.

---

## Liens

- Conventions backend : `tchalanet-server/docs/conventions/`
  - `event_model.md`, `idempotency.md`, `timezone.md`, `inter_domain_calls.md`
- Architecture backend : `tchalanet-server/docs/ARCHITECTURE.md`
- Functional domains : `docs/02-functional/domains/sales.md`, `limitpolicy.md`, `session.md`
