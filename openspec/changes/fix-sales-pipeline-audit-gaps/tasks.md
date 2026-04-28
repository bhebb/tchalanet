## Status: DRAFT

## 1. Cross-domain SQL leak — `DrawResultProjectionCatalog`

- [ ] 1.1 Créer `core.drawresult.api.DrawResultProjection` (record `id, slotKey, occurredAt, lot1, lot2, lot3, pick3, twoDigits[]`)
- [ ] 1.2 Créer `core.drawresult.api.DrawResultProjectionCatalog` (interface, `findById(DrawResultId)`)
- [ ] 1.3 Implémenter `core.drawresult.internal.read.DrawResultProjectionCatalogImpl` (réutilise `DrawResultJdbcReaderAdapter` ou `ResultSlotCatalog` pour assembler — JOIN reste interne au domaine étendu drawresult+catalog.resultslot)
- [ ] 1.4 Modifier `core.sales.application.port.out.DrawResultViewPort` :
  - Soit supprimer si `DrawResultProjectionCatalog` peut être consommé directement
  - Soit garder comme wrapper qui délègue à `DrawResultProjectionCatalog`
- [ ] 1.5 Supprimer `core.sales.infra.persistence.adapter.DrawResultViewPortJdbcAdapter`
- [ ] 1.6 Adapter `RecordDrawTicketsResultCommandHandler` (le seul consommateur) pour le nouveau type
- [ ] 1.7 Tests unitaires `DrawResultProjectionCatalogImplTest`

## 2. Persistence layer pollution — `TicketPrintViewAssembler`

- [ ] 2.1 Créer `core.sales.application.service.TicketPrintViewAssembler` qui prend `(Ticket, Locale)` et orchestre les lookups `DrawLookupPort`, `OutletReaderPort`, `PosSessionReaderPort`
- [ ] 2.2 Migrer `getTicketPrintView` depuis `JpaTicketRepositoryAdapter` vers `TicketPrintViewAssembler`
- [ ] 2.3 Supprimer les dépendances `DrawLookupPort`, `OutletReaderPort`, `PosSessionReaderPort` du `JpaTicketRepositoryAdapter`
- [ ] 2.4 `TicketReaderPort.getTicketPrintView` migre dans un nouveau port `TicketPrintViewPort` ou directement appelé depuis le query handler
- [ ] 2.5 Tests unitaires `TicketPrintViewAssemblerTest`

## 3. `SalesLedgerListener` refactor

- [ ] 3.1 Créer `core.ledger.application.command.model.RecordTicketSaleLedgerCommand(tenantId, ticketId, stakeCents, occurredAt)` (`Command<Void>`)
- [ ] 3.2 Créer `core.ledger.application.command.handler.RecordTicketSaleLedgerCommandHandler` (déplace la logique de `recordTicketSale`)
- [ ] 3.3 Supprimer `core.ledger.application.port.in.RecordLedgerFromSalesPort`
- [ ] 3.4 Refondre `SalesLedgerListener` : `@TransactionalEventListener(AFTER_COMMIT)` + `commandBus.send(new RecordTicketSaleLedgerCommand(...))` ; pas de catch
- [ ] 3.5 Tests `SalesLedgerListenerTest` (mock `CommandBus`) + `RecordTicketSaleLedgerCommandHandlerTest`

## 4. Cancel command unification

- [ ] 4.1 Supprimer `core.sales.application.command.model.CancelTicketCommand`
- [ ] 4.2 `TicketWebMapper.toCancelTicketCommand` → `TicketWebMapper.toCancelSaleCommand` qui retourne directement `CancelSaleCommand`
- [ ] 4.3 Adapter `TicketController.cancel` si signature change
- [ ] 4.4 Renommer `CancelTicketRequest` → `CancelSaleRequest` (ou conserver — décision en review)
- [ ] 4.5 Vérifier qu'aucun autre consommateur n'utilise `CancelTicketCommand`

## 5. Approve/Reject orphans cleanup

- [ ] 5.1 Supprimer `ApprovePendingTicketSaleCommand`
- [ ] 5.2 Supprimer `RejectPendingTicketSaleCommand`
- [ ] 5.3 Vérifier 0 consommateur

## 6. Dead code — `ExpireTicketsCommand`

