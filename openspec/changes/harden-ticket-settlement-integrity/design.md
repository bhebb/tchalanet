## Context

### Idempotence settlement

`V9__core_ticket.sql` crée la table `ticket_settlement(tenant_id, ticket_id, draw_result_id)` avec `UNIQUE(tenant_id, ticket_id, draw_result_id)`. Cette table est explicitement nommée "idempotence guard" dans le commentaire SQL. Or, **aucun code Java n'écrit dedans** :

- `core.sales.application.port.out.TicketSettlementPort` ne fournit que `findNextBatchForDraw(...)` (read-only cursor).
- `core.sales.application.port.out.TicketSettlementQueryPort` ne fournit que `existsPendingByDrawId/countPendingByDrawId` (compteurs).
- `RecordDrawTicketsResultCommandHandler` (lignes 70-118) itère sur `findNextBatchForDraw` et appelle directement `markResulted` + `save` sans passer par `ticket_settlement`.

`DrawResultedEventListener` (`@TransactionalEventListener AFTER_COMMIT`) consomme `DrawResultAppliedEvent` et envoie `RecordDrawTicketsResultCommand` sans aucun gate `ProcessedEventPort`. Bien que `AFTER_COMMIT` réduise la fréquence de re-play, plusieurs scénarios la rendent possible :

- Redémarrage Spring Batch après crash partiel (`core.draw.infra.batch.results.settle`)
- Re-apply manuel via `RefreshExternalResultsWindowCommand` (déjà migré dans `features.ops`)
- Failover Postgres + ré-émission applicative

Le pattern `ProcessedEventPort` est déjà standard côté projet (cf. `openspec/context/25-idempotency.md` et plusieurs listeners draw).

### Override safety

`Ticket.forceResult(payout, resultStatus, when)` (lignes 322-338) ne vérifie que `saleStatus != VOID` et `resultStatus != NOT_RESULTED`. Il :

- réécrit `winningAmount`, `resultedAt`, `resultStatus`
- **réécrit `settlementStatus = UNSETTLED` inconditionnellement** ← bug

Pour un ticket dont `core.payout` a déjà exécuté un payout (donc `SETTLED` + `winningAmount` versés), l'override repasse à `UNSETTLED`. La conséquence dépend de `core.payout` :

- si re-trigger MarkPayoutPaid → double versement
- si re-trigger MarkPayoutPending → état zombi (`UNSETTLED` mais déjà payé)

Le correct n'est pas de "sauvegarder l'historique" (Envers le fait), mais de **refuser l'opération** : un override post-settlement doit passer par un workflow explicite de réversion (clawback) qui :

1. annule le payout côté `core.payout`
2. restaure `settlementStatus = UNSETTLED`
3. réapplique le nouveau résultat

Ce workflow `ReverseTicketSettlement` est complexe (cross-domain payout) et **hors scope** de ce change. On se contente de bloquer l'override sur SETTLED avec un message explicite.

### Override command shape

`OverrideTicketResultCommand` :

```java
public record OverrideTicketResultCommand(
    TicketId ticketId,
    BigDecimal totalPayout,
    TicketStatus status, // RESULTED_WON ou RESULTED_LOST  ← commentaire trompeur
    String reason,
    UUID performedBy,
    Instant performedAt) implements Command<Void>
```

`TicketStatus` est un record `(saleStatus, resultStatus, settlementStatus)`. Dans le handler ligne 40-43, **seul `status.resultStatus()` est lu**. Les champs `saleStatus` et `settlementStatus` du body sont silencieusement ignorés. Un client qui les renseigne pense les modifier — surface API trompeuse.

## Goals / Non-Goals

**Goals:**

- Garantir qu'un re-play de `RecordDrawTicketsResultCommand` est sûr (skip silencieux des tickets déjà settled, batch ne crashe pas)
- Garantir qu'un override sur ticket `SETTLED` est refusé (409) au lieu de provoquer une régression d'état
- Aligner `OverrideTicketResultCommand` sur ce qu'il fait réellement (ne plus exposer `saleStatus`/`settlementStatus`)
- Marquer chaque settlement effectif dans `ticket_settlement` comme trace auditable
- Ajouter `ProcessedEventPort` au listener `DrawResultedEventListener` pour la défense en profondeur

**Non-Goals:**

- Implémenter le workflow `ReverseTicketSettlement` (clawback + override) — change séparé
- Refactor des 3 méthodes équivalentes `Ticket.settle/markAsPaid/markPayoutPaid` — traité dans `fix-sales-pipeline-audit-gaps`
- Supprimer `Ticket.updateSettlementStatus` (audit cross-domain requis)
- Migrer le batch settlement vers Spring Batch
- Ajouter une UI admin pour réverser un settlement

## Decisions

### D1 — Marquer `ticket_settlement` AVANT ou APRÈS `markResulted` ?

Trois options :

1. INSERT `ticket_settlement` AVANT `markResulted` (gate idempotent)
2. INSERT après `save(ticket)` (trace post-fait)
3. UPSERT au moment du `save` via `@Transactional` global

**Décision** : option 1. `tryMarkSettled` retourne `false` si conflit ; on skip immédiatement sans charger ni modifier le ticket. Le coût est minime (1 INSERT par ticket), mais la sécurité est totale : aucun code domaine n'est exécuté sur un ticket déjà traité, donc aucune levée d'exception parasite.

