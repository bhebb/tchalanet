# Configuration admin des jeux, barèmes et résultats

## Objectif

Ce guide explique comment un admin tenant configure les jeux vendus au POS :

- quels jeux sont actifs pour le tenant ;
- quelles options de pari sont offertes au vendeur ;
- quelles règles de gain sont appliquées à chaque option ;
- comment un seller-terminal peut avoir un override ;
- comment Maryaj gratis est configuré ;
- comment les résultats sont appliqués aux tickets ;
- où utiliser la page de simulation.

La règle centrale est simple : la vente ne calcule pas un gain potentiel. Elle fige les règles applicables au ticket. Le gain réalisé est calculé uniquement quand le résultat officiel est appliqué au draw.

## 1. Configurer les jeux du tenant

Dans l’admin, utilisez la page **Jeux & tarifs**.

Pour chaque jeu, configurez :

| Champ | Effet |
|---|---|
| Actif / inactif | Autorise ou interdit la vente du jeu pour le tenant. |
| Visible POS | Affiche ou masque le jeu pour les vendeurs. |
| Mise minimum | Mise minimale acceptée par ligne. |
| Mise maximum | Mise maximale acceptée par ligne. |
| Ordre d’affichage | Position du jeu dans le POS. |
| Horaires de vente | Fenêtre optionnelle, en plus du statut du draw. |

Un jeu peut être disponible dans le catalogue mais inactif pour un tenant. Dans ce cas, il ne doit pas être proposé à la vente.

## 2. Configurer les options de pari

Certains jeux ont une seule forme de pari. D’autres supportent plusieurs options commerciales.

Exemples :

| Jeu | Options possibles | Exemple vendeur |
|---|---|---|
| Borlette / Bolet | Lot 1, Lot 2, Lot 3 selon le catalogue | `12` en Lot 1 |
| Maryaj | Exact, permuté | `12 x 34` exact ou reverse |
| Loto 3 | Exact, box, exact + box | `123` exact ou box |
| Loto 4 / Loto 5 | Options selon le catalogue | sélection multi-chiffres |
| Maryaj gratis | Exact, permuté selon la config du jeu | ligne gratuite générée ou choisie |

Dans l’écran de configuration du jeu :

- activez seulement les options que le tenant veut vendre ;
- masquez du POS les options temporairement non offertes ;
- choisissez une option par défaut si le workflow vendeur doit être rapide ;
- utilisez une sélection explicite quand le vendeur doit choisir l’option ;
- utilisez l’option automatique uniquement quand le backend peut résoudre l’option sans ambiguïté.

Exemple : si `HT_MARYAJ` offre `Exact` et `Permuté`, l’admin peut rendre les deux visibles. Le vendeur sélectionne ensuite l’option au moment de la vente.

## 3. Configurer les règles de gain

Une règle de gain est configurée par jeu, type de pari, option et variante de pricing.

Deux types existent :

| Type | Usage |
|---|---|
| Multiplicateur de mise | Le gain réalisé = mise éligible x multiplicateur. |
| Montant fixe | Le gain réalisé = montant fixe configuré, indépendamment de la mise. |

Exemples :

| Configuration | Sens |
|---|---|
| `HT_BOLET / Lot 1 / multiplicateur 50` | Une mise de 10 HTG gagne 500 HTG si Lot 1 sort. |
| `HT_LOTO3 / Exact / multiplicateur 500` | Une mise de 20 HTG gagne 10 000 HTG si exact. |
| `HT_MARYAJ_GRATIS / Exact / montant fixe 2 000` | Une ligne Maryaj gratis exact gagnante paie 2 000 HTG. |
| `HT_MARYAJ_GRATIS / Permuté / montant fixe 10 000` | Une ligne Maryaj gratis permutée gagnante paie 10 000 HTG. |

Les jeux payants V0 utilisent des multiplicateurs. `HT_MARYAJ_GRATIS` utilise des montants fixes.

## 4. Overrides par seller-terminal

Un seller-terminal peut avoir une règle différente du tenant, sans changer le type de règle.

Exemple :

| Niveau | Jeu / option | Règle |
|---|---|---|
| Tenant | Maryaj gratis exact | montant fixe 1 000 HTG |
| Terminal Jean | Maryaj gratis exact | montant fixe 2 000 HTG |

Dans ce cas :

- les ventes faites par Jean après l’override utilisent 2 000 HTG ;
- les autres terminaux restent à 1 000 HTG ;
- les tickets vendus avant l’override gardent leur snapshot d’origine ;
- l’override ne peut pas transformer un montant fixe en multiplicateur.

La résolution effective est :

```text
override seller-terminal actif -> règle tenant -> erreur si absent
```

