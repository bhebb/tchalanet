# Domaine `core.promotion` — Règles commerciales de jeux

## 1. Pourquoi ce domaine

Les besoins clients ne se limitent pas à `Maryaj gratuit`.

Exemples métier :

```text
- Si le client achète plus de 1000 HTG, il a droit à 1 Maryaj gratuit.
- Le 25 décembre avant midi, le premier lot paie 60x au lieu de 50x.
- Un tenant A paie le premier lot 50x, un tenant B paie 60x pendant une campagne.
- Aujourd'hui, 10 % de rabais sur Lotto 3.
- Bonus fixe de 100 HTG si le ticket gagne au premier lot.
- Commission boost pour certains vendeurs ou outlets.
```

Ces règles peuvent influencer :

```text
- le panier de vente ;
- le total payé ;
- les lignes gratuites ;
- l'exposition aux limites ;
- le settlement ;
- le payout ;
- les commissions ;
- les rapports et audits.
```

Elles ne doivent donc pas être modélisées comme un simple flag dans `catalog.game`.

## 2. Définition

Dans Tchalanet, une promotion est une règle commerciale versionnée qui peut modifier :

```text
- l'éligibilité ;
- le prix payé ;
- les lignes gratuites ;
- les multiplicateurs de payout ;
- les bonus fixes ;
- les commissions.
```

## 3. Frontière d'architecture

```text
catalog.game
  Définit les jeux : BOLET, MARYAJ, LOTTO_3, LOTTO_4, LOTTO_5.
  Peut indiquer si un jeu supporte des récompenses gratuites.
  Ne décide jamais qu'un client a droit à une promotion.

core.promotion
  Possède les règles commerciales, leur version, leur période d'activité,
  leurs conditions et leurs effets.
  Évalue un contexte et retourne une décision.

core.sales
  Appelle promotion en preview et confirmation de vente.
  Stocke les snapshots des effets applicables aux tickets/lignes.

core.limitpolicy
  Calcule l'exposition avec `effective_stake_amount` et les payout modifiers applicables.

core.settlement / core.drawresult
  Utilise les snapshots stockées sur les tickets/lignes pour calculer les gains.

core.payout
  Paie le montant final calculé.
```

## 4. Contrat stable

`core.promotion` expose un contrat stable :

```text
PromotionEvaluationContext -> PromotionDecision
```

Les consommateurs ne connaissent jamais le moteur interne. En V1, le moteur est `SimplePromotionRuleEngine`. En V2/V3, il pourra être remplacé par un moteur de règles sans modifier les domaines consommateurs.

## 5. Phases d'évaluation

```text
PREVIEW
  Montrer promotions disponibles, warnings, rabais, free lines possibles.

SALE_CONFIRMATION
  Revalider côté serveur et produire les snapshots à stocker.

SETTLEMENT
  Appliquer ou relire les snapshots métier pour le calcul des gains.

PAYOUT
  Lire le montant final calculé, audit/paiement.
```

## 6. Types d'effets canoniques

```text
FREE_GAME_LINE
  Ajoute une ligne gratuite, ex. Maryaj gratuit.

DISCOUNT_FIXED / DISCOUNT_PERCENT
  Réduit le montant payé.

PAYOUT_MULTIPLIER_OVERRIDE
  Remplace le multiplicateur normal par un autre multiplicateur.
  Exemple : premier lot 60x au lieu de 50x.

PAYOUT_MULTIPLIER_BOOST
  Ajoute un boost au multiplicateur de base.
  Exemple : +10x sur premier lot.

PAYOUT_FIXED_BONUS
  Ajoute un bonus fixe si la ligne gagne.

COMMISSION_MODIFIER
  Modifie la commission vendeur/outlet.
```

## 7. Multiplicateurs variables par tenant

Les multiplicateurs de payout promotionnels sont variables par tenant.

Règle :

```text
- une promotion appartient toujours à un tenant ;
- `effect_json` porte le multiplicateur ou boost applicable ;
- `ticket_line_applied_rule.effect_snapshot_json` stocke la valeur réellement appliquée ;
- settlement utilise la snapshot, jamais la règle active courante ;
- un changement de config ne modifie jamais les tickets déjà vendus.
```

Exemple snapshot :

```json
{
  "effectType": "PAYOUT_MULTIPLIER_OVERRIDE",
  "ruleCode": "CHRISTMAS_FIRST_PRIZE_60X",
  "ruleVersion": 3,
  "gameCode": "BOLET",
  "prizeRank": "FIRST",
  "baseMultiplier": 50,
  "appliedMultiplier": 60,
  "tenantId": "contextual",
  "appliedAt": "2026-12-25T15:30:00Z"
}
```

## 8. Montants canoniques à distinguer

```text
paid_amount
  Montant payé par le client.

effective_stake_amount
  Montant utilisé pour calculer exposition/gain.

base_payout_multiplier
  Multiplicateur normal du jeu/tenant.

applied_payout_multiplier
  Multiplicateur après règle promotionnelle.

promotion_value_amount
  Valeur commerciale offerte.

final_payout_amount
  Montant final payable si gagnant.
```

## 9. Snapshots obligatoires

Tout effet qui peut modifier un gain futur doit être snapshoté au moment de la vente.

```text
Bon : snapshot métier canonique.
Mauvais : snapshot de script Drools/DMN/SpEL ou JSON engine interne.
```

## 10. Offline

Par défaut : `offline_allowed = false`.

Les promotions time-sensitive, threshold-sensitive ou payout-sensitive sont online-only en V1.

V2 possible : snapshots signées de règles offline avec revalidation serveur au sync.
