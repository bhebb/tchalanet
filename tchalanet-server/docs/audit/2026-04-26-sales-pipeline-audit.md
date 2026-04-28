# Audit Pipeline Sales — État des lieux

**Date** : 2026-04-26
**Scope** : `core.sales` (vente + vérification de tickets) + domaines connexes consommés/consommateurs (`core.draw`, `core.drawresult`, `core.session`, `core.outlet`, `core.terminal`, `core.address`, `core.limitpolicy`, `core.autonomy`, `core.ledger`, `core.payout`, `core.audit`, `core.accesscontrol`, `catalog.pricing`, `catalog.settings`, `features.stats`)
**Méthode** : lecture code source réel uniquement — `DOMAIN_SALES.md` non pris en compte comme référence

---

## Domaine 1 : `core.sales.domain` (`com.tchalanet.server.core.sales.domain`)

### 1. Structure de packages

```
core/sales/domain/
├── model/
│   ├── Ticket.java                  (agrégat — classe finale @Getter, mutable)
│   ├── TicketLine.java              (record)
│   └── TicketVerificationResult.java (record + Line nested)
├── service/
│   ├── TicketSaleFactory.java       (@Component)
│   ├── TicketSalePolicy.java        (@Component — orchestration cross-domain)
│   ├── DrawCutoffRule.java          (n/a — vit dans application/rule/)
│   ├── BetSelectionNormalizer.java
│   ├── TicketWinningCalculator.java
│   ├── DrawResultMatchView.java     (interface utilisée par calculator)
│   └── DrawResultView.java
└── event/
    ├── TicketPlacedEvent.java
    ├── TicketCancelledEvent.java
    ├── TicketResultedEvent.java
    ├── TicketResultOverriddenEvent.java
    ├── TicketPaidEvent.java
    └── TicketPaymentPendingEvent.java
```

### 2. Agrégat `Ticket`

**`Ticket`** (`src/main/java/com/tchalanet/server/core/sales/domain/model/Ticket.java`)

- Classe **non-finale, mutable**, exposée via Lombok `@Getter`
- Champs : `id (TicketId)`, `tenantId`, `terminalId`, `sessionId (nullable)`, `drawId`, `ticketCode`, `publicCode (nullable)`, `currency`, `saleStatus`, `resultStatus`, `settlementStatus`, `totalAmount`, `winningAmount (nullable)`, `resultedAt (nullable)`, `approvalRequestId (UUID nullable)`, `lines`, `createdAt`, `updatedAt`
- 3 enums de statut **séparés** (split status pattern) :
  - `TicketSaleStatus` : `SOLD`, `PENDING_APPROVAL`, `VOID`, `REJECTED`
  - `TicketResultStatus` : `NOT_RESULTED`, `WON`, `LOST`, `OVERRIDDEN`
  - `TicketSettlementStatus` : `UNSETTLED`, `SETTLED`
- **Invariants enforcés** (constructeur privé) :
  - `lines` non vide
  - Si `resultStatus == NOT_RESULTED` → `winningAmount` et `resultedAt` doivent être `null` ; `settlementStatus` ne peut pas être `SETTLED`
  - Sinon → `winningAmount >= 0` et `resultedAt != null`
- **Factories** : `sell()`, `pendingApproval()`, `requestApproval()`, `rehydrate()`
- **Transitions exposées** : `approve()`, `reject()`, `voidTicket()`, `markResulted()`, `forceResult()` (2 surcharges), `settle()`, `markAsPaid()`, `markPayoutPending()`, `markPayoutPaid()`
- **Brèche d'encapsulation** : `updateSettlementStatus(TicketSettlementStatus)` est une méthode mutateur publique sans contrôle d'invariant — bypass de la state machine.
- **Doublon sémantique** : `markAsPaid()`, `markPayoutPaid()`, `settle()` font la même chose (`settlementStatus = SETTLED + touch`).
- **No-op fonctionnel** : `markPayoutPending()` réaffecte `UNSETTLED` (état déjà présent) — ne change rien observable.
- **Méthode `totalPayout()`** : retourne `winningAmount` ou `ZERO` — nom trompeur (suggère un agrégat, ce n'est qu'un getter avec coalescence).
- **Accesseurs `id()`, `tenantId()`, `terminalId()`, `drawId()`** ajoutés en plus des getters Lombok — commentaire `// Record-style accessors (some legacy code expects ...)` → dette de migration record/class.

### 3. `TicketLine` (record)

- Champs : `gameCode (GameCode)`, `selection (String — déjà normalisé)`, `stake (BigDecimal scale 2)`, `oddsSnapshot (scale 4)`, `potentialPayout (scale 2)`, `betType`, `betOption (Short nullable)`
- **Invariants enforcés** dans le compact constructor :
  - `stake > 0` ; `oddsSnapshot > 0` ; `potentialPayout >= 0`
  - `betOption` requis si `betType.requiresBetOption()` (LOTTO4_PATTERN/LOTTO5_PATTERN), `null` sinon, range 1..3
  - **Cohérence calculée** : `potentialPayout == round(stake * oddsSnapshot, 2, HALF_UP)` — exception si mismatch
- Anomalie : la cohérence `potentialPayout` est ré-vérifiée par le record alors que `TicketLinePreparationService.toTicketLines()` la calcule déjà — double calcul.

### 4. Events publiés

| Event                         | Champs clés                                                                                                                                   | Mode publication                              | Consommateurs                                                                                                                                                                                                                                                                                                      |
| ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `TicketPlacedEvent`           | `tenantId, ticketId, outletId, agentId, terminalId, sessionId, drawId, drawChannelId (nullable), gameCode, stakeCents, currencyCode, lines[]` | `AfterCommit.run()` (sell + approve handlers) | `core.limitpolicy.LimitPolicyEventsListener` (AFTER_COMMIT → `ApplyTicketExposureCommand`), `core.session.SalesSessionTotalsProjectionListener`, `features.stats.StatsAggregatesEventListener`, `features.stats.StatsDailyUpdater`, `core.sales.SalesLedgerListener` (**`@EventListener` sync, NON AFTER_COMMIT**) |
| `TicketCancelledEvent`        | `tenantId, ticketId, terminalId, sessionId, performedBy (raw UUID), reason, totalStakeCents, currency, drawId`                                | `AfterCommit.run()`                           | `core.limitpolicy.LimitPolicyEventsListener`, stats, session totals                                                                                                                                                                                                                                                |
| `TicketResultedEvent`         | `tenantId, ticketId, resultStatus, settlementStatus, totalPayout`                                                                             | `AfterCommit.run()`                           | `core.payout` (via `RecordTicketSaleLedgerCommandHandler` pour stake mais ici résultat — non confirmé), stats                                                                                                                                                                                                      |
| `TicketResultOverriddenEvent` | `tenantId, ticketId, drawId, winningAmount, resultStatus, reason, performedBy`                                                                | `AfterCommit.run()`                           | Aucun listener confirmé hors logs                                                                                                                                                                                                                                                                                  |
| `TicketPaidEvent`             | (publié par `core.payout.MarkTicketPayoutPaidCommandHandler`)                                                                                 | `AfterCommit.run()`                           | Stats / ledger                                                                                                                                                                                                                                                                                                     |
| `TicketPaymentPendingEvent`   | défini mais usage non confirmé                                                                                                                | —                                             | —                                                                                                                                                                                                                                                                                                                  |

**Anomalies events** :

- `TicketPlacedEvent.agentId` est nullable et le path `approve` peut publier `agentId=null, outletId=null, drawChannelId=null` (ligne 99-114 de `ApproveTicketSaleCommandHandler`) si la session n'est pas re-trouvée — l'event devient quasi-vide alors qu'il est consommé pour exposure et stats.
- `TicketCancelledEvent.performedBy` est un `UUID` brut — devrait être `AgentId` ou `UserId` typé.
- `SalesLedgerListener` (dans `infra.event/`) utilise `@EventListener` (synchrone, in-tx) au lieu de `@TransactionalEventListener(AFTER_COMMIT)` — incohérent avec la convention "thin listener after-commit". L'exception est attrapée et loggée → ledger silencieusement raté possible.

### 5. Anomalies détectées (domain)

