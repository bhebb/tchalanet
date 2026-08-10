# Audit UX des pages admin

Date : 2026-08-09  
Portee : portail admin web, avec priorite mobile et administrateurs peu a l'aise avec les outils TI.  
Sources lues : routes admin Angular, libelles HT `surface-admin.json`, audit HTML temporaire, OpenSpec `admin-quick-actions-ux-v1`.

## Principe de priorisation

Les pages sont classees par usage attendu sur le terrain :

1. **Operations quotidiennes** : ce que l'admin regarde ou fait pendant la journee.
2. **Operations de controle** : actions en cas de probleme, fraude, erreur ou reclamation.
3. **Configuration initiale** : fait une fois au demarrage, puis rarement modifie.
4. **Configuration avancee** : reservee aux admins formes ou support Tchalanet.

La navigation mobile doit exposer d'abord les operations. La configuration peut rester dans une
section repliee, parce qu'un tenansye bolèt ne la modifie pas tous les jours.

## Synthese prioritaire

| Priorite | Page / parcours | Pourquoi | Amelioration recommandee |
|---|---|---|---|
| P1 | Dashboard admin | Point d'entree mobile | Ajouter des quick actions visuelles : `Bloke nimewo`, `Tiraj yo`, `Machann yo`, `Vann tikè`, `Tcheke tikè`, `Rapò`. |
| P1 | Tirages | Controle quotidien des tirages ouverts/fermes | Actions visibles sur ligne : fermer, rouvrir si permis, proposer resultat, bloquer numero sur ce tirage. |
| P1 | Machann | Controle des vendeurs et machines | Boutons directs : bloquer/debloquer, reset PIN, voir tickets, voir rapport. |
| P1 | Bloquer un numero | Action d'urgence | Parcours court : numero + tirage preselectionne + confirmer. Garder les options avancees cachees. |
| P1 | Titres pages + sidenav | Aujourd'hui certains titres ne matchent pas le menu | Aligner chaque titre avec le libelle menu et le vocabulaire metier. |
| P2 | Tickets | Recherche, verification, reclamations | Rendre `Tcheke tikè` plus visible que les vues internes. Terminal optionnel si possible. |
| P2 | Rapports | Consultation quotidienne ou fin de journee | Unifier les rapports avec tabs et presets de dates coherents. |
| P2 | POS admin | Demo, depannage, vente exceptionnelle | Preselection du dernier machann, avertir doublons, continuer la vente sans retourner a la liste. |
| P2 | Maryaj gratis | Configuration sensible | Expliquer les modes avec exemples, validation inline. |
| P3 | Config jeux / baremes / tirages | Setup initial | Garder dans configuration repliee, mais clarifier chaque libelle. |

## Navigation recommandee

### Operations

- `Akèy` ou `Tablo debò` : garder le dashboard en premier. `Akèy` est plus simple, mais `Tablo debò`
  est deja compris si on l'utilise partout.
- `Tiraj yo` : parent vers tous les tirages.
- `Machann yo` : parent vers liste machann.
- `Tikè` : parent vers voir tickets, vendre, verifier.
- `Rapò` : parent vers rapport quotidien en priorite.
- `Kontwòl nimewo` : si on veut exposer le blocage de numero comme operation. Sinon laisser sous config,
  mais ajouter `Bloke nimewo` en quick action dashboard.

### Configuration

- `Konfigirasyon` : section repliee par defaut sur mobile.
- `Konfigire tiraj yo` : horaires/canaux de tirage.
- `Jwèt & tarif` ou `Jwèt yo` : jeux disponibles et regles de paiement.
- `Maryaj gratis` : promotion/config metier.
- `Limit jeneral` / `Règ limit yo` : limites globales.
- `Antrepriz mwen` ou `Biznis mwen` : profil, apparence, support, notifications. Eviter `Bank mwen`
  tant qu'on n'est pas sur que tous les clients se reconnaissent dans ce mot.

## Pages operationnelles les plus utilisees

### 1. Dashboard admin

Route : `/app/admin` ou `/app/admin/dashboard`  
Libelle actuel : `Tablo debò`  
Usage : premier ecran, mobile, resume des ventes/tirages/alertes.

Ce qui doit etre clair :

- L'admin doit voir quoi faire maintenant, pas seulement des indicateurs.
- Les actions d'urgence doivent etre visibles sans ouvrir le sidenav.
- Les cartes doivent utiliser icone + texte court.

Ameliorations :

- Ajouter une grille 2 colonnes de quick actions mobile :
  - `Bloke nimewo`
  - `Tiraj yo`
  - `Machann yo`
  - `Vann tikè`
  - `Tcheke tikè`
  - `Rapò`
- Faire remonter les alertes operationnelles : tirage ouvert depuis longtemps, resultat manquant,
  machann bloque, ventes anormales, notifications non lues.
