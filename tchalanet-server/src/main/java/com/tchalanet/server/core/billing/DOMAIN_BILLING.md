> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/ledger.md` (billing cross-domain)

# Domaine core.billing — Facturation (tenant)

> Gère la facturation au niveau tenant: génération de factures, agrégation de charges, suivi des paiements, et états de règlement. Domaine transverse (non critique argent opérationnel), aligné avec ledger pour cohérence.

---

## 1. Rôle du domaine

**Responsabilité principale**

- Émettre et gérer des factures pour un tenant (abonnement, frais d’usage, commissions, ajustements).
- Suivre les règlements (paiements reçus) et l’état des factures.

**Ce que le domaine fait**

- Agrège des “charges” (usage/commissions/ajustements) sur une période.
- Génère des “invoices” (factures) avec détail des lignes.
- Enregistre des “billing payments” pour règlement des factures.
- Expose des vues paginées & exports.

**Ce que le domaine ne fait pas**

- Ne gère pas la vente de tickets ni les gains (sales/payout).
- Ne remplace pas le journal comptable (ledger) mais peut y publier des entrées synthétiques.
- Ne fait pas la réconciliation bancaire externe (ERP/PSP).

---

## 2. Modèle métier (agrégats / entités)

### Entités / agrégats principaux

- `BillingCharge` — charge élémentaire (type, amount, currency, occurredAt, meta).
- `BillingInvoice` — facture agrégée (period, totalAmount, status, lines).
- `BillingPayment` — règlement d’une facture (amount, method, postedAt, status).

### Invariants métier

- Une facture `PAID` est figée (append-only pour corrections via `Adjustment`).
- La somme des `BillingPayment` net ne doit pas dépasser `invoice.total`.
- Les “charges” sont immuables, corrections via `BillingAdjustment`.

> Valeur métier clé :
> Garantir une facture fiable par période et un solde clair, avec corrections traçables.

---

## 3. Cas d’utilisation (ports d’entrée)

- `GenerateInvoiceCommandHandler(tenantId, period)` — agrège charges et crée la facture.
- `PostBillingPaymentCommandHandler(invoiceId, amount, method)` — enregistre un paiement.
- `ListInvoicesQueryHandler(tenantId, page, size)` — liste paginée.
- `GetInvoiceDetailsQueryHandler(invoiceId)` — détail facture.
- `CreateBillingAdjustmentCommandHandler(invoiceId, delta, reason)` — correction append-only.

---

## 4. Ports de sortie (dépendances externes)

- `BillingChargeRepoPort` — lecture/écriture des charges.
- `BillingInvoiceRepoPort` — persistance des factures.
- `BillingPaymentRepoPort` — persistance des paiements.
- `LedgerPostingPort` (optionnel) — poster une écriture synthétique (INVOICE/INVOICE_PAYMENT).

---

## 5. Mapping & DTOs (convention)

- MapStruct pour mapper `infra.web.model` ↔ `application.command/query.model`.
- DTOs:
  - `GenerateInvoiceRequest` / `InvoiceResponse` (list/detail)
  - `PostBillingPaymentRequest` / `BillingPaymentResponse`
  - `BillingAdjustmentRequest` / `BillingAdjustmentResponse`
- Records immuables pour command/query models.

---

## 6. Règles métier importantes

- Statuts facture: `OPEN` → `PARTIALLY_PAID` → `PAID` (ou `VOIDED`).
- Paiement: `POSTED` (puis éventuellement `REVERSED`).
- Idempotence: `PostBillingPayment` doit supporter `Idempotency-Key` (unique tenant+key).
- Ajustements: append-only, jamais de modification destructive.

---

## 7. Intégration avec les autres domaines

Dépend de :

- `core.sales` (charges liées à ventes/commissions si appliquées).
- `core.payout` (ajustements éventuels).

Utilisé par :

- `core.ledger` (post d’entries synthétiques INVOICE/PAYMENT si requis).
- `features.reporting` (export factures).

---

## 8. Notes techniques

- Multi-tenant strict (tenant-scoped) → `BaseTenantEntity` pour factures/charges/paiements.
- RLS appliqué; toutes lectures sont tenant-safe.
- MapStruct; wrappers d’ID hors JPA; UUID en JPA.
- Transactions: `@TchTx` pour commands qui écrivent.

---

## 9. Incohérences / TODO

- Confirmer la source des “charges” (catalog des types + calculs d’agrégation).
- Définir si on publie systématiquement vers ledger (ou seulement certaines lignes).
- Préciser les méthodes de paiement supportées (CASH | TRANSFER | MOBILE_MONEY | OTHER).
