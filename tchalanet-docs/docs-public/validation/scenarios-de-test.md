# Scénarios de test

!!! warning "Environnement de validation"
    **Environnement : STAGING**  
    **Données : tests uniquement**  
    Aucun ticket ou montant n'a de valeur réelle.  
    **Version testée : à renseigner avant livraison**  
    **Dernière mise à jour : 2026-07-23**

Cette page liste les scénarios minimum à exécuter pour valider la livraison
client. Chaque scénario doit être joué dans l'environnement staging, avec des
données de test uniquement.

Si un scénario échoue, arrêtez le scénario, notez l'évidence et ouvrez un
signalement. Ne corrigez pas les données au hasard pour continuer.

## Les 6 scénarios obligatoires

Ces scénarios doivent être joués au minimum pour valider la POC :

| Obligatoire | Scénario | Pourquoi |
|---|---|---|
| 1 | `ADMIN-LOGIN-01` | Confirmer l'accès admin. |
| 2 | `ADMIN-CONFIG-01` | Confirmer que le santral est lisible et configurable. |
| 3 | `ADMIN-TERMINAL-01` | Confirmer qu'un terminal POS peut être préparé. |
| 4 | `POS-PIN-01` | Confirmer la connexion terminal et le changement de PIN. |
| 5 | `POS-SALE-01` | Confirmer vente, double confirmation et reçu. |
| 6 | `ADMIN-RESULT-01` | Confirmer la lecture ou saisie résultat selon permission. |

## Format

| Champ | Description |
|---|---|
| ID | Identifiant stable du scénario |
| Rôle | Administrateur, propriétaire, super administrateur ou vendeur/POS |
| Préconditions | Données ou accès nécessaires |
| Étapes | Actions à exécuter |
| Résultat attendu | Ce qui doit être observé |
| Évidence à joindre | Capture, code ticket, heure, trace/error ID |
| Statut | Passé, bloqué, échoué, non testé |

## ADMIN-LOGIN-01 — Connexion admin

**Rôle** : Administrateur

**Préconditions**

- Compte admin staging fourni.
- URL admin staging disponible.

**Étapes**

1. Ouvrir le portail admin staging.
2. Se connecter avec le compte fourni.
3. Ouvrir le tableau de bord.

**Résultat attendu**

- L'utilisateur arrive dans l'espace admin.
- Le nom ou avatar du compte est visible.
- Aucune erreur de permission n'apparaît.

**Évidence à joindre**

- Capture du tableau de bord.
- Heure de connexion.

**Statut** : Passé / Bloqué / Échoué / Non testé

## ADMIN-CONFIG-01 — Configuration générale lisible

**Rôle** : Administrateur

**Préconditions**

- Compte admin connecté.
- Tenant ou santral de test disponible.

**Étapes**

1. Ouvrir **Configuration générale**.
2. Lire les blocs obligatoires.
3. Lire les blocs optionnels.
4. Ouvrir les actions proposées sur les blocs bloquants.

**Résultat attendu**

- Les blocs indiquent clairement ce qui est terminé.
- Les étapes bloquantes expliquent quoi faire.
- Les blocs optionnels ne sont pas mélangés aux obligations.

**Évidence à joindre**

- Capture de la configuration générale.
- Libellé exact si un bloc est ambigu.

**Statut** : Passé / Bloqué / Échoué / Non testé

## ADMIN-TERMINAL-01 — Création ou vérification terminal POS

**Rôle** : Administrateur ou propriétaire

**Préconditions**

- Accès à la section terminaux POS.

**Étapes**

1. Ouvrir la liste des terminaux POS.
2. Vérifier qu'un terminal de test existe.
3. Vérifier son statut.
4. Vérifier les informations vendeur ou santral liées.

**Résultat attendu**

- Le terminal de test est visible.
- Son statut est compréhensible.
- Les informations nécessaires à la connexion POS sont disponibles selon les permissions.

**Évidence à joindre**

- Code ou nom du terminal.
- Capture du statut.

**Statut** : Passé / Bloqué / Échoué / Non testé

## POS-PIN-01 — Connexion POS et changement de PIN

**Rôle** : Vendeur/POS

**Préconditions**

- Terminal POS actif.
- PIN temporaire fourni.
- Application mobile staging installée.

**Étapes**

1. Ouvrir l'app mobile POS.
2. Entrer le terminal et le PIN temporaire.
3. Changer le PIN si demandé.
4. Se reconnecter avec le nouveau PIN.

**Résultat attendu**

- Le terminal se connecte.
- Le changement de PIN est confirmé.
- Le vendeur arrive sur l'accueil POS.

**Évidence à joindre**

- Capture de l'accueil POS.
- Terminal utilisé.
- Heure du test.

**Statut** : Passé / Bloqué / Échoué / Non testé

## POS-SALE-01 — Vente simple et impression

**Rôle** : Vendeur/POS

**Préconditions**

- Terminal actif.
- PIN modifié.
- Tirage ouvert.

**Étapes**

