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

Les scores restent un détail interne du resolver. Ils ne doivent pas être exposés dans l'UI normale. L'UI exprime uniquement l'héritage : tenant → draw channel → seller terminal.

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

L'ajout du scope SELLER_TERMINAL doit utiliser le même mécanisme d'idempotence que les projections existantes. Un replay du même `TicketPlacedEvent` ne doit jamais doubler les compteurs TENANT, DRAW_CHANNEL ou SELLER_TERMINAL.

**Mise min/max par jeu** : stockée dans `TenantGame.minStake / maxStake` — système séparé du limitpolicy, pas concerné par ce change.

### Numéros chauds POS

Le vendeur doit pouvoir voir ses propres top numéros pour un tirage, sur le scope SELLER_TERMINAL.

Dans ce change, "numéro chaud" signifie une sélection classée par montant total misé (`totalStake DESC`) pour le seller terminal courant et le draw courant.

Un numéro chaud n'est pas nécessairement un numéro à risque :
- **Top / chaud** = volume de ventes important.
- **À risque** = exposition proche du plafond résolu.

Si une règle `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW` est active, la vue peut enrichir chaque numéro chaud avec : exposition actuelle, limite résolue, ratio exposure/limit.

La donnée SELLER_TERMINAL n'existe pas encore — nécessite d'activer la projection SELLER_TERMINAL dans `ExposureProjectorAdapter`.

Aucun backfill historique n'est effectué dans ce change. Après déploiement, les projections SELLER_TERMINAL ne contiennent que les ventes projetées depuis leur activation. Les top numéros et expositions terminal peuvent donc être partiels pendant la période de montée en charge.

### Page de détail draw côté mobile

`SellerTerminalDrawReportPage` est la page de détail d'un tirage en POS (Flutter). Elle appelle aujourd'hui deux endpoints :
1. `GET /tenant/cashier/tickets/stats?date=YYYY-MM-DD` → stats journalières avec breakdown par drawId (totalCents, ticketCount, winningsCents, sellerCommissionCents)
2. `GET /tenant/cashier/tickets?fromDate=...&drawId=...` → liste des tickets vendus sur ce tirage

Elle n'appelle aucun endpoint de top sélections ni d'exposition — le `PosDrawsController` existant (`/tenant/cashier/draws`) n'expose que `GET /available`.

---

## Pourquoi

- L'admin configure les limites hors contexte (page dédiée séparée des draw/terminal).
- La page "global" est générique, peu compréhensible.
- L'action la plus fréquente (bloquer un numéro sur un tirage) nécessite 3 clics alors qu'elle devrait être accessible depuis le détail du tirage.
- Les exposures (numéros approchant leur plafond) ne sont pas encore exposées en UI — le backend possède déjà la query `GetExposureAlertsOverviewQuery` et la projection `DrawExposureJpaEntity`, mais aucun endpoint REST ni vue frontend ne les exploitent.
- Les admins ne doivent pas avoir à comprendre les scopes techniques ou leurs scores pour configurer correctement une limite.

---

## What — Vision cible

### 1. Config de limite dans le contexte

Un **bloc "Limites"** réutilisable (`LimitPolicyBlockComponent`) est affiché dans :
- **Setup → Config tenant** : règles TENANT, défaut global pour tous les draws/terminaux
- **Détail draw channel** : règles DRAW_CHANNEL, override du tenant
- **Création / édition seller terminal** : règles SELLER_TERMINAL, override du draw/tenant

Chaque champ est nullable. La valeur héritée et sa provenance sont affichées :

```
Valeur renseignée :
Maximum par ligne  [ 300 ]

Valeur vide :
Maximum par ligne  [     ]  Hérite du tenant · 500 G

Sur seller terminal (override draw channel présent) :
Maximum par ligne  [     ]  Hérite du tirage · 300 G

Sur seller terminal (pas d'override draw channel) :
Maximum par ligne  [     ]  Hérite du tenant · 500 G
```

Le composant affiche la valeur résolue et sa provenance, sans exposer les scores internes du LimitResolver.

**Organisation du bloc** — grouper les règles par intention, pas par liste technique :

| Groupe | ruleKeys |
|---|---|
| **Vente** | Mise max par ligne, Nombre max de lignes, Mise max du ticket |
| **Restrictions** | Types de jeu bloqués, Numéros bloqués |
| **Exposition** | Mise cumulée max par numéro et tirage, Nombre max de ventes par numéro et tirage |

