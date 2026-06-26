# Tchalanet — Admin tenant V0 : sidenav, pages et actions métier

## Statut

Proposition V0 — basée sur le mapping actuel des endpoints backend et sur la vision produit validée pendant la discussion.

## Vision produit

L’admin tenant n’est pas un opérateur technique. C’est un gestionnaire qui veut surtout :

- suivre les ventes ;
- contrôler les vendeurs ;
- définir des limites ;
- configurer les jeux, tarifs, gains et commissions ;
- renseigner ou suivre les résultats ;
- analyser les performances par tirage, vendeur et période ;
- intervenir sur des cas exceptionnels comme un ticket à vérifier ou un tirage à annuler.

Les tickets individuels existent, mais ils ne sont pas le centre de l’expérience admin. L’admin ne veut pas contrôler chaque ticket un par un. Il veut comprendre rapidement ce qui se passe dans son espace.

Le vocabulaire doit donc rester simple :

| Terme technique | Libellé admin |
|---|---|
| tenant | Mon entreprise / Mon espace |
| seller terminal | Vendeur / Terminal vendeur |
| draw | Tirage |
| draw result | Résultat |
| odds | Gains à payer / Barème de gains |
| commissionRate | Commission |
| limit policy | Limite |
| ticket sell | Vente |
| lock draw | Bloquer la vente |
| unlock draw | Réouvrir la vente |
| manual result | Entrer résultat |

---

# 1. Sidenav Admin tenant V0

```text
Administration

Accueil

Configuration générale

Vendeurs
├── Liste des vendeurs
└── Nouveau vendeur

Tirages
├── Tous les tirages
├── Tirages en cours
├── Tirages passés
├── Matrice des tirages
└── Configuration des tirages

Limites
├── Limites système
├── Limite générale
├── Par vendeur
├── Par numéro
├── Par jeu
└── Par tirage

Contrôles de vente
├── Jeux & tarifs
├── Gains à payer
└── Commissions

Promotions
├── Maryaj gratis
└── Autres promotions

Rapports
├── Ventes
├── Vendeurs
├── Tirages
└── Exportations

Tickets
├── Liste des tickets
├── Vendre
└── Vérifier

Mon entreprise
├── Identité
├── Adresse
├── Apparence
├── Paramètres
└── Support

Aide
```

## Ordre de priorité V0

1. Accueil
2. Configuration générale
3. Vendeurs
4. Tirages
5. Limites
6. Contrôles de vente
7. Rapports
8. Promotions
9. Tickets
10. Mon entreprise
11. Aide

---

# 2. Accueil

## Objectif

L’accueil est le cockpit de gestion. Il doit répondre rapidement à cinq questions :

1. Combien l’espace a vendu aujourd’hui ?
2. Quels tirages sont ouverts ?
3. Quels tirages viennent de passer ?
4. Quels résultats manquent ?
5. Quels vendeurs performent ou posent problème ?

## Widgets recommandés

- Ventes aujourd’hui
- Tickets vendus
- Vendeurs actifs
- Tirages ouverts
- Résultats manquants
- Top vendeurs
- Alertes de limites
- Derniers tirages passés

## Exemple de rendu

```text
Accueil

Aujourd’hui
- Ventes : 42 300 HTG
- Tickets : 388
- Vendeurs actifs : 14
- Tirages ouverts : 3

À traiter
- 2 résultats manquants
- 1 vendeur proche de sa limite
- 1 tirage fermé sans résultat

Top vendeurs
1. Jean — 8 400 HTG
2. Marie — 7 900 HTG
3. Paul — 6 200 HTG
```

## Endpoints / controllers possibles

| Besoin | Endpoint | Controller |
|---|---|---|
| Vue admin | `GET /admin/overview` | `TenantAdminOverviewController` |
| KPIs tenant | `GET /tenant/reports/tenant-kpis` | `GetTenantKpisController` |
| Tirages du jour | `GET /admin/draws/today` | `DrawQueryAdminController` |
| Derniers tirages avec résultats | `GET /admin/draws/latest-with-results` | `DrawQueryAdminController` |
| Commission overview | `GET /admin/commission/overview` | `TenantAdminCommissionController` |

