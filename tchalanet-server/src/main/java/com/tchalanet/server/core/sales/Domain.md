# Domaine Sales / Tickets

> Domaine annoncé mais encore en cours de conception / implémentation.  
> Cette fiche sert de garde-fou pour la suite.

---

## 1. Rôle du domaine

**Responsabilité principale**

Assurer la création, la gestion et le cycle de vie des tickets de jeu (vente, annulation, consultation) pour un tenant donné, en les liant aux tirages (`draw`), aux points de vente et aux sessions POS.

**Ce que le domaine fait**

- TODO: détailler les cas d’usage (vente ticket, annulation, ré-impression, consultation historique, lien avec offline, etc.).

...

## 9. Domaines existants (référence)

À titre indicatif, les domaines actuellement présents dans Tchalanet :

- `accesscontrol` — permissions & rôles par tenant.
- `audit` — audit applicatif & révisions.
- `draw` — tirages & résultats.
- `sales` / `ticket` — **(ce domaine)** création & gestion des tickets.
- `session` — sessions POS & vendeurs.
- `tenantconfig` — configuration de tenant (limites, odds, etc.).
- `pagemodel` — configuration dynamique des pages publiques/privées.
- `identity` — utilisateurs & profils (hors auth Keycloak).
