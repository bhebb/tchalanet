# Plan V0 — Seller Terminal : navigation minimale, actions utiles et UX terminal

## 1. Vision

Le seller terminal n'est pas un admin. C'est un utilisateur de vente sur un terminal.

Il veut principalement :

- vendre rapidement ;
- voir si son terminal est prêt ;
- consulter ses statistiques simples ;
- vérifier un ticket ;
- réimprimer un ticket récent si autorisé ;
- changer son PIN si nécessaire.

Il ne doit pas gérer de configuration métier complexe. La configuration doit être faite par l'admin tenant ou par le superadmin en contexte tenant.

```text
Superadmin = plateforme, ops, référentiels, support, sécurité.
Admin tenant = gestion, contrôle, limites, vendeurs, ventes, tirages.
Seller terminal = vendre, vérifier, consulter, réimprimer.
```

## 2. Règles UX pour terminal

### 2.1 Minimum de navigation

Le terminal doit avoir très peu d'entrées.

```text
Terminal vendeur
├── Accueil
├── Vendre
├── Tickets
├── Tirages
├── Statistiques
└── Mon terminal
```

Option encore plus compacte pour mobile / petit écran :

```text
Bas de navigation
├── Accueil
├── Vendre
├── Tickets
└── Plus
```

Dans `Plus` :

```text
Plus
├── Tirages
├── Statistiques
├── Mon terminal
└── Aide
```

### 2.2 Pas de vocabulaire technique

À éviter côté vendeur :

```text
cashier
seller-terminal
runtime
readiness
odds
limit policy
batch
provider
slot
```

À utiliser :

```text
vendeur
terminal
prêt à vendre
ticket
tirage
vente
statistiques
PIN
```

### 2.3 Le terminal doit toujours guider l'utilisateur

Si le terminal n'est pas prêt, le vendeur ne doit pas voir une erreur technique. Il doit voir :

```text
Votre terminal n'est pas prêt à vendre.
Raison : aucun tirage ouvert pour le moment.
```

ou :

```text
Votre terminal est bloqué. Contactez votre administrateur.
```

## 3. Endpoints existants à utiliser

Les endpoints POS / terminal extraits du mapping actuel sont :

| Endpoint | Usage vendeur | Controller |
|---|---|---|
| `GET /tenant/cashier/home` | Accueil terminal | `PosHomeController` |
| `GET /tenant/cashier/readiness` | Vérifier si le terminal peut vendre | `PosHomeController` |
| `GET /tenant/cashier/draws/available` | Tirages disponibles à la vente | `PosDrawsController` |
| `GET /tenant/cashier/games/available` | Jeux disponibles à la vente | `PosGamesController` |
| `POST /tenant/cashier/tickets/preview` | Prévisualiser un ticket avant vente | `PosTicketsController` |
| `POST /tenant/cashier/tickets/sell` | Vendre un ticket | `PosTicketsController` |
| `POST /tenant/cashier/tickets/verify` | Vérifier un ticket | `PosTicketsController` |
| `GET /tenant/cashier/tickets` | Liste des tickets du terminal | `PosTicketsController` |
| `GET /tenant/cashier/tickets/stats` | Statistiques simples du vendeur | `PosTicketsController` |
| `GET /tenant/seller-terminal/me` | Infos du terminal vendeur | `SellerTerminalMeController` |
| `POST /tenant/seller-terminal/me/change-pin` | Changer le PIN | `SellerTerminalMeController` |
| `GET /tenant/seller-terminal/operational-context` | Contexte opérationnel du terminal | `CurrentOperationalContextController` |
| `GET /tenant/me/operational-context` | Alias contexte opérationnel | `CurrentOperationalContextController` |

> Note V0 : les endpoints contiennent encore `cashier`, mais l'UX doit afficher `vendeur` / `terminal vendeur`.

## 4. Sidenav / navigation seller terminal

## 4.1 Accueil

### Objectif

La page Accueil répond à :

- Est-ce que je peux vendre maintenant ?
- Combien ai-je vendu aujourd'hui ?
- Quels tirages sont ouverts ?
- Quels sont mes derniers tickets ?

### Page

```text
SellerHomePage
```

### Endpoints

```http
GET /tenant/cashier/home
GET /tenant/cashier/readiness
GET /tenant/cashier/tickets/stats
GET /tenant/cashier/draws/available
GET /tenant/seller-terminal/me
```

### Widgets utiles

```text
Statut du terminal
- Prêt à vendre / Bloqué / PIN à changer / Aucun tirage ouvert

Mes ventes aujourd'hui
- Montant vendu
- Nombre de tickets
- Tirages actifs

Actions rapides
- Vendre un ticket
- Vérifier un ticket
- Voir mes tickets

Derniers tickets
- Code
- Heure
- Montant
- Statut
```

### Actions

```text
Vendre
Vérifier un ticket
Voir mes tickets
Changer mon PIN si requis
```

### États à gérer