## Gap possible

Pour éviter plusieurs appels côté web, on pourrait ajouter plus tard un endpoint BFF :

```http
GET /admin/dashboard
```

Réponse attendue :

```json
{
  "todaySales": {},
  "openDraws": [],
  "recentDraws": [],
  "missingResults": [],
  "topSellers": [],
  "limitAlerts": []
}
```

---

# 3. Configuration générale

## Objectif

La configuration générale aide l’admin à rendre son espace prêt à fonctionner.

Elle doit présenter les étapes sous forme de checklist simple, comme dans l’écran actuel.

## Sections

| Section | Sens utilisateur | Action principale |
|---|---|---|
| Identité | Informations de base de l’espace | Voir / modifier |
| Adresse | Adresse de l’entreprise | Compléter |
| Jeux & tarifs | Jeux disponibles et prix de vente | Configurer |
| Tirages | Canaux de tirage vendus | Configurer |
| Tirages générés | Tirages créés dans le calendrier | Générer / vérifier |
| Apparence | Thème et couleurs | Choisir |
| Promotions | Maryaj gratis et autres promotions | Activer |
| Vendeurs | Création du premier vendeur | Créer vendeur |

## Libellés à utiliser

| Clé actuelle / technique | Libellé final |
|---|---|
| `admin.setup.title` | Configuration générale |
| `admin.setup.description` | Complétez les éléments nécessaires pour utiliser votre espace. |
| `admin.setup.section.identity` | Identité |
| `admin.setup.section.address` | Adresse |
| `admin.setup.section.games` | Jeux & tarifs |
| `admin.setup.section.drawChannels` | Tirages |
| `admin.setup.section.generatedDraws` | Tirages générés |
| `admin.setup.section.theme` | Apparence |
| `admin.setup.section.promotions` | Promotions |
| `advancedSettings` | Paramètres avancés |

## Endpoints / controllers

| Besoin | Endpoint | Controller |
|---|---|---|
| Jeux & tarifs setup | `GET /admin/setup/games-pricing` | `AdminSetupController` |
| Matrice tirages/ventes | `GET /admin/setup/draw-sales-matrix` | `AdminSetupController` |
| Config tenant | `GET /admin/tenant-config` | `AdminTenantConfigController` |
| Adresse | `GET /admin/tenant/address` | `AdminTenantController` |
| Thème | `GET /admin/theme` | `TenantThemeAdminController` |
| Jeux tenant | `GET /admin/games` | `TenantGameAdminController` |

---

# 4. Vendeurs

## Objectif

L’admin veut contrôler ses vendeurs : qui vend, combien, avec quelle commission, quelles limites, et quel statut.

## Sidenav

```text
Vendeurs
├── Liste des vendeurs
└── Nouveau vendeur
```

## Liste des vendeurs

### Colonnes recommandées

| Colonne | Valeur |
|---|---|
| Nom | Nom affiché du vendeur |
| Code vendeur | Code utilisé pour identifier le terminal |
| Téléphone | Téléphone optionnel |
| Statut | Actif / bloqué / à configurer |
| Ventes aujourd’hui | Montant vendu aujourd’hui |
| Tickets aujourd’hui | Nombre de tickets |
| Commission | Commission générale ou spécifique |
| Limite active | Limite du vendeur si configurée |
| Dernière activité | Dernière vente ou connexion |
| Actions | Menu ligne |

### Actions ligne

- Voir vendeur
- Modifier commission
- Configurer limites
- Reset PIN
- Bloquer / réactiver
- Voir performance
- Voir ventes du jour

## Nouveau vendeur

Champs V0 :

- Code vendeur / terminal
- Nom affiché
- Prénom
- Nom
- Téléphone
- Commission spécifique optionnelle
- PIN initial

## Endpoints / controllers

| Besoin | Endpoint | Controller |
|---|---|---|
| Liste vendeurs | `GET /admin/seller-terminals` | `SellerTerminalAdminController` |
| Créer vendeur | `POST /admin/seller-terminals` | `SellerTerminalAdminController` |
| Commissions vendeurs | `GET /admin/commission/sellers` | `TenantAdminCommissionController` |
| Limites vendeurs | `GET /admin/policies/limits/assignments` | `LimitPolicyAdminController` |

