# Domaine Ledger

> Domaine annoncé mais encore en cours de conception / implémentation.  
> Cette fiche sert de stub pour la future comptabilité interne.

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

# Domaine core.payout — Claims & Payments

> Gère le calcul/ingestion des settlements, création des PayoutClaims, exécution des PayoutPayments, statuts agrégés.

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/payout.md`

---

## 1. Rôle du domaine

- Ouvrir des claims pour tickets SETTLED.
- Poster des paiements (split possible) avec idempotence.
- Fermer les claims quand payé net == due.

**Ne fait pas**

- Émission ticket.
- Écritures comptables (ledger).

---

## 2. Modèle & invariants

- `PayoutClaim`: OPEN → PARTIALLY_PAID → PAID (VOIDED possible).
- `PayoutPayment`: POSTED → REVERSED.
- Invariants:
  - netPaid <= amount_due.
  - Paiement immutable (append-only); reversal via entry miroir.

---

## 3. Use Cases

- `OpenPayoutClaimCommandHandler`
- `PostPayoutPaymentCommandHandler`
- `ReversePayoutPaymentCommandHandler`

---

## 4. Ports out

- `PayoutClaimRepoPort`
- `PayoutPaymentRepoPort`
- (futur) provider paiement externe

---

## 5. Événements

- `PayoutClaimOpenedEvent`, `PayoutPaymentPostedEvent`, `PayoutPaymentReversedEvent`, `PayoutClaimClosedEvent`.
- Publier via `AfterCommit`.

---

## 6. Idempotence & Concurrence

- Idempotency-Key obligatoire pour post payment.
- Optimistic lock `version` sur claim.

---

## 7. Mapping & DTOs

- MapStruct; DTO `PayoutClaimResponse`, `PayoutPaymentResponse`.
- Wrappers ID en web.

---

## 8. Intégrations

- `core.sales` fournit tickets SETTLED.
- `core.ledger` poste entries sur events.

---

## 9. Notes techniques

- Multi-tenant strict; RLS.
- Unique constraint idempotence sur payments.

---

## 10. Incohérences / TODO

- Confirmer statuts et transitions exacts implémentés.
- Vérifier clé d’unicité ledger pour idempotence.
