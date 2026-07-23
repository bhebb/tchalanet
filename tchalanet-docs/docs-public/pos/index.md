# Guide Vendeur / Terminal POS

!!! warning "Environnement de validation"
    **Environnement : STAGING**  
    **Données : tests uniquement**  
    Aucun ticket, reçu ou montant n'a de valeur réelle.  
    **Version testée : à renseigner avant livraison**  
    **Dernière mise à jour : 2026-07-23**

Le vendeur utilise le mobile POS pour vendre, imprimer ou réimprimer des tickets
et consulter l'activité autorisée du terminal. Pendant la validation, l'objectif
est de confirmer qu'un vendeur peut faire une vente simple de bout en bout sans
aide technique.

## Avant de commencer

Vérifiez que vous avez :

- l'application mobile staging installée ;
- un terminal de test actif ;
- un PIN temporaire ou un PIN déjà configuré ;
- un tirage ouvert ;
- une imprimante ou méthode de reçu si le test impression est demandé.

## Ce que le vendeur valide

- Installation ou ouverture de l'app mobile staging.
- Connexion terminal.
- Changement de PIN si demandé.
- Vente simple.
- Confirmation de vente.
- Impression ou reçu.
- Consultation et réimpression de ticket.

## Parcours conseillé

Pour une version courte avec étapes visuelles, ouvrez
[Vendre un ticket](../parcours/vendre-un-ticket.md).

1. Ouvrir l'app mobile POS.
2. Se connecter avec le terminal fourni.
3. Changer le PIN si l'app le demande.
4. Ouvrir un tirage disponible.
5. Ajouter une mise simple.
6. Confirmer la vente.
7. Vérifier le numéro de ticket.
8. Imprimer ou afficher le reçu.
9. Retrouver le ticket dans l'historique.
10. Réimprimer si l'action est disponible.

## Résultats attendus

| Étape | Ce que vous devez voir |
|---|---|
| Connexion | L'accueil POS s'ouvre avec le bon terminal |
| Changement PIN | Le nouveau PIN est accepté |
| Tirage | Un tirage vendable est visible |
| Mise | Le montant saisi est clair avant confirmation |
| Confirmation | Un numéro de ticket est créé |
| Reçu | Le reçu contient terminal, tirage, mises, total et code ticket |
| Historique | Le ticket vendu est retrouvable |
| Réimpression | Le reçu réimprimé correspond au même ticket |

## Actions permises

| Action | Attendu |
|---|---|
| Connexion terminal | Accès avec contexte terminal correct |
| Changement PIN | Nouveau PIN accepté, ancien refusé si retesté |
| Vente simple | Ticket créé avec montant exact |
| Impression | Reçu lisible avec terminal, tirage, mises |
| Consultation ticket | Ticket retrouvé par historique ou code |

## Erreurs courantes

| Symptôme | À noter |
|---|---|
| PIN refusé | Terminal utilisé, heure, message exact |
| Tirage fermé | Tirage, heure, capture |
| Impression impossible | Modèle appareil/imprimante, capture |
| Montant incorrect | Mise saisie, montant attendu, montant affiché |

## Quand arrêter le test

Arrêtez le scénario et signalez si :

- la vente est confirmée mais aucun code ticket n'apparaît ;
- le montant du reçu diffère du montant confirmé ;
- le ticket existe dans l'historique mais le reçu ne correspond pas ;
- l'app permet de vendre sur un tirage clairement fermé ;
- le terminal affiché n'est pas celui utilisé pour se connecter.