## Gaps probables à prévoir

```http
POST /admin/seller-terminals/{sellerTerminalId}/pin-reset
PATCH /admin/seller-terminals/{sellerTerminalId}/status
PUT /admin/commission/sellers/{sellerTerminalId}
GET /admin/seller-terminals/{sellerTerminalId}/performance
```

---

# 5. Tirages

## Objectif

La page Tirages est une page de gestion, pas seulement une table technique.

L’admin veut voir :

- les tirages en cours ;
- les tirages récemment passés ;
- les tirages à venir ;
- combien chaque tirage a vendu ;
- combien chaque vendeur a vendu pour un tirage ;
- les résultats manquants ;
- les actions disponibles : entrer résultat, bloquer vente, annuler, archiver.

## Sidenav

```text
Tirages
├── Tous les tirages
├── Tirages en cours
├── Tirages passés
├── Matrice des tirages
└── Configuration des tirages
```

## Liste des tirages

### Colonnes recommandées

| Colonne | Description |
|---|---|
| Tirage | Nom métier : Florida Soir, NY Matin, etc. |
| Date/heure | Heure du tirage |
| Statut | À venir, ouvert, fermé, résulté, annulé, archivé |
| Total vendu | Montant vendu sur ce tirage |
| Tickets | Nombre de tickets vendus |
| Vendeurs actifs | Nombre de vendeurs ayant vendu sur ce tirage |
| Résultat | Manquant, provisoire, confirmé |
| Actions | Menu ligne |

### Actions ligne

- Voir détail
- Entrer résultat
- Bloquer la vente
- Réouvrir la vente
- Annuler le tirage
- Archiver
- Exporter

### Actions bulk

Quand un ou plusieurs tirages sont sélectionnés :

- Bloquer la vente
- Réouvrir la vente
- Annuler
- Archiver
- Exporter

Les actions doivent être activées ou désactivées selon les statuts sélectionnés.

| Statut | Actions visibles |
|---|---|
| À venir | Voir, Annuler, Archiver |
| Ouvert | Voir, Bloquer la vente, Annuler si autorisé |
| Fermé | Voir, Entrer résultat, Annuler si autorisé, Archiver |
| Résulté | Voir, Exporter, Archiver |
| Annulé | Voir, Archiver |
| Archivé | Voir seulement |

## Détail tirage — exemple métier

```text
Détail tirage — Florida Soir — Hier 21:00

Résumé
- Total vendu : 18 900 HTG
- Tickets vendus : 202
- Vendeurs actifs : 11
- Gains potentiels : X HTG
- Résultat : manquant / confirmé / provisoire
- Statut : fermé

Actions
[Entrer résultat] [Bloquer la vente] [Annuler tirage] [Exporter]
```

### Résumé

| Indicateur | Valeur |
|---|---:|
| Total vendu | 18 900 HTG |
| Tickets vendus | 202 |
| Vendeurs actifs | 11 |
| Gains potentiels | X HTG |
| Résultat | Manquant / confirmé / provisoire |
| Statut | Fermé |

### Ventes par vendeur

| Vendeur | Montant vendu | Tickets | Commission estimée | Statut |
|---|---:|---:|---:|---|
| Jean | 4 200 HTG | 44 | 630 HTG | Actif |
| Marie | 3 800 HTG | 39 | 570 HTG | Actif |
| Paul | 2 100 HTG | 21 | 315 HTG | Actif |

### Sélections les plus jouées

| Sélection | Montant | Tickets | Exposition |
|---|---:|---:|---|
| 12-15-20 | 2 400 HTG | 18 | Élevée |
| 04-08-11 | 1 900 HTG | 14 | Moyenne |

### Onglets recommandés

```text
Résumé
Vendeurs
Sélections
Résultat
Tickets
Historique
```

L’onglet par défaut doit être **Résumé**. Les tickets sont secondaires.

### Actions du détail

| Action | Condition |
|---|---|
| Entrer résultat | Tirage fermé sans résultat |
| Bloquer la vente | Tirage ouvert |
| Réouvrir la vente | Tirage bloqué |
| Annuler tirage | Tirage à venir, ouvert ou fermé selon règles |
| Archiver | Tirage terminé, annulé ou ancien |
| Exporter | Toujours disponible si données présentes |