- **`Ticket.updateSettlementStatus()`** : mutator public sans contrôle d'invariant.
- **3 méthodes équivalentes** : `settle()`, `markAsPaid()`, `markPayoutPaid()`.
- **`markPayoutPending()`** : no-op fonctionnel.
- **`Ticket.totalPayout()`** : nom trompeur (getter de `winningAmount`).
- **Mélange `@Getter` Lombok + accesseurs `id()/tenantId()` style record** : double surface API.
- **`Ticket` n'expose pas `cancelReason`, `canceledAt`** alors que la table `ticket` n'a pas non plus de tels champs — pas d'audit du `void` au niveau domaine (uniquement via Envers + event).
- **`TicketLine` re-vérifie `potentialPayout`** alors que c'est déjà calculé en amont (double work).
- **`TicketVerificationResult` retourne `Address` complet** (même masqué) — fuite potentielle (cf. domaine 2).

---

## Domaine 2 : `core.sales.application` (Use cases & ports)

### 1. Structure de packages

```
core/sales/application/
├── command/
│   ├── handler/
│   │   ├── SellTicketCommandHandler.java
│   │   ├── ApproveTicketSaleCommandHandler.java
│   │   ├── RejectTicketSaleCommandHandler.java
│   │   ├── CancelSaleCommandHandler.java
│   │   ├── OverrideTicketResultCommandHandler.java
│   │   ├── RecordDrawTicketsResultCommandHandler.java
│   │   └── ArchiveTicketsCommandHandler.java
│   └── model/
│       ├── SellTicketCommand.java        (+ LineCommand nested)
│       ├── SellTicketResult.java         (Ticket + outcome + approvalRequestId)
│       ├── SellTicketOutcome.java        (enum: SUCCESS, SUCCESS_WITH_WARNINGS, PENDING_APPROVAL)
│       ├── ApproveTicketSaleCommand.java
│       ├── RejectTicketSaleCommand.java
│       ├── CancelSaleCommand.java
│       ├── CancelTicketCommand.java      (alternative)
│       ├── OverrideTicketResultCommand.java
│       ├── RecordDrawTicketsResultCommand.java
│       ├── ArchiveTicketsCommand.java
│       ├── ExpireTicketsCommand.java     (record vide, sans champs)
│       ├── ApprovePendingTicketSaleCommand.java
│       ├── RejectPendingTicketSaleCommand.java
│       ├── TicketApprovedResult.java / TicketRejectedResult.java
│       ├── CancelSaleResult.java (+ CancelOutcome)
│       ├── LimitNotice.java
│       └── SellTicketResult.java
├── query/
│   ├── handler/
│   │   ├── ListTicketsQueryHandler.java
│   │   ├── GetTicketDetailsQueryHandler.java
│   │   ├── VerifyPublicTicketQueryHandler.java
│   │   ├── ListRecentTicketsForCashierQueryHandler.java
│   │   ├── GetTicketPrintEscPosQueryHandler.java
│   │   ├── GetTicketPrintPdfQueryHandler.java
│   │   ├── GetTicketQrPngByPublicCodeQueryHandler.java
│   │   ├── ExportDailySalesQueryHandler.java
│   │   ├── GetAgentDailySalesQueryHandler.java
│   │   └── QrPayloadBuilder.java
│   └── model/
│       ├── ListTicketsQuery.java         (+ TicketFilter)
│       ├── GetTicketDetailsQuery.java
│       ├── GetTicketDetailsByIdQuery.java
│       ├── GetTicketDetailsByPublicCodeQuery.java
│       ├── VerifyPublicTicketQuery.java
│       ├── TicketDetailsView.java
│       ├── TicketSummaryView.java
│       ├── ListRecentTicketsForCashierQuery.java
│       ├── ExportDailySalesQuery.java
│       ├── GetAgentDailySalesQuery.java
│       ├── AgentDailySalesDto.java
│       ├── GetTicketPrintEscPosQuery.java
│       ├── GetTicketPrintPdfQuery.java
│       └── GetTicketQrPngByPublicCodeQuery.java
├── port/out/
│   ├── TicketReaderPort.java
│   ├── TicketWritterPort.java               (typo: "Writter")
│   ├── TicketSettlementPort.java            (cursor batch reader)
│   ├── TicketSettlementQueryPort.java       (existsPendingByDrawId)
│   ├── TicketEventPublisherPort.java        (utilisé ? voir anomalies)
│   ├── TicketNumberGeneratorPort.java
│   ├── TicketPublicCodeGeneratorPort.java
│   ├── DrawResultViewPort.java              (read-only sur draw_result, voir anomalies)
│   ├── TicketPrintView.java
│   └── TicketPrintLine.java
├── service/
│   └── TicketLinePreparationService.java
├── rule/
│   └── DrawCutoffRule.java
├── model/
│   └── TicketStatus.java (record agrégeant les 3 statuts)
├── event/
│   └── DrawResultedEventListener.java       (consume DrawResultAppliedEvent)
└── print/
    ├── EscPosTicketReceiptFormatter.java
    ├── PdfTicketReceiptFormatter.java
    ├── AbstractTicketReceiptFormatter.java
    ├── TicketReceiptFormatter.java
    └── TicketPrintViewMapper.java
```

### 2. Commands existantes

| Command                                                              | Champs                                                                                  | Handler                                              | Action                                                                                             | Idempotence                                                                                                                        |
| -------------------------------------------------------------------- | --------------------------------------------------------------------------------------- | ---------------------------------------------------- | -------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `SellTicketCommand`                                                  | `tenantId, terminalId, cashierId, drawId, lines[], currency`                            | `SellTicketCommandHandler`                           | Crée `Ticket` (SOLD ou PENDING_APPROVAL), publie `TicketPlacedEvent`                               | Aucune (génère `ticketCode` + `publicCode` aléatoires sans retry collision visible)                                                |
| `ApproveTicketSaleCommand`                                           | `ticketId, approvedBy, reason`                                                          | `ApproveTicketSaleCommandHandler`                    | Re-valide cutoff + session, transition PENDING_APPROVAL→SOLD, publie `TicketPlacedEvent`           | Aucun garde ; double appel → `ProblemRest.conflict` car statut change                                                              |
| `RejectTicketSaleCommand`                                            | `ticketId, rejectedBy, reason`                                                          | `RejectTicketSaleCommandHandler`                     | PENDING_APPROVAL→REJECTED                                                                          | Garde via statut                                                                                                                   |
| `CancelSaleCommand`                                                  | `tenantId, ticketId, performedBy, reason, currency`                                     | `CancelSaleCommandHandler`                           | SOLD/PENDING→VOID, publie `TicketCancelledEvent`, ré-évalue limites                                | Garde via statut domaine                                                                                                           |
| `CancelTicketCommand`                                                | `ticketId, reason, performedBy (UUID)`                                                  | (mappé en `CancelSaleCommand` via `TicketWebMapper`) | Idem                                                                                               | Idem                                                                                                                               |
| `OverrideTicketResultCommand`                                        | `ticketId, totalPayout, status (TicketStatus), reason, performedBy (UUID), performedAt` | `OverrideTicketResultCommandHandler`                 | `Ticket.forceResult(payout, resultStatus, when)` ; `@RequiresPermission("ticket.result.override")` | Aucune ; pas de check anti double-override                                                                                         |
| `RecordDrawTicketsResultCommand`                                     | `tenantId, drawId, drawResultId, occurredAt`                                            | `RecordDrawTicketsResultCommandHandler`              | Itère batchs SOLD+NOT_RESULTED → `markResulted` + publie `TicketResultedEvent`                     | **N'utilise pas la table `ticket_settlement`** prévue par la migration ; idempotence indirecte via `markResulted` qui exige `SOLD` |
| `ArchiveTicketsCommand`                                              | `tenantId, cutoffDate`                                                                  | `ArchiveTicketsCommandHandler`                       | Soft-delete bulk via `archiveOldTickets(cutoff)`                                                   | Aucune protection multi-instance (pas de ShedLock)                                                                                 |
| `ExpireTicketsCommand`                                               | (aucun champ)                                                                           | **Aucun handler trouvé**                             | —                                                                                                  | Code mort                                                                                                                          |
| `ApprovePendingTicketSaleCommand` / `RejectPendingTicketSaleCommand` | n/a                                                                                     | **Aucun handler localisé**                           | —                                                                                                  | Doublon présumé de `Approve/RejectTicketSaleCommand`                                                                               |

### 3. Queries existantes

