## Context

Cet omnibus regroupe 14 anomalies P1/P2 issues de l'audit. Il complète les 3 P0 (`secure-sales-ticket-endpoints`, `harden-ticket-settlement-integrity`, `harden-public-ticket-verification`) qui sont volontairement isolés pour rester atomiques et review-ables séparément. Cet omnibus suit le pattern précédent de `fix-draw-pipeline-audit-gaps` (archivé 2026-04-26).

### Anomalie 1 — `DrawResultViewPortJdbcAdapter` SQL cross-domain

`core.sales.infra.persistence.adapter.DrawResultViewPortJdbcAdapter` exécute :

```sql
SELECT dr.id, rs.key, dr.occurred_at, dr.haiti_result, dr.source_result
FROM draw_result dr
JOIN result_slot rs ON rs.id = dr.result_slot_id
WHERE dr.id = ?
```

Ce SQL touche `draw_result` (table de `core.drawresult`) et `result_slot` (table de `catalog.resultslot`) depuis un adapter de `core.sales`. C'est une violation directe de la règle d'isolation (cf. `openspec/context/10-non-negotiables.md`).

### Anomalie 2 — `JpaTicketRepositoryAdapter` orchestre 3 domaines

L'adapter persistence consomme `DrawLookupPort`, `OutletReaderPort`, `PosSessionReaderPort` pour `getTicketPrintView`. Un adapter de persistence doit ne contenir que de la translation entité↔domain. L'orchestration appartient à la couche `application.service`.

### Anomalie 3 — `SalesLedgerListener` synchrone + port-in direct

```java
@EventListener  // ← devrait être @TransactionalEventListener(AFTER_COMMIT)
public void onTicketPlaced(TicketPlacedEvent event) {
  try {
    ledgerPort.recordTicketSale(...);  // ← port-in direct cross-domain
  } catch (Exception e) {
    log.error(...);  // ← exception silencieusement absorbée
  }
}
```

Trois problèmes en un. Le pattern attendu (cf. `openspec/context/25-idempotency.md` et autres listeners projet) :

1. `@TransactionalEventListener(AFTER_COMMIT)` pour découpler du producer
2. `CommandBus.send(...)` pour la cross-domain communication (pas de port-in direct)
3. Catch ciblé + propagation des exceptions inattendues

### Anomalies 4-6 — Doublons de modèle

| Doublon                                                         | Status                                                                                                  |
| --------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `CancelSaleCommand` vs `CancelTicketCommand`                    | `CancelTicketCommand` est mappé en `CancelSaleCommand` par `TicketWebMapper` — pure indirection inutile |
| `ApproveTicketSaleCommand` vs `ApprovePendingTicketSaleCommand` | Le second n'a pas de handler localisé — code orphelin                                                   |
| `RejectTicketSaleCommand` vs `RejectPendingTicketSaleCommand`   | idem                                                                                                    |
| `Ticket.settle` vs `markAsPaid` vs `markPayoutPaid`             | Les 3 méthodes font la même chose (`settlementStatus = SETTLED + touch`)                                |
| `Ticket.markPayoutPending`                                      | No-op (réécrit `UNSETTLED` sur `UNSETTLED`)                                                             |

### Anomalie 7 — `SellTicketRequest` body trompeur

```java
public record SellTicketRequest(
    @NotNull UUID tenantId,      // ← écrasé par contexte
    @NotNull UUID terminalId,
    UUID sessionId,              // ← jamais lu
    @NotNull UUID cashierId,     // ← écrasé par contexte
    @NotNull UUID drawId,
    ...
)
```

Un client peut croire qu'il envoie un `tenantId` choisi alors que le serveur l'écrase silencieusement. Faille de surface API.

### Anomalies 8-9 — Typed IDs ratés

`CancelTicketCommand.performedBy: UUID`, `OverrideTicketResultCommand.performedBy: UUID`, `TicketCancelledEvent.performedBy: UUID`, `TicketResultOverriddenEvent.performedBy: UUID` — tous devraient être `UserId` (typed wrapper, cf. `openspec/context/20-backend-rules.md` §typed-ids).

### Anomalie 10 — `TicketWritterPort` typo

Double 't'. Renommage trivial mais touche tous les consommateurs.

### Anomalie 11 — Timezone leak

```java
context = new LimitContext(..., java.time.ZoneId.systemDefault());
```

La timezone correcte est celle du `DrawChannel` (déjà chargé via `Draw.drawChannel().timezone()`). Hardcoder `systemDefault()` rend le calcul de fenêtres temporelles dépendant de la JVM host.

### Anomalie 12 — `LOTTO5_PATTERN` option 3 silent fail

```java
case 3 -> false; // MVP: option 3 not supported until rule is confirmed
```

Un ticket gagnant sur option 3 est silencieusement marqué LOST. Sans signal côté ops. Au minimum lever ; idéalement implémenter.

### Anomalie 13 — JPA ↔ migration discordance `publicCode`

```java
@Column(name = "public_code", length = 32)  // nullable = true par défaut
```