## Endpoints / controllers

| Besoin | Endpoint | Controller |
|---|---|---|
| Tous les tirages | `GET /admin/draws` | `DrawQueryAdminController` |
| Tirages du jour | `GET /admin/draws/today` | `DrawQueryAdminController` |
| Tirages à venir | `GET /admin/draws/upcoming` | `DrawQueryAdminController` |
| Derniers tirages avec résultats | `GET /admin/draws/latest-with-results` | `DrawQueryAdminController` |
| Détail tirage | `GET /admin/draws/{drawId}` | `DrawQueryAdminController` |
| Entrer résultat | `POST /admin/draws/{drawId}/manual-result` | `DrawAdminOpsController` |
| Annuler | `POST /admin/draws/{drawId}/cancel` | `DrawAdminOpsController` |
| Bloquer la vente | `POST /admin/draws/{drawId}/lock` | `DrawAdminOpsController` |
| Réouvrir la vente | `POST /admin/draws/{drawId}/unlock` | `DrawAdminOpsController` |
| Archiver | `POST /admin/draws/{drawId}/archive` | `DrawAdminOpsController` |
| Matrice tirages | `GET /admin/setup/draw-sales-matrix` | `AdminSetupController` |

## Gaps recommandés

Pour que la page détail ait de la valeur managériale, il faut ajouter des queries agrégées :

```http
GET /admin/draws/{drawId}/dashboard
GET /admin/draws/{drawId}/sales-summary
GET /admin/draws/{drawId}/seller-sales
GET /admin/draws/{drawId}/selection-exposure
```

L’option la plus propre est un BFF de page :

```http
GET /admin/draws/{drawId}/dashboard
```

Réponse :

```json
{
  "draw": {},
  "salesSummary": {
    "totalSold": "18900",
    "ticketCount": 202,
    "activeSellerCount": 11,
    "potentialPayout": null,
    "resultStatus": "MISSING"
  },
  "sellerBreakdown": [],
  "selectionExposure": [],
  "availableActions": []
}
```

---

# 6. Limites

## Objectif

Les limites sont un pilier de contrôle. Elles doivent être visibles directement dans le menu.

L’admin veut limiter :

- le total vendu dans son espace ;
- ce qu’un vendeur peut vendre ;
- une sélection trop jouée ;
- un numéro spécifique ;
- un jeu ;
- un tirage.

## Sidenav

```text
Limites
├── Limites système
├── Limite générale
├── Par vendeur
├── Par numéro
├── Par jeu
└── Par tirage
```

## Explication des sections

| Section | Sens utilisateur | Modification |
|---|---|---|
| Limites système | Règles imposées par la plateforme | Lecture seule |
| Limite générale | Limite générale de mon espace | Modifiable |
| Par vendeur | Limite spécifique à un vendeur | Modifiable |
| Par numéro | Limite sur un numéro ou une sélection | Modifiable |
| Par jeu | Limite par jeu | Modifiable |
| Par tirage | Limite sur un tirage précis | Modifiable |

## Exemples de libellés

```text
Limite générale
- Maximum vendu par tirage : 100 000 HTG

Par vendeur
- Jean : maximum 20 000 HTG par jour

Par numéro
- Numéro 12 : maximum 10 000 HTG par tirage

Par sélection
- 12-15-20 : maximum 5 000 HTG sur Florida Soir

Par jeu
- Pick 3 : maximum 50 000 HTG par tirage

Par tirage
- Florida Soir : maximum 80 000 HTG
```

## Endpoints / controllers

| Besoin | Endpoint | Controller |
|---|---|---|
| Règles disponibles | `GET /admin/policies/limits/rules` | `LimitPolicyAdminController` |
| Affectations de limites | `GET /admin/policies/limits/assignments` | `LimitPolicyAdminController` |
| Créer / modifier limite | `PUT /admin/policies/limits/assignments` | `LimitPolicyAdminController` |

## Routing UI recommandé

Même page, filtrée par type :

