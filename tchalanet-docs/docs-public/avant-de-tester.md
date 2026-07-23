# Avant de tester

!!! warning "Environnement de validation"
    **Environnement : STAGING**  
    **Données : tests uniquement**  
    Aucun ticket, montant, reçu ou rapport n'a de valeur réelle.  
    **Version documentation : POC 2026-07-23**  
    **Versions admin/mobile : voir la fiche d'accès testeur**  
    **Dernière mise à jour : 2026-07-23**

Cette page prépare la séance de test. Lisez-la avant d'exécuter les scénarios.

Avant de commencer, complétez la [fiche d'accès testeur](validation/acces-testeur.md).

## Accès nécessaires

| Besoin | Qui le fournit | À vérifier |
|---|---|---|
| URL du portail admin | Équipe Tchalanet | La page de connexion s'ouvre |
| Compte administrateur | Équipe Tchalanet | Connexion possible au portail admin |
| Compte super administrateur | Équipe Tchalanet | Uniquement pour les tests support |
| App mobile POS | Firebase App Distribution | L'app s'installe et s'ouvre |
| Terminal POS de test | Administrateur ou équipe Tchalanet | Code terminal et PIN disponibles |
| Données de test | Équipe Tchalanet | Santral, tirages et jeux prêts |

## Règles importantes

- Ne testez pas en production sauf consigne explicite.
- Ne considérez jamais un ticket staging comme réel.
- Ne partagez pas de PIN ou compte dans une capture publique.
- Ne modifiez pas un tenant ou santral qui n'est pas prévu pour la validation.
- Si une étape bloque, notez l'état avant de tenter autre chose.

## Ordre conseillé

1. Administrateur : connexion et configuration générale.
2. Administrateur ou propriétaire : terminal POS visible et prêt.
3. POS : connexion terminal et changement de PIN.
4. POS : vente simple et reçu.
5. POS : consultation ou réimpression ticket.
6. Administrateur : tickets et rapport de base.
7. Administrateur ou super administrateur : résultats et statuts.

## Ce qu'il faut joindre en cas de problème

| Cas | Évidence utile |
|---|---|
| Problème de connexion | Heure, rôle, compte ou terminal, message exact |
| Problème de vente | Code terminal, tirage, mise, capture avant confirmation |
| Problème de reçu | Code ticket, photo du reçu, montant attendu |
| Problème admin | URL, page, action, message, capture |
| Problème résultat | Provider ou tirage, statut affiché, heure |

Utilisez le format complet : [Signaler un problème](validation/signaler-un-probleme.md).
