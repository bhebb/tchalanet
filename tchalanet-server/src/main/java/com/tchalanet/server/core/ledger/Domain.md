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