```text
/app/admin/limits?scope=system
/app/admin/limits?scope=global
/app/admin/limits?scope=seller
/app/admin/limits?scope=number
/app/admin/limits?scope=game
/app/admin/limits?scope=draw
```

---

# 7. Contrôles de vente

## Objectif

Cette section regroupe les règles qui déterminent :

- ce qu’on vend ;
- combien on paie si le client gagne ;
- combien on paie aux vendeurs.

Il n’y a pas de notion de **prime** en V0. On ne l’affiche donc pas.

## Sidenav

```text
Contrôles de vente
├── Jeux & tarifs
├── Gains à payer
└── Commissions
```

---

## 7.1 Jeux & tarifs

### Signification

Jeux & tarifs = ce que l’espace vend et à quel prix.

Exemples :

- Pick 3 activé ;
- Pick 4 activé ;
- Maryaj activé ;
- tirages disponibles ;
- mise minimum ;
- mise maximum par ticket ;
- prix ou montant de base par jeu.

### Endpoints / controllers

| Besoin | Endpoint | Controller |
|---|---|---|
| Jeux tenant | `GET /admin/games` | `TenantGameAdminController` |
| Catalogue jeux | `GET /admin/games/catalog` | `TenantGameAdminController` |
| Setup jeux & pricing | `GET /admin/setup/games-pricing` | `AdminSetupController` |
| Matrice jeux/tirages | `GET /admin/setup/draw-sales-matrix` | `AdminSetupController` |

---

## 7.2 Gains à payer

### Signification

Gains à payer = les odds, mais exprimées en langage admin.

```text
Si un client gagne, combien l’espace doit lui payer ?
```

Mapping métier :

| Concept admin | Concept technique |
|---|---|
| Barème général | Odds globales du tenant |
| Exceptions par vendeur | Odds spécifiques par seller-terminal |

### Exemple

```text
Barème général
- Pick 3 direct : 10 HTG misé → 5 000 HTG payé
- Pick 3 box : 10 HTG misé → 800 HTG payé

Exception par vendeur
- Jean utilise le barème spécial A
- Marie utilise le barème général
```

### Endpoints / controllers

| Besoin | Endpoint | Controller |
|---|---|---|
| Lire barème tenant | `GET /admin/controls/odds` | `PricingOverrideAdminController` |

### Gaps probables

Si on veut gérer odds globales + odds par vendeur, il faudra probablement exposer :

```http
GET /admin/controls/odds/default
PUT /admin/controls/odds/default
GET /admin/controls/odds/seller-overrides
PUT /admin/controls/odds/sellers/{sellerTerminalId}
DELETE /admin/controls/odds/sellers/{sellerTerminalId}
```

---

## 7.3 Commissions

### Signification

Commissions = ce que l’espace donne aux vendeurs.

Mapping métier :

| Concept admin | Concept technique |
|---|---|
| Commission générale | Commission par défaut du tenant |
| Commission par vendeur | Commission spécifique du seller-terminal |

### Exemple

```text
Commission générale : 15 %

Commissions par vendeur
- Jean : 15 %
- Marie : 12 %
- Paul : 18 %
```

### Endpoints / controllers

| Besoin | Endpoint | Controller |
|---|---|---|
| Vue commission | `GET /admin/commission/overview` | `TenantAdminCommissionController` |
| Modifier commission générale | `PUT /admin/commission/default-rate` | `TenantAdminCommissionController` |
| Lister commissions vendeurs | `GET /admin/commission/sellers` | `TenantAdminCommissionController` |

### Gap probable

Pour modifier la commission d’un vendeur directement depuis l’écran commission :

```http
PUT /admin/commission/sellers/{sellerTerminalId}
```

ou via une action du controller `SellerTerminalAdminController` si la commission reste une propriété du seller-terminal.

---

# 8. Promotions

## Sidenav

```text
Promotions
├── Maryaj gratis
└── Autres promotions
```

## Maryaj gratis

Promotion métier forte, donc entrée dédiée.

| Besoin | Endpoint | Controller |
|---|---|---|
| Voir promotions | `GET /admin/promotions/campaigns` | `PromotionCampaignAdminController` |
| Activer Maryaj gratis | `POST /admin/promotions/campaigns/templates/default-maryaj-gratis/instantiate` | `PromotionCampaignAdminController` |

