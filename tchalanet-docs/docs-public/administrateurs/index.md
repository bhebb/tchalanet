# Guide Administrateurs

!!! warning "Environnement de validation"
    **Environnement : STAGING**  
    **Données : tests uniquement**  
    Aucun ticket ou montant n'a de valeur réelle.  
    **Version testée : à renseigner avant livraison**  
    **Dernière mise à jour : 2026-07-23**

L'administrateur configure et exécute les opérations quotidiennes autorisées
pour son santral. Son objectif pendant la validation est de confirmer que le portail
admin permet de préparer la vente, suivre les tickets et consulter les résultats
sans ambiguïté.

L'administrateur ne gère pas toute la plateforme. Les actions cross-santral ou
provider global relèvent du super administrateur.

## Avant de commencer

Vérifiez que vous avez :

- un compte admin staging ;
- l'URL du portail admin ;
- au moins un santral ou tenant de test ;
- au moins un terminal POS de test ;
- un navigateur récent.

Si un accès manque, ouvrez un signalement avec le rôle `Administrateur` et l'étape
bloquante.

## Ce que l'administrateur valide

- Connexion au portail admin.
- Configuration générale visible et compréhensible.
- Jeux disponibles et paramètres nécessaires.
- Canaux de tirage configurés.
- Terminaux POS visibles.
- Résultats consultables ou saisissables selon permission.
- Tickets et rapports de base consultables.

## Parcours conseillé

Pour une version courte avec étapes visuelles, ouvrez
[Préparer un santral](../parcours/preparer-un-santral.md).

1. Ouvrir le portail admin staging.
2. Se connecter avec le compte fourni.
3. Vérifier que le tableau de bord s'affiche sans erreur.
4. Aller dans **Configuration générale**.
5. Vérifier les blocs obligatoires et optionnels.
6. Vérifier les jeux disponibles.
7. Vérifier les canaux de tirage.
8. Ouvrir les terminaux POS.
9. Ouvrir la section tickets.
10. Ouvrir les résultats.
11. Ouvrir un rapport de base.

## Résultats attendus par étape

| Étape | Ce que vous devez voir |
|---|---|
| Connexion | L'espace admin s'ouvre, votre profil ou avatar est visible |
| Configuration générale | Les étapes complétées, bloquantes et optionnelles sont compréhensibles |
| Jeux | Les jeux autorisés sont visibles, sans doublon évident |
| Canaux de tirage | Les canaux configurés affichent leur statut |
| Terminaux POS | Les terminaux disponibles ou à configurer sont listés clairement |
| Tickets | Les tickets récents sont consultables ou l'état vide est expliqué |
| Résultats | Chaque résultat a un statut lisible : provisoire, confirmé, manuel ou à corriger |
| Rapports | Les totaux de test s'affichent sans être présentés comme production |

## Préparer un santral prêt à vendre

Avant qu'un vendeur puisse utiliser le POS, l'administrateur doit vérifier que
le santral est prêt sur quatre points :

1. les informations générales sont complètes ;
2. les jeux vendus sont actifs ;
3. les canaux de tirage nécessaires sont configurés ;
4. au moins un terminal POS peut se connecter.

Si une page indique que la configuration est incomplète, l'écran doit expliquer
la prochaine action. Un bloc bloquant sans explication est un problème à
signaler.

## Configurer les jeux

Dans la section des jeux, vérifiez que chaque jeu vendu par le santral est clair
pour le vendeur.

| Élément | Ce que l'administrateur vérifie |
|---|---|
| Actif / inactif | Le jeu peut ou ne peut pas être vendu |
| Visible au POS | Le vendeur voit seulement les jeux qu'il doit vendre |
| Mise minimum | Une mise trop petite est refusée clairement |
| Mise maximum | Une mise trop grande est refusée clairement |
| Ordre d'affichage | Les jeux importants sont faciles à trouver |
| Horaires ou tirages | Le vendeur ne peut pas vendre sur un tirage fermé |

Un jeu peut exister dans le catalogue Tchalanet mais ne pas être actif pour un
santral. Dans ce cas, il ne doit pas apparaître comme vendable au POS.

## Configurer les options de pari

Certains jeux ont une seule option. D'autres proposent plusieurs façons de jouer.
L'administrateur doit vérifier que les options visibles correspondent à l'offre réelle du
santral.