vs migration :

```sql
public_code varchar(32) NOT NULL
```

Un `null` en mémoire passerait la validation JPA → exception au flush.

### Anomalie 14 — `Locale.FRENCH` hardcodé + scheduler manquant

`getTicketPrintView` ignore le contexte locale. Et `ArchiveTicketsCommand` n'est jamais déclenché — handler orphelin.

### Anomalie 15 — Pas de retry sur collision codes

`TimeBasedTicketNumberGenerator` et `CrockfordPublicCodeGenerator` sont aléatoires. La probabilité de collision est faible mais non nulle. Sur conflit DB, l'exception remonte au cashier sans recovery.

## Goals / Non-Goals

**Goals:**

- Supprimer toute violation de couche/domaine pointée par l'audit
- Unifier les commandes/events/méthodes en doublon
- Activer le scheduler d'archivage (avec ShedLock)
- Aligner JPA et migrations sur les invariants
- Externaliser la timezone et la locale
- Catch propre + observabilité dans les listeners

**Non-Goals:**

- Refonte du modèle de pricing (`catalog.pricing`)
- Implémentation `LOTTO5` option 3 (besoin clarification produit — fallback : lever explicite)
- Workflow `ReverseTicketSettlement` (change séparé)
- Migration `@Secured` → `@PreAuthorize` (cohérence projet — change séparé)

## Decisions

### D1 — `DrawResultProjection` dans `core.drawresult.api`

Trois options :

1. Créer un port + projection dédié dans `core.drawresult.api`
2. Réutiliser un port existant si présent
3. Conserver le SQL JOIN mais l'isoler dans un repo dédié

**Décision** : option 1. `core.drawresult.api.DrawResultProjectionCatalog` exposé comme contract public ; `DrawResultProjection` record contient exactement ce dont sales a besoin (lot1..3, pick3, twoDigits, slotKey, occurredAt). Implémentation côté `core.drawresult.internal` peut faire le JOIN avec `result_slot` car les deux tables appartiennent au même domaine étendu (`core.drawresult` consomme `catalog.resultslot.api`).

### D2 — `TicketPrintViewAssembler` dans `application.service`

Trois options :

1. Service applicatif dédié
2. Use case query handler dédié
3. Garder dans l'adapter (statu quo)

**Décision** : option 1. Le service est appelé par `getTicketPrintView` qui est lui-même un read-side helper, pas un use case CQRS. Service applicatif aligne avec le pattern (`TicketSalePolicy`, `TicketLinePreparationService`).

### D3 — `Ticket.settle` API unifiée

Garder `settle(when)` : transition `UNSETTLED → SETTLED` avec garde `resultStatus != NOT_RESULTED`. Supprimer `markAsPaid`, `markPayoutPaid`, `markPayoutPending`.

`core.payout.MarkTicketPayoutPaidCommandHandler` adapte → appelle `ticket.settle(now)`.

### D4 — Scheduler archive + retention

Cron quotidien `0 0 3 * * *` UTC. Retention default 90 jours (configurable via `tch.sales.archive.retention-days`). `@SchedulerLock(name = "ticket-archive", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")`.

Pas de scheduler pour `ExpireTickets` car la commande est supprimée (code mort).

### D5 — Retry collision codes

Stratégie : retry up to 3 attempts. Au-delà, lever `TicketCodeGenerationException` mappée par `ProblemRest` en `503 Service Unavailable` (le client peut retry).

Implémentation dans `JpaTicketRepositoryAdapter.save` :

```java
for (int i = 0; i < MAX_RETRIES; i++) {
  try { return save(...); }
  catch (DataIntegrityViolationException e) {
    if (!isCodeCollision(e)) throw e;
    ticket = ticket.withRegeneratedCodes(numberGen, codeGen);
  }
}
throw new TicketCodeGenerationException(...);
```

`Ticket.withRegeneratedCodes(...)` : nouvelle méthode domain qui retourne une copie avec nouveaux `ticketCode`/`publicCode`. Les anciens codes ne sont jamais persistés (rollback transactionnel).

### D6 — `LOTTO5_PATTERN` option 3 fallback

Décision MVP : lever `UnsupportedOperationException("LOTTO5_PATTERN option 3 not yet implemented — see PRODUCT-XXX")` dans `TicketWinningCalculator.lotto5`. Mieux qu'un silent `false`. Le batch settlement remonte cette exception → ticket non traité (skip + log ERROR), batch continue grâce au mécanisme idempotence du change `harden-ticket-settlement-integrity`.

### D7 — `SalesLedgerListener` migration

Trois options :

1. Garder le pattern listener mais corriger (`@TransactionalEventListener(AFTER_COMMIT)`)
2. Migrer entièrement vers `CommandBus.send` (un nouveau handler `RecordTicketSaleLedgerCommandHandler` côté `core.ledger`)
3. Garder le port-in mais ajouter `AFTER_COMMIT`