## Autres promotions

| Besoin | Endpoint | Controller |
|---|---|---|
| Lister campagnes | `GET /admin/promotions/campaigns` | `PromotionCampaignAdminController` |
| Créer campagne | `POST /admin/promotions/campaigns` | `PromotionCampaignAdminController` |
| Ajouter règle | `POST /admin/promotions/campaigns/{campaignId}/rules` | `PromotionRuleAdminController` |

## Actions attendues

- Voir promotion
- Créer promotion
- Activer Maryaj gratis
- Ajouter règle
- Suspendre / désactiver si endpoints lifecycle ajoutés
- Archiver si endpoint ajouté

---

# 9. Rapports

## Objectif

Les rapports doivent aider l’admin à comprendre la performance de son espace, pas à lire des logs.

## Sidenav

```text
Rapports
├── Ventes
├── Vendeurs
├── Tirages
└── Exportations
```

## Rapports ventes

| Besoin | Endpoint | Controller |
|---|---|---|
| Ventes par période et jeu | `GET /tenant/reports/sales-by-period-and-game` | `GetSalesReportByPeriodAndGameController` |
| KPIs tenant | `GET /tenant/reports/tenant-kpis` | `GetTenantKpisController` |

## Rapports vendeurs

Sources actuelles possibles :

| Besoin | Endpoint | Controller |
|---|---|---|
| Liste vendeurs | `GET /admin/seller-terminals` | `SellerTerminalAdminController` |
| Commissions vendeurs | `GET /admin/commission/sellers` | `TenantAdminCommissionController` |
| KPIs tenant | `GET /tenant/reports/tenant-kpis` | `GetTenantKpisController` |

Gap possible :

```http
GET /admin/reports/seller-performance
```

## Rapports tirages

Sources actuelles possibles :

| Besoin | Endpoint | Controller |
|---|---|---|
| Tirages | `GET /admin/draws` | `DrawQueryAdminController` |
| Tirages récents avec résultats | `GET /admin/draws/latest-with-results` | `DrawQueryAdminController` |
| KPIs tenant | `GET /tenant/reports/tenant-kpis` | `GetTenantKpisController` |

Gaps possibles :

```http
GET /admin/reports/draw-sales
GET /admin/draws/{drawId}/sales-summary
GET /admin/draws/{drawId}/seller-sales
```

---

# 10. Tickets

## Objectif

Tickets est une section secondaire pour l’admin.

Elle sert à :

- rechercher un ticket ;
- vérifier un ticket ;
- vendre depuis l’espace admin si l’option est activée ;
- aider un client en cas de plainte.

## Sidenav

```text
Tickets
├── Liste des tickets
├── Vendre
└── Vérifier
```

## Mapping

| Lien | Endpoint | Controller |
|---|---|---|
| Liste des tickets | `GET /tenant/tickets` | `TicketQueryController` |
| Vendre | `POST /tenant/tickets` | `TicketSalesController` |
| Préparer vente | `POST /tenant/sales/preparations` | `SalePreparationController` |
| Vérifier public | `POST /public/tickets/verify` | `TicketVerifyController` |
| Vérifier POS | `POST /tenant/cashier/tickets/verify` | `PosTicketsController` |

## Décision V0

La vente depuis l’espace admin est utile, mais pas prioritaire. Elle peut être visible seulement si la permission ou la feature est activée.

---

# 11. Mon entreprise

## Objectif

Permettre à l’admin de gérer les informations de son entreprise/espace.

## Sidenav

```text
Mon entreprise
├── Identité
├── Adresse
├── Apparence
├── Paramètres
└── Support
```

## Mapping