| Query                                                             | Handler                                   | Retour                       | Notes                                                                       |
| ----------------------------------------------------------------- | ----------------------------------------- | ---------------------------- | --------------------------------------------------------------------------- |
| `ListTicketsQuery(filter, pageRequest)`                           | `ListTicketsQueryHandler`                 | `TchPage<TicketSummaryView>` | Audit `LIST` émis dans le handler ; filter `tenantId` toujours `null` (RLS) |
| `GetTicketDetailsQuery(ticketId)`                                 | `GetTicketDetailsQueryHandler`            | `TicketDetailsView`          | Pas d'audit (décision v1)                                                   |
| `VerifyPublicTicketQuery(publicCode, now)`                        | `VerifyPublicTicketQueryHandler`          | `TicketVerificationResult`   | Public ; visibilité via `SettingsCatalog` (default 14 j)                    |
| `ListRecentTicketsForCashierQuery`                                | `ListRecentTicketsForCashierQueryHandler` | `List<Ticket>`               | —                                                                           |
| `GetTicketPrintEscPosQuery` / `GetTicketPrintPdfQuery`            | handlers idem                             | `byte[]`                     | Print via `TicketReceiptFormatter`                                          |
| `GetTicketQrPngByPublicCodeQuery(publicCode, size)`               | `GetTicketQrPngByPublicCodeQueryHandler`  | `byte[]`                     | Public, cacheable 1h                                                        |
| `ExportDailySalesQuery(from, to)`                                 | `ExportDailySalesQueryHandler`            | `byte[]` CSV                 | RLS-scoped                                                                  |
| `GetAgentDailySalesQuery(from, to)`                               | `GetAgentDailySalesQueryHandler`          | `List<AgentDailySalesDto>`   | Aggregate JPQL                                                              |
| `GetTicketDetailsByIdQuery` / `GetTicketDetailsByPublicCodeQuery` | handlers non trouvés                      | —                            | Variantes possiblement non câblées                                          |

### 4. Service `TicketSalePolicy` (orchestration sell)

`PreparedSale` record retourné par `prepareSale(SellTicketCommand)` :

1. `validateSession(tenantId, terminalId)` → `posSessionPort.findOpenByTerminal(...)` ; **lève `SecurityException`** (devrait être un `ProblemRest`) si pas de session ou outlet bloqué.
2. `drawCutoffRule.requireBeforeCutoff(drawId)` → `GetDrawQuery` puis comparaison `now > draw.cutoffAt()`.
3. `ticketLinePreparationService.normalize(lines)` → normalisation selection via `BetSelectionNormalizer`.
4. `mergeDuplicates(lines)` → fusion par `(gameCode, selection, betType, betOption)`, sommation des stakes.
5. `evaluateLimitsAndAutonomy()` :
   - Construit un `LimitContext` avec `OperationType.SALE` et **`ZoneId.systemDefault()`** (non-déterministe, dépendant de la JVM).
   - `queryBus.send(EvaluateLimitPolicyQuery)` → `LimitEvaluationView`.
   - Si `BLOCK` et `!autonomyPolicy.requireApprovalOnBlock()` → `ProblemRest.limitBlocked(...)`.
6. `toTicketLines(tenantId, merged)` → `pricingCatalog.oddsFor(...)` × `stake` = `potentialPayout`.

### 5. Service `DrawCutoffRule`

- `requireBeforeCutoff(DrawId)` : `queryBus.send(GetDrawQuery)`, compare `now (clock)` vs `draw.cutoffAt().toInstant()`.
- `Instant cutoff = draw.cutoffAt().toInstant();` — implique `cutoffAt` est un `OffsetDateTime`/`ZonedDateTime`. AMBIGU : type exact non confirmé.

### 6. Service `TicketLinePreparationService`

- `normalize` : appelle `BetSelectionNormalizer.normalize(betType, selection)` pour chaque ligne ; valide `betOption` selon `betType`.
- `mergeDuplicates` : groupe par 4-uple ; **somme les stakes des doublons**.
- `toTicketLines` : appelle `pricingCatalog.oddsFor(tenantId, gameCode.name(), betType, betOption)` puis calcule `potentialPayout = stake × odds`.
- Anomalie : `requireStake` est appelé 3 fois sur la même valeur (normalize + mergeDuplicates + toTicketLines) — sur-validation.

### 7. Anomalies détectées (application)

