# Signaler un problème

!!! warning "Environnement de validation"
    **Environnement : STAGING**  
    **Données : tests uniquement**  
    Aucun ticket ou montant n'a de valeur réelle.  
    **Version testée : à renseigner avant livraison**  
    **Dernière mise à jour : 2026-07-23**

Utilisez ce format pour que l'équipe puisse reproduire et corriger rapidement.
Un signalement incomplet ralentit souvent la correction ; l'objectif est de
donner assez de contexte sans écrire un long rapport.

## Format attendu

```text
Titre :

Environnement : STAGING
Rôle :
Scénario :
Date et heure :
Application : Portail admin / Mobile POS
Version application :
Appareil :
OS / navigateur :
Utilisateur ou terminal :
Code ticket :
Trace ID ou message erreur :
Sévérité :

Préconditions :

Étapes exécutées :
1.
2.
3.

Résultat attendu :

Résultat obtenu :

Évidence jointe :
- capture
- photo reçu
- code ticket
- message exact
```

## Exemple rempli

```text
Titre : POS-SALE-01 — le reçu affiche un montant différent

Environnement : STAGING
Rôle : Vendeur/POS
Scénario : POS-SALE-01
Date et heure : 2026-07-26 10:42
Application : Mobile POS
Version application : 1.0.0-stg
Appareil : Samsung A15
OS / navigateur : Android 15
Utilisateur ou terminal : POS-TEST-02
Code ticket : 40CP-JBMR
Trace ID ou message erreur : aucun message
Sévérité : Haut

Préconditions :
- Terminal connecté
- Tirage ouvert
- Mise de test 100

Étapes exécutées :
1. Ouvrir le tirage NY Midday.
2. Ajouter une mise de 100.
3. Confirmer la vente.
4. Ouvrir le reçu.

Résultat attendu :
Le reçu affiche un total de 100.

Résultat obtenu :
Le reçu affiche un total de 110.

Évidence jointe :
- capture du panier avant confirmation
- photo du reçu
- code ticket 40CP-JBMR
```

## Sévérité proposée

| Niveau | Quand l'utiliser |
|---|---|
| Bloquant | Impossible de se connecter, vendre, confirmer ou accéder à un parcours principal |
| Haut | Action importante cassée mais contournement possible |
| Moyen | Incohérence, libellé confus, mauvais statut, filtre incorrect |
| Bas | Texte, alignement, détail visuel non bloquant |

## Bon signalement

Un bon signalement permet de répondre à trois questions :

1. Qui a testé ?
2. Qu'a-t-il fait exactement ?
3. Qu'a-t-il vu à la place du résultat attendu ?

## À éviter

- "Ça ne marche pas" sans scénario.
- Capture sans heure ni rôle.
- Modifier les données après l'erreur sans noter l'état initial.
- Rejouer plusieurs fois avec des comptes différents sans le préciser.