| Lien | Endpoint | Controller |
|---|---|---|
| Identité | `PUT /admin/tenant` | `AdminTenantController` |
| Adresse | `GET /admin/tenant/address` | `AdminTenantController` |
| Adresse | `PUT /admin/tenant/address` | `AdminTenantController` |
| Apparence | `GET /admin/theme` | `TenantThemeAdminController` |
| Apparence presets | `GET /admin/theme/presets` | `TenantThemeAdminController` |
| Appliquer thème | `POST /admin/theme/preset` | `TenantThemeAdminController` |
| Modifier apparence | `PATCH /admin/theme/settings` | `TenantThemeAdminController` |
| Désactiver thème | `DELETE /admin/theme` | `TenantThemeAdminController` |
| Paramètres | `GET /admin/tenant-config` | `AdminTenantConfigController` |
| Documents | `GET /admin/tenant-config/document` | `AdminTenantConfigController` |
| Paramètres internes | `PUT /admin/tenant-config/internal-settings` | `AdminTenantConfigController` |
| Support / communication | `GET /admin/tenant-config/communication` | `AdminTenantConfigController` |

## Gap probable

Pour modifier clairement les informations de support/communication :

```http
PUT /admin/tenant-config/communication
```

---

# 12. Aide

## Objectif

Section simple pour l’admin :

- comprendre les concepts ;
- contacter le support ;
- lire les guides de base.

## Contenu recommandé

```text
Aide
├── Comment créer un vendeur ?
├── Comment définir une limite ?
├── Comment entrer un résultat ?
├── Comment vérifier un ticket ?
└── Contacter le support
```

---

# 13. Actions internes et sélection

## Principe

Le menu donne accès aux grandes pages. Les actions métier apparaissent dans la page, selon le contexte.

```text
Sidenav = navigation stable
Page = objet métier
Sélection = contexte d’action
Actions = disponibles selon statut, permission et nombre de lignes sélectionnées
```

## Exemple : sélection de tirages

Si l’admin sélectionne plusieurs tirages :

```text
3 tirages sélectionnés
[Annuler] [Bloquer la vente] [Réouvrir] [Archiver] [Exporter]
```

Chaque action doit être calculée selon les statuts sélectionnés.

## Exemple : sélection de vendeurs

```text
5 vendeurs sélectionnés
[Activer] [Bloquer] [Modifier commission] [Définir limite] [Exporter]
```

## Exemple : sélection de limites

```text
4 limites sélectionnées
[Modifier] [Désactiver] [Supprimer] [Exporter]
```

---

# 14. Registry front recommandé

Créer un registry d’actions par domaine.

Exemple :

```ts
export type DrawAction =
  | 'view'
  | 'manualResult'
  | 'cancel'
  | 'lock'
  | 'unlock'
  | 'archive'
  | 'export';

export interface DomainActionDefinition<TStatus extends string> {
  id: string;
  labelKey: string;
  icon: string;
  endpoint?: string;
  bulk: boolean;
  dangerous?: boolean;
  requiresReason?: boolean;
  allowedStatuses?: TStatus[];
  requiredPermissions?: string[];
}
```

Les pages utilisent ce registry pour afficher les actions disponibles.

---

# 15. Règles de présentation

## À faire

- Utiliser des noms simples : Vendeurs, Tirages, Limites, Ventes, Rapports.
- Définir chaque concept métier dans l’écran.
- Afficher les actions utiles dans les pages, pas dans le menu.
- Mettre les tickets comme outil secondaire.
- Mettre les limites comme section principale.
- Montrer les ventes par vendeur et par tirage.
- Afficher les résultats manquants comme élément à traiter.

## À éviter

- Afficher `odds`, `limit policy`, `draw result`, `batch`, `tenant`, `seller terminal` dans les libellés admin.
- Mettre les providers dans l’admin tenant.
- Faire de la liste des tickets la page principale.
- Créer une page pour chaque controller.
- Afficher des actions techniques sans explication.

---

# 16. Synthèse finale

Le menu admin V0 doit être orienté gestion :

```text
Accueil
Configuration générale
Vendeurs
Tirages
Limites
Contrôles de vente
Promotions
Rapports
Tickets
Mon entreprise
Aide
```

Le cœur de la valeur admin est :

```text
Contrôler les vendeurs
Contrôler les limites
Suivre les ventes par tirage
Suivre les ventes par vendeur
Renseigner les résultats manquants
Comprendre les performances
```

Le détail d’un tirage doit être une vraie page de gestion, avec ventes, vendeurs, sélections, exposition, résultat et actions. Les tickets restent disponibles, mais seulement comme onglet ou recherche secondaire.