| Jeu | Exemples d'options visibles pour le vendeur |
|---|---|
| Bolet / Borlette | Lot 1, Lot 2, Lot 3 selon l'offre |
| Maryaj | Exact, permuté |
| Loto 3 | Exact, box, exact + box |
| Loto 4 / Loto 5 | Options selon l'offre configurée |
| Maryaj gratis | Option gratuite générée ou choisie selon configuration |

Résultat attendu : le vendeur ne doit pas deviner. Si une option est disponible,
son nom doit être compréhensible et le comportement doit être cohérent au moment
de la vente.

## Configurer les barèmes

Les barèmes expliquent comment un ticket gagnant sera payé après résultat. Pour
la validation client, l'administrateur doit vérifier que les valeurs affichées sont
compréhensibles et rattachées au bon jeu.

| Type de barème | Exemple de lecture |
|---|---|
| Multiplicateur | Une mise de 10 avec multiplicateur 50 donne 500 si gagnante |
| Montant fixe | Une ligne gratuite gagnante paie le montant fixe configuré |

Point important : un changement de barème ne doit pas modifier les tickets déjà
vendus. Un ticket garde les règles visibles au moment de sa vente.

## Maryaj gratis

Si Maryaj gratis est activé, l'administrateur doit pouvoir répondre à ces questions :

| Question | Ce qu'il faut vérifier |
|---|---|
| La promotion est-elle active ? | Le statut est visible |
| Qui y a droit ? | Les conditions sont lisibles |
| Combien de lignes gratuites ? | La quantité est claire |
| Qui choisit le numéro ? | Le système, le vendeur ou le client selon la règle |
| Peut-on régénérer ? | Le nombre de régénérations autorisées est clair |

Résultat attendu : le vendeur ne doit pas appliquer la promotion à la main si
elle est automatique. La ligne gratuite doit apparaître au bon moment dans le
parcours POS.

## Résultats et tickets

Les résultats déterminent si les tickets vendus sont gagnants ou perdants.
Pendant la validation, l'administrateur doit surtout vérifier les statuts.

| Statut affiché | Ce que cela signifie pour l'administrateur |
|---|---|
| Provisoire | Visible, mais pas prêt pour règlement final |
| Confirmé | Validé et utilisable |
| Manuel | Saisi ou corrigé par une personne autorisée |
| À corriger | Incomplet ou incohérent, ne doit pas être finalisé |

Un résultat incomplet ne doit pas déclencher un règlement final. Si l'interface
permet de finaliser un résultat visiblement incomplet, signalez-le.

## Simulation et règles publiques

Si une page de simulation ou de règles est disponible, elle sert à expliquer les
jeux et les gains théoriques. Elle ne remplace pas le ticket réel ni le résultat
confirmé. Le montant officiel est celui calculé après résultat validé, à partir
du ticket vendu.

## Actions permises

| Action | Attendu |
|---|---|
| Voir la configuration générale | Les étapes complétées et bloquantes sont claires |
| Configurer les jeux autorisés | Les jeux disponibles s'affichent sans doublon |
| Configurer les canaux | Les canaux nécessaires sont visibles et sauvegardables |
| Consulter tickets | Les tickets récents sont filtrables |
| Consulter résultats | Les résultats ont un statut clair |

## Checklist avant d'ouvrir la vente

- Les informations générales du santral sont complètes.
- Les jeux vendus sont actifs et visibles au POS.
- Les options de pari visibles correspondent à l'offre.
- Les mises minimum et maximum sont configurées.
- Les canaux de tirage nécessaires sont actifs.
- Au moins un terminal POS peut se connecter.
- Les règles Maryaj gratis sont claires si la promotion est utilisée.
- Les vendeurs savent que les données staging ne sont pas réelles.

## Quand arrêter et signaler

Signalez immédiatement si :

- une page admin reste blanche ou charge indéfiniment ;
- une sauvegarde affiche un succès mais la donnée ne change pas ;
- un statut dit "bloqué" sans expliquer quoi faire ;
- un résultat peut être appliqué alors qu'il est incomplet ou provisoire ;
- un bouton dangereux est visible sans permission claire.

## Actions interdites ou à escalader

- Override global de résultats sans permission.
- Activation provider hors périmètre tenant.
- Modification de données cross-tenant.
- Suppression ou reset non demandé.

## Erreurs courantes à noter

| Symptôme | À joindre |
|---|---|
| Page bloquée ou vide | URL, heure, rôle, capture |
| Statut confus | Libellé exact et capture |
| Sauvegarde impossible | Message d'erreur, étape, données saisies |
| Résultat incohérent | Provider, tirage, heure, statut affiché |
