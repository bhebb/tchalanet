# Domaine Sales / Tickets

> Domaine implémenté avec architecture hexagonale (CQRS léger).  
> Gestion des tickets de jeu : vente, annulation, paiement, impression et consultation.

---

## 1. Rôle du domaine

**Responsabilité principale**

Assurer la création, la gestion et le cycle de vie des tickets de jeu (vente, annulation, consultation) pour un tenant donné, en les liant aux tirages (`draw`), aux points de vente et aux sessions POS. Le domaine maintient l'intégrité des tickets et publie des événements pour les intégrations cross-domaines (ex: ledger).

**Ce que le domaine fait**

- **Vente de tickets** : Création de tickets avec lignes de jeu, génération de code public sécurisé, validation des limites.
- **Annulation de tickets** : Annulation avec raison, publication d'événements.
- **Gestion des paiements** : Marquage comme paiement en attente ou payé, avec audit.
- **Impression** : Génération de texte plain pour impression des tickets.
- **Consultation** : Liste paginée, détails complets, vérification publique par code.
- **Intégrations** : Événements pour ledger (enregistrement des ventes/paiements).

**Ce que le domaine ne fait pas**

- Gestion des tirages (domaine `draw`).
- Gestion des sessions POS (domaine `session`).
- Authentification/autorisation (domaine `accesscontrol`).
- Stockage physique (infrastructure).

---

## 2. Modèle de domaine

### Agrégats principaux

- **Ticket** : Agrégat racine représentant un ticket de jeu.
  - États : `CREATED`, `RESULTED_WIN`, `RESULTED_LOSS`, `PAYMENT_PENDING`, `PAID`, `CANCELLED`.
  - Transitions : Vente → Résultat → Paiement → Payé, ou Annulation à tout moment.
  - Lignes : Liste de `TicketLine` (gameCode, selection, stake, potentialPayout).

### Événements domaine

- `TicketPlacedEvent` : Publication lors de la vente (pour ledger).
- `TicketVoidedEvent` : Publication lors d'annulation.
- `TicketPaymentPendingEvent` : Publication lors de marquage paiement en attente.
- `TicketPaidEvent` : Publication lors de paiement.

---

## 3. Architecture hexagonale

### Ports d'entrée (application)

- **Commandes** :
  - `SellTicketCommand` → `SellTicketCommandHandler` (crée ticket, génère codes, publie événement).
  - `CancelTicketCommand` → `CancelTicketCommandHandler` (annule ticket).
  - `MarkPaymentPendingCommand` → `MarkPaymentPendingCommandHandler` (marque paiement en attente).
  - `MarkTicketPaidCommand` → `MarkTicketPaidCommand` (marque payé).
  - `PrintTicketCommand` → `PrintTicketUseCaseHandler` (génère texte d'impression).

- **Requêtes** :
  - `ListTicketsQuery` → `ListTicketsQueryHandler` (liste paginée avec filtres).
  - `GetTicketDetailsQuery` → `GetTicketDetailsQueryHandler` (détails complets).
  - `VerifyPublicTicketQuery` → `VerifyPublicTicketQueryHandler` (vérification publique).

### Ports de sortie (infrastructure)

- **TicketWriterPort** : Sauvegarde des tickets.
- **TicketReaderPort** : Lecture des tickets (avec filtres, pagination).
- **TicketNumberGeneratorPort** : Génération de code interne.
- **TicketPublicCodeGeneratorPort** : Génération de code public sécurisé (Crockford base32).
- **TicketPrinterPort** : Rendu texte plain pour impression.
- **TicketEventPublisherPort** : Publication d'événements Spring.

### Adaptateurs infrastructure

- **JpaTicketRepositoryAdapter** : Implémentation JPA pour lecture/écriture.
- **LogOnlyTicketPrinterAdapter** : Stub pour impression (log seulement).
- **SpringApplicationEventPublisherAdapter** : Publication d'événements.

---

## 4. Contraintes métier

- **Tenant-scoped** : Tous les accès filtrés par tenant (via RLS ou contexte).
- **Append-only** : Pas de suppression physique, seulement marquage annulé.
- **Codes uniques** : `ticketCode` unique par tenant, `publicCode` unique global.
- **Validation** : Stake > 0, limites par jeu/tenant (via `limitpolicy`).
- **Audit** : Tous changements tracés avec `performedBy`.

---

## 5. Points d'intégration

- **Ledger** : Écoute `TicketPlacedEvent` pour enregistrer les ventes (CRÉDIT).
- **Audit** : Logs des actions sensibles.
- **LimitPolicy** : Validation des limites avant vente.
- **Draw** : Référence aux tirages pour validation.

---

## 6. Endpoints API

### Contrôleur privé (`TicketController`)

- `POST /api/tickets` : Vente de ticket.
- `GET /api/tickets` : Liste paginée (avec filtres : terminal, draw, status, dates).
- `GET /api/tickets/{id}` : Détails ticket.
- `PATCH /api/tickets/{id}/cancel` : Annulation.
- `PATCH /api/tickets/{id}/payment-pending` : Paiement en attente.
- `PATCH /api/tickets/{id}/paid` : Marqué payé.
- `GET /api/tickets/{id}/print` : Impression.

### Contrôleur public (`PublicTicketController`)

- `GET /ticket/{publicCode}` : Vérification publique (avec headers sécurité).

---

## 7. Qualité & tests

- **Tests unitaires** : Handlers, mappers, domaine.
- **Tests d'intégration** : Endpoints, événements.
- **Validation** : Bean validation sur requests, domaine invariants.

---

## 8. Évolution future

- Support offline (synchro tickets hors ligne).
- Ré-impression avec historique.
- Exports CSV/PDF.
- Intégration payouts pour gains élevés.

---

## 9. Domaines existants (référence)

À titre indicatif, les domaines actuellement présents dans Tchalanet :

- `accesscontrol` — permissions & rôles par tenant.
- `audit` — audit applicatif & révisions.
- `draw` — tirages & résultats.
- `ledger` — comptabilité append-only.
- `limitpolicy` — limites de jeu par tenant/joueur.
- `pagemodel` — configuration dynamique des pages publiques/privées.
- `payout` — gestion des paiements de gains.
- `sales` / `ticket` — **(ce domaine)** création & gestion des tickets.
- `session` — sessions POS & vendeurs.
- `tenantconfig` — configuration de tenant (limites, odds, etc.).
- `user` — utilisateurs & profils (hors auth Keycloak).