1. Ouvrir le tirage.
2. Ajouter une mise simple.
3. Vérifier le récapitulatif avant confirmation.
4. Confirmer une première fois.
5. Confirmer une seconde fois si une double confirmation est demandée.
6. Imprimer ou afficher le reçu.

**Résultat attendu**

- Un numéro de ticket est créé.
- Le montant total est exact.
- La double confirmation protège l'action finale.
- Le reçu contient le terminal, le tirage et les mises.

**Évidence à joindre**

- Code ticket.
- Capture ou photo du reçu.
- Montant attendu et montant affiché.

**Statut** : Passé / Bloqué / Échoué / Non testé

## POS-KEYBOARD-01 — Test clavier et saisie rapide

**Rôle** : Vendeur/POS

**Préconditions**

- Terminal actif.
- Tirage ouvert.
- Appareil utilisé par un vendeur réel ou proche du matériel cible.

**Étapes**

1. Ouvrir une vente.
2. Saisir une sélection avec le clavier numérique.
3. Corriger une valeur.
4. Ajouter une deuxième ligne.
5. Naviguer jusqu'à la confirmation sans perdre la saisie.

**Résultat attendu**

- Le clavier s'ouvre au bon moment.
- Les champs restent lisibles.
- La correction ne supprime pas les autres lignes.
- Le total reste cohérent après modification.

**Évidence à joindre**

- Appareil et version OS.
- Capture avant confirmation.
- Montant attendu et montant affiché.

**Statut** : Passé / Bloqué / Échoué / Non testé

## POS-CANCEL-01 — Annulation avant confirmation

**Rôle** : Vendeur/POS

**Préconditions**

- Terminal connecté.
- Tirage ouvert.

**Étapes**

1. Ajouter une mise.
2. Revenir en arrière ou annuler avant confirmation.
3. Vérifier l'historique.

**Résultat attendu**

- Aucun ticket confirmé n'est créé.
- L'historique ne contient pas la vente annulée.

**Évidence à joindre**

- Heure du test.
- Capture de l'historique.

**Statut** : Passé / Bloqué / Échoué / Non testé

## POS-TICKET-01 — Consultation et réimpression

**Rôle** : Vendeur/POS

**Préconditions**

- Ticket créé par `POS-SALE-01`.

**Étapes**

1. Ouvrir l'historique ou la recherche ticket.
2. Retrouver le ticket.
3. Ouvrir le détail.
4. Réimprimer si l'action est disponible.

**Résultat attendu**

- Le ticket est retrouvé.
- Le détail affiche tirage, mises, montant et statut.
- La réimpression produit le même ticket ou reçu.

**Évidence à joindre**

- Code ticket.
- Capture du détail.
- Photo ou capture de réimpression.

**Statut** : Passé / Bloqué / Échoué / Non testé

## ADMIN-RESULT-01 — Saisie ou consultation résultat

**Rôle** : Administrateur

**Préconditions**

- Permission résultat confirmée pour le compte.
- Tirage de test disponible.

**Étapes**

1. Ouvrir la section résultats.
2. Sélectionner un tirage.
3. Consulter le statut.
4. Saisir un résultat uniquement si le scénario le permet.
5. Utiliser le [formulaire de résultat](formulaire-resultat.md) pour noter les lots et statuts.

**Résultat attendu**

- Le statut est clair.
- Les actions disponibles correspondent aux permissions.
- Toute saisie autorisée affiche un audit ou une mise à jour visible.

**Évidence à joindre**

- Provider ou tirage.
- Statut avant/après.
- Heure et utilisateur affiché.

**Statut** : Passé / Bloqué / Échoué / Non testé

## SUPERADMIN-RESULT-01 — Correction autorisée d'un résultat

**Rôle** : Super administrateur

**Préconditions**

- Santral de test disponible.
- Tirage de test disponible.
- Compte super administrateur autorisé.

**Étapes**

1. Ouvrir le santral de test.
2. Ouvrir les résultats du tirage choisi.
3. Vérifier le statut initial.
4. Effectuer une correction uniquement si le scénario de test le demande.
5. Vérifier le statut final et l'audit.

**Résultat attendu**

- Le statut initial est compréhensible.
- La correction demande une action explicite.
- Le résultat corrigé affiche une trace d'audit.
- Aucun résultat incomplet ne devient final sans confirmation claire.

**Évidence à joindre**

- Santral.
- Tirage ou provider.
- Statut avant/après.
- Capture de l'audit.

**Statut** : Passé / Bloqué / Échoué / Non testé

## ADMIN-REPORT-01 — Rapport de base

**Rôle** : Administrateur ou propriétaire

**Préconditions**

- Au moins une vente de test confirmée.

**Étapes**

1. Ouvrir la section rapports.
2. Choisir la période du jour.
3. Vérifier les totaux.

**Résultat attendu**

- Le rapport s'affiche.
- Les totaux sont cohérents avec les ventes de test.
- Aucun montant réel n'est présenté comme production.

**Évidence à joindre**

- Capture du rapport.
- Codes tickets utilisés.

**Statut** : Passé / Bloqué / Échoué / Non testé