| État | Message UX | Action |
|---|---|---|
| Terminal prêt | `Votre terminal est prêt à vendre.` | Afficher bouton `Vendre` |
| Aucun tirage ouvert | `Aucun tirage ouvert pour le moment.` | Afficher tirages à venir si disponible |
| Terminal bloqué | `Votre terminal est bloqué. Contactez votre administrateur.` | Désactiver vente |
| PIN à changer | `Vous devez changer votre PIN avant de vendre.` | Rediriger vers `Changer PIN` |
| Hors connexion | `Connexion indisponible. Réessayez.` | V0 : pas de vente offline |

## 4.2 Vendre

### Objectif

Permettre au vendeur de vendre rapidement un ticket.

### Page

```text
SellerTicketSalePage
```

### Endpoints

```http
GET  /tenant/cashier/draws/available
GET  /tenant/cashier/games/available
POST /tenant/cashier/tickets/preview
POST /tenant/cashier/tickets/sell
```

### UX recommandée

Le flow doit être court :

```text
1. Choisir tirage
2. Choisir jeu
3. Entrer sélection(s)
4. Entrer mise
5. Prévisualiser
6. Confirmer vente
7. Afficher ticket + imprimer/réimprimer
```

### Écran de vente

```text
Vendre un ticket

Tirage
[Florida Soir - ouvert jusqu'à 20:55]

Jeu
[Pick 3] [Pick 4] [Maryaj]

Sélection
[12] [15] [20]

Mise
[50 HTG]

[Prévisualiser]
```

### Prévisualisation

La preview doit afficher :

```text
Tirage
Jeu
Sélection
Mise totale
Gains potentiels
Promotion appliquée, si Maryaj gratis
Commission non nécessaire côté vendeur, sauf décision produit
```

Actions :

```text
Confirmer vente
Modifier
Annuler
```

### Après vente réussie

```text
Ticket vendu
Code : ABC123
Montant : 50 HTG
Tirage : Florida Soir

[Imprimer]
[Réimprimer]
[Nouvelle vente]
[Voir ticket]
```

## 4.3 Tickets

### Objectif

Le vendeur doit pouvoir retrouver, vérifier et réimprimer ses tickets récents.

### Page

```text
SellerTicketsPage
```

### Endpoints

```http
GET  /tenant/cashier/tickets
POST /tenant/cashier/tickets/verify
```

### Sections

```text
Tickets récents
Recherche par code
Vérification ticket
```

### Liste des tickets

Colonnes simples :

```text
Heure
Code ticket
Tirage
Montant
Statut
Actions
```

Actions ligne :

```text
Voir
Vérifier
Réimprimer
```

### Réimpression

V0 : la réimpression doit être limitée.

Règles recommandées :

```text
Le vendeur peut réimprimer seulement ses propres tickets.
Le vendeur peut réimprimer seulement les tickets récents.
Chaque réimpression est auditée.
Le ticket réimprimé doit afficher la mention "Réimpression".
```

Endpoint à prévoir si absent :

```http
POST /tenant/cashier/tickets/{ticketId}/reprint
```

ou :

```http
POST /tenant/tickets/{ticketId}/reprint
```

Permission :

```text
seller_terminal.ticket.reprint_own
```

## 4.4 Tirages

### Objectif

Le vendeur veut savoir ce qui est ouvert à la vente.

Il ne gère pas les tirages. Il les consulte seulement.

### Page

```text
SellerAvailableDrawsPage
```

### Endpoint

```http
GET /tenant/cashier/draws/available
```

### Contenu

```text
Tirages ouverts
- Canal
- Heure de fermeture
- Jeux disponibles
- Statut
```

Exemple :

```text
Florida Soir
Ouvert jusqu'à 20:55
Jeux : Pick 3, Pick 4, Maryaj
[Vendre]
```

### Actions

```text
Vendre sur ce tirage
Actualiser
```

Pas d'action :

```text
Annuler
Bloquer
Entrer résultat
Configurer
```

Ces actions appartiennent à l'admin tenant.

## 4.5 Statistiques

### Objectif

Le vendeur veut voir ses performances simples.

### Page

```text
SellerStatsPage
```

### Endpoint

```http
GET /tenant/cashier/tickets/stats
```

### Contenu V0

```text
Aujourd'hui
- Montant vendu
- Nombre de tickets
- Moyenne par ticket
- Dernière vente

Par tirage
- Florida Soir : 4 500 HTG / 42 tickets
- New York Matin : 2 300 HTG / 21 tickets

Période simple
- Aujourd'hui
- Hier
- 7 derniers jours
```

### Ce qu'on n'affiche pas V0

```text
Rentabilité globale tenant
Commissions détaillées si sensible
Classement complet des vendeurs
Données des autres vendeurs
```

Le vendeur voit ses stats, pas celles des autres.

## 4.6 Mon terminal

### Objectif

Le vendeur doit voir les informations de son terminal et gérer son PIN.

### Page

```text
SellerTerminalProfilePage
```

