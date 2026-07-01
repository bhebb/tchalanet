# Maryaj gratis

Maryaj gratis est une offre spéciale qui ajoute des lignes Maryaj gratuites à un ticket pendant la vente. Elle est pilotée par une configuration tenant, mais le calcul final doit rester côté backend pour garantir la cohérence entre POS, ticket imprimé, audit, rapports et settlement.

## Objectif métier

Permettre à un tenant d'offrir des lignes Maryaj gratuites selon une règle claire :

- un nombre fixe de lignes gratuites quand le ticket est éligible ;
- un nombre de lignes calculé par tranche régulière de montant payé ;
- ou un nombre de lignes calculé par paliers commerciaux.

Le vendeur ne doit pas calculer manuellement le nombre de lignes. L'interface affiche le total courant du ticket et le backend décide combien de lignes gratuites peuvent être générées.

## Parcours de vente

1. Le vendeur ajoute les lignes payantes du ticket.
2. À chaque ajout, modification ou suppression de ligne, l'interface recalcule et affiche le total des lignes payantes.
3. Le vendeur clique sur l'action `Maryaj gratis`.
4. Le backend reçoit le panier courant et évalue la règle Maryaj gratis.
5. Le backend retourne les lignes gratuites proposées ou générées.
6. Selon la configuration, les numéros sont générés automatiquement ou choisis par le vendeur.
7. Le vendeur peut régénérer les numéros si la configuration l'autorise.
8. Le ticket est confirmé avec les lignes payantes et les lignes Maryaj gratuites.

## Total affiché pendant la saisie

Le POS ou l'interface web doit afficher le total payé du panier en temps réel.

Ce total sert à informer le vendeur avant l'application de Maryaj gratis :

- total des lignes payantes ;
- nombre estimé de lignes gratuites si le mode est par tranche ;
- paliers applicables si le mode est par paliers ;
- plafond éventuel de lignes gratuites ;
- message clair si le ticket n'est pas encore éligible.

Le total affiché côté client est indicatif. Le backend recalcule toujours le total éligible au moment de l'application et de la confirmation.

## Modes d'attribution

### Quantité fixe

`quantityMode = FIXED`

La campagne génère un nombre fixe de lignes Maryaj gratuites quand le ticket respecte la condition d'éligibilité.

Exemple :

| Paramètre | Valeur |
|---|---:|
| `minPaidTotal` | 10 000 HTG |
| `quantity` | 5 |

Résultat :

- ticket de 9 999 HTG : aucune ligne Maryaj gratis ;
- ticket de 10 000 HTG ou plus : 5 lignes Maryaj gratis.

Le montant au-dessus du seuil ne change pas la quantité.

### Par tranche de montant payé

`quantityMode = PER_PAID_AMOUNT`

La campagne calcule les lignes gratuites à partir du total payé.

Exemple :

| Paramètre | Valeur |
|---|---:|
| `stepPaidAmount` | 1 000 HTG |
| `quantityPerStep` | 2 |
| `maxQuantity` | 10 |

Règle :

```text
freeLines = floor(paidTotal / stepPaidAmount) * quantityPerStep
freeLines = min(freeLines, maxQuantity)
```

Résultat :

| Total payé | Lignes Maryaj gratis |
|---:|---:|
| 999 HTG | 0 |
| 1 000 HTG | 2 |
| 2 500 HTG | 4 |
| 5 000 HTG | 10 |
| 6 000 HTG | 10 |

### Par paliers de montant payé

`quantityMode = TIERED_PAID_AMOUNT`

La campagne choisit la quantité à partir d'intervalles configurés. Ce mode correspond aux offres commerciales de type :

| Total payé | Lignes Maryaj gratis |
|---:|---:|
| 100 HTG à 199 HTG | 1 |
| 200 HTG à 499 HTG | 2 |
| 500 HTG et plus | 3 |

Les bornes sont inclusives. Une borne maximale vide signifie `et plus`.

Ce mode est recommandé quand la communication client utilise des paliers lisibles plutôt qu'une formule régulière.

## Paramètres fonctionnels

| Paramètre | Sens |
|---|---|
| `quantityMode` | Mode de calcul : `FIXED`, `PER_PAID_AMOUNT` ou `TIERED_PAID_AMOUNT`. |
| `quantity` | Nombre de lignes gratuites en mode fixe. |
| `minPaidTotal` | Montant payé minimum pour déclencher l'offre. |
| `stepPaidAmount` | Montant d'une tranche en mode par montant payé. |
| `quantityPerStep` | Nombre de lignes gratuites par tranche. Valeur par défaut : 1. |
| `maxQuantity` | Nombre maximum de lignes gratuites par ticket. |
| `quantityTiers` | Liste des paliers en mode par paliers : montant minimum, montant maximum optionnel, quantité. |
| `payoutBaseAmount` | Gain associé à chaque Maryaj gagnant. |
| `choiceMode` | Numéros générés automatiquement ou choisis par le vendeur. |
| `regenerableBeforeConfirm` | Autorise une nouvelle génération avant validation du ticket. |
| `maxRegenerationsBeforeConfirm` | Nombre maximum de régénérations autorisées. |

## Responsabilités

### Interface vendeur

- Affiche le total payé courant.
- Affiche l'estimation Maryaj gratis quand les paramètres sont connus.
- Déclenche l'action `Maryaj gratis`.
- Affiche les lignes gratuites retournées par le backend.
- Ne décide pas définitivement du nombre de lignes gratuites.

### Backend

- Recalcule le total payé éligible.
- Applique la règle active du tenant.
- Calcule la quantité finale de lignes gratuites.
- Génère ou valide les sélections Maryaj.
- Persiste les lignes promotionnelles avec leur traçabilité.
- Retourne les erreurs ou avertissements métier avec des codes stables.

### Ticket, rapports et audit

Les lignes Maryaj gratis doivent être visibles comme lignes promotionnelles :

- sur le ticket imprimé ;
- dans le détail du ticket ;
- dans les rapports de vente ;
- dans les rapports de promotion ;
- dans l'audit et les snapshots promotionnels.

## Règles de lisibilité UI

L'écran admin doit utiliser des libellés métier :

- `Gain par Maryaj gagnant`, pas `montant offert par ligne` ;
- `Nombre de Maryaj gratuits par ticket`, pas `quantity` ;
- `Par tranche de vente`, pas `PER_PAID_AMOUNT` ;
- `Par paliers de vente`, pas `TIERED_PAID_AMOUNT` ;
- `Régénérations autorisées`, avec une aide courte ;
- `Plafond de lignes gratuites`, pas `maxQuantity` seul.

L'écran vendeur doit rester orienté action :

- total payé ;
- offre disponible ;
- nombre de lignes gratuites ;
- bouton `Appliquer Maryaj gratis` ;
- bouton de régénération seulement si autorisé.

## Points à stabiliser

- Définir si Maryaj gratis utilise toujours une option de pari par défaut ou si l'admin peut choisir l'option.
- Porter `betOption` dans l'effet Maryaj si le backend doit générer des lignes avec une option explicite.
- Définir les statuts de campagne visibles : active, en pause, expirée, à configurer.
- Définir les libellés exacts pour les tickets imprimés et rapports.
