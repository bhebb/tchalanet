## Status: DRAFT

## 1. Port idempotence + adapter

- [ ] 1.1 Créer `core.sales.application.port.out.TicketSettlementIdempotencyPort` avec deux méthodes : `boolean tryMarkSettled(TenantId, TicketId, DrawResultId)` et `boolean isAlreadySettled(TenantId, TicketId, DrawResultId)`
- [ ] 1.2 Créer `core.sales.infra.persistence.adapter.TicketSettlementIdempotencyJpaAdapter` (`@Component`) qui INSERT dans `ticket_settlement` ; catch `DataIntegrityViolationException` → return `false`
- [ ] 1.3 Créer `core.sales.infra.persistence.repository.TicketSettlementIdempotencyJpaRepository` (Spring Data) ou exécuter via `JdbcTemplate` selon convention projet
- [ ] 1.4 Tests intégration `TicketSettlementIdempotencyAdapterIT` (Testcontainers Postgres) :
  - INSERT initial → `true`
  - INSERT identique → `false` (conflit UNIQUE)
  - `isAlreadySettled` retourne cohérent

## 2. Domain — `Ticket.forceResult` refuse SETTLED

- [ ] 2.1 `Ticket.forceResult(BigDecimal payout, Instant when)` (overload 1 ligne 307) — ajouter guard `if (settlementStatus == SETTLED) throw new IllegalStateException("Cannot override result of an already SETTLED ticket")` au tout début
- [ ] 2.2 `Ticket.forceResult(BigDecimal payout, TicketResultStatus, Instant)` (overload 2 ligne 322) — ajouter le même guard
- [ ] 2.3 Mettre à jour la JavaDoc des deux overloads (mention explicite : "Refuses if ticket is SETTLED. Use ReverseSettlement workflow for clawback.")
- [ ] 2.4 Tests unitaires `TicketForceResultTest` :
  - SETTLED → `IllegalStateException`
  - UNSETTLED + WON → succès
  - UNSETTLED + LOST → succès
  - VOID → `IllegalStateException` (comportement existant conservé)

## 3. Override command — suppression `status: TicketStatus`

- [ ] 3.1 `OverrideTicketResultCommand` — remplacer le champ `TicketStatus status` par `TicketResultStatus resultStatus`
- [ ] 3.2 `OverrideTicketResultRequest` (web) — remplacer le champ `status: TicketStatus` par `resultStatus: TicketResultStatus` ; supprimer les imports inutilisés
- [ ] 3.3 `TicketController.overrideResult` — adapter la construction de la commande (suppression des `request.status() == null ? null : ...`)
- [ ] 3.4 Tests `OverrideTicketResultCommandTest` — vérifier la nouvelle forme

## 4. Handler override — guard SETTLED + 409

- [ ] 4.1 `OverrideTicketResultCommandHandler.handle` — après le chargement du ticket, ajouter `if (ticket.getSettlementStatus() == TicketSettlementStatus.SETTLED) throw ProblemRest.conflict("Cannot override result of an already SETTLED ticket. Use ReverseSettlement workflow.")`
- [ ] 4.2 Conserver le check existant `resultStatus ∈ {WON, LOST}`
- [ ] 4.3 Tests `OverrideTicketResultCommandHandlerTest` :
  - Ticket UNSETTLED + WON → succès + `TicketResultOverriddenEvent` publié
  - Ticket SETTLED → 409 conflict, aucun event publié
  - Ticket VOID → erreur (chemin existant inchangé)

## 5. Handler settlement — utilisation `ticket_settlement`

- [ ] 5.1 `RecordDrawTicketsResultCommandHandler` — injecter `TicketSettlementIdempotencyPort` via constructor
- [ ] 5.2 Dans la boucle (ligne 82+), avant `markResulted`, appeler `tryMarkSettled(saved.getTenantId(), ticket.getId(), cmd.drawResultId())` ; si `false` → `skipped++` + `continue`
- [ ] 5.3 Étendre `RecordDrawTicketsResultResult` avec un champ `long skipped` (en plus de `processed, won, lost`) — ou logger sans modifier la signature si tracking PR breaking
- [ ] 5.4 Mettre à jour le log de fin : `End ticket settlement ... processed={} won={} lost={} skipped={}`
- [ ] 5.5 Tests `RecordDrawTicketsResultCommandHandlerTest` :
  - Scenario "premier run, 3 tickets" → `processed=3, won=2, lost=1, skipped=0`
  - Scenario "re-play, 3 tickets dont 2 déjà dans `ticket_settlement`" → `processed=1, skipped=2, batch ne crashe pas`
  - Scenario "re-play, ticket déjà WON+ ticket*settlement vide" → cas dégradé : `tryMarkSettled` réussit puis `markResulted` lève (SOLD requis) ; valider le comportement attendu (rollback ? log error ?) — \_décision de design à prendre dans la review*

## 6. Listener — `ProcessedEventPort` sur `DrawResultedEventListener`

- [ ] 6.1 Injecter `ProcessedEventPort` dans `DrawResultedEventListener`
- [ ] 6.2 Construire la clé `(eventId, "RecordDrawTicketsResult")` au début du handler
- [ ] 6.3 Si `processedEvent.isProcessed(key)` → log INFO + return ; sinon → envoyer la commande puis `processedEvent.markProcessed(key)`
- [ ] 6.4 Tests `DrawResultedEventListenerTest` :
  - Premier appel avec eventId X → commande envoyée + marquée
  - Second appel avec même eventId X → commande NON envoyée + log INFO

## 7. Documentation

- [ ] 7.1 `tchalanet-server/src/main/java/com/tchalanet/server/core/sales/DOMAIN_SALES.md` :
  - §6 (Événements) — mention `ProcessedEventPort` sur `DrawResultedEventListener`
  - §8 (Notes techniques / Idempotence) — décrire l'usage de `ticket_settlement`
  - §9 — retirer les anomalies P0 traitées
- [ ] 7.2 `tchalanet-docs/docs/02-functional/flows/sell-ticket.md` §Outcome OVERRIDE — ajouter le cas 409 SETTLED
- [ ] 7.3 `tchalanet-docs/docs/02-functional/flows/draw-execution.md` §Settle — ajouter mention idempotence `ticket_settlement`
- [ ] 7.4 Créer un placeholder TODO `docs/decisions/sales-pipeline-decisions.md` (D1 idempotence, D4 override 409, D5 OverrideRequest shape) si le fichier n'existe pas encore

## 8. Vérification finale

- [ ] 8.1 `./mvnw clean verify` → build vert + tous tests passent
- [ ] 8.2 Vérifier sur la sortie SQL Hibernate qu'un INSERT `ticket_settlement` est émis par ticket settled (test d'intégration manuel ou via @SqlGroup)
- [ ] 8.3 CHANGELOG : `BREAKING (admin API) — OverrideTicketResultRequest field 'status' replaced by 'resultStatus'`
- [ ] 8.4 Coordonner front admin pour adapter le payload de l'override
- [ ] 8.5 Communiquer aux ops (cf. `features.ops`) la nouvelle réponse 409 sur override SETTLED