**Décision** : option 2. Aligne avec le pattern projet (cross-domain via bus). `core.ledger.application.command.RecordTicketSaleLedgerCommand(tenantId, ticketId, stakeCents, occurredAt)` ; handler dans `core.ledger`. `SalesLedgerListener` devient ultra-fin :

```java
@TransactionalEventListener(AFTER_COMMIT)
public void onTicketPlaced(TicketPlacedEvent event) {
  commandBus.send(new RecordTicketSaleLedgerCommand(
      event.tenantId(), event.ticketId(), event.stakeCents(), event.occurredAt()));
}
```

Plus de catch global ; les exceptions remontent et sont gérées par le bus.

### D8 — Typed `UserId` partout

Migration linéaire. Tous les `UUID performedBy` → `UserId performedBy`. Adapter web mapper. Adapter sérialisation event si schéma JSON change (vérifier `EventId.of(UUID)` style — le pattern projet est de sérialiser le UUID via `UserId.value().toString()`).

### D9 — ArchUnit `SalesIsolationArchTest`

Test bloquant : aucun fichier dans `core.sales.infra.persistence.adapter.*` ne doit contenir de string SQL référençant des tables hors `{ticket, ticket_line, ticket_settlement, pricing_odds}`. Détection par regex sur le code source (ArchUnit + scan @Query / String literals contenant `FROM <table>`).

## Risks / Trade-offs

- **[Risque] Renommage `TicketWritterPort` cassant** : tous les consommateurs internes doivent être adaptés. → Mitigation : refactor IDE avec preview ; tests compilation immédiate.
- **[Risque] Suppression `markAsPaid` etc. casse `core.payout`** : `MarkTicketPayoutPaidCommandHandler` appelle `ticket.markPayoutPaid()`. → Adapter dans le même change ; tests d'intégration `core.payout` à valider.
- **[Risque] `SellTicketRequest` body breaking** : POS Flutter peut envoyer `tenantId` etc. → CHANGELOG explicite ; coordination mobile + accept compat (les champs ignorés en lecture, on les laisse pour 1 release puis on les supprime). **Décision** : suppression directe (les champs étaient écrasés, donc inutilisés en pratique).
- **[Risque] Suppression `DrawResultViewPortJdbcAdapter`** : régression performance si nouveau path ajoute latence. → Mesurer ; le JOIN était simple, l'appel via port reste un `findById` indexé.
- **[Risque] Locale dynamique impacte existing prints** : `getTicketPrintView` actuellement FR par défaut ; si `Accept-Language` non envoyé → fallback FR. **Décision** : default FR conservé, override possible.
- **[Risque] `LOTTO5` option 3 lève au lieu de silencieusement LOST** : si des tickets en prod utilisent option 3 (à vérifier), le batch settlement les skip et nécessite intervention manuelle. → Mesurer la volumétrie en prod avant déploiement ; si > 0, repousser jusqu'à clarification produit.
- **[Trade-off] Omnibus large** : 14 anomalies dans un seul change = review longue. → Acceptable pour un change "audit-gaps" (précédent draw l'a fait avec succès) ; alternative : splitter en micro-changes mais coût coordination élevé.

## Migration Plan

1. Créer `DrawResultProjection` + `DrawResultProjectionCatalog` côté `core.drawresult.api` + impl
2. Migrer `core.sales` vers le nouveau port ; supprimer `DrawResultViewPortJdbcAdapter`
3. Extraire `TicketPrintViewAssembler` ; refactor `JpaTicketRepositoryAdapter` (suppression dépendances cross-domain)
4. Renommer `TicketWritterPort` → `TicketWriterPort`
5. Unifier les commands : supprimer `CancelTicketCommand`, `ApprovePending*`, `RejectPending*`, `ExpireTicketsCommand`
6. Unifier `Ticket.settle` ; adapter `core.payout`
7. Supprimer `Ticket.updateSettlementStatus` ; vérifier 0 consommateur
8. Suppression champs `tenantId/sessionId/cashierId` de `SellTicketRequest`
9. Migration `UUID performedBy` → `UserId` (cancel + override + events)
10. Refondre `SalesLedgerListener` (AFTER_COMMIT + CommandBus + nouveau handler `core.ledger`)
11. Externaliser timezone + locale
12. Activer `TicketArchiveScheduler` avec ShedLock
13. Implémenter retry collision codes
14. JPA `publicCode` `nullable = false`
15. `LOTTO5` option 3 lève
16. Tests + ArchUnit + docs

Rollback : possible par micro-revert pour chaque sous-bloc ; les modifications sont indépendantes.

## Open Questions

- Q1 : Combien de tickets en prod utilisent `LOTTO5_PATTERN` option 3 ? (impact décision D6)
- Q2 : `Ticket.updateSettlementStatus` est-il consommé ailleurs ? (`grep` complet à faire en début d'implémentation)
- Q3 : `core.payout` est-il actif ou stub ? (impact migration `markPayoutPaid → settle`)
- Q4 : Faut-il un endpoint admin pour déclencher manuellement `ArchiveTicketsCommand` (en plus du scheduler) ? (probable yes pour ops)