### Endpoints

```http
GET  /tenant/seller-terminal/me
POST /tenant/seller-terminal/me/change-pin
GET  /tenant/seller-terminal/operational-context
```

### Contenu

```text
Nom du terminal
Code vendeur
Statut
Tenant / entreprise
Dernière activité
PIN à changer : oui/non
```

### Actions

```text
Changer mon PIN
Actualiser statut
Contacter administrateur
```

## 5. Actions internes seller terminal

## 5.1 Actions principales

| Page | Action | Endpoint |
|---|---|---|
| Accueil | Vendre | Navigation vers `Vendre` |
| Accueil | Vérifier ticket | Navigation vers `Tickets > Vérifier` |
| Vendre | Prévisualiser ticket | `POST /tenant/cashier/tickets/preview` |
| Vendre | Confirmer vente | `POST /tenant/cashier/tickets/sell` |
| Tickets | Lister tickets | `GET /tenant/cashier/tickets` |
| Tickets | Vérifier ticket | `POST /tenant/cashier/tickets/verify` |
| Tickets | Réimprimer | endpoint à prévoir |
| Tirages | Voir tirages ouverts | `GET /tenant/cashier/draws/available` |
| Statistiques | Voir stats | `GET /tenant/cashier/tickets/stats` |
| Mon terminal | Changer PIN | `POST /tenant/seller-terminal/me/change-pin` |

## 5.2 Actions interdites au vendeur

Le seller terminal ne doit pas pouvoir :

```text
créer un vendeur
modifier une commission
modifier une limite
annuler un tirage
bloquer / ouvrir un tirage
entrer un résultat
modifier les jeux et tarifs
voir les ventes des autres vendeurs
modifier une promotion
accéder aux opérations platform
```

## 6. Permissions V0 recommandées

```text
seller_terminal.me.read
seller_terminal.pin.change
pos.home.read
ticket.sell
ticket.read_own
ticket.verify
ticket.reprint_own
cashier.draws.read_available
cashier.games.read_available
cashier.stats.read_own
```

Si on veut garder les noms existants à court terme, on peut mapper :

```text
cashier.* = alias technique existant
seller_terminal.* = langage cible
```

## 7. UX terminal : composants prioritaires

## 7.1 TerminalStatusBanner

Affiche l'état du terminal.

```text
Prêt à vendre
PIN à changer
Terminal bloqué
Aucun tirage ouvert
Connexion indisponible
```

## 7.2 QuickSaleCard

Action principale visible sur Accueil.

```text
[Vendre un ticket]
```

## 7.3 RecentTicketsList

Liste courte des derniers tickets.

```text
Code
Heure
Montant
Action réimprimer
```

## 7.4 AvailableDrawCard

Carte tirage disponible.

```text
Nom du tirage
Heure de fermeture
Jeux disponibles
Bouton vendre
```

## 7.5 SellerStatsCard

Résumé simple.

```text
Vendu aujourd'hui
Tickets vendus
Dernière vente
```

## 8. Gaps backend utiles

D'après les endpoints actuels, les gaps probables pour une UX terminal complète sont :

## 8.1 Réimpression ticket

```http
POST /tenant/cashier/tickets/{ticketId}/reprint
```

ou :

```http
POST /tenant/tickets/{ticketId}/reprint
```

Règles :

```text
own ticket only
terminal actif
audit obligatoire
mention Réimpression
limite de temps ou compteur
```

## 8.2 Détail ticket vendeur

```http
GET /tenant/cashier/tickets/{ticketId}
```

ou recherche par code :

```http
GET /tenant/cashier/tickets/by-code/{ticketCode}
```

## 8.3 Stats par période

Si `GET /tenant/cashier/tickets/stats` ne prend pas de période, prévoir :

```http
GET /tenant/cashier/tickets/stats?from=2026-06-24&to=2026-06-24
```

## 8.4 Readiness enrichie

`GET /tenant/cashier/readiness` doit retourner des raisons lisibles :

```json
{
  "ready": false,
  "reasonCode": "NO_OPEN_DRAW",
  "message": "Aucun tirage ouvert pour le moment.",
  "blocking": true
}
```

## 9. Menu final V0

Version desktop/tablette :

```text
Terminal vendeur
├── Accueil
├── Vendre
├── Tickets
├── Tirages
├── Statistiques
└── Mon terminal
```

Version mobile / terminal compact :

```text
Bas de navigation
├── Accueil
├── Vendre
├── Tickets
└── Plus

Plus
├── Tirages
├── Statistiques
├── Mon terminal
└── Aide
```

## 10. Priorités V0

```text
1. Accueil terminal avec readiness + stats + action vendre
2. Vente ticket : preview + sell
3. Tickets : liste récente + vérifier + réimprimer
4. Tirages disponibles
5. Mon terminal : changer PIN
6. Statistiques simples
```

Le vendeur ne configure presque rien. Toute configuration importante reste côté admin tenant.

