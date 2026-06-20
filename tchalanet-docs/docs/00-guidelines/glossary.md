# Glossaire métier Tchalanet

**Version**: 2.0.0 | **Date**: 2026-06-20

**Ubiquitous Language** — Termes partagés entre business, dev, mobile, backend

---

## Concepts principaux

### Tenant

Organisation cliente (opérateur de jeux). Multi-tenant strict avec RLS PostgreSQL.

### Draw (Tirage)

Événement de tirage planifié (ex: Tirage Loto du 15/01/2026 à 20h). Contient des slots de résultats.

### Slot

Position de résultat dans un draw (ex: "first", "second", "bonus"). Chaque slot a un set de numéros gagnants.

### Ticket

Pari émis par un joueur. Contient des lignes (lines) avec numéros joués. Peut être `ISSUED`, `CANCELLED`, `SETTLED`.

### Ticket Line

Ligne de jeu sur un ticket (ex: 5 numéros + 2 étoiles). Associée à un game code et bet type.

### Public Code

Code court signé permettant vérification publique d'un ticket sans exposer ID interne.

### Sale (Vente)

Transaction d'émission d'un ticket. Montant payé par le client.

### Payout (Paiement gagnant)

Règlement d'un gain. Composé d'un **Claim** (montant dû) et de **Payments** (paiements effectués, possiblement split).

### Claim

Demande de paiement (montant dû). Statut : `OPEN`, `PARTIALLY_PAID`, `PAID`, `VOIDED`.

### Payment

Paiement effectué (cash, mobile money, transfer). Statut : `POSTED`, `REVERSED`.

### Ledger (Journal comptable)

Journal append-only des écritures comptables. Chaque entrée référence un événement (TICKET, PAYOUT).

### Settlement (Règlement ticket)

Opération de calcul des gains après un draw. Ticket passe de `ISSUED` à `SETTLED`.

### SellerTerminal

**Acteur de vente unique.** Remplace les anciens concepts Seller + Terminal + SalesSession qui sont retirés.

Un SellerTerminal est :
- une identité d'authentification Firebase permanente (PIN-based)
- une unité de vente et de facturation
- un acteur opérationnel autonome : il n'a pas besoin d'une session ouverte pour vendre

Un SellerTerminal peut être associé optionnellement à un `Outlet` (groupement géographique).

> **Termes retirés** : `Terminal` (hardware), `SalesSession` (session POS), `Seller` (machann/vendeur séparé), `Cashier` (rôle/flow).  
> Ne pas utiliser ces termes dans le nouveau code ou les nouvelles docs.

### Outlet (Point de vente)

Groupement géographique optionnel pour les SellerTerminals. Un SellerTerminal peut être rattaché à un Outlet ou opérer sans Outlet.

L'Outlet n'est plus un prérequis opérationnel — c'était une contrainte de l'ancien modèle Seller+Terminal+Session.

### Limit Policy

Règle métier définissant limites (montant max ticket, nombre tickets par jour, etc.).

### Game Code

Identifiant d'un type de jeu (ex: "LOTO", "KENO"). Défini dans catalog.

### Bet Type

Type de pari dans un jeu (ex: "SIMPLE", "MULTIPLE", "SYSTEM").

### Result Slot (Global)

Catalogue global des types de slots de résultats (ex: "first", "second", "bonus"). Pas tenant-scoped.

### Role

Rôle utilisateur applicable aux `APP_USER` (ex: `TENANT_ADMIN`, `SUPER_ADMIN`).  
Les `SELLER_TERMINAL` n'ont pas de rôles — leurs permissions sont attachées directement à l'entité.

### Permission

Permission fine-grained (ex: `ticket.sell`, `payout.approve`). Évaluée par `TchPermissionEvaluator`.

### Context (Request Context)

Contexte de requête HTTP contenant tenant, acteur, actorType, roles/permissions, deleted_visibility.  
Source de vérité pour RLS et business logic.

### TchActorType

Type d'acteur dans le `TchRequestContext` :

| Valeur | Description |
|---|---|
| `APP_USER` | Utilisateur humain (web, mobile admin) — mappé depuis Firebase vers `app_user` |
| `SELLER_TERMINAL` | Terminal de vente — mappé depuis Firebase vers `seller_terminal` |
| `SYSTEM` | Batch/scheduler — jamais produit par HTTP |

### RLS (Row-Level Security)

Politique PostgreSQL filtrant automatiquement les lignes par tenant. Dernière ligne de défense.

### Soft Delete

Suppression logique via `deleted_at`. Visibilité contrôlée par `deleted_visibility` (super admin only).

---

## Workflows

### SellerTerminal Provisioning (Création)