## 5. Maryaj gratis

Maryaj gratis est une campagne promotionnelle qui ajoute une ou plusieurs lignes `HT_MARYAJ_GRATIS` à un ticket éligible.

L’admin configure :

| Paramètre | Exemple |
|---|---|
| Campagne active / pause | Activer ou suspendre Maryaj gratis. |
| Éligibilité | Ticket payé minimum de 100 HTG. |
| Quantité | 1 ligne gratuite par ticket, ou par tranche. |
| Mode de choix | Généré automatiquement ou choisi par le vendeur. |
| Régénérations | Autoriser 3 régénérations avant confirmation. |
| Jeu cible | `HT_MARYAJ_GRATIS`. |

Le gain d’une ligne Maryaj gratis ne vient pas de la campagne. Il vient de la configuration pricing du jeu `HT_MARYAJ_GRATIS`.

Exemple complet :

1. Le tenant active Maryaj gratis pour tout le monde.
2. La campagne donne 1 ligne gratuite si le ticket payé atteint 100 HTG.
3. Le tenant configure `Exact = 1 000 HTG`.
4. Le terminal Jean override `Exact = 2 000 HTG`.
5. Un ticket vendu par Jean reçoit une ligne gratuite avec snapshot `Exact = 2 000 HTG`.
6. Un ticket vendu par un autre terminal reçoit la ligne avec snapshot `Exact = 1 000 HTG`.

## 6. Vente et snapshot

Au moment de la vente, le backend fige :

- le jeu ;
- le type de pari ;
- l’option commerciale ;
- la règle de gain effective ;
- les montants de mise et charges ;
- les informations de promotion appliquée.

Il ne calcule pas un gain potentiel.

Exemple de non-rétroactivité :

1. Le 3 juillet, `Lot 1 = x60`.
2. Un ticket est vendu le 3 juillet.
3. Le 4 juillet, l’admin change `Lot 1 = x50`.
4. Si on réimprime ou settle le ticket du 3 juillet, il garde `x60`.
5. Les tickets vendus après le changement utilisent `x50`.

## 7. Résultats et settlement

Les résultats suivent ce pipeline :

1. `core.drawresult` ingère ou reçoit un résultat.
2. Le résultat peut être `PROVISIONAL` ou `CONFIRMED`.
3. `core.draw` applique le résultat au draw fermé.
4. `core.sales` évalue les tickets du draw avec les snapshots de vente.
5. Le ticket devient gagnant ou perdant.
6. Le montant réalisé est persisté sur la ligne et sur le ticket.

Le settlement exige un résultat confirmé. Un résultat provisoire peut être affiché ou appliqué au draw, mais ne doit pas déclencher le paiement final.

Exemple Maryaj :

- Ticket : Maryaj `12 x 34`, option permutée, snapshot montant fixe 10 000 HTG.
- Résultat : `34 x 12`.
- Le backend reconnaît le reverse selon l’option snapshotée.
- Le gain réalisé devient 10 000 HTG.

## 8. Simulation des résultats

Le web expose deux surfaces distinctes :

| Surface | Usage |
|---|---|
| Admin résultat | Détail d’un résultat officiel, avec tabs Angular **Résultats**, **Combinaisons** et **Brut**. |
| Public `/rules` | Règles des jeux et simulation indicative pour expliquer les options et les gains théoriques. |

La surface admin est portée par `admin-draw-result-detail.page.html`. Elle assemble les composants Angular :

- `tch-console-draw-result-summary` pour le résumé métier ;
- `tch-console-draw-result-combinations` pour les combinaisons projetées ;
- `tch-console-draw-result-raw` pour le payload brut provider.

La surface publique est portée par `PublicRulesPage` sur la route `/rules`.

Cette page aide à comprendre les règles et à faire une simulation indicative. Elle ne remplace pas :

- le ticket réel ;
- les règles du seller-terminal ;
- le statut du draw ;
- le résultat confirmé ;
- le settlement backend.

L’admin peut l’utiliser pour expliquer les jeux, mais le montant payable officiel reste celui calculé par le backend après résultat confirmé, à partir du snapshot du ticket.

## 9. Checklist admin

Avant d’ouvrir la vente :

- chaque jeu vendu est actif et visible au POS ;
- les mises min/max sont configurées ;
- les options de pari visibles correspondent à l’offre commerciale ;
- chaque variante vendable a une règle de gain ;
- Maryaj gratis a une campagne active si l’offre est utilisée ;
- `HT_MARYAJ_GRATIS` a ses montants fixes configurés ;
- les overrides seller-terminal sensibles sont documentés ;
- les vendeurs savent que la simulation est indicative.