- [ ] 6.1 Supprimer `core.sales.application.command.model.ExpireTicketsCommand`
- [ ] 6.2 Vérifier 0 consommateur

## 7. `Ticket` API cleanup

- [ ] 7.1 Garder uniquement `Ticket.settle(Instant when)` ; supprimer `markAsPaid`, `markPayoutPaid`, `markPayoutPending`
- [ ] 7.2 Adapter `core.payout.MarkTicketPayoutPaidCommandHandler` → appel `ticket.settle(now)`
- [ ] 7.3 Vérifier 0 autre consommateur des méthodes supprimées
- [ ] 7.4 Supprimer `Ticket.updateSettlementStatus(...)` ; vérifier 0 consommateur (`grep` cross-projet)
- [ ] 7.5 Supprimer la méthode `Ticket.totalPayout()` ou la renommer `Ticket.winningAmountOrZero()` (clarification du nom)
- [ ] 7.6 Supprimer les accesseurs `id() / tenantId() / terminalId() / drawId()` redondants avec les getters Lombok (audit consommateurs avant)

## 8. `SellTicketRequest` body cleanup

- [ ] 8.1 Supprimer `tenantId`, `sessionId`, `cashierId` de `SellTicketRequest`
- [ ] 8.2 Adapter `TicketWebMapper.toSellCommand` (continue de tirer du contexte)
- [ ] 8.3 Adapter les tests existants
- [ ] 8.4 CHANGELOG : `BREAKING (tenant API)`

## 9. Typed IDs `performedBy`

- [ ] 9.1 `CancelSaleCommand.performedBy: UserId` (vérifier — peut déjà être typé)
- [ ] 9.2 `OverrideTicketResultCommand.performedBy: UUID` → `UserId`
- [ ] 9.3 `TicketCancelledEvent.performedBy: UUID` → `UserId`
- [ ] 9.4 `TicketResultOverriddenEvent.performedBy: UUID` → `UserId`
- [ ] 9.5 Adapter les request web (`OverrideTicketResultRequest`, `CancelTicketRequest`)
- [ ] 9.6 Vérifier que la sérialisation JSON reste compatible (ou breaking si schéma diffère)

## 10. Renommage `TicketWritterPort` → `TicketWriterPort`

- [ ] 10.1 Renommer le fichier + interface
- [ ] 10.2 Refactor IDE pour adapter tous les consommateurs
- [ ] 10.3 Vérifier compilation

## 11. Suppression `TicketEventPublisherPort`

- [ ] 11.1 Vérifier 0 consommateur (publication via `DomainEventPublisher` partout)
- [ ] 11.2 Supprimer le port

## 12. Timezone leak

- [ ] 12.1 `TicketSalePolicy.evaluateLimitsAndAutonomy` — récupérer `draw.drawChannel().timezone()` et passer dans `LimitContext`
- [ ] 12.2 `CancelSaleCommandHandler.evaluateCancelLimits` — idem (charger le draw via `DrawLookupPort` si pas déjà disponible)
- [ ] 12.3 Aucun `ZoneId.systemDefault()` ne doit subsister dans `core.sales` (ArchUnit possible)

## 13. `LOTTO5_PATTERN` option 3

- [ ] 13.1 Mesurer en prod si des tickets utilisent option 3 (avant déploiement)
- [ ] 13.2 `TicketWinningCalculator.lotto5` case 3 → `throw new UnsupportedOperationException("LOTTO5_PATTERN option 3 not yet implemented")`
- [ ] 13.3 Tests `TicketWinningCalculatorTest` mis à jour
- [ ] 13.4 Le batch settlement (avec idempotence du change `harden-ticket-settlement-integrity`) skip le ticket et log ERROR — vérifier le comportement

## 14. JPA ↔ migration sync `publicCode`

- [ ] 14.1 `TicketEntity.publicCode` annoter `nullable = false` :
  ```java
  @Column(name = "public_code", length = 32, nullable = false)
  private String publicCode;
  ```
- [ ] 14.2 Aligner `Ticket` agrégat (`publicCode` ne doit plus être null)
- [ ] 14.3 Vérifier les factories `Ticket.sell/pendingApproval` génèrent toujours un publicCode

## 15. Locale dynamique sur print

