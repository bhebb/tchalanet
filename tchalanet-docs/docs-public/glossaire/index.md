# Glossaire Tchalanet

Cette page explique les mots utilisés dans Tchalanet. Elle aide les testeurs,
administrateurs, propriétaires et vendeurs POS à parler des mêmes objets avec
les mêmes termes.

## Concepts principaux

| Terme | Sens dans Tchalanet |
|---|---|
| Santral | Entreprise ou organisation qui vend et suit ses opérations dans Tchalanet. |
| Propriétaire | Personne qui supervise le santral : activité, terminaux, vendeurs, limites, commissions et rapports. |
| Administrateur | Personne qui configure et opère le santral au quotidien. |
| Super administrateur | Personne côté plateforme qui aide plusieurs santrals et peut effectuer certaines actions de support. |
| Vendeur | Personne ou poste qui vend des tickets à partir du POS. |
| Terminal POS | Point de vente utilisé pour vendre, imprimer ou consulter les tickets. |
| Jeu | Produit vendu, par exemple un type de loto ou de pari. |
| Canal de tirage | Configuration qui relie un jeu vendable à un horaire, un provider et une règle de vente. |
| Tirage | Événement daté sur lequel les ventes sont prises, puis fermé et résulté. |
| Résultat | Numéros ou valeurs officielles associées à un tirage. |
| Provider | Source externe ou manuelle qui fournit les résultats d'un État ou d'un opérateur. |
| Résultat manuel | Résultat saisi par une personne autorisée lorsqu'un provider automatique ne fournit pas le résultat à temps. |
| Override | Remplacement contrôlé d'un résultat déjà présent, avec audit attendu. |
| Settlement | Traitement final des tickets après résultat confirmé : gagnant, perdant, payable ou non payable selon les règles. |
| Reçu | Preuve imprimée ou affichée après une vente. |
| Ticket | Vente confirmée avec un code de référence et des lignes de mise. |
| Ligne de ticket | Une sélection ou mise individuelle à l'intérieur d'un ticket. |
| Limite | Règle qui empêche de dépasser un montant, une quantité ou un seuil autorisé. |
| Commission | Règle commerciale qui détermine la rémunération ou le suivi commercial. |

## Statuts utiles

| Statut | Sens simple |
|---|---|
| Planifié | Le tirage existe, mais la vente n'est pas encore ouverte. |
| Ouvert | Le tirage accepte les ventes. |
| Fermé | La vente est terminée pour ce tirage. |
| Résulté | Un résultat est lié au tirage. |
| Réglé | Les tickets ont été traités selon le résultat. |
| Annulé | Le tirage ne doit plus être vendu ni réglé normalement. |
| Provisoire | Le résultat existe, mais il n'est pas encore validé pour règlement final. |
| Confirmé | Le résultat est validé. |
| À corriger | Le résultat est incomplet ou incohérent et nécessite une action autorisée. |
| Corrigé manuellement | Un résultat a été remplacé ou corrigé par une action autorisée. |

## À retenir pour les tests

- Un tirage ouvert doit permettre la vente.
- Un tirage fermé ne doit plus permettre la vente.
- Un résultat provisoire ne doit pas être traité comme final.
- Un ticket ne doit devenir gagnant ou perdant qu'après résultat confirmé et règlement.
- Une correction de résultat doit laisser une trace claire : qui, quand, pourquoi.

Voir aussi : [Cycle des jobs de tirage](../tirages-resultats/jobs-tirages.md) et
[Providers supportés](../tirages-resultats/providers.md).
