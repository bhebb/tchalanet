# Domaine Ledger

> Domaine annoncé mais encore en cours de conception / implémentation.  
> Cette fiche sert de stub pour la future comptabilité interne.

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/ledger.md`

---

## 1. Rôle du domaine

**Responsabilité principale**

Modéliser le “grand livre” interne de Tchalanet : mouvements financiers, soldes de caisses/PDV/tenant, et journaux comptables liés aux ventes, paiements et ajustements.

**Ce que le domaine fait**

- TODO: détailler les cas d’usage (enregistrement mouvements, consultation soldes, rapprochements, export comptable, etc.).

...

## 9. Domaines existants (référence)

À titre indicatif, les domaines actuellement présents dans Tchalanet :

- `accesscontrol` — permissions & rôles par tenant.
- `audit` — audit applicatif & révisions.
- `draw` — tirages & résultats.
- `sales` / `ticket` — création & gestion des tickets.
- `payout` — calcul et paiement des gains.
- `ledger` — **(ce domaine)** journalisation des mouvements et soldes.
- `session` — sessions POS & vendeurs.
- `tenantconfig` — configuration de tenant (limites, odds, etc.).
- `pagemodel` — configuration dynamique des pages publiques/privées.
- `identity` — utilisateurs & profils (hors auth Keycloak).

# Domaine core.ledger — Journal comptable (append-only)

> Modélise et enregistre les écritures comptables internes (DEBIT/CREDIT ou amount signé) de manière immuable, dérivées des événements métier.

---

## 1. Rôle du domaine

- Post des entries (SALE, SALE_CANCEL, PAYOUT, PAYOUT_REVERSAL, ADJUSTMENT...).
- Idempotence: refuser le double-post.
- Exposer des lectures (byRef, byRange, byType).

**Ne fait pas**

- Calcul métier (gains, limites).
- AuthZ (hors guards admin).

---

## 2. Modèle & invariants

- `LedgerEntry`: id, tenant_id, occurred_at, direction (DEBIT|CREDIT ou signed), amount, currency, entry_type, ref_type, ref_id, correlation_id, memo, created_at, created_by.
- Invariants:
  - Une entry n’est jamais modifiée/supprimée.
  - Correction = entry de reversal.

---

## 3. Use Cases

- `PostLedgerEntryCommandHandler`
- `ListLedgerEntriesQueryHandler`

---

## 4. Ports

- `LedgerEntryRepoPort`

---

## 5. Événements & Idempotence

- Consomme: TicketIssued/Cancelled/Settled, PayoutPaymentPosted/Reversed.
- Idempotence via unique:
  - `(tenant_id, entry_type, ref_type, ref_id, correlation_id)`.

---

## 6. Mapping & DTOs

- MapStruct; DTO `LedgerEntryResponse`.
- Wrappers ID partout hors JPA.

---

## 7. Notes techniques

- Multi-tenant & RLS.
- Index par ref_type/ref_id pour lecture.

---

## 8. Incohérences / TODO

- Choix définitif: direction vs montant signé.
- Confirmer clé d’unicité exacte et sa gestion.
