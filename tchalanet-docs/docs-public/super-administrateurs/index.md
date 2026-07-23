# Guide Super administrateurs

!!! warning "Environnement de validation"
    **Environnement : STAGING**  
    **Données : tests uniquement**  
    Aucun ticket ou montant n'a de valeur réelle.  
    **Version testée : à renseigner avant livraison**  
    **Dernière mise à jour : 2026-07-23**

Le super administrateur fait le support plateforme et les opérations qui
dépassent un seul santral. Pendant la validation, il confirme qu'un santral peut
être aidé sans confusion : état du santral, providers, résultats, statuts et
actions de correction autorisées.

Le super administrateur doit être prudent : ses actions peuvent toucher
plusieurs santrals ou modifier l'état opérationnel d'un résultat.

## Ce que le super administrateur valide

- Accès plateforme.
- Liste et état des santrals.
- Configuration provider.
- Statuts de résultats.
- Support tenant.
- Override encadré lorsque permis.

## Parcours conseillé

1. Se connecter au portail plateforme.
2. Ouvrir la liste des santrals.
3. Sélectionner un santral de test.
4. Vérifier providers activés et disponibles.
5. Vérifier résultats récents et statuts.
6. Simuler une action de support autorisée.
7. Vérifier l'audit visible après modification.

## Résultats attendus

| Zone | Ce que vous devez voir |
|---|---|
| Liste santrals | Le santral de test est retrouvable |
| Détail santral | L'état général et les prochaines actions sont compréhensibles |
| Providers | Les providers disponibles, actifs ou manuels sont distingués |
| Résultats | Le statut indique clairement si le résultat est provisoire, confirmé ou corrigé |
| Support | Toute action sensible demande une intention claire |
| Audit | Une modification visible affiche qui a changé quoi et quand |

## Actions permises

| Action | Attendu |
|---|---|
| Consulter santral | État et configuration lisibles |
| Vérifier provider | Disponible, actif ou manuel clairement distingué |
| Contrôler résultat | Statut cohérent et source visible |
| Override autorisé | Audit et statut mis à jour |

## Résultats et providers : lecture client

Un résultat automatique ne veut pas dire que le paiement est possible
immédiatement. Pour la validation client, retenez cette règle :

| Statut affiché | Sens pour le test |
|---|---|
| Provisoire | Résultat reçu ou saisi, mais pas encore prêt pour règlement final |
| Confirmé | Résultat validé et utilisable par le système |
| Corrigé manuellement | Résultat remplacé par une action autorisée, avec audit attendu |
| À corriger | Données incomplètes ou incohérentes, aucune action finale ne doit partir |

Si un résultat manque des lots attendus ou semble incomplet, il doit rester dans
un état visible mais non final. Le testeur ne doit pas considérer ce résultat
comme payable.

## Actions à éviter en test sans consigne

- Modifier un santral client hors jeu de test.
- Forcer settlement sans scénario prévu.
- Activer un provider non prévu.
- Effacer des données de résultats.

## Évidence utile

Pour tout override ou support : noter tenant, provider, tirage, ancien statut,
nouveau statut, heure et utilisateur affiché.

## Quand signaler

Signalez si :

- un résultat incomplet peut être confirmé sans avertissement ;
- une action d'override ne laisse pas d'audit visible ;
- un provider inactif produit quand même des tirages actifs ;
- un tenant affiche des données qui semblent appartenir à un autre tenant ;
- les statuts résultat, tirage et ticket ne racontent pas la même histoire.