- Garder la configuration hors du premier ecran, sauf si le setup n'est pas termine.

Decision recommandee :

- Garder dashboard comme lien 1.
- Renommer eventuellement en `Akèy` plus tard, mais seulement si tous les titres/pages suivent.

### 2. Tirages

Route : `/app/admin/draws`  
Sous-pages : `results`, `channels`, detail `:id`  
Libelles actuels : `Tiraj`, `Tiraj ki louvri`, `Tiraj ki fèmen`, `Rezilta`, `Konfigire tiraj yo`

Usage : controle quotidien des tirages ouverts, fermes, resultats recus ou manquants.

Ce qui doit etre clair :

- Quel tirage est ouvert maintenant.
- Quel tirage attend son resultat.
- Ce que l'admin peut faire sur un tirage precis.

Ameliorations :

- Sur chaque ligne de tirage, rendre les actions principales visibles :
  - `Fèmen tiraj`
  - `Pwopoze rezilta`
  - `Bloke nimewo sou tiraj sa`
  - `Gade detay`
- Garder le menu `...` seulement pour actions rares.
- Dans la fiche detail tirage, ajouter une barre d'actions en haut avec les memes actions.
- Separer clairement operation et config :
  - `Tiraj yo` = controle des tirages generes.
  - `Konfigire tiraj yo` = config des horaires/canaux.

Point de vocabulaire :

- `Tiraj ouvè` sonne moins naturel. Garder `Tiraj ki louvri`.
- Garder `Tiraj ki fèmen`.
- Garder `Konfigire tiraj yo` pour la configuration.

### 3. Machann / machines POS

Route : `/app/admin/seller-terminals`  
Sous-pages : `new`, `commissions`, `:sellerTerminalId`, `:sellerTerminalId/overrides`, `sell`  
Libelles actuels : `Machann & machin`, `Lis machann yo`, `Kreye yon machann`

Usage : voir les vendeurs, creer un vendeur, bloquer/debloquer, reset PIN, depanner une machine.

Ce qui doit etre clair :

- Pour l'admin, le sujet principal est le machann.
- `Machin POS` et `Tèminal POS` doivent designer la machine, pas remplacer le machann dans les titres.
- Les actions de securite ne doivent pas etre cachees.

Ameliorations :

- Renommer le parent en `Machann yo`.
- Dans la liste :
  - bouton visible `Bloke` si actif ;
  - bouton visible `Debloke` si bloque ;
  - action visible ou secondaire `Reyinisyalize PIN`.
- Dans la fiche machann :
  - barre d'actions : `Bloke/Debloke`, `Reyinisyalize PIN`, `Gade tikè`, `Rapò machann`, `Règ espesyal`.
  - afficher clairement l'etat de la machine POS liee.
- Apres creation d'un machann, afficher les prochaines actions :
  - activer la machine ;
  - imprimer/partager les infos de connexion ;
  - tester une vente.

Libelles recommandes :

- Parent : `Machann yo`
- Liste : `Lis machann yo`
- Creation : `Kreye yon machann`
- Detail : `Fich machann`
- Overrides : `Règ espesyal machann`
- Machine : `Machin POS` dans les champs techniques.

### 4. Bloquer un numero / limites

Route : `/app/admin/limits/number` dans le shell `/app/admin/limits`  
Autres sous-pages : `system`, `global`, `draw`, `seller-terminal`  
Libelles actuels : `Limit`, `Rezime limit yo`, `Limit jeneral`, `Pa nimewo`, `Pa tiraj`, `Pa machann / machin`

Usage : controle d'urgence quand un numero ne doit plus etre vendu, ou configuration des limites de vente.

Ce qui doit etre clair :

- Il y a deux besoins differents :
  - operation : bloquer un numero maintenant ;
  - configuration : definir les limites de vente.
- L'admin ne doit pas comprendre le modele interne des limites pour bloquer un numero.

Ameliorations :

- Ajouter une action rapide `Bloke nimewo` depuis dashboard.
- Depuis un tirage ouvert, ajouter `Bloke nimewo sou tiraj sa`.
- Dialog simplifie :
  - champ numero ;
  - tirage preselectionne ;
  - bouton `Bloke nimewo`;
  - lien `Plis opsyon` pour WARN, duree, regles avancees.
- Renommer la page operationnelle `Pa nimewo` en `Kontwòl nimewo` si elle sert surtout au blocage.
- Garder `Limit jeneral` pour les limites globales de mise.

Decision recommandee :

- Ne pas supprimer le module `Limit`.
- Ne pas mettre tout sous `Kontwòl nimewo`, car les limites globales et par machann restent de la config.
- Faire remonter seulement le parcours `Bloke nimewo`.

### 5. Tickets

