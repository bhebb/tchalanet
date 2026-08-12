# limits-contextual-config-v1

## Statut
`proposal` — 2026-08-12

## Contexte

La section "Limits" de l'admin portal a actuellement 5 pages séparées (overview, global, draw, number, seller-terminal). Elle est difficile à naviguer car les règles sont isolées de leur contexte naturel : l'admin qui configure un draw channel doit quitter le draw pour aller dans les limits.

### Système de limite existant (résumé technique)

Le backend (`LimitResolver`) applique un **score par scope** — la règle la plus spécifique gagne :

| Scope | Score | Override |
|---|---|---|
| TENANT | 10 | Défaut global |
| DRAW_CHANNEL | 30 | Override le tenant pour ce tirage |
| SELLER_TERMINAL / AGENT | 60 | Override les deux |

**7 ruleKeys exposés** (via `rules.v1.json`) :

| ruleKey | Catégorie | Stateful |
|---|---|---|
| MAX_STAKE_PER_LINE | TICKET | non |
| MAX_LINES_PER_TICKET | TICKET | non |
| MAX_STAKE_PER_TICKET | TICKET | non |
| BLOCK_BET_TYPE | BLOCKING | non |
| BLOCK_SELECTION_PER_DRAW | BLOCKING | non |
| MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW | EXPOSURE | **oui** |
| MAX_SALES_COUNT_PER_SELECTION_PER_DRAW | EXPOSURE | **oui** |

Les règles stateful (`stateless: false`) lisent l'accumulation de ventes réelles en base (`DrawExposureJpaEntity`). Aujourd'hui la projection (`ExposureProjectorAdapter.scopesFor()`) ne couvre que TENANT et DRAW_CHANNEL — **SELLER_TERMINAL n'est pas projeté** bien que `TicketPlacedEvent.context().sellerTerminalId()` soit disponible.

**Mise min/max par jeu** : stockée dans `TenantGame.minStake / maxStake` — système séparé du limitpolicy, pas concerné par ce change.

**Numéros chauds POS** : le vendeur doit pouvoir voir ses propres top numéros (scope SELLER_TERMINAL). La donnée n'existe pas encore — nécessite d'activer la projection SELLER_TERMINAL dans `ExposureProjectorAdapter`.

**Page de détail draw côté mobile** : `SellerTerminalDrawReportPage` est la page de détail d'un tirage en POS (Flutter). Elle appelle aujourd'hui deux endpoints :
1. `GET /tenant/cashier/tickets/stats?date=YYYY-MM-DD` → stats journalières avec breakdown par drawId (totalCents, ticketCount, winningsCents, sellerCommissionCents)
2. `GET /tenant/cashier/tickets?fromDate=...&drawId=...` → liste des tickets vendus sur ce tirage

Elle n'appelle aucun endpoint de top sélections ni d'exposition — le `PosDrawsController` existant (`/tenant/cashier/draws`) n'expose que `GET /available`.

---

## Pourquoi

- L'admin configure les limites hors contexte (page dédiée séparée des draw/terminal).
- La page "global" est générique, peu compréhensible.
- L'action la plus fréquente (bloquer un numéro sur un tirage) nécessite 3 clics alors qu'elle devrait être accessible depuis le détail du tirage.
- Les exposures (numéros chauds approchant leur plafond) ne sont pas encore exposées en UI — le backend est prêt (`GetExposureAlertsOverviewQuery` + `DrawExposureJpaEntity`) mais aucun endpoint REST ni vue frontend.

---

## What — Vision cible

### 1. Config de limite dans le contexte

Un **bloc "Limites"** réutilisable (`LimitPolicyBlockComponent`) avec un champ nullable par ruleKey, affiché dans :

- **Setup → Config tenant** : règles TENANT (défaut global pour tous les draws/terminaux)
- **Détail draw channel** : règles DRAW_CHANNEL (override par tirage, nullable = hérite du tenant)
- **Création / édition seller terminal** : règles SELLER_TERMINAL (override par terminal, nullable = hérite)

Chaque champ nullable signifie "hériter du niveau supérieur". Les libellés affichent la valeur résolue en grisé quand le champ est vide (`= hérite : 500 HTG`).

### 2. Section Limits → réduite à 2 rôles

| Page | Contenu |
|---|---|
| **Vue active** (`/limits`) | Tableau lecture seule des règles actives résolues par scope (tenant + draw + terminal) — audit, pas config |
| **Numéros bloqués** (`/limits/number`) | Liste des BLOCK_SELECTION actifs + bouton "Bloke nimero" rapide |