- **Typo `TicketWritterPort`** (double 't') ; convention serait `TicketWriterPort`.
- **`SellTicketCommand.cashierId`** est typé `UserId` mais le mapper l'écrase avec celui du contexte → champ inutile/dangereux dans la command.
- **`SellTicketCommandHandler` peut NPE** : `prepared.session()` est documenté `// may be null` puis `session.outletId()` est appelé sans garde (ligne 113-117) si la branche `!BLOCK`.
- **`SellTicketCommandHandler` vérifie "mixed gameCode"** APRÈS persistence du ticket (lignes 90-95) — ticket sauvegardé puis exception → état orphelin si la transaction venait à ne pas rollback (TchTx couvre).
- **`approvalRequestId = UUID.randomUUID()`** dans `SellTicketCommandHandler` avec `// TODO: integrate approval domain later` → champ existe en DB (`approval_request_id`) mais non lié à un domaine d'approbation.
- **`TicketSalePolicy.evaluateLimitsAndAutonomy`** et **`CancelSaleCommandHandler.evaluateCancelLimits`** : `ZoneId.systemDefault()` hardcodé (timezone leak).
- **`CancelTicketCommand.performedBy`** : `UUID` brut au lieu de `UserId`.
- **`OverrideTicketResultCommand.performedBy`** : idem `UUID` brut.
- **`OverrideTicketResultCommand.status`** : utilise `TicketStatus` complet alors que seul `resultStatus` est lu — surface API trompeuse.
- **`RecordDrawTicketsResultCommandHandler`** :
  - **Ne consigne rien dans `ticket_settlement`** (table prévue par migration `V9__core_ticket.sql` pour idempotence) — un `DrawResultAppliedEvent` rejoué resettlerait les tickets via `markResulted` → exception sur tickets déjà passés en `WON/LOST` (l'exception remonte et stoppe le batch).
  - `cursorId = new UUID(0L, 0L)` comme valeur de seed — `00000000-...` est une valeur magique non documentée.
  - `toMatchView(null)` retourne un `DrawResultMatchView` à `null` — branche morte (handler lève déjà `IllegalStateException` plus haut).
  - Pas de borne supérieure sur le batch loop — risque de timeout transactionnel sur très gros draws (`@TchTx` couvre toute la boucle).
- **`TicketWinningCalculator.lotto5` option 3** : retourne `false` en MVP (`// MVP: option 3 not supported until rule is confirmed`) — silent fail (un ticket WON serait calculé comme LOST).
- **`VerifyPublicTicketQueryHandler`** :
  - Exception générique `try { ... } catch (Exception ignored) {}` autour du chargement outlet/address — masque les erreurs.
  - `payoutStatus` calculé sur `potentialPayout` (avant tirage) au lieu de `winningAmount` (après tirage) — incohérent : un ticket résolu LOST conserverait `POTENTIAL_WIN` dans la réponse publique.
  - `maskTerminal` retourne 8 premiers caractères d'un UUID + `…` — préfixe largement suffisant pour rapprochement avec données internes.
  - `maskAddress` ne masque pas l'`id` ni le `tenantId` de l'`Address` — fuit l'identifiant interne.
  - `SettingsCatalog.resolve(...)` exception → fallback silencieux à 14 jours sans log.
- **`ListTicketsQueryHandler`** émet l'audit log via `try/catch/log.warn` — silencieux si défaillant.
- **`SalesTicketAdminAdapter.refuseNewTickets/allowNewTickets`** : `// no-op v1` — interface implémentée vide.
- **`ExpireTicketsCommand`** : sans champ et sans handler → code mort.
- **`ApprovePendingTicketSaleCommand` / `RejectPendingTicketSaleCommand`** : doublons sans handler localisé.

---

## Domaine 3 : `core.sales.infra` (Persistence + web + bridge + scheduler)

### 1. Structure de packages

```
core/sales/infra/
├── persistence/
│   ├── TicketEntity.java            (BaseTenantEntity → table ticket)
│   ├── TicketLineEntity.java        (table ticket_line)
│   ├── repository/
│   │   ├── SpringTicketJpaRepository.java
│   │   ├── TicketSettlementJpaRepository.java   (table ticket_settlement)
│   │   └── TicketSettlementQueryRepository.java
│   ├── adapter/
│   │   ├── JpaTicketRepositoryAdapter.java       (implémente Reader + Writer)
│   │   ├── TicketSettlementJpaAdapter.java       (cursor batch)
│   │   ├── TicketSettlementQueryJpaAdapter.java
│   │   └── DrawResultViewPortJdbcAdapter.java    (SQL JOIN draw_result + result_slot)
│   └── mapper/
│       └── TicketMapper.java
├── web/
│   ├── TicketController.java         (path /tenant/tickets)
│   ├── PublicTicketController.java   (path /public/tickets)
│   ├── mapper/TicketWebMapper.java
│   └── model/                        (Sell/Cancel/Override Requests + responses)
├── bridge/
│   └── SalesTicketAdminAdapter.java  (implémente core.outlet.SalesTicketAdminPort)
├── event/
│   └── SalesLedgerListener.java      (@EventListener — non AFTER_COMMIT)
└── generator/
    ├── TimeBasedTicketNumberGenerator.java   (TCK-YYMMDD-HHMMSS-XXXXXX-C)
    └── CrockfordPublicCodeGenerator.java     (12 chars Base32)
```

### 2. Entité JPA `TicketEntity`

- Table : `ticket`
- Hérite de : `BaseTenantEntity` (tenantId + audit fields + RLS)
- **Champs JPA** : `terminalId (UUID raw)`, `drawId (UUID raw)`, `sessionId (UUID raw, nullable)`, `ticketCode`, `publicCode (length 32, nullable en JPA)`, `saleStatus`, `resultStatus`, `settlementStatus`, `currency`, `totalAmount`, `winningAmount (nullable)`, `resultedAt (nullable)`, `approvalRequestId (UUID nullable)`, `lines (@OneToMany cascade ALL orphanRemoval)`
- **Aucune `@UniqueConstraint` annotée** dans `@Table` pour `(tenantId, ticketCode)` ni `publicCode` ; les contraintes `uq_ticket_tenant_code` et `uq_ticket_public_code` n'existent que dans la migration Flyway `V9__core_ticket.sql`.
- **Discordance `nullable` `publicCode`** : `@Column(name = "public_code", length = 32)` en JPA = nullable=true par défaut, alors que la migration définit `public_code varchar(32) NOT NULL`. Tout `null` en mémoire passerait la validation JPA et exploserait au flush.
- `@Audited` (Envers).
- Pas de champ `canceledAt` ni `voidedAt` — perte de la timestamp de cancel hors `updatedAt`.

### 3. Tables liées (`V9__core_ticket.sql`)

- `pricing_odds` (catalog) : snapshot odds par tenant/game/bet_type/bet_option avec UNIQUE actif partiel.
- `ticket` : invariants CHECK SQL renforcés :
  - `chk_ticket_result_fields` (cohérence NOT_RESULTED ↔ winning_amount/resulted_at).
  - `chk_ticket_settlement_requires_result` (SETTLED implique result_status WON/LOST/OVERRIDDEN).
  - UNIQUE `(id, tenant_id)` pour FK composite vers `ticket_line`.
- `ticket_line` : FK composite `(ticket_id, tenant_id)` + `chk_ticket_line_bet_option`.
- `ticket_settlement` : table d'idempotence avec `UNIQUE(tenant_id, ticket_id, draw_result_id)` — **présente en DB mais non écrite par le code Java**.

### 4. Adapter `JpaTicketRepositoryAdapter`

- **Implémente `TicketReaderPort` ET `TicketWritterPort`** dans la même classe (mélange de responsabilités, accepté par convention `JpaXxxAdapter` ?).
- Dépendances injectées hétérogènes :
  - `DrawLookupPort` (cross-domain `core.draw`)
  - `OutletReaderPort` (cross-domain `core.outlet`)
  - `SalesSessionReaderPort` (cross-domain `core.session`)
  - `TchContextResolver`
- `getTicketPrintView(ticketId)` charge `Ticket + Draw + Channel + Outlet + Session + Locale.FRENCH` — locale hardcodée (`// TODO: get locale from context`).
- `archiveOldTickets(cutoff)` : `UPDATE ... SET deletedAt = now WHERE createdAt < cutoff AND deletedAt IS NULL` — scopé tenant via RLS uniquement.
- `findWithLinesById` : récupère `contextResolver.currentOrNull()` mais ne l'utilise pas — code mort.

### 5. Adapter `DrawResultViewPortJdbcAdapter`

- SQL : `select dr.id, rs.key as slot_key, dr.occurred_at, dr.haiti_result, dr.source_result FROM draw_result dr JOIN result_slot rs ON rs.id = dr.result_slot_id WHERE dr.id = ?`
- **JOIN cross-domain** (`draw_result` est dans `core.drawresult`, `result_slot` dans `catalog.resultslot`) via SQL brut depuis `core.sales` → bypass complet des ports/API des domaines source.
- Parse JSONB `haiti_result` (priorité) sinon `source_result` → extrait `lot1, lot2, lot3, pick3, two_digits[]`.
- Conversion silencieuse en `null` si parsing JSON échoue.

### 6. Adapter `TicketSettlementJpaAdapter`

- Cursor (`createdAt`, `id`) keyset pagination.
- **Sur-pagination** : `repo.findBatchForDrawWithLines(...)` + `.stream().limit(pageSize)` — la limit de la query est probablement déjà appliquée → double protection, perte de clarté.

### 7. Generators

- **`TimeBasedTicketNumberGenerator`** : format `TCK-YYMMDD-HHMMSS-XXXXXX-C` ; check digit modulo 10 sur la somme des chars (faible — ne détecte pas toutes les permutations).
- **`CrockfordPublicCodeGenerator`** : 12 chars Base32 (Crockford), `SecureRandom` ; espace `32^12 ≈ 1.15e18`.
- **Aucune logique de retry sur collision** dans `JpaTicketRepositoryAdapter.save()` — un conflit sur `uq_ticket_tenant_code` ou `uq_ticket_public_code` remonte une `DataIntegrityViolationException` directement à l'appelant.

### 8. Controllers HTTP

#### `TicketController` (`/tenant/tickets`)

- Auth granulaire `@Secured` par méthode :
  - `POST /` (sell) : `ROLE_CASHIER, ROLE_ADMIN, ROLE_SUPER_ADMIN`
  - `POST /{id}/approve`, `POST /{id}/reject` : `ROLE_ADMIN, ROLE_SUPER_ADMIN`
  - `GET /` (list), `GET /{id}` (details) : `ROLE_CASHIER, ROLE_ADMIN, ROLE_SUPER_ADMIN`
  - `PATCH /{id}/result/override` : `ROLE_ADMIN, ROLE_SUPER_ADMIN` (et `@RequiresPermission("ticket.result.override")` côté handler)
- **`PATCH /{id}/cancel`** : **AUCUN `@Secured`** — accessible à tout utilisateur authentifié.
- **`GET /{id}/print`, `GET /{id}/print.escpos`, `GET /{id}/print.pdf`** : **AUCUN `@Secured`** — impression sans contrôle de rôle.
- Retours :
  - `sell` : `ResponseEntity<ApiResponse<TicketResponse>>` avec status 201/202 — conforme.
  - `approve/reject/details/list/cancel/override` : `ApiResponse<T>` — conforme.
  - `print` : `ResponseEntity<String>` (Base64 du PDF) — non conforme `ApiResponse<T>`.
  - `printEscpos` / `printPdf` : `byte[]` direct — non conforme (acceptable pour content-type binaire).

#### `PublicTicketController` (`/public/tickets`)

- `GET /verify/{publicCode}` : retourne `ResponseEntity<?>` (raw) ; 404 si absent ; headers `X-Robots-Tag: noindex, nofollow` + `Cache-Control: no-store`. **Non conforme `ApiResponse<T>`**.
- `GET /qr/{publicCode}.png` : retourne `byte[]` PNG ; cache 1h. Catch `IllegalArgumentException` → 404. **Pas de rate-limiting visible** ni de protection brute-force sur le `publicCode` (12 chars).

### 9. Bridge `SalesTicketAdminAdapter`

- Implémente `core.outlet.application.port.out.SalesTicketAdminPort`.
- `getCloseStats(outletId, from, to)` : 6 `count(...)` sur `SpringTicketJpaRepository` filtrés par `sessionId IN (...)` issus de `SessionLookupPort.findSessionIds(...)` → projection `TicketCloseStats(total, sold, voided, resultedWin, resultedLoss, paid)`.
- `refuseNewTickets()` / `allowNewTickets()` : **no-op v1** — l'autorité du blocage est ailleurs (`OutletLookupPort.isSalesBlocked` lu depuis `TicketSalePolicy.validateSession`).

### 10. Listener `SalesLedgerListener` (infra/event/)

- `@EventListener` (synchrone, dans la même tx que la publication) — **incohérent avec convention "thin listener AFTER_COMMIT"**.
- Catch global `try { ... } catch (Exception e) { log.error ... }` — exception silencée (mais comme c'est sync in-tx, l'écriture ledger fait partie de la même tx que le ticket → couplage transactionnel fort).
- Appelle `ledgerPort.recordTicketSale(tenantId, ticketId, stakeCents, occurredAt)` — port `RecordLedgerFromSalesPort` côté `core.ledger.application.port.in`. **Couplage direct port-in**, qui est le pattern attendu pour features mais inhabituel pour un listener.

### 11. Listener `DrawResultedEventListener` (application/event/)

- `@TransactionalEventListener(AFTER_COMMIT)` — conforme.
- Consomme `core.draw.domain.event.DrawResultAppliedEvent`.
- Émet `RecordDrawTicketsResultCommand(tenantId, drawId, drawResultId, occurredAt)` via `commandBus.send(...)`.
- **Aucune idempotence** : pas de `ProcessedEventPort`, pas de check `ticket_settlement` ; un re-publish (rare avec AFTER_COMMIT mais possible en cas de redémarrage Spring Batch + ré-application) re-traverserait tous les tickets.
- Pas de log d'échec si `commandBus.send` lève.

### 12. Schedulers

**Aucun scheduler dans `core.sales`**. Aucun cron pour :

- Archivage (`ArchiveTicketsCommand`) — handler existe, déclencheur absent.
- Expiration (`ExpireTicketsCommand`) — pas de handler.
- Re-tentative settlement en cas de PROVISIONAL → FINAL.

### 13. Cache

- Aucun `@Cacheable` / `@CacheEvict` détecté dans `core.sales`.
- Le QR PNG est servi avec `Cache-Control: public, max-age=3600` côté HTTP (CDN-cacheable).

### 14. Anomalies détectées (infra)

- **Sécurité absente sur `cancel` et `print*`** dans `TicketController`.
- **`PublicTicketController.verify`** non conforme `ApiResponse<T>` ; raw `ResponseEntity<?>`.
- **`SellTicketRequest`** contient `tenantId, sessionId, cashierId` dans le body — **`tenantId` et `cashierId` sont silencieusement écrasés** par le contexte (`TicketWebMapper`), `sessionId` du body n'est jamais lu (la session est résolue par `validateSession(tenantId, terminalId)`). Champs trompeurs/inutiles.
- **Aucun retry sur collision** ticketCode/publicCode lors du save.
- **`TicketEntity` ↔ migration discordance** sur `public_code NOT NULL`.
- **`DrawResultViewPortJdbcAdapter`** : SQL JOIN cross-domain bypassant les ports `core.drawresult.api`.
- **`SalesLedgerListener`** : `@EventListener` synchrone incohérent (convention attendue : `@TransactionalEventListener(AFTER_COMMIT)`).
- **`JpaTicketRepositoryAdapter`** : implémente Reader+Writer dans la même classe + dépendances cross-domain (`DrawLookupPort`, `OutletReaderPort`, `SalesSessionReaderPort`).
- **`getTicketPrintView`** : `Locale.FRENCH` hardcodé.
- **`SalesTicketAdminAdapter.refuseNewTickets/allowNewTickets`** : no-op publié à `core.outlet`.
- **Pas de scheduler d'archivage** — `ArchiveTicketsCommand` est un handler orphelin.

---

## Pipelines fonctionnels

### Pipeline A — Vente d'un ticket (sell)

```
POST /tenant/tickets  (SellTicketRequest)
   │
   ▼
TicketController.sell()  [@Secured CASHIER/ADMIN/SUPER_ADMIN]
   │
   ▼  (TicketWebMapper.toSellCommand — écrase tenantId/cashierId via contexte)
SellTicketCommand → CommandBus
   │
   ▼
SellTicketCommandHandler  [@TchTx]
   │
   ├─ TicketSalePolicy.prepareSale(cmd)
   │     ├─ validateSession(tenantId, terminalId)
   │     │     ├─ SalesSessionReaderPort.findOpenByTerminal()       (core.session)
   │     │     └─ OutletLookupPort.isSalesBlocked(outletId)       (core.outlet)
   │     ├─ DrawCutoffRule.requireBeforeCutoff(drawId)
   │     │     └─ QueryBus.send(GetDrawQuery)                     (core.draw)
   │     ├─ TicketLinePreparationService.normalize(lines)
   │     │     └─ BetSelectionNormalizer.normalize(...)
   │     ├─ TicketLinePreparationService.mergeDuplicates(lines)
   │     ├─ evaluateLimitsAndAutonomy()
   │     │     ├─ QueryBus.send(EvaluateLimitPolicyQuery)         (core.limitpolicy)
   │     │     └─ ResolveAutonomyPolicyService.resolve(...)       (core.autonomy)
   │     │           └─ throw ProblemRest.limitBlocked(...) si BLOCK + !approval
   │     └─ TicketLinePreparationService.toTicketLines(tenantId, lines)
   │           └─ PricingCatalog.oddsFor(tenantId, gameCode, betType, betOption)  (catalog.pricing)
   │
   ├─ Si limits.outcome == BLOCK :
   │     ├─ TicketSaleFactory.newPendingApprovalTicket(...)
   │     │     ├─ TicketNumberGeneratorPort.generate()
   │     │     └─ TicketPublicCodeGeneratorPort.generate()
   │     ├─ TicketWritterPort.save(ticket)
   │     ├─ ApiResponseContext.addNotice(APPROVAL_REQUIRED + approvalRequestId aléatoire)
   │     └─ return SellTicketResult(saved, PENDING_APPROVAL, approvalRequestId)
   │
   └─ Sinon :
         ├─ TicketSaleFactory.newSoldTicket(...)
         ├─ TicketWritterPort.save(ticket)
         ├─ Vérifie "single gameCode per ticket" (MVP) — exception après save !
         ├─ Construit TicketPlacedEvent (lines en cents)
         ├─ AfterCommit.run(() -> publisher.publish(placed))
         └─ return SellTicketResult(saved, SUCCESS|SUCCESS_WITH_WARNINGS, null)
   │
   ▼  (response 201 CREATED ou 202 ACCEPTED selon outcome)
ApiResponse<TicketResponse>

   ▼  (AFTER COMMIT)
LimitPolicyEventsListener.on(TicketPlacedEvent)
   └─ CommandBus.send(ApplyTicketExposureCommand)              (core.limitpolicy)

SalesSessionTotalsProjectionListener.on(TicketPlacedEvent)      (core.session)
StatsAggregatesEventListener / StatsDailyUpdater              (features.stats)

   ▼  (DURANT TX — @EventListener synchrone, anomalie)
SalesLedgerListener.onTicketPlaced(TicketPlacedEvent)
   └─ RecordLedgerFromSalesPort.recordTicketSale(...)         (core.ledger)
         └─ exception silencieusement loggée
```

**Outcomes possibles** :

- `SUCCESS` → 201 Created, `ApiResponse.created(ticket)`.
- `SUCCESS_WITH_WARNINGS` → 201 Created (mêmes données, notices côté `ApiResponseContext`).
- `PENDING_APPROVAL` → 202 Accepted, notice `APPROVAL_REQUIRED` avec `approvalRequestId`.

**Approval workflow** : `POST /tenant/tickets/{id}/approve` avec `approvedBy` + `reason` (query params) → `ApproveTicketSaleCommand` → re-validation cutoff + session + transition `PENDING_APPROVAL → SOLD` → `TicketPlacedEvent` (avec champs `outletId/agentId/drawChannelId` potentiellement null, voir anomalie).

**Reject workflow** : `POST /tenant/tickets/{id}/reject` → `PENDING_APPROVAL → REJECTED`. Aucun event publié.

### Pipeline B — Vérification publique d'un ticket

```
GET /public/tickets/verify/{publicCode}  (PublicTicketController.verify)
   │
   ▼
QueryBus.send(VerifyPublicTicketQuery(publicCode, now))
   │
   ▼
VerifyPublicTicketQueryHandler
   │
   ├─ Normalisation : trim + uppercase + remove '-' et ' '
   ├─ TicketReaderPort.findByPublicCode(code)                  (JPA, RLS désactivé pour public ?)
   │     └─ null → return null (controller → 404)
   │
   ├─ resolveVisibilityDays(tenantId)
   │     └─ SettingsCatalog.resolve(forTenant(tenantId, ["ticket.verification"]))  (catalog.settings)
   │           └─ default 14 si exception ou settings absente
   │
   ├─ isVisible(ticket, now, visibilityDays)
   │     └─ createdAt + visibilityDays > now ?
   │
   ├─ Si NON visible :
   │     └─ return TicketVerificationResult avec :
   │           ticketId, publicCode, terminalMasked, createdAt
   │           saleStatus/resultStatus/settlementStatus = null
   │           drawId = null, totalAmount = null, potentialTotalPayout = null
   │           outletName = null, outletAddress = null, lines = []
   │           payoutStatus = "EXPIRED"
   │
   └─ Si visible :
         ├─ Construit lines (gameCode, selection, stake, potentialPayout)
         ├─ potentialTotal = sum(potentialPayout)
         ├─ payoutStatus = "POTENTIAL_WIN" si > 0 sinon "NO_PAYOUT"
         │     ⚠ ne reflète PAS resultStatus ni settlementStatus
         ├─ Best-effort lookup outlet (catch Exception ignoré) :
         │     ├─ TerminalReaderPort.findById(tenantId, terminalId)   (core.terminal)
         │     ├─ OutletReaderPort.findById(outletId)                  (core.outlet)
         │     └─ AddressReaderPort.findById(tenantId, addressId)      (core.address)
         ├─ maskAddress(outlet.address) : garde city + country uniquement
         │     ⚠ ne masque pas address.id ni address.tenantId
         └─ return TicketVerificationResult complet (statuts inclus)
   │
   ▼
ResponseEntity.ok().body(result)
   Headers : X-Robots-Tag: noindex, nofollow ; Cache-Control: no-store
```

**Réponse JSON** : record `TicketVerificationResult { ticketId, publicCode, saleStatus, resultStatus, settlementStatus, drawId, terminalMasked, createdAt, totalAmount, potentialTotalPayout, payoutStatus, outletName, outletAddress, lines[] }`.

**Surfaces sensibles exposées publiquement** :

- `ticketId` (UUID interne) — exposé tel quel.
- `drawId` (UUID interne).
- `terminalMasked` — 8 premiers chars du UUID + `…`.
- `outletAddress.id`, `outletAddress.tenantId` (UUID internes).
- `outletAddress.city`, `outletAddress.country`.
- `outletName` (raison sociale).

**QR PNG** : `GET /public/tickets/qr/{publicCode}.png?size=280` → `QrPayloadBuilder` (non lu intégralement) → ZXing rendu via `QrRenderer`. Cache 1h.

### Pipeline C — Settlement après tirage (post draw apply)

```
core.draw publie DrawResultAppliedEvent (AfterCommit)
   │
   ▼  (AFTER COMMIT)
DrawResultedEventListener.onDrawResultApplied(event)         (core.sales.application.event)
   │
   └─ CommandBus.send(RecordDrawTicketsResultCommand(tenantId, drawId, drawResultId, occurredAt))
         │
         ▼
RecordDrawTicketsResultCommandHandler  [@TchTx]
   │
   ├─ DrawResultViewPort.findById(drawResultId)              (JDBC SQL JOIN draw_result + result_slot)
   │     └─ DrawResultMinimalView { id, slotKey, occurredAt, lot1, lot2, lot3, pick3, twoDigits[] }
   │
   └─ Loop keyset cursor (createdAt, id) batchs de 250 :
         ├─ TicketSettlementPort.findNextBatchForDraw(drawId, cursor, limit=250)
         │     └─ JPA: SOLD + NOT_RESULTED tickets ordered by (createdAt, id)
         │
         └─ For each ticket :
               ├─ winningAmount = TicketWinningCalculator.calculateWinningAmount(ticket, drawResultView)
               │     └─ switch betType :
               │           MATCH_1_2D / MATCH_2_2D / MATCH_3_2D → twoDigits set lookup
               │           MARRIAGE_2D2D → 2 picks dans twoDigits
               │           LOTTO3_3D → equals(pick3)
               │           LOTTO4_PATTERN → split(2,4) match (lot1/lot2/lot3) selon betOption
               │           LOTTO5_PATTERN → split(3,5) match (pick3 + lot2/lot3) selon betOption
               │                            ⚠ option 3 hardcoded false (MVP)
               ├─ ticket.markResulted(winningAmount, now)
               │     └─ resultStatus = winningAmount > 0 ? WON : LOST
               │     └─ settlementStatus reste UNSETTLED
               ├─ TicketWritterPort.save(ticket)
               └─ AfterCommit.run(() -> publisher.publish(TicketResultedEvent))

   ▼ (AFTER COMMIT)
StatsAggregatesEventListener / autres consommateurs        (features.stats)
core.payout (claim ouvert si WON ?)                         (cross-domain non confirmé en lecture)
```

**Anomalie idempotence** : `ticket_settlement` (table prévue) n'est ni écrite ni vérifiée. Re-déclencher `RecordDrawTicketsResultCommand` lèverait `IllegalStateException` au premier ticket déjà passé en `WON/LOST` (via `requireSaleStatus(SOLD)` dans `markResulted`) → boucle abandonnée.

**Anomalie payout** : la transition `RESULTED → SETTLED` n'est PAS effectuée par ce handler. `settlementStatus = UNSETTLED` reste. Le passage à `SETTLED` est délégué à `core.payout.MarkTicketPayoutPaidCommandHandler.markPayoutPaid()` quand le payout est exécuté.

### Pipeline D — Override admin

```
PATCH /tenant/tickets/{id}/result/override  (OverrideTicketResultRequest)
   ├─ @Secured ROLE_ADMIN, ROLE_SUPER_ADMIN
   └─ Handler @RequiresPermission("ticket.result.override")
   │
   ▼
OverrideTicketResultCommand → CommandBus → OverrideTicketResultCommandHandler [@TchTx]
   │
   ├─ TicketReaderPort.findWithLinesById(ticketId)  → 404 sinon
   ├─ Validation : totalPayout >= 0 ; resultStatus ∈ {WON, LOST}
   ├─ ticket.forceResult(totalPayout, resultStatus, when)
   │     └─ refuse si saleStatus == VOID
   │     └─ resultStatus = WON|LOST|OVERRIDDEN selon overload
   │     └─ settlementStatus = UNSETTLED (revert si déjà SETTLED — anomalie)
   ├─ TicketWritterPort.save(ticket)
   └─ AfterCommit.run(() -> publisher.publish(TicketResultOverriddenEvent))
```

**Anomalies override** :

- Aucun garde anti double-override (un override sur un ticket déjà OVERRIDDEN passe).
- `forceResult` repasse `settlementStatus` à `UNSETTLED` même si le ticket était déjà `SETTLED` → casse l'historique de paiement.
- `OverrideTicketResultCommand.status` est un `TicketStatus` complet mais seul `resultStatus` est lu → `saleStatus` et `settlementStatus` du body sont silencieusement ignorés.

### Pipeline E — Cancel

```
PATCH /tenant/tickets/{id}/cancel  (CancelTicketRequest)
   ⚠ AUCUN @Secured sur l'endpoint
   │
   ▼
CancelTicketCommand (mappé en CancelSaleCommand par TicketWebMapper)
   │
   ▼
CancelSaleCommandHandler [@TchTx]
   │
   ├─ TicketReaderPort.findWithLinesById(ticketId) → 404 sinon
   ├─ Lookup outletId via SalesSessionReaderPort.findById(sessionId)
   ├─ evaluateCancelLimits(...)  (LimitContext OperationType.CANCEL)
   │     └─ ZoneId.systemDefault() — anomalie timezone
   ├─ enforceCancelDecisionMatrix(...)  (BLOCK + !approval → ProblemRest.limitBlocked)
   ├─ ticket.voidTicket(now)
   │     └─ refuse si saleStatus ∉ {SOLD, PENDING_APPROVAL}
   ├─ TicketWritterPort.save(ticket)
   └─ AfterCommit.run(() -> publisher.publish(TicketCancelledEvent))
```

---

## Synthèse transverse

### A. Frontières domaine

**1. Imports `core.X.infra.*` depuis `core.sales` ?**
Aucun détecté dans le périmètre.

**2. Imports `core.X.domain.event.*` depuis `core.sales` ?**

- `core.sales.application.event.DrawResultedEventListener` importe `core.draw.domain.event.DrawResultAppliedEvent` — **conforme** (consommer un event d'un autre domaine via son package `domain.event` est attendu).

**3. Imports `core.X.application.port.*` depuis `core.sales` ?**

- `core.sales.application.rule.DrawCutoffRule` consomme `core.draw.application.query.model.GetDrawQuery` via `QueryBus` — **conforme**.
- `core.sales.application.command.handler.SellTicketCommandHandler` (via `TicketSalePolicy`) consomme `core.session.application.port.out.SalesSessionReaderPort`, `core.outlet.application.port.out.OutletLookupPort`, `core.limitpolicy.application.query.model.EvaluateLimitPolicyQuery`, `core.autonomy.application.service.ResolveAutonomyPolicyService` — **6 dépendances cross-domain dans une seule policy**.
- `core.sales.application.query.handler.VerifyPublicTicketQueryHandler` consomme `core.address.application.port.AddressReaderPort`, `core.outlet.application.port.out.OutletReaderPort`, `core.terminal.application.port.out.TerminalReaderPort`, `catalog.settings.api.SettingsCatalog` — orchestrateur de fait.
- `core.sales.infra.persistence.adapter.JpaTicketRepositoryAdapter` consomme `core.draw.application.port.out.DrawLookupPort`, `core.outlet.application.port.out.OutletReaderPort`, `core.session.application.port.out.SalesSessionReaderPort` — un adapter persistence avec dépendances vers 3 autres domaines pour assembler `TicketPrintView`.

**4. SQL cross-domain ?**

- `DrawResultViewPortJdbcAdapter` : SQL `SELECT ... FROM draw_result JOIN result_slot ON ...` depuis `core.sales` → tables appartenant à `core.drawresult` et `catalog.resultslot`. **Violation stricte** d'isolation persistence.

### B. Naming events

| Event                         | Package                   | Producer                                                                 | Consumer principal                                                                                                                                                                                                     | Mode publication                                  |
| ----------------------------- | ------------------------- | ------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------- |
| `TicketPlacedEvent`           | `core.sales.domain.event` | `core.sales.SellTicketCommandHandler`, `ApproveTicketSaleCommandHandler` | `core.limitpolicy.LimitPolicyEventsListener`, `core.session.SalesSessionTotalsProjectionListener`, `features.stats.StatsAggregatesEventListener`, `features.stats.StatsDailyUpdater`, `core.sales.SalesLedgerListener` | `AfterCommit` (sauf `SalesLedgerListener` = sync) |
| `TicketCancelledEvent`        | `core.sales.domain.event` | `CancelSaleCommandHandler`                                               | limitpolicy, session, stats                                                                                                                                                                                            | `AfterCommit`                                     |
| `TicketResultedEvent`         | `core.sales.domain.event` | `RecordDrawTicketsResultCommandHandler`                                  | stats                                                                                                                                                                                                                  | `AfterCommit`                                     |
| `TicketResultOverriddenEvent` | `core.sales.domain.event` | `OverrideTicketResultCommandHandler`                                     | aucun listener confirmé                                                                                                                                                                                                | `AfterCommit`                                     |
| `TicketPaidEvent`             | `core.sales.domain.event` | `core.payout.MarkTicketPayoutPaidCommandHandler` (cross-domain)          | stats                                                                                                                                                                                                                  | `AfterCommit`                                     |
| `TicketPaymentPendingEvent`   | `core.sales.domain.event` | usage non confirmé                                                       | —                                                                                                                                                                                                                      | —                                                 |

**Anomalie naming** : `TicketPlacedEvent` est publié à la fois pour la vente initiale (sell) et pour l'approbation (approve d'un PENDING_APPROVAL) — l'event ne distingue pas les deux cas. Les consommateurs `LimitPolicyEventsListener` et `StatsDailyUpdater` traitent identiquement, ce qui peut induire une double comptabilisation si la chaîne sell→approval est mal modélisée.

### C. Sécurité

| Endpoint                                     | Path             | Auth déclarée                                                                   |
| -------------------------------------------- | ---------------- | ------------------------------------------------------------------------------- |
| `POST /tenant/tickets`                       | sell             | `@Secured CASHIER, ADMIN, SUPER_ADMIN`                                          |
| `POST /tenant/tickets/{id}/approve`          | approve          | `@Secured ADMIN, SUPER_ADMIN`                                                   |
| `POST /tenant/tickets/{id}/reject`           | reject           | `@Secured ADMIN, SUPER_ADMIN`                                                   |
| `GET /tenant/tickets`                        | list             | `@Secured CASHIER, ADMIN, SUPER_ADMIN`                                          |
| `GET /tenant/tickets/{id}`                   | details          | `@Secured CASHIER, ADMIN, SUPER_ADMIN`                                          |
| `PATCH /tenant/tickets/{id}/cancel`          | cancel           | **AUCUNE**                                                                      |
| `GET /tenant/tickets/{id}/print`             | print PDF base64 | **AUCUNE**                                                                      |
| `GET /tenant/tickets/{id}/print.escpos`      | print ESC/POS    | **AUCUNE**                                                                      |
| `GET /tenant/tickets/{id}/print.pdf`         | print PDF        | **AUCUNE**                                                                      |
| `PATCH /tenant/tickets/{id}/result/override` | override         | `@Secured ADMIN, SUPER_ADMIN` + `@RequiresPermission("ticket.result.override")` |
| `GET /public/tickets/verify/{publicCode}`    | public verify    | (public, intentionnel)                                                          |
| `GET /public/tickets/qr/{publicCode}.png`    | public QR        | (public, intentionnel)                                                          |

**Anomalies sécurité** :

- `cancel`, `print`, `print.escpos`, `print.pdf` : aucun rôle requis sur `/tenant/tickets/**` — un utilisateur authentifié sans rôle métier peut annuler ou imprimer.
- Aucun rate-limiting confirmé sur `/public/tickets/verify/{publicCode}` ni `/public/tickets/qr/{publicCode}.png` (commentaire dans `VerifyPublicTicketQueryHandler` mentionne "Rate-limit at controller layer" — non implémenté).

### D. Conformité ApiResponse

| Controller / endpoint                                               | Retour                                          |
| ------------------------------------------------------------------- | ----------------------------------------------- |
| `TicketController.sell/approve/reject/list/details/cancel/override` | `ApiResponse<T>` — conforme                     |
| `TicketController.print`                                            | `ResponseEntity<String>` Base64 — non conforme  |
| `TicketController.printEscpos / printPdf`                           | `byte[]` — acceptable (binaire)                 |
| `PublicTicketController.verify`                                     | `ResponseEntity<?>` raw — **non conforme**      |
| `PublicTicketController.qrPng`                                      | `ResponseEntity<byte[]>` — acceptable (binaire) |

### E. Schedulers & Idempotence

| Scheduler | Cron | Mécanisme d'idempotence |
| --------- | ---- | ----------------------- |
| (aucun)   | —    | —                       |

`ArchiveTicketsCommand` n'a pas de scheduler câblé. `ExpireTicketsCommand` n'a ni scheduler ni handler.

**Settlement** : déclenché en réaction à `DrawResultAppliedEvent` (event AFTER_COMMIT). **Pas de protection idempotente côté `core.sales`** :

- `ticket_settlement` (table d'idempotence prévue par migration) jamais écrite.
- `ProcessedEventPort` non utilisé dans `DrawResultedEventListener`.
- Re-publish → `IllegalStateException` au premier ticket déjà résolu, batch arrête prématurément.

### F. Sources de vérité multiples

1. **Cancel command** : 2 records distincts (`CancelSaleCommand` et `CancelTicketCommand`) ; `CancelTicketCommand` est mappé en `CancelSaleCommand` par `TicketWebMapper`. Doublon de modèle.
2. **Approve/Reject command** : 4 records (`ApproveTicketSaleCommand`/`RejectTicketSaleCommand` actifs + `ApprovePendingTicketSaleCommand`/`RejectPendingTicketSaleCommand` orphelins). Sources non unifiées.
3. **`SellTicketRequest.tenantId/cashierId`** : présents dans le body web mais écrasés par contexte → sources contradictoires en surface API publique.
4. **`SellTicketRequest.sessionId`** : champ body jamais lu (la session est résolue serveur-side).
5. **`Ticket.settle()` / `markAsPaid()` / `markPayoutPaid()`** : 3 méthodes domaine équivalentes.
6. **`DrawResultMinimalView`** (port out de `core.sales`) duplique partiellement le payload Haïti déjà défini dans `core.haiti.domain.lottery.model` — le port aurait dû dépendre des types canoniques.

### G. Calculs `Instant`/`ZoneId`

- `TicketSalePolicy.evaluateLimitsAndAutonomy` : `ZoneId.systemDefault()` (ligne 136).
- `CancelSaleCommandHandler.evaluateCancelLimits` : `ZoneId.systemDefault()` (ligne 138).
- `TimeBasedTicketNumberGenerator` : `ZoneOffset.UTC` — déterministe et conforme.
- `DrawCutoffRule.requireBeforeCutoff` : `Instant.now(clock)` + `draw.cutoffAt().toInstant()` — déterministe (utilise Clock injecté).

### H. Gaps fonctionnels

| Fichier                                                              | Contenu                                                                               |
| -------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| `SellTicketCommandHandler.java` ligne 64                             | `var approvalRequestId = UUID.randomUUID(); // TODO: integrate approval domain later` |
| `JpaTicketRepositoryAdapter.java` ligne 199                          | `// TODO: get locale from context or request`                                         |
| `TicketWebMapper.java` ligne 122                                     | `// NOTE: performedBy devrait venir du ctx (RLS), pas du body.`                       |
| `SalesTicketAdminAdapter.java` lignes 41-46                          | `// no-op v1` sur `refuseNewTickets`/`allowNewTickets`                                |
| `TicketWinningCalculator.java` ligne 109                             | `case 3 -> false; // MVP: option 3 not supported until rule is confirmed`             |
| `Ticket.java` ligne 432                                              | `// Record-style accessors (some legacy code expects .id(), .tenantId() etc.)`        |
| `ExpireTicketsCommand.java`                                          | Record vide, sans handler                                                             |
| `ApprovePendingTicketSaleCommand` / `RejectPendingTicketSaleCommand` | Sans handler                                                                          |

### I. Couplage cross-domain (résumé)

| Direction                            | Type                 | Détail                                                                                                                |
| ------------------------------------ | -------------------- | --------------------------------------------------------------------------------------------------------------------- |
| `core.sales` → `core.draw`           | Query + port         | `GetDrawQuery`, `DrawLookupPort`, `DrawChannelLabelResolver`, `DrawOccurrenceLabelResolver`, `DrawResultAppliedEvent` |
| `core.sales` → `core.drawresult`     | SQL brut (JDBC JOIN) | `DrawResultViewPortJdbcAdapter` JOIN `draw_result` + `result_slot`                                                    |
| `core.sales` → `core.session`        | Port                 | `SalesSessionReaderPort`                                                                                              |
| `core.sales` → `core.outlet`         | Port                 | `OutletReaderPort`, `OutletLookupPort`, `SessionLookupPort`, `SalesTicketAdminPort` (impl)                            |
| `core.sales` → `core.terminal`       | Port                 | `TerminalReaderPort`                                                                                                  |
| `core.sales` → `core.address`        | Port                 | `AddressReaderPort`                                                                                                   |
| `core.sales` → `core.limitpolicy`    | Query                | `EvaluateLimitPolicyQuery`                                                                                            |
| `core.sales` → `core.autonomy`       | Service              | `ResolveAutonomyPolicyService`                                                                                        |
| `core.sales` → `core.audit`          | Handler direct       | `AuditLoggingCommandHandler` (instance, pas via bus)                                                                  |
| `core.sales` → `core.ledger`         | Port-in direct       | `RecordLedgerFromSalesPort` (depuis `SalesLedgerListener`)                                                            |
| `core.sales` → `core.accesscontrol`  | Annotation           | `@RequiresPermission`                                                                                                 |
| `core.sales` → `catalog.pricing`     | API                  | `PricingCatalog`                                                                                                      |
| `core.sales` → `catalog.settings`    | API                  | `SettingsCatalog`                                                                                                     |
| `core.sales` → `catalog.drawchannel` | View                 | `DrawChannelView`                                                                                                     |
| `core.payout` → `core.sales`         | Ports + events       | `TicketReaderPort`, `TicketWritterPort`, `TicketPaidEvent`, `TicketPaymentPendingEvent`                               |
| `core.limitpolicy` → `core.sales`    | Event                | `TicketPlacedEvent`                                                                                                   |
| `core.session` → `core.sales`        | Event                | `TicketPlacedEvent` (via `SalesSessionTotalsProjectionListener`)                                                      |
| `features.stats` → `core.sales`      | Event                | `TicketPlacedEvent`                                                                                                   |
| `core.outlet` → `core.sales`         | Port impl            | `SalesTicketAdminPort` (via bridge)                                                                                   |

---

## Conclusion exécutive — Top 5 chantiers urgents

**1. Sécurité P0 — Endpoints `cancel` et `print*` sans `@Secured`**

`PATCH /tenant/tickets/{id}/cancel` et `GET /tenant/tickets/{id}/print[.pdf|.escpos]` n'ont aucune annotation `@Secured`. Tout principal authentifié (même sans rôle métier) peut annuler un ticket ou en imprimer un duplicata. Pour le cancel, des limites + autonomies sont évaluées côté handler, mais aucune barrière de rôle.

Fichier : `src/main/java/com/tchalanet/server/core/sales/infra/web/TicketController.java` lignes 207-215, 218-225, 230-235, 239-244.

**2. Idempotence settlement absente — re-publish casse le batch**

`RecordDrawTicketsResultCommandHandler` n'écrit ni ne consulte la table `ticket_settlement` (créée pour ce besoin par `V9__core_ticket.sql`). `DrawResultedEventListener` n'utilise pas `ProcessedEventPort`. Une re-application `DrawResultAppliedEvent` lève `IllegalStateException` (via `markResulted` qui exige `SOLD`) → la boucle de settlement s'interrompt sur le premier ticket déjà traité.

Fichiers : `src/main/java/com/tchalanet/server/core/sales/application/command/handler/RecordDrawTicketsResultCommandHandler.java` ; `src/main/java/com/tchalanet/server/core/sales/application/event/DrawResultedEventListener.java`.

**3. Override casse l'historique de paiement**

`Ticket.forceResult(payout, resultStatus, when)` réaffecte systématiquement `settlementStatus = UNSETTLED`. Un ticket déjà `SETTLED` (payout exécuté) ré-overridé revient à `UNSETTLED` sans trace ni garde, ouvrant la porte à un double payout par `core.payout`.

Fichier : `src/main/java/com/tchalanet/server/core/sales/domain/model/Ticket.java` lignes 307-338.

**4. Verify public expose des données sensibles + statut incohérent**

`VerifyPublicTicketQueryHandler.toVisibleResult` retourne :

- `ticketId` (UUID interne) en clair.
- `outletAddress.id` et `outletAddress.tenantId` non masqués (le `maskAddress` n'efface que `line1/line2/region/postal/normalizedKey`).
- `payoutStatus` calculé sur `potentialPayout` (avant tirage) au lieu de `winningAmount` réel : un ticket `LOST` peut afficher `POTENTIAL_WIN`.
- Catch `Exception ignored` autour du chargement outlet → erreurs masquées.

`PublicTicketController.verify` retourne `ResponseEntity<?>` raw au lieu d'`ApiResponse<T>`, et aucun rate-limiting n'est implémenté malgré le commentaire d'intention.

Fichiers : `src/main/java/com/tchalanet/server/core/sales/application/query/handler/VerifyPublicTicketQueryHandler.java` ; `src/main/java/com/tchalanet/server/core/sales/infra/web/PublicTicketController.java`.

**5. Violations d'isolation et incohérences de couches**

(a) `DrawResultViewPortJdbcAdapter` exécute un SQL `SELECT ... FROM draw_result JOIN result_slot ON ...` depuis `core.sales`, accédant directement aux tables de `core.drawresult` et `catalog.resultslot`.

(b) `JpaTicketRepositoryAdapter` (couche infra-persistence) consomme `DrawLookupPort`, `OutletReaderPort`, `SalesSessionReaderPort` pour assembler `TicketPrintView` — un adapter persistence ne devrait pas orchestrer 3 autres domaines.

(c) `SalesLedgerListener` utilise `@EventListener` (synchrone, dans la tx du producer) au lieu de `@TransactionalEventListener(AFTER_COMMIT)`, et appelle directement un port-in de `core.ledger` au lieu de passer par le bus. Le catch global `Exception` masque toute défaillance ledger.

(d) `SellTicketRequest` body contient `tenantId`, `sessionId`, `cashierId` — silencieusement écrasés (tenantId/cashierId) ou ignorés (sessionId) par `TicketWebMapper`. Surface API trompeuse.

(e) Doublons de modèle : `CancelSaleCommand` vs `CancelTicketCommand` ; `Approve/RejectTicketSaleCommand` vs `Approve/RejectPendingTicketSaleCommand` orphelins. `Ticket.settle/markAsPaid/markPayoutPaid` équivalents.

Fichiers : `src/main/java/com/tchalanet/server/core/sales/infra/persistence/adapter/DrawResultViewPortJdbcAdapter.java` ; `src/main/java/com/tchalanet/server/core/sales/infra/persistence/adapter/JpaTicketRepositoryAdapter.java` ; `src/main/java/com/tchalanet/server/core/sales/infra/event/SalesLedgerListener.java` ; `src/main/java/com/tchalanet/server/core/sales/infra/web/model/SellTicketRequest.java` ; `src/main/java/com/tchalanet/server/core/sales/application/command/model/`.