Les règles les moins courantes peuvent être placées sous une zone "Avancé". La configuration affichée par contexte peut être limitée aux règles réellement pertinentes pour ce scope.

**Collapse par groupe** — chaque groupe (Vente / Restrictions / Exposition) est individuellement collapsible. L'utilisateur peut ouvrir Vente sans ouvrir Exposition. Le composant expose un input `[defaultExpandedGroups]` (liste des groupes ouverts par défaut) que le contexte parent contrôle. L'état expand/collapse est un signal local, non persisté.

Defaults par contexte :

| Contexte | Groupes ouverts par défaut | Raison |
|---|---|---|
| Provisioning tenant (platform) | aucun — tout replié | Écran dense, limites optionnelles au provisioning |
| Setup tenant (admin config) | **Vente** seulement | Config la plus fréquente, Restrictions et Exposition sur demande |
| Draw channel detail | **Vente** + **Restrictions** | Override courant : mise max et blocages ; Exposition sur demande |
| Seller terminal create/edit | aucun — tout replié | Exception, pas la règle |

**Placement par contexte** :
- **Provisioning tenant (platform)** : onglet ou section "Limites" dédiée dans le formulaire de provisioning
- **Setup tenant (admin)** : bloc "Limites" dans la page de config générale (aux côtés du nom, timezone, etc.), même niveau que les autres blocs de config

### 2. Section Limits → réduite à 2 rôles

| Page | Contenu |
|---|---|
| **Vue active** (`/limits`) | Tableau lecture seule des règles actives résolues par scope (tenant + draw + terminal) — audit, pas config |
| **Numéros bloqués** (`/limits/number`) | Liste des BLOCK_SELECTION actifs + bouton "Bloke nimero" rapide |

Pages supprimées du menu : `global`, `draw`, `seller-terminal`. Les routes Angular restent actives pour compatibilité et usage avancé. La vue `/limits/draw` reste accessible via un lien "Vue avancée".

Limits devient principalement une surface d'observation/audit ; la configuration quotidienne revient dans son contexte naturel.

### 3. Action contextuelle "Bloke nimero" dans le détail d'un tirage

Sur la page détail d'un tirage ouvert :
- Bouton **"Bloke nimero"** dans la zone d'actions du page header
- Ouvre le `BlockNumberQuickDialogComponent` existant
- Draw channel pré-sélectionné — pas de picker de tirage dans ce contexte
- Les numéros déjà bloqués sur ce draw sont affichés en bas du dialog dans une liste légère

Le dialog reste réutilisable depuis `/limits/number` avec sélection manuelle du draw/channel.

### 4. Exposures — API core/admin avancée

Exposer la query existante via un endpoint admin mono-domain :

```
GET /admin/policies/limits/exposure-alerts?drawId={drawId}&scope={scope}&targetId={targetId}&limit=10
```

Retourne `ExposureAlertsOverviewView`. Cet endpoint sert à : vue Limits avancée, audit, diagnostic, surfaces génériques limitpolicy. Il n'est **pas** la source principale du détail Draw Admin.

### 5. Admin Draw Detail — BFF unique

Ajouter `GET /admin/draws/{drawId}/overview` dans `features/tenantadmin/draw`.

Le BFF agrège les données nécessaires à l'écran :

```json
{
  "draw": {},
  "channel": {},
  "result": {},
  "topSelections": [],
  "effectiveLimits": {},
  "exposureAlerts": []
}
```

Le BFF orchestre uniquement des queries des domaines propriétaires. Il ne recalcule aucune limite, aucun montant ou ratio métier qui appartient à `core.limitpolicy` / `core.sales`.

**Frontend draw detail** — deux sections distinctes :

```
Top sélections
34   14 500 G
12   11 200 G
09    8 900 G

Numéros à risque
34   14 500 / 15 000   97 %
09    8 900 / 10 000   89 %
```

Un top numéro peut ne pas être à risque, et un numéro moins vendu peut être à risque si son plafond est inférieur. "Numéros à risque" est visible seulement lorsqu'une règle résolue `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW` s'applique au draw channel.

**Codage couleur du ratio** (commun admin draw detail et mobile "Nimero cho") :
- < 50 % → vert (normal)
- 50–79 % → orange (attention)
- ≥ 80 % → rouge (critique)

