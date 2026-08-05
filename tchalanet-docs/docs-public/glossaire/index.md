# Glossaire Tchalanet

Cette page fixe les mots utilisés dans Tchalanet, dans les trois langues du produit :
**français, anglais, kreyòl ayisyen**. Elle aide les testeurs, administrateurs, propriétaires
et vendeurs à parler des mêmes objets avec les mêmes termes — et sert de référence obligatoire
pour toute traduction d'interface.

## Deux registres, à ne pas confondre

Chaque concept porte **deux noms**. Les confondre est la faute la plus fréquente sur ce produit.

| Registre | Où il s'applique | Exemple |
|---|---|---|
| **Domaine** | code, base de données, API, docs techniques | `SellerTerminal` |
| **Public** | écrans, messages d'erreur, docs publiques | Vendeur / Seller / Vandè |

!!! warning "Règle"
    Un nom de classe ne s'affiche jamais à l'écran. Si un texte d'interface contient
    `seller-terminal`, `tenant` ou `TENANT_ADMIN`, c'est un défaut de traduction, pas un choix.

## Acteurs

| Domaine | Français | English | Kreyòl | Sens |
|---|---|---|---|---|
| `SellerTerminal` | **Vendeur** | Seller | Tèminal POS | Qui vend les tickets. Identité permanente avec PIN. Unité de vente et de facturation. |
| `Tenant` | **Santral** | Operator | Santral | L'entreprise qui vend et suit ses opérations dans Tchalanet. |
| `Tenant` *(vu de l'intérieur)* | **Espace** | Workspace | Espas | Le même objet, quand on s'adresse à quelqu'un qui est dedans : « votre espace ». |
| — | **Propriétaire** | Owner | Pwopriyetè | Personne qui supervise le santral : activité, vendeurs, limites, commissions, rapports. |
| `TENANT_ADMIN` | **Administrateur** | Administrator | Administratè | Configure et opère le santral au quotidien. |
| `SUPER_ADMIN` | **Super administrateur** | Platform administrator | Sipè administratè | Côté plateforme, accompagne plusieurs santrals. |
| `OPERATOR` | **Superviseur** | Supervisor | Sipèvizè | Rôle interne de supervision. |
| — | **Gestionnaire de réseau** | Network manager | Jesyonè rezo | Terme public/commercial. **N'est pas** le rôle `OPERATOR`. |

!!! info "Exception kreyòl assumée"
    Le kreyòl garde **« Tèminal POS »** là où le français dit « Vendeur ». Le terme est installé
    chez les vendeurs bòlèt : on ne l'aligne pas de force sur le français. **« Vandè »** reste
    juste quand le texte désigne la personne plutôt que le poste. En revanche `seller-terminal`
    — l'identifiant de code — est interdit dans les trois langues.

!!! note "Termes retirés"
    **Terminal POS**, **Session de caisse**, **Caissier**, **Point de vente (Outlet)** ne sont plus
    des concepts du produit. Ils ont fusionné dans le **Vendeur**. Un vendeur n'a pas besoin
    d'ouvrir une session pour vendre. Ne pas réintroduire ces mots dans une interface ou une doc.

## Objets de vente

| Domaine | Français | English | Kreyòl | Sens |
|---|---|---|---|---|
| `Ticket` | **Ticket** | Ticket | Tikè | Vente confirmée, avec un code de référence et des lignes de mise. |
| `TicketLine` | **Ligne** | Line | Liy | Une sélection ou mise individuelle à l'intérieur d'un ticket. |
| `Sale` | **Vente** | Sale | Vant | La transaction qui émet le ticket. |
| `PublicCode` | **Code public** | Public code | Kòd piblik | Code court imprimé sur le reçu, qui permet de vérifier un ticket sans compte. |
| `Receipt` | **Reçu** | Receipt | Resi | Preuve imprimée ou affichée après une vente. |
| `Settlement` | **Règlement** | Settlement | Regleman | Traitement des tickets après résultat confirmé : gagnant, perdant, payable ou non. |

## Jeux et tirages

| Domaine | Français | English | Kreyòl | Sens |
|---|---|---|---|---|
| `GameCode` | **Jeu** | Game | Jwèt | Produit vendu, par exemple un type de loto ou de pari. |
| `BetType` | **Type de pari** | Bet type | Tip pari | Forme de mise à l'intérieur d'un jeu. |
| `DrawChannel` | **Canal de tirage** | Draw channel | Kanal tiraj | Relie un jeu vendable à un horaire, un provider et une règle de vente. |
| `Draw` | **Tirage** | Draw | Tiraj | Événement daté sur lequel les ventes sont prises, puis fermé et résulté. |
| `Result` | **Résultat** | Result | Rezilta | Numéros ou valeurs officielles associés à un tirage. |
| `ResultSlot` | **Position de résultat** | Result slot | Pozisyon rezilta | Place d'un résultat dans un tirage (première, deuxième, bonus). |
| — | **Provider** | Provider | Provider | Source externe ou manuelle qui fournit les résultats d'un État ou d'un opérateur. |
| — | **Résultat manuel** | Manual result | Rezilta manyèl | Résultat saisi par une personne autorisée quand le provider ne fournit rien à temps. |
| — | **Correction de résultat** | Result override | Koreksyon rezilta | Remplacement contrôlé d'un résultat déjà présent, avec audit. |

## Règles commerciales

| Domaine | Français | English | Kreyòl | Sens |
|---|---|---|---|---|
| `LimitPolicy` | **Limite** | Limit | Limit | Empêche de dépasser un montant, une quantité ou un seuil autorisé. |
| `Commission` | **Commission** | Commission | Komisyon | Détermine la rémunération du vendeur. |
| `Plan` | **Plan** | Plan | Plan | Abonnement du santral ; ouvre ou ferme des fonctionnalités. |

## Statuts

| Français | English | Kreyòl | Sens |
|---|---|---|---|
| Planifié | Scheduled | Planifye | Le tirage existe, mais la vente n'est pas encore ouverte. |
| Ouvert | Open | Ouvè | Le tirage accepte les ventes. |
| Fermé | Closed | Fèmen | La vente est terminée pour ce tirage. |
| Résulté | Resulted | Ak rezilta | Un résultat est lié au tirage. |
| Réglé | Settled | Regle | Les tickets ont été traités selon le résultat. |
| Annulé | Cancelled | Anile | Le tirage ne doit plus être vendu ni réglé. |
| Provisoire | Provisional | Pwovizwa | Le résultat existe, mais n'est pas validé pour règlement final. |
| Confirmé | Confirmed | Konfime | Le résultat est validé. |
| À corriger | Needs review | Pou korije | Le résultat est incomplet ou incohérent. |
| Corrigé manuellement | Manually corrected | Korije alamen | Un résultat a été remplacé par une action autorisée. |

## Règles d'écriture

| | Français | English | Kreyòl |
|---|---|---|---|
| Adresse | Vouvoiement | Impératif direct, court | `ou` |
| Boutons | Infinitif — « Enregistrer » | Impératif — « Save » | Infinitif — « Anrejistre » |
| Erreurs | Cause puis action, sans blâmer l'utilisateur | idem | idem |
| Apostrophe | typographique `’` | droite `'` | droite `'` |
| Interdits | `!`, majuscules criées, « Oups » | idem | idem |

Le kreyòl suit l'**orthographe officielle IPN**. Les formes `apre`, `kounye`, `nimewo`, `rezilta`,
`menm` sont correctes telles quelles : ne pas les re-franciser. En revanche `ankò`, `pwoblèm`,
`sèvis`, `kòd`, `lòt` portent bien un accent grave.

## À retenir pour les tests

- Un tirage ouvert doit permettre la vente.
- Un tirage fermé ne doit plus permettre la vente.
- Un résultat provisoire ne doit pas être traité comme final.
- Un ticket ne devient gagnant ou perdant qu'après résultat confirmé et règlement.
- Une correction de résultat doit laisser une trace claire : qui, quand, pourquoi.

Voir aussi : [Cycle des jobs de tirage](../tirages-resultats/jobs-tirages.md) et
[Providers supportés](../tirages-resultats/providers.md).
