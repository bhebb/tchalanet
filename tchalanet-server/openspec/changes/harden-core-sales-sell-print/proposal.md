# Change: harden-core-sales-sell-print

## Status

Proposed

## Why

L'audit du pipeline `core.sales` a révélé plusieurs incohérences autour des chemins critiques **sell** et **print**.

Ces incohérences rendent le domaine difficile à maintenir et risquent de contaminer les prochaines features POS :

- `SellTicketCommandHandler` mélange encore logique métier, notices web, génération d'identifiants non persistés et publication d'event.
- `approvalRequestId` est généré après création du ticket pending mais n'est pas persisté dans l'agrégat.
- `session` est théoriquement nullable, alors que le handler et `TicketPlacedEvent` utilisent `session.outletId()`, `session.userId()` et `session.id()`.
- La vérification `mixedGameCodes` arrive après `ticketWriter.save(...)`, donc trop tard.
- `TicketLinePreparationService` expose plusieurs étapes séparées (`normalize`, `mergeDuplicates`, `toTicketLines`) sans méthode canonique unique.
- `publicCode` est encore nullable côté domaine/JPA alors que le contrat ticket public exige un code toujours présent.
- Les générateurs `ticketCode` / `publicCode` n'ont pas de retry contrôlé en cas de collision DB.
- `TicketWritterPort` contient une faute de nommage propagée.
- `JpaTicketRepositoryAdapter` assemble `TicketPrintView` en appelant `DrawLookupPort`, `OutletReaderPort`, `SalesSessionReaderPort` et `TicketPrintViewMapper`, ce qui pollue la couche persistence.
- Le print expose un terminal UUID masqué au lieu d'un `terminalLabel` humain.
- Le formatter utilise `ZoneId.systemDefault()`.
- Le PDF ticket utilise une hauteur fixe et peut couper les gros tickets.
- L'endpoint `/tenant/tickets/{id}/print` retourne un PDF base64 en `text/plain`, alors que `/print.pdf` et `/print.escpos` couvrent déjà les vrais usages.
- Le futur `features.pos` doit consommer `SellTicketCommand`, pas redéfinir `Ticket`, `TicketEntry`, `PlaceTicketCommand` ou la logique de vente.

Ce changement stabilise le cœur **sell + print** avant l'implémentation POS.

## What Changes

### Sell

- Garder `SellTicketCommand` comme commande canonique de vente dans `core.sales`.
- Rendre la session obligatoire pour vendre en MVP.
- Ajouter une méthode canonique `TicketLinePreparationService.prepare(tenantId, lines)`.
- Normaliser strictement les mises (`scale <= 2`, canonical scale 2).
- Vérifier les odds nulles et utiliser une scale 4 cohérente.
- Déplacer la vérification `mixedGameCodes` avant tout save.
- Générer un `approvalRequestId` via `IdGenerator` / typed id et le persister dans le ticket `PENDING_APPROVAL`.
- Retirer `ApiResponseContext` du handler autant que possible ; retourner les warnings/approval info via `SellTicketResult`.
- Garder `TicketPlacedEvent` publié `AfterCommit.run(...)`.

### Ticket codes

- Garder les générateurs existants :
  - `TimeBasedTicketNumberGenerator`
  - `CrockfordPublicCodeGenerator`
- Rendre `publicCode` obligatoire dans :
  - agrégat `Ticket`
  - `TicketEntity`
  - factories `sell` / `pendingApproval`
  - mappers
- Ajouter une stratégie de retry collision max 3 sur :
  - `uq_ticket_tenant_code`
  - `uq_ticket_public_code`
- Lever `TicketCodeGenerationException` après 3 échecs, mappée HTTP 503.

### Print

- Extraire `getTicketPrintView` hors de `JpaTicketRepositoryAdapter`.
- Créer `TicketPrintViewAssembler` dans `core.sales.application.service`.
- `JpaTicketRepositoryAdapter` ne garde que les opérations persistence ticket.
- `TicketPrintViewAssembler` orchestre :
  - `TicketReaderPort.findWithLinesById`
  - `DrawLookupPort`
  - `SalesSessionReaderPort`
  - `OutletReaderPort`
  - `TerminalReaderPort`
  - `TicketPrintViewMapper`
- `TicketPrintView` doit exposer un `terminalLabel` humain, jamais un UUID masqué.
- Remplacer `ZoneId.systemDefault()` par une zone explicite issue du contexte, du tenant ou de la vue print.
- Grouper les lignes print par `gameCode + betType + betOption`.
- Calculer dynamiquement la hauteur du PDF.
- Gérer `qrPng == null`.
- Conserver :
  - `/tenant/tickets/{ticketId}/print.pdf`
  - `/tenant/tickets/{ticketId}/print.escpos`
- Déprécier ou supprimer `/tenant/tickets/{ticketId}/print` base64 `text/plain`.

### Cleanup

- Renommer `TicketWritterPort` → `TicketWriterPort`.
- Supprimer `TicketEventPublisherPort` si aucun consommateur réel.
- Ne pas introduire `PlaceTicketCommand`.
- Ne pas introduire `TicketEntry` / multi-entry dans ce changement.

## Capabilities

### Added

- `sales-ticket-printing`

### Modified

- `sales-ticket-lifecycle`
- `sales-event-publishing`

## Dependencies

- `harden-ticket-settlement-integrity` should own settlement idempotency, `ProcessedEventPort`, and override guard on `SETTLED` tickets.
- `harden-public-ticket-verification` should own public verification DTO shape, masking, payoutStatus, visibility fallback, logs and metrics.
- `pos-v0-features` should consume `SellTicketCommand` and print endpoints, not redefine sales internals.

## Non Scope

This change does not own:

- Public verify DTO shape.
- Public ticket masking.
- Settlement idempotency.
- Override of already settled tickets.
- POS BFF endpoints.
- Flutter screens.
- Multi-entry ticket model.
- Moving `drawId` from ticket to ticket entries.
- Full payout workflow redesign.