Le ratio est calculé côté backend (`exposure / resolvedLimit`) et exposé dans le payload — le frontend ne recalcule pas.

### 6. Projection SELLER_TERMINAL

Dans `ExposureProjectorAdapter.scopesFor()`, ajouter le scope terminal lorsque disponible :

```java
LimitScopeRef.sellerTerminal(event.context().sellerTerminalId())
```

Projection cible d'un `TicketPlacedEvent` : TENANT + DRAW_CHANNEL + SELLER_TERMINAL. Le traitement reste idempotent par `eventId`.

Tests minimum requis :
- projection TENANT
- projection DRAW_CHANNEL
- projection SELLER_TERMINAL lorsque `sellerTerminalId` est présent
- absence du scope SELLER_TERMINAL lorsque l'event n'en contient pas
- replay du même event = aucun double comptage

### 7. POS Draw Detail BFF

Ajouter `@GetMapping("/{drawId}/detail")` dans `PosDrawsController` existant.

Le seller terminal **ne doit jamais être fourni par le client**. Le scope est résolu depuis le Request Context :

```
scope    = SELLER_TERMINAL
targetId = ctx.sellerTerminalIdRequired()
```

Aucun `?sellerTerminalId=...` n'est accepté.

Le BFF agrège : draw info, topSelections SELLER_TERMINAL, effective exposure limit, exposure SELLER_TERMINAL.

L'endpoint reste consultable après fermeture du tirage (pour que `SellerTerminalDrawReportPage` reste cohérente pendant la transition OPEN → CLOSED). Les informations d'exposition/action sont marquées inactives quand le tirage n'est plus OPEN :

```json
// Tirage OPEN
{ "draw": {}, "topSelections": [], "exposure": { "active": true, "limitConfigured": true, "alerts": [] } }

// Tirage CLOSED
{ "draw": {}, "topSelections": [], "exposure": { "active": false, "limitConfigured": true, "alerts": [] } }
```

### 8. POS Flutter — "Nimero cho"

`SellerTerminalDrawReportPage` continue d'appeler `/tenant/cashier/tickets/stats` et `/tenant/cashier/tickets` — ces deux appels ne sont pas remplacés.

Ajouter l'appel `GET /tenant/cashier/draws/{drawId}/detail` pour les informations complémentaires.

Nouveau widget **"Nimero cho"** — top sélections du terminal pour ce tirage, triées par montant total misé descendant :

```
Nimero cho
34      1 450 G
12      1 200 G
09        850 G

Avec limite configurée :
34      1 450 / 1 500 G      97 %
12      1 200 / 2 000 G      60 %
```

Le ratio d'exposition est une information complémentaire ; il ne change pas la définition du classement. La section exposition/risque est active uniquement pour un draw OPEN. Les top selections peuvent rester visibles dans le rapport après fermeture.

---

## Impact

### Backend — core.limitpolicy
- Activer projection SELLER_TERMINAL dans `ExposureProjectorAdapter.scopesFor()`.
- Conserver idempotence du projector (par `eventId`).
- Exposer `GetExposureAlertsOverviewQuery` via endpoint admin limitpolicy.
- Ajouter/adapter les queries nécessaires aux BFF sans exposer repositories ou JPA entities.

### Backend — features/tenantadmin
Nouveau BFF `GET /admin/draws/{drawId}/overview` — draw + channel + result + top selections + effective limits + exposure alerts. Aucune logique métier critique dans la feature.

### Backend — features/pos
Nouveau endpoint `GET /tenant/cashier/draws/{drawId}/detail` dans `PosDrawsController` existant. Résout automatiquement le seller terminal depuis `TchRequestContext`.

### Backend — pagemodel
Modifier `tchalanet-app/src/main/resources/pagemodel/fragments/private/tenantadmin/private_shell_tenantadmin.json` :
- Retirer `limits-global`, `limits-draw`, `limits-seller` de la section `limits`
- Retirer `sellers-limits` de la section `sellers`
- Garder `limits-overview` + `limits-number`

Les routes restent actives.

### Web — admin-portal
Nouveau composant `LimitPolicyBlockComponent` :
- **Mobile-first** : colonne à 360 dp, grille à partir de 600 dp (cible POS Sunmi V2)
- `OnPush`, signals, `standalone: true`, dans `libs/ui/console/`
- Règles groupées par intention (Vente / Restrictions / Exposition)
- Affiche valeur héritée + provenance