Routes : `/app/admin/tickets`, `/app/admin/tickets/verify`, `/app/admin/tickets/:ticketId`,
`/app/admin/pos/sale`  
Libelles actuels : `Tikè`, `Wè tikè yo`, `Vann yon tikè`, `Verifye yon tikè`

Usage : reclamation client, verification d'un ticket, retrouver une vente. La vente admin existe mais
n'est pas le coeur du metier admin.

Ce qui doit etre clair :

- L'admin peut vendre, mais ce n'est pas l'usage principal.
- `Tcheke tikè` est plus oral que `Verifye yon tikè`.
- Voir un ticket doit etre plus facile depuis une recherche.

Ameliorations :

- Garder `Vann yon tikè`, mais en second niveau.
- Mettre en avant :
  - `Wè tikè yo`
  - `Tcheke tikè`
- Sur mobile, permettre de coller/taper le code ticket directement.
- Si techniquement possible, rendre le terminal optionnel pour verifier un ticket.
- Dans la fiche ticket, afficher les actions attendues :
  - reenprimer ;
  - voir machann ;
  - voir tirage ;
  - etat resultat/paiement.

Libelles recommandes :

- `Wè tikè yo`
- `Tcheke tikè`
- `Vann yon tikè`

### 6. Rapports

Routes : `/app/admin/reports/overview`, `daily`, `sales`, `sellers`, `draws`, `financials`  
Libelles actuels : `Rapò`, `Rapò jounen`, `Vant`, `Rapò pa machann`, `Rapò pa tiraj`, `Finans`

Usage : fin de journee, controle machann, controle tirage, reconciliation.

Ce qui doit etre clair :

- L'admin doit comprendre quel rapport utiliser selon la question.
- Les rapports doivent etre comparables avec les memes presets de date.
- Le rapport quotidien doit etre le premier.

Ameliorations :

- Parent `Rapò` doit mener au rapport quotidien ou a une vue simple qui explique les rapports.
- Ajouter une tab bar commune dans chaque page rapport :
  - `Jounen`
  - `Machann`
  - `Tiraj`
  - `Finans`
  - `Vant`
- Unifier les filtres de dates :
  - aujourd'hui ;
  - 7 jours ;
  - 30 jours ;
  - ce mois.
- Dans chaque rapport, ajouter un bloc court :
  - ce que ce rapport repond ;
  - formule des totaux importants ;
  - quand l'utiliser.

Libelles recommandes :

- `Rapò jounen`
- `Rapò machann`
- `Rapò tiraj`
- `Rapò finans`

## Pages de controle importantes

### 7. POS admin / vendre un ticket

Route : `/app/admin/pos/sale` puis `/app/admin/pos/sale/:sellerTerminalId`  
Usage : demo, depannage, vente exceptionnelle par admin.

Ameliorations :

- Preselectionner le dernier machann utilise.
- Si un seul machann actif, sauter la selection.
- Si un machann est bloque, afficher pourquoi et proposer `Debloke` si l'admin a le droit.
- Dans le formulaire de vente :
  - apres la saisie des numeros, focus automatique vers `Mise` ;
  - avertir si le numero existe deja dans le ticket ;
  - afficher clairement le tirage choisi.
- Remplacer les textes hardcodes par i18n.

### 8. Resultats

Route : `/app/admin/draws/results` et detail `/app/admin/draws/results/:resultId`  
Usage : verifier les resultats recus automatiquement, comprendre les overrides/propositions.

Ameliorations :

- Expliquer clairement la source :
  - `Rezilta otomatik`
  - `Rezilta manyèl`
  - `Pwopozisyon rezilta`
  - `Korije pa Tchalanet`
- Si un resultat auto manque, proposer l'action `Pwopoze rezilta`, pas une saisie libre definitive.
- Dans les notifications admin, distinguer :
  - resultat auto arrive ;
  - resultat propose par admin ;
  - resultat confirme/applique ;
  - erreur fournisseur.

### 9. Notifications

Route : `/app/admin/notifications`  
Usage : comprendre ce qui demande attention.

Ameliorations :

- Grouper par type :
  - resultats ;
  - tickets/paiements ;
  - machann ;
  - systeme.
- Eviter les notifications repetitives identiques.
- Chaque notification doit avoir une action :
  - `Gade tiraj`
  - `Gade rezilta`
  - `Gade machann`
  - `Make kòm li`.
- Revoir la strategie :
  - admin : resultat auto arrive, resultat confirme, echec fournisseur, tirage bloque, vente anormale ;
  - vendeur : ticket vendu, ticket annule/refuse, tirage ferme, resultat publie si ses tickets sont concernes.

## Pages de configuration initiale

### 10. Configuration generale / setup

Route : `/app/admin/setup`  
Libelles actuels : `Konfigirasyon`, `Enskripsyon`, `Paramèt`

Usage : setup initial du tenansye bolèt.

Ameliorations :