Pages supprimées du menu : `global`, `draw` (accessible en lien "Vue avancée" si besoin admin).

### 3. Action contextuelle "Bloke nimero" dans le détail d'un tirage

Sur la page détail d'un tirage ouvert :
- Bouton **"Bloke nimero"** dans la zone d'actions de la page header.
- Ouvre le `BlockNumberQuickDialogComponent` existant avec le draw channel **pré-sélectionné** (pas de picker de tirage dans le dialog).
- Les numéros déjà bloqués sur ce draw sont affichés en bas du dialog (liste légère).

### 4. Exposures — endpoint backend + widget draw

**Backend (nouveau endpoint)** :
```
GET /admin/policies/limits/exposure-alerts?drawId={drawId}&scope=DRAW_CHANNEL&targetId={channelId}&limit=10
```
Retourne `ExposureAlertsOverviewView` — déjà implémenté dans le query handler, pas encore exposé.

**Frontend draw detail** :
- Section "Numéros à risque" sous les top 5 sélections actuelles (qui viennent des financials).
- Affiche les numéros approchant leur plafond `MAX_STAKE_EXPOSURE` avec un ratio (barre de progression ou chip coloré).
- Visible seulement si une règle `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW` est configurée pour ce draw channel.

---

## Impact

### Backend
- **Activer projection SELLER_TERMINAL** dans `ExposureProjectorAdapter.scopesFor()` — ajouter `LimitScopeRef.sellerTerminal(event.context().sellerTerminalId())` si présent. Toutes les ventes futures sont alors tracées par terminal.
- **BFF admin** : nouveau endpoint `GET /admin/draws/{drawId}/overview` dans `features/tenantadmin/draw` — agrège draw channel info + draw + résultat + top selections (core/sales) + exposure DRAW_CHANNEL (core/limitpolicy). Remplace les deux appels séparés actuels du frontend.
- **BFF POS** : nouveau endpoint `GET /tenant/cashier/draws/{drawId}/detail` dans `features/pos/draws/PosDrawsController` (controller existant, ajouter `@GetMapping("/{drawId}/detail")`) — agrège draw info + top selections scope SELLER_TERMINAL + exposure SELLER_TERMINAL. Uniquement si le tirage est OPEN.

### Web — admin-portal
- Nouveau composant `LimitPolicyBlockComponent` (champs par ruleKey, nullable, avec valeur héritée).
- Intégrer dans : setup tenant config, draw channel detail, seller terminal form.
- Réduire menu limits : supprimer `global` et `draw` du nav, garder `overview` + `number`.
- Ajouter bouton "Bloke nimero" sur le détail du tirage (pré-sélection du channel).
- Nouveau widget "Numéros à risque" sur le détail du tirage (appel exposure-alerts).
- Nouveau service call `getExposureAlerts(drawId, channelId)` dans `AdminLimitsApi`.

### Mobile — POS Flutter
- `SellerTerminalDrawReportPage` continue d'appeler `/tenant/cashier/tickets/stats` (stats + breakdown par draw) et `/tenant/cashier/tickets` (liste tickets) — pas touché.
- Ajouter un appel au nouveau BFF `GET /tenant/cashier/draws/{drawId}/detail` pour obtenir les top sélections SELLER_TERMINAL et l'exposition.
- Nouveau widget "Nimero cho" dans `SellerTerminalDrawReportPage` : liste des top numéros du terminal pour ce tirage, avec ratio d'exposition si `MAX_STAKE_EXPOSURE` est configuré. Visible seulement si le tirage est OPEN.

---

## Non-goals

- **Ne pas migrer** les données existantes — les assignments TENANT existants restent valides.
- **Ne pas implémenter** les 6 ruleKeys non exposés (`MAX_TICKET_COUNT_PER_AGENT_PER_WINDOW`, etc.).
- **Ne pas toucher** la page `seller-terminal` dans limits (restera accessible mais hors menu principal).
- **Pas de reset** de l'interface `UpsertLimitDialog` existante — elle reste pour la vue avancée `/limits/draw`.
- **Pas de mise min/max** par jeu dans ce change (c'est `TenantGame`, pas limitpolicy).

---

## Context packs

- `openspec/context/10-non-negotiables.md`
- `openspec/context/30-frontend-rules.md`
- `openspec/context/20-backend-rules.md`

## Near-code références

- `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/limitpolicy/`
- `tchalanet-web (worktree admin-ux-v1)/apps/admin-portal/src/app/features/limits/`
- `tchalanet-web (worktree admin-ux-v1)/apps/admin-portal/src/app/features/draws/pages/detail/`
