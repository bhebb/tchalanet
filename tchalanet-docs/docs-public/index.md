# Tchalanet — Documentation

!!! warning "Environnement de validation"
    **Environnement : STAGING**  
    **Données : tests uniquement**  
    Aucun ticket, montant, reçu ou rapport affiché dans cet environnement n'a de
    valeur réelle.  
    **Version documentation : POC 2026-07-23**  
    **Version portail admin : à renseigner dans la fiche d'accès**  
    **Version mobile POS : à renseigner dans la fiche d'accès**  
    **Dernière mise à jour : 2026-07-23**

Bienvenue dans la documentation Tchalanet. Pour la livraison en cours, cette
surface accompagne surtout les testeurs qui valident l'application mobile POS et
le portail admin. Elle pourra ensuite devenir la documentation publique des
parcours d'utilisation.

Elle sert à répondre à trois questions simples :

1. Que dois-je tester ?
2. Quel résultat dois-je voir ?
3. Comment signaler clairement un problème ?

[Ouvrir le portail admin](https://test.tchalanet.com){ .md-button .md-button--primary }
[Préparer l'installation POS](validation/acces-testeur.md){ .md-button }

Ce site ne contient pas la documentation technique interne. Les pages sont
écrites pour les testeurs, les responsables d'exploitation et les utilisateurs
qui doivent valider les parcours réels.

## Choisir son parcours

| Rôle | Responsabilité principale | Guide |
|---|---|---|
| **Propriétaire** | Supervise l'entreprise ou le santral : vendeurs, terminaux, limites, commissions, activité | [Guide Propriétaires](proprietaires/index.md) |
| **Administrateur** | Configure et exécute les opérations quotidiennes autorisées | [Guide Administrateurs](administrateurs/index.md) |
| **Super administrateur** | Support plateforme, santrals, providers, statuts, overrides autorisés | [Guide Super administrateurs](super-administrateurs/index.md) |
| **Vendeur / POS** | Vente, tickets, impression, réimpression et consultation | [Guide Vendeur / POS](pos/index.md) |

## À valider ce week-end

1. Accès admin et navigation principale.
2. Configuration générale du santral.
3. Jeux et canaux de tirage nécessaires à la vente.
4. Terminal POS visible et utilisable.
5. Connexion mobile POS avec PIN.
6. Vente simple, confirmation et reçu.
7. Consultation ou réimpression d'un ticket.
8. Résultats visibles avec statut compréhensible.
9. Rapport de base après activité de test.

## Périmètre exact de la POC

| Inclus | Hors périmètre |
|---|---|
| Portail admin staging | Production réelle |
| Mobile POS Android staging | iOS |
| Vente simple avec reçu | Paiement réel |
| Consultation et réimpression ticket | Gestion complète des réclamations |
| Providers NY et FL automatiques | Activation automatique de tous les providers |
| Résultats manuels ou automatiques selon provider | Reporting financier final de production |
| Rapport de base après ventes de test | Comptabilité complète |

## Parcours express

Si vous avez peu de temps, commencez par ces deux parcours :

- [Préparer un santral](parcours/preparer-un-santral.md)
- [Vendre un ticket](parcours/vendre-un-ticket.md)

Pour comprendre le vocabulaire, les tirages et les résultats :

- [Glossaire Tchalanet](glossaire/index.md)
- [Cycle des jobs de tirage](tirages-resultats/jobs-tirages.md)
- [Providers supportés](tirages-resultats/providers.md)
- [Vérifier ou confirmer](tirages-resultats/verifier-confirmer.md)

## Applications

| Surface | Lien |
|---|---|
| Portail admin staging | [Ouvrir le portail admin](https://test.tchalanet.com) |
| Mobile POS Android | Lien Firebase App Distribution indiqué dans la [fiche d'accès testeur](validation/acces-testeur.md) |
| Signalement de problème | [Format attendu](validation/signaler-un-probleme.md) |

## Méthode de test recommandée

1. Lire [Avant de tester](avant-de-tester.md).
2. Ouvrir le guide correspondant à votre rôle.
3. Exécuter les étapes dans l'ordre.
4. Vérifier le résultat attendu après chaque étape.
5. Noter immédiatement tout écart.
6. Utiliser la page [Signaler un problème](validation/signaler-un-probleme.md)
   si le résultat obtenu ne correspond pas au guide.

## Règle de remontée

Si un parcours bloque, ne continuez pas avec des suppositions. Notez le scénario,
l'étape, l'heure, le rôle utilisé, le résultat attendu, le résultat obtenu et
joignez une capture ou le code ticket si disponible.