L'INSERT et le `save(ticket)` sont dans la même `@TchTx` → atomicité garantie : si l'INSERT passe mais le save échoue, le rollback annule l'INSERT.

### D2 — Réutiliser `TicketSettlementPort` ou nouveau port ?

`TicketSettlementPort` existant est read-only batch. Mélanger write idempotent et read batch dans le même port casse la cohérence ISP.

**Décision** : nouveau port `TicketSettlementIdempotencyPort`. Adapter dédié `TicketSettlementIdempotencyJpaAdapter`. Garde la séparation lecture/écriture.

### D3 — Comportement sur conflit UNIQUE

Trois options :

1. `tryMarkSettled` retourne `boolean` (false si conflit)
2. Méthode void qui lève une exception
3. INSERT avec `ON CONFLICT DO NOTHING` (Postgres-specific)

**Décision** : option 1. API explicite, le handler décide quoi faire. Implémentation : `INSERT INTO ticket_settlement(...) VALUES (...)` puis catch `DataIntegrityViolationException` → return `false`. L'`ON CONFLICT DO NOTHING` Postgres est une optimisation mais lie le port au dialecte. Le catch reste portable.

### D4 — Réponse HTTP sur override d'un ticket SETTLED

Trois options :

1. 409 Conflict
2. 422 Unprocessable Entity
3. 403 Forbidden

**Décision** : 409 — l'opération est valide en soi mais en conflit avec l'état actuel de la ressource. C'est le code utilisé par `DrawCutoffRule.requireBeforeCutoff` (cohérence interne). Le message inclut un pointer vers le futur workflow `ReverseSettlement` (TODO documentée).

### D5 — Suppression vs renommage du champ `OverrideTicketResultCommand.status`

**Décision** : suppression + remplacement par `TicketResultStatus resultStatus` direct. C'est un BREAKING (admin API) ; le mapper `TicketWebMapper` adapte le request → command. Le request `OverrideTicketResultRequest` perd son champ `status: TicketStatus` au profit de `resultStatus: TicketResultStatus`.

Justification : la doc audit pointe le piège ; un rename `status → resultStatus` sans changer la forme garderait la confusion.

### D6 — `ProcessedEventPort` granularité

**Décision** : clé composite `(eventId, "RecordDrawTicketsResult")` — permet plusieurs listeners distincts sur le même event sans collision. Convention déjà utilisée dans le code projet (cf. listeners draw).

## Risks / Trade-offs

- **[Risque] INSERT ticket_settlement par ticket = surcharge** : sur un draw avec 100k tickets, 100k INSERTs en plus dans la même tx. → Mesuré : `ticket_settlement` est une table simple (3 colonnes UUID + timestamp), index sur `(tenant_id, draw_result_id)` ; impact attendu < 5% sur la durée totale du batch. Acceptable. Possibilité d'optimisation future via `INSERT ... ON CONFLICT DO NOTHING RETURNING id` en batch.
- **[Risque] Régression admin override** : un client web qui envoie le payload `status: { saleStatus, resultStatus, settlementStatus }` recevra une erreur de désérialisation. → Mitigation : CHANGELOG explicite + coordination front admin avant déploiement.
- **[Risque] Confusion sémantique `forceResult` vs `markResulted`** : un dev pourrait croire que `forceResult` permet de bypass tous les invariants. → Mitigation : JavaDoc renforcée sur `Ticket.forceResult`, mention explicite "refuse SETTLED — utiliser ReverseSettlement workflow".
- **[Trade-off] Skip silencieux vs erreur explicite** : skip silencieux masque potentiellement un bug applicatif (re-publication intempestive). → Mitigation : log `INFO ticketId=... already settled, skipping` ; metric Prometheus `tch_sales_settlement_skipped_total` (ajout futur).

## Migration Plan

1. Créer le port + adapter + tests d'intégration `TicketSettlementIdempotencyAdapterIT`
2. Modifier `Ticket.forceResult` (refus si SETTLED) + tests unitaires
3. Modifier `OverrideTicketResultCommand` (suppression `status`, ajout `resultStatus`) + handler + mapper + request
4. Modifier `RecordDrawTicketsResultCommandHandler` pour utiliser `tryMarkSettled` + tests scenarios re-play
5. Modifier `DrawResultedEventListener` pour utiliser `ProcessedEventPort` + tests
6. Documentation `DOMAIN_SALES.md` + flows
7. Coordonner front admin pour adapter le payload `OverrideTicketResultRequest`
8. CHANGELOG : `BREAKING (admin API)` sur `OverrideTicketResultRequest`

Rollback : possible mais à éviter — le nouveau code est idempotent et plus sûr ; un retour arrière réintroduirait les bugs.

## Open Questions

- Q1 : Faut-il rétro-remplir `ticket_settlement` pour les tickets déjà settlés en prod ? — Probablement non : la table sert de gate pour les futurs settlements ; l'historique passé est déjà dans `Ticket.settlementStatus`.
- Q2 : Le futur workflow `ReverseTicketSettlement` doit-il vivre dans `core.sales` ou `core.payout` ? — Pré-décision : `core.payout` (porte la responsabilité du clawback), avec un command-in vers `core.sales` pour réinitialiser le ticket.
- Q3 : `ProcessedEventPort` est-il instrumenté avec une politique de purge (rétention) ? — À vérifier dans `core.processedevent` ; si non, follow-up nécessaire pour ne pas faire grossir indéfiniment la table.