- [ ] 15.1 `TicketPrintViewAssembler` (ou son successeur) reçoit `Locale` en paramètre
- [ ] 15.2 Le query handler de print extrait `Locale` depuis le contexte web (`Accept-Language` header)
- [ ] 15.3 Default `Locale.FRENCH` si non fourni
- [ ] 15.4 Tests unitaires (FR + EN minimum)

## 16. Scheduler archive

- [ ] 16.1 Créer `core.sales.config.TicketArchiveProperties` (`@ConfigurationProperties("tch.sales.archive")` : `enabled, retentionDays, cron`)
- [ ] 16.2 Créer `core.sales.infra.scheduler.TicketArchiveScheduler` :
  - `@Scheduled(cron = "${tch.sales.archive.cron:0 0 3 * * *}", zone = "UTC")`
  - `@SchedulerLock(name = "ticket-archive", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")`
  - Calcule `cutoff = now - retentionDays`
  - Envoie `ArchiveTicketsCommand`
- [ ] 16.3 Configuration `application.yaml` (defaults documentés)
- [ ] 16.4 Tests unitaires + intégration (vérifier ShedLock)

## 17. Retry collision codes

- [ ] 17.1 Créer `core.sales.application.command.model.TicketCodeGenerationException`
- [ ] 17.2 Mapper l'exception en `503 Service Unavailable` côté `ProblemRest`
- [ ] 17.3 `Ticket.withRegeneratedCodes(numberGen, codeGen)` méthode domain qui retourne une copie avec nouveaux codes
- [ ] 17.4 `JpaTicketRepositoryAdapter.save` boucle retry max 3 sur `DataIntegrityViolationException` (filtrée sur les contraintes `uq_ticket_tenant_code` et `uq_ticket_public_code`)
- [ ] 17.5 Au-delà de 3 retries → lever `TicketCodeGenerationException`
- [ ] 17.6 Tests intégration `TicketSaveCollisionRetryIT` (Testcontainers, force collision, vérifier retry)

## 18. ArchUnit `SalesIsolationArchTest`

- [ ] 18.1 Créer `src/test/java/com/tchalanet/server/arch/SalesIsolationArchTest.java`
- [ ] 18.2 Règle : aucune classe dans `core.sales.infra.persistence.adapter.*` ne doit contenir de string littérale matchant `(?i)from\s+(draw_result|result_slot|draw|draw_channel|outlet|terminal|address|...)`
- [ ] 18.3 Vérifier `./mvnw test -Dtest=SalesIsolationArchTest` passe

## 19. Documentation

- [ ] 19.1 `tchalanet-server/src/main/java/com/tchalanet/server/core/sales/DOMAIN_SALES.md` :
  - §3 (use cases) — supprimer les commands obsolètes
  - §4 (ports) — renommer `TicketWriterPort`, supprimer `TicketEventPublisherPort`
  - §6 (events) — `UserId` typé sur `performedBy`
  - §7 (intégrations) — `core.ledger` via command, `DrawResultProjectionCatalog`
  - §8 (notes techniques) — scheduler archive ; retry collision ; locale dynamique
  - §9 — retirer toutes les anomalies P1/P2 traitées
- [ ] 19.2 `tchalanet-docs/docs/02-functional/flows/sell-ticket.md` — supprimer les annotations "⚠ écrasé/ignoré" sur `tenantId/sessionId/cashierId`
- [ ] 19.3 `docs/conventions/inter_domain_calls.md` — règle "no cross-domain SQL JOIN in adapters" + référence ArchUnit `SalesIsolationArchTest`
- [ ] 19.4 `docs/decisions/sales-pipeline-decisions.md` — décisions D1-D9
- [ ] 19.5 Mettre à jour `openspec/specs/sales-ticket-lifecycle/spec.md` et `openspec/specs/sales-event-publishing/spec.md` (créés par ce change)

## 20. Vérification finale

- [ ] 20.1 `./mvnw clean verify` → build vert + tous tests
- [ ] 20.2 Mesurer la perf settlement avant/après (changement `DrawResultViewPort`)
- [ ] 20.3 Coordonner mobile (POS) + web (admin) pour les BREAKING changes (`SellTicketRequest`, `OverrideTicketResultRequest`, etc.)
- [ ] 20.4 CHANGELOG complet
- [ ] 20.5 Vérifier l'inventaire d'audit `2026-04-26-sales-pipeline-audit.md` §H — toutes les anomalies P1/P2 doivent être marquées résolues
