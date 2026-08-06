# Design — Base terminologique trilingue

Ce document est la **source de vérité éditoriale** de la passe. Il sera repris dans
`tchalanet-docs/docs-public/glossaire/index.md` à la tâche 1.2.

## Principe : deux registres

Chaque concept a **deux noms**, et confondre les deux est la faute de fond du corpus actuel.

| Registre | Où il s'applique | Exemple |
|---|---|---|
| **Domaine** | code Java/TS, noms de tables, endpoints, `docs/00-guidelines/glossary.md` | `SellerTerminal` |
| **Public** | valeurs des fichiers i18n, `docs-public/`, tout écran | « Vendeur » |

> Le nom de la **clé** i18n appartient au registre domaine (`admin.sellerTerminals.list.title`).
> La **valeur** appartient au registre public (« Vendeurs »). Une clé n'est jamais lue par un
> utilisateur ; une valeur ne contient jamais un nom de classe.

## Table terminologique

| Domaine (code) | FR public | EN public | HT public | Note |
|---|---|---|---|---|
| `SellerTerminal` | **Vendeur** | **Seller** | **Tèminal POS** | jamais « seller-terminal », « caissier ». Voir l'exception kreyòl ci-dessous. |
| `Tenant` (objet géré, vue plateforme) | **Santral** | **Operator** | **Santral** | ce que le super-admin administre |
| `Tenant` (auto-référence, console admin) | **Espace** | **Workspace** | **Espas** | « votre espace », « your workspace » |
| `Draw` | **Tirage** | **Draw** | **Tiraj** | |
| `DrawChannel` | **Canal de tirage** | **Draw channel** | **Kanal tiraj** | |
| `Ticket` | **Ticket** | **Ticket** | **Tikè** | |
| `TicketLine` | **Ligne** | **Line** | **Liy** | « ligne de ticket » si ambigu |
| `Sale` | **Vente** | **Sale** | **Vant** | |
| `Settlement` | **Règlement** | **Settlement** | **Regleman** | jamais « settlement » en FR |
| `Result` | **Résultat** | **Result** | **Rezilta** | |
| `ResultSlot` | **Position de résultat** | **Result slot** | **Pozisyon rezilta** | |
| `PublicCode` | **Code public** | **Public code** | **Kòd piblik** | |
| `Receipt` | **Reçu** | **Receipt** | **Resi** | |
| `LimitPolicy` | **Limite** | **Limit** | **Limit** | « règle de limite » au pluriel de règles |
| `Commission` | **Commission** | **Commission** | **Komisyon** | |
| `Plan` / entitlement | **Plan** | **Plan** | **Plan** | |
| `GameCode` | **Jeu** | **Game** | **Jwèt** | |
| `BetType` | **Type de pari** | **Bet type** | **Tip pari** | |
| `TENANT_ADMIN` | **Administrateur** | **Administrator** | **Administratè** | jamais le code du rôle dans la copy |
| `SUPER_ADMIN` | **Super administrateur** | **Platform administrator** | **Sipè administratè** | |
| `OPERATOR` (rôle) | **Superviseur** | **Supervisor** | **Sipèvizè** | ⚠ ne pas confondre avec « opérateur » marketing |
| — (public/marketing) | **Gestionnaire de réseau** | **Network manager** | **Jesyonè rezo** | convention publique, pas un rôle technique |

## Exception kreyòl assumée — « Tèminal POS »

Le kreyòl garde **« Tèminal POS »** (250 occurrences) là où le FR dit « Vendeur » et l'EN « Seller ».

Ce n'est pas un retard d'alignement, c'est un choix : le terme est installé chez les vendeurs
bòlèt sur le terrain, et le kreyòl du corpus est la locale la mieux écrite des trois. On
n'impose pas une cohérence de tableau à une langue au prix de ce que ses locuteurs reconnaissent.

- « Vandè » reste acceptable quand le texte désigne explicitement **la personne** plutôt que le poste.
- L'audit **ne compte pas** « tèminal POS » comme un défaut en HT. Il le compte toujours en FR et EN.
- En revanche `seller-terminal` (l'identifiant de code) reste interdit dans **les trois** langues.

## Termes bannis de la copy

| Interdit | Pourquoi | Remplacer par |
|---|---|---|
| `seller-terminal`, `seller terminal`, `SellerTerminal` | nom de classe | Vendeur / Seller / Vandè |
| `tenant` (FR/EN) | terme d'architecture multi-tenant | Santral·Espace / Operator·Workspace |
| `outlet`, `session`, `cashier`, `caissier` | concepts **retirés** du modèle | reformuler, ou supprimer la clé |
| `TENANT_ADMIN`, `SUPER_ADMIN`, `SELLER_TERMINAL` | codes de rôle | libellé du rôle |
| `snake_case`, `camelCase` bruts | identifiants | libellé |

## Piège : « connexion »

Le FR emploie **« Connexion » pour deux choses** : l'authentification, et la période de vente
ouverte d'un vendeur. L'EN l'a traduit mot à mot en « Connection », qui ne veut rien dire dans
le second sens.

- **« Connexion » / « Sign-in » / « Koneksyon »** → authentification, **uniquement**.
- La période de vente : le concept `SalesSession` est **retiré du modèle**. Les clés
  `cashier.connexion.*`, `cashier.session.*`, `cashier.connection.*` sont à vérifier contre le
  code avant toute traduction — la plupart sont des orphelines à supprimer (tâche 2.1).

## Registre et ton

| | FR | EN | HT |
|---|---|---|---|
| Adresse | vouvoiement (« Vérifiez votre ticket ») | impératif direct, court | `ou` (« Verifye tikè ou ») |
| Boutons | infinitif (« Enregistrer ») | impératif (« Save ») | infinitif (« Anrejistre ») |
| Erreurs | cause + action, sans blâme | idem | idem |
| Ponctuation | apostrophe typographique `’`, espace fine avant `? !` | apostrophe droite `'` | apostrophe droite `'` |
| Interdits | `!`, majuscules criées, « Oups » | idem | idem |

## Orthographe kreyòl (IPN)

Orthographe officielle IPN. Corrections à appliquer :

| Fautif | Correct | Occurrences |
|---|---|---|
| `anko` | `ankò` | 15 |
| `pwoblem` | `pwoblèm` | 2 |
| `sevis` | `sèvis` | 1 |

> **Ne pas « corriger »** `apre`, `kounye`, `nimewo`, `rezilta`, `menm` : ces formes **sont**
> correctes en IPN. Le réflexe de re-franciser le kreyòl est la faute la plus fréquente sur ce
> type de corpus.

## Contraintes techniques à préserver

- **Placeholders `{{name}}` identiques** entre les trois locales (aujourd'hui : 0 écart, à ne pas casser).
- **Parité de clés stricte** FR = EN = HT après la passe.
- La longueur EN dépasse rarement le FR ; le **HT est en moyenne 10–15 % plus long** — vérifier les
  libellés de boutons et de colonnes contraints.
