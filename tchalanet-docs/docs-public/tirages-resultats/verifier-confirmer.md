# Vérifier ou confirmer

Dans Tchalanet, **vérifier** et **confirmer** ne veulent pas dire la même chose.
Cette différence est importante pour éviter de régler des tickets sur un mauvais
résultat.

## Vérifier

Vérifier signifie regarder, comparer et constater.

Exemples :

- consulter un résultat reçu ;
- comparer les numéros avec la source officielle ;
- vérifier qu'un tirage fermé a bien un résultat ;
- vérifier qu'un ticket affiche le bon statut ;
- vérifier qu'un provider est automatique ou manuel.

Vérifier ne doit pas déclencher de règlement final.

## Confirmer

Confirmer signifie valider qu'un résultat peut être utilisé par le système.

Une confirmation peut permettre les étapes suivantes :

- appliquer le résultat au tirage ;
- calculer les tickets gagnants ou perdants ;
- rendre les statuts finaux visibles ;
- préparer les rapports et paiements selon les règles.

## Règle simple

| Situation | Action attendue |
|---|---|
| Résultat reçu mais pas encore comparé | Vérifier |
| Résultat incomplet | Ne pas confirmer |
| Résultat contradictoire | Signaler ou corriger selon permission |
| Résultat manuel saisi par administrateur | Vérifier puis confirmer si autorisé |
| Résultat validé par super administrateur | Confirmer ou corriger selon scénario |

## Double confirmation

Pour les actions sensibles, le testeur doit vérifier qu'une double confirmation
est présente ou qu'une intention claire est demandée.

Exemples d'actions sensibles :

- confirmer un résultat ;
- corriger un résultat ;
- remplacer un résultat existant ;
- lancer une action qui déclenche le règlement.

La double confirmation doit rappeler le provider, le tirage, la date et l'impact
de l'action.