Intégrer dans : Setup tenant config, Draw Channel Detail, Seller Terminal create/edit.

### Web — navigation
Modifier `private-navigation.model.ts` en miroir du pagemodel backend, **dans le même commit** : retirer `limits-global`, `limits-draw`, `limits-seller`, `sellers-limits`. Garder `overview` + `number`.

### Web — draw detail
- Bouton "Bloke nimero" avec channel pré-sélectionné dans le dialog
- Utiliser le BFF `/admin/draws/{drawId}/overview` comme source principale de la page
- Ne pas déclencher d'appel frontend indépendant vers `AdminLimitsApi.getExposureAlerts()` — les exposures sont déjà incluses dans l'overview
- Sections distinctes "Top sélections" et "Numéros à risque"
- L'état est possédé par la feature/page ; les composants de présentation reçoivent leurs données par inputs et ne font pas d'appel HTTP direct

### Mobile — POS Flutter
- Conserver les endpoints actuels stats + tickets
- Ajouter `/tenant/cashier/draws/{drawId}/detail`
- Ajouter widget "Nimero cho"
- Ne jamais transmettre de `sellerTerminalId` depuis le client
- Gérer proprement le passage OPEN → CLOSED sans casser la page

---

## Non-goals

- **Ne pas migrer/backfiller** les données existantes de projection SELLER_TERMINAL.
- **Ne pas implémenter** les 6 ruleKeys non exposés (`MAX_TICKET_COUNT_PER_AGENT_PER_WINDOW`, etc.).
- **Ne pas supprimer** les anciennes routes Limits — elles restent accessibles hors navigation principale.
- **Pas de reset** de l'interface `UpsertLimitDialog` existante — elle reste pour la vue avancée `/limits/draw`.
- **Pas de mise min/max** par jeu dans ce change (`TenantGame`, pas limitpolicy).
- **Ne pas fusionner** les statistiques/tickets existants de `SellerTerminalDrawReportPage` dans le nouveau BFF POS.
- **Ne pas exposer** les scores internes TENANT=10 / DRAW_CHANNEL=30 / SELLER_TERMINAL=60 dans l'UI.

---

## Tests / critères d'acceptation

### Backend — unitaires (couverture 100 % du code nouveau et modifié)

**Projection SELLER_TERMINAL** (`ExposureProjectorAdapterTest`)
- Event avec `sellerTerminalId` → 3 scopes projetés (TENANT, DRAW_CHANNEL, SELLER_TERMINAL).
- Event sans `sellerTerminalId` → 2 scopes seulement.
- Replay du même `eventId` → aucun double comptage sur aucun scope.

**Cascade override** (`LimitResolverTest`)
- TENANT=500 + DRAW_CHANNEL=300 + SELLER_TERMINAL=200 → résolu à 200.
- TENANT=500 + DRAW_CHANNEL=300, sans SELLER_TERMINAL → résolu à 300.
- TENANT=500 uniquement → résolu à 500.
- Aucune règle configurée → résultat null, pas d'erreur.

**BFF query handlers**
- `PosDrawDetailHandler` appelle `ctx.sellerTerminalIdRequired()` — aucun paramètre client accepté.
- Draw CLOSED → payload avec `exposure.active = false`, `topSelections` présent.

### Backend — Spring IT (Testcontainers, même base que `BusinessRuntimeIntegrationTestBase`)

**Cas 1 — Bloquer un numéro**
- Configurer `BLOCK_SELECTION_PER_DRAW` scope TENANT pour la sélection "34".
- Vente avec "34" → `SellTicketOutcome` REJECTED, `BreachOutcome.ruleKey = BLOCK_SELECTION_PER_DRAW`.
- Aucun ticket persisted.

