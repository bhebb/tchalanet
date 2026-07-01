# Autres promotions

Cette page regroupe les promotions qui ne sont pas Maryaj gratis. Elles restent des campagnes promotionnelles génériques et ne doivent pas être confondues avec la configuration d'un jeu gratuit dédié.

## Objectif

Permettre au tenant de configurer des offres commerciales limitées dans le temps, applicables pendant la vente, sans modifier la définition catalogue des jeux.

## Types de promotions

| Type | Effet attendu | Exemple |
|---|---|---|
| Ligne gratuite | Ajoute une ligne promotionnelle | Jeu offert après condition d'achat. |
| Boost de gain | Modifie le gain potentiel sur des lignes ciblées | Cote bonifiée pendant une période. |
| Frais offerts | Annule une charge | Frais de service offerts. |

## Règles générales

- Une promotion doit avoir une période de validité.
- Une promotion doit exposer une condition d'éligibilité lisible.
- Une promotion ne doit pas afficher de JSON brut à l'admin.
- Le backend reste responsable de l'évaluation finale.
- Les effets appliqués doivent être traçables sur le ticket et dans les rapports.

## Expérience admin

L'admin doit pouvoir comprendre une promotion sans connaître les noms techniques backend.

L'écran doit afficher :

- nom de la campagne ;
- statut ;
- période ;
- priorité ;
- condition d'éligibilité ;
- effet appliqué ;
- résumé lisible avant activation.

## Expérience vendeur

Pendant la vente, une promotion peut être :

- invisible si elle s'applique automatiquement ;
- proposée si le vendeur ou le client doit accepter ;
- bloquée si elle nécessite une information manquante.

Le vendeur ne doit pas voir de payload technique. Les messages doivent expliquer l'action possible.

## Différence avec Maryaj gratis

Maryaj gratis a une page dédiée parce qu'il se comporte comme un jeu gratuit piloté par une règle commerciale :

- lignes Maryaj générées ;
- sélection automatique ou vendeur ;
- régénération éventuelle ;
- impact ticket et rapports plus visible.

Les autres promotions restent dans un écran générique tant qu'elles ne nécessitent pas un workflow métier spécialisé.