1. Admin tenant crée le SellerTerminal (code, nom, PIN initial)
2. Backend provisionne l'identité Firebase (email fictif + PIN)
3. SellerTerminal créé : statut `ACTIVE`, `mustChangePin = true`
4. Admin remet le PIN temporaire au vendeur physiquement
5. Vendeur se connecte, change son PIN

### Sell Ticket (Vente ticket)

1. Client choisit numéros
2. POS valide limites (Limit Policy)
3. Backend émet ticket (`ISSUED`)
4. Ledger enregistre vente
5. Ticket imprimé

### Verify Ticket (Vérification publique)

1. Client scanne QR code ou saisit public code
2. Backend valide signature
3. Retourne statut ticket (VALID / CANCELLED / PENDING_SYNC / EXPIRED)

### Claim Payout (Réclamation gain)

1. Ticket est `SETTLED` avec winning amount > 0
2. Système ouvre Payout Claim (`OPEN`)
3. Opérateur effectue Payment(s) (possiblement split)
4. Claim passe `PARTIALLY_PAID` → `PAID`
5. Ledger enregistre paiements

### Draw Execution (Exécution tirage)

1. Draw planifié (scheduled)
2. Système ou admin déclenche tirage
3. Résultats publiés (slots + numéros gagnants)
4. Tickets associés sont settled (calcul gains)
5. Payout Claims créés pour gagnants

---

## Bounded Contexts (Domaines)

### core.sales

Owns: Ticket lifecycle, emission, annulation, limits validation

### core.payout

Owns: Payout Claims, Payments, split payments

### core.ledger

Owns: Ledger entries (append-only), balances

### core.draws

Owns: Draw lifecycle, results, slots

### core.limits

Owns: Limit policies, validation rules

### core.sellerterminal

Owns: SellerTerminal lifecycle, Firebase provisioning, PIN management, statut

### core.accesscontrol

Owns: Roles, permissions, user management (APP_USER uniquement)

### features.cashier

Owns: POS home BFF, readiness check, cashier flow (sans session ni binding)

### features.pagemodel

Owns: Dynamic page resolution (BFF), widget orchestration

### catalog.\*

Owns: Reference data (game codes, bet types, result slots)

---

## États (State Machines)

### Ticket

- `ISSUED` → ticket émis, payé
- `CANCELLED` → annulé (avant settlement)
- `SETTLED` → gains calculés, figé
- `EXPIRED` → hors validité

### SellerTerminal

- `ACTIVE` → terminal actif, peut vendre
- `INACTIVE` → inactif (créé mais pas encore activé)
- `BLOCKED` → bloqué temporairement (par admin)
- `DISABLED` → désactivé définitivement

### Payout Claim

- `OPEN` → dû, rien payé
- `PARTIALLY_PAID` → partiellement payé
- `PAID` → totalement payé
- `VOIDED` → annulé (fraude, correction)

### Payment

- `POSTED` → paiement enregistré
- `REVERSED` → paiement annulé (reversal)

---

## Termes techniques (architecture)

### Hexagonal Architecture

Ports & Adapters. Domain au centre, infra à l'extérieur.

### CQRS

Command Query Responsibility Segregation. Commands (write) + Queries (read) séparés.

### CommandBus

Bus de dispatch des commands (write operations).

### QueryBus

Bus de dispatch des queries (read operations).

### Port (Out)

Interface définie par domain pour dépendance externe (ex: `TicketReaderPort`).

### Adapter

Implémentation d'un port (ex: `TicketReaderAdapter` pour JPA).

### Handler

Use case handler (ex: `IssueTicketHandler`, `ListTicketsHandler`).

### @TchTx

Annotation pour délimiter transaction (write commands).

### DomainEventPublisher

Publie événements métier (ex: `TicketIssuedEvent`).

### AfterCommit

Utilitaire pour exécuter code après commit transaction (ex: publish events).

### ApiResponse<T>

Enveloppe standard pour réponses API (success, created, pending, warn, partial).

### TchPage<T>

Abstraction pagination (pas Spring `Page`).

### TchPageRequest

Request pagination avec `@TchPaging` annotation.

### Typed ID Wrapper

Record encapsulant UUID (ex: `TenantId`, `TicketId`, `SellerTerminalId`). Utilisé partout sauf persistence.

### BaseTenantEntity

JPA base class pour entités tenant-scoped (has `tenant_id`).

### BaseEntity

JPA base class pour entités globales/platform (no tenant_id).

---

## Acronymes

- **ADR** : Architecture Decision Record
- **BFF** : Backend For Frontend
- **RLS** : Row-Level Security
- **POS** : Point Of Sale
- **SDD** : Specification-Driven Development
- **DTO** : Data Transfer Object
- **JPA** : Java Persistence API
- **ORM** : Object-Relational Mapping

---

**Maintenu par** : équipe Tchalanet  
**Dernière mise à jour** : 2026-06-20