- Garder cette section repliee sur mobile apres setup termine.
- Transformer la checklist en parcours plus explicite :
  - configurer l'entreprise ;
  - creer un machann ;
  - configurer jeux/tarifs ;
  - configurer tirages ;
  - tester une vente ;
  - verifier un ticket.
- Chaque etape doit avoir un bouton d'action direct.

### 11. Jeux et tarifs

Route : `/app/admin/games` et `/app/admin/games/channel-matrix`  
Libelles actuels : `Jwèt`, `Jwèt pa kanal tiraj`

Usage : definir les jeux disponibles et leurs regles par tirage.

Ameliorations :

- Garder `Jwèt yo` ou `Jwèt & tarif`.
- Sur la page overview, expliquer les jeux supportes avec exemples simples.
- Mettre en avant :
  - activer/desactiver un jeu ;
  - voir/modifier baremes ;
  - verifier quels jeux sont ouverts par tirage.
- Pour la matrice, renommer `kanal` si possible en langage plus clair :
  - `Jwèt pa tiraj`
  - ou `Jwèt sou chak tiraj`

### 12. Configurer les tirages

Route : `/app/admin/draws/channels`  
Libelle actuel : `Konfigire tiraj yo`

Usage : config initiale des horaires/draw channels/providers.

Ameliorations :

- Garder sous configuration.
- Remplacer les heures texte par time picker.
- Expliquer chaque provider/fournisseur :
  - etat/institution qui publie le resultat ;
  - auto ou manuel ;
  - slots supportes.
- Afficher clairement actif/inactif.
- Ajouter une confirmation claire avant desactiver un tirage.

### 13. Maryaj gratis

Route : `/app/admin/maryaj-gratis`  
Libelle actuel : `Maryaj gratis`

Usage : promotion metier, probablement rare mais sensible.

Ameliorations :

- Garder le libelle dans les trois langues.
- Ajouter descriptions des modes :
  - montant fixe ;
  - selon montant paye ;
  - paliers.
- Ajouter exemples chiffres.
- Remplacer snackbar de validation par erreurs inline.
- Resumer l'impact : combien de maryaj offerts pour un ticket exemple.

### 14. Commissions / configuration machann

Routes : `/app/admin/controls/commissions`, `/app/admin/seller-terminals/commissions`  
Libelle actuel : `Komisyon`, page `Konfigire machin yo`

Usage : config de base et overrides machann.

Ameliorations :

- Ne pas nommer cette zone seulement `Komisyon` si elle configure aussi les machines.
- Clarifier :
  - commission default ;
  - commission speciale par machann ;
  - bareme special par machann.
- Dans un changement default, afficher `X machann ap afekte`.
- Dans un override, utiliser un vocabulaire creole stable :
  - `Règ pa defo`
  - `Règ espesyal machann`

### 15. Entreprise / apparence / support

Routes : `/app/admin/business-profile`, `/app/admin/company/appearance`,
`/app/admin/company/settings`, `/app/admin/company/support`  
Libelle actuel : `Antrepriz mwen`

Usage : rarement modifie apres setup.

Ameliorations :

- Garder dans configuration repliee.
- Preferer `Antrepriz mwen` ou `Biznis mwen`.
- Ne pas utiliser `Bank mwen` sans validation terrain.
- Pages support et notifications doivent rester faciles a retrouver, mais pas dans les actions quotidiennes.

## Backlog recommande

### Lot 1 — avant test client

- Aligner sidenav et titres de pages sur les libelles metier.
- Ajouter quick actions dashboard avec icones.
- Simplifier `Bloke nimewo`.
- Rendre `Bloke machann` et `Reyinisyalize PIN` visibles.
- Ajouter actions directes dans detail machann et detail tirage.
- Corriger hardcodes FR/HT dans POS et verification ticket.

### Lot 2 — operations solides

- Tab bar rapports.
- Presets dates communs.
- Actions inline tirage.
- Notifications admin propres avec actions.
- Resultats : distinguer auto, manuel, proposition, confirmation.

### Lot 3 — configuration plus pedagogique

- Time picker pour tirages.
- Aide inline jeux/tarifs.
- Maryaj gratis avec exemples.
- Setup sous forme de checklist actionnable.
- Documentation visuelle avec captures et fleches.

## Points a valider avec captures

- Est-ce que les boutons critiques sont visibles sur mobile sans scroll excessif ?
- Est-ce que le titre de page correspond exactement au menu ?
- Est-ce qu'une personne peut bloquer un numero sans comprendre les limites internes ?
- Est-ce qu'un admin peut trouver rapidement le tirage ouvert et le fermer ?
- Est-ce qu'un admin peut bloquer un machann depuis la liste et depuis la fiche ?
- Est-ce que les rapports repondent aux questions terrain : combien vendu, par qui, sur quel tirage,
  combien a payer ?