**Cas 2 — Override de scope (TENANT → DRAW_CHANNEL → SELLER_TERMINAL)**
- Configurer `MAX_STAKE_PER_LINE` : TENANT=500, DRAW_CHANNEL=300, SELLER_TERMINAL=100.
- Vente à 150 HTG → REJECTED, limite effective = 100 (SELLER_TERMINAL l'emporte).
- Vente à 50 HTG → APPROVED.

**Cas 3 — Exposition cumulative**
- Configurer `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW` scope DRAW_CHANNEL, limite = 1 000 HTG, sélection "12".
- 9 ventes à 100 HTG → APPROVED, compteur DRAW_CHANNEL = 900.
- 1 vente à 200 HTG → REJECTED (dépasserait 1 000), compteur inchangé à 900.
- Replay d'un event déjà projeté → compteur inchangé (idempotence).

### Web — e2e Playwright (`apps/web-e2e/src/admin-portal/`)

**Navigation** (`limits-nav-simplification.spec.ts`)
- `limits-global`, `limits-draw`, `limits-seller` absents du sidenav (section limits et section sellers).
- `/limits` (overview) et `/limits/number` présents et navigables.
- Routes avancées (`/limits/global`, `/limits/draw`) directement routables → pas de redirect.
- Testé à 360 dp et 1 280 dp. Sélecteurs sur `href` (stable, indépendant de la locale).

**LimitPolicyBlockComponent** (`limit-policy-block.spec.ts`)
- Champ vide → valeur héritée et provenance affichées.
- Saisie d'une valeur → override émis ; effacement → retour état hérité.
- Viewport 360 dp : chaque ruleKey sur une ligne, pas d'overflow horizontal.

**Bloke nimero dans le draw detail** (`draw-detail-block-number.spec.ts`)
- Draw OPEN → bouton visible, clic ouvre dialog avec channel pré-sélectionné, pas de picker draw.
- Draw CLOSED → bouton absent.

**Numéros à risque dans le draw detail** (`draw-detail-exposure.spec.ts`)
- `exposureAlerts` non vide dans la réponse BFF → section visible avec ratio affiché.
- `exposureAlerts` vide ou `limitConfigured: false` → section absente.

### Backend — e2e Python (`testing/e2e/tests/business_critical/test_limit_policy_scenarios.py`)

Tests contre un serveur réel (pytest + httpx, marqueurs `L2 / business_critical / slow`). Même pattern que `test_business_day_scenarios.py` : provision via API, draw ouvert, assertions HTTP directes.

**Cas 1 — Bloquer un numéro**
- Admin configure `BLOCK_SELECTION_PER_DRAW` scope TENANT pour "34".
- Seller terminal vend "34" → 422 / outcome REJECTED.
- Aucun ticket créé. `GET /admin/draws/{drawId}/overview` → pas de stake sur "34".

**Cas 2 — Override de scope**
- Configurer `MAX_STAKE_PER_LINE` : TENANT=500, DRAW_CHANNEL=300, SELLER_TERMINAL=100.
- Vente à 150 HTG → REJECTED, `effectiveLimit = 100`, `resolvedScope = SELLER_TERMINAL`.
- Vente à 50 HTG → APPROVED.
- `GET /admin/draws/{drawId}/overview` → `effectiveLimits` confirme la résolution.

**Cas 3 — Exposition cumulative**
- Configurer `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW` scope DRAW_CHANNEL, limite = 1 000 HTG, sélection "12".
- 9 ventes à 100 HTG → APPROVED ; `GET /tenant/cashier/draws/{drawId}/detail` → ratio ≈ 0.90.
- 10e vente à 200 HTG → REJECTED ; compteur inchangé à 900.
- Aucun `sellerTerminalId` client requis — résolu depuis le Request Context.

### Perf — Locust (`testing/e2e/loadtest/`)

- **`CashierUser`** : ajouter tâche `read_draw_detail` (`@task(1)`, tag `read`) — `GET /tenant/cashier/draws/{drawId}/detail`. Objectif : p95 < 300 ms à 20 users.
- **`AdminUser`** (à créer si absent, `weight = 1`) : tâche `read_draw_overview` — `GET /admin/draws/{drawId}/overview`. Objectif : p95 < 500 ms.
- Critère de non-régression : p95 de `sell_basket` inchangé à ± 10 % avant/après.

---

## Context packs

- `openspec/context/10-non-negotiables.md`
- `openspec/context/30-frontend-rules.md`
- `openspec/context/20-backend-rules.md`

## Near-code références

- `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/limitpolicy/`
- `tchalanet-web (worktree admin-ux-v1)/apps/admin-portal/src/app/features/limits/`
- `tchalanet-web (worktree admin-ux-v1)/apps/admin-portal/src/app/features/draws/pages/detail/`
