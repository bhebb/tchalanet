# Design

## Principle

Une page console doit être immédiatement reconnaissable comme une page Tchalanet, même lorsque son
domaine métier change. La cohérence porte sur la hiérarchie, les espacements, les surfaces, les
actions et les états, pas sur la répétition du même contenu.

## Detail surface

Toutes les pages de détail suivent cette structure :

```text
tch-admin-page-shell
  meta: statut, type, devise ou fuseau si utile
  actions: retour puis actions métier
  tch-admin-detail-layout
    main: identité et sections métier
    aside: résumé, métriques et raccourcis contextuels
```

Règles :

- Le panneau de droite utilise une primitive partagée de résumé et les mêmes tokens que le contenu
  principal. Une feature ne crée pas une carte sombre ou une grille de métriques ad hoc.
- L’identité principale, le statut et les actions prioritaires sont visibles sans chercher dans
  une section secondaire.
- Les faits sont rendus avec une structure `dt/dd` ou une primitive équivalente, avec des libellés
  traduits et des valeurs formatées.
- Les sections utilisent `tch-admin-section-card`, avec le même en-tête, la même icône, la même
  bordure et le même comportement responsive.
- Sur petit écran, l’aside passe sous le contenu principal sans perte d’action ni d’information.
- Les UUID et identifiants internes ne sont jamais l’identité principale. Ils restent secondaires,
  copiables ou réservés à une section technique.

## Table surface

Toutes les listes administratives utilisent `tch-admin-list-surface` ou une primitive de tableau
partagée compatible avec le playbook.

Règles :

- recherche, filtres, tri, pagination et état de l’URL suivent les conventions du feature playbook ;
- l’identité métier est affichée en premier, avec code lisible et libellé secondaire si nécessaire ;
- les statuts utilisent `tch-status-badge` et un mapping explicite ;
- les actions de ligne sont regroupées dans un menu `…`, avec tooltip/aria-label et pending local ;
- les valeurs monétaires, dates, taux et compteurs utilisent un format partagé ;
- les états « aucune donnée » et « aucun résultat pour ces filtres » restent distincts ;
- un UUID brut ne doit pas remplacer un nom, un code de tirage, un canal ou une date lisible ;
- le tableau reste utilisable au clavier et bascule vers une présentation lisible sur mobile.
- sur mobile, une ligne devient une carte autonome : l'identité est un lien de détail, les faits
  essentiels restent visibles et les actions ne doivent jamais être coupées par un débordement
  horizontal ; le tableau reste la présentation desktop.
- les KPI de collection apparaissent avant la recherche et les filtres. Ils décrivent le périmètre
  de la collection et sa période ; ils ne sont interactifs que lorsqu'ils ont un filtre métier
  explicite.

## Form surface

Les formulaires de page et de dialogue suivent la même hiérarchie : contexte, champs, feedback,
actions.

Règles :

- les labels, aides et erreurs sont traduits ; aucun texte métier codé en dur dans un nouvel écran ;
- les sections de champs standards réutilisent les composants console existants ;
- la validation client est affichée au champ et la validation serveur est appliquée au champ quand
  elle est adressable, sinon dans un résumé ou une erreur de section ;
- une mutation désactive le submit, affiche son état pending et ne blanchit pas les données déjà
  saisies ;
- le succès est visible près de l’action ou dans la section concernée, puis les données sont
  rechargées sans fermer silencieusement le contexte ;
- annuler, fermer et enregistrer ont des positions, libellés et comportements constants ;
- les champs conservent une largeur et une hiérarchie stables sur desktop et mobile.

## Entity-specific detail metrics

Les KPI de détail ne sont pas obligatoires pour toutes les entités. Un terminal vendeur expose un
bloc de métriques de la journée (tickets, ventes brutes, commission et revenu net estimé), puis
son résumé d'identité et ses sections métier. Un ticket reste un document unitaire : son résumé
porte déjà les montants et le contexte vendeur/terminal, donc il ne reçoit pas de bloc KPI séparé.

## Shared component boundaries

Les primitives candidates sont :

- `AdminDetailLayout` pour la grille main/aside ;
- `AdminFormLayout` pour la grille champs/aperçu et le footer d'actions des formulaires routés ;
- un panneau partagé de résumé de détail pour identité, statut, métriques et liens ;
- `AdminSectionCard` et `AdminMetricCard` pour les surfaces ;
- `AdminListSurface`, `tch-pagination` et les tables Material pour les listes ;
- `TchStatusBadge`, `TchFieldError`, `TchFormErrorSummary`, `TchNotice` et `tchMutation` pour le
  feedback.

Une nouvelle primitive n’est créée que si elle supprime une duplication réelle entre au moins deux
features. Les pages ne doivent pas importer les styles d’une autre feature pour simuler une primitive.

## Stable structural selectors

Les primitives de console exposent des `data-testid` stables pour les tests e2e de structure :

```text
admin-page-shell
admin-page-header
admin-page-actions
admin-page-body
admin-form-layout
admin-form-main
admin-form-aside
admin-form-footer
admin-refresh-button
admin-list-surface
admin-list-toolbar
admin-list-content
admin-list-footer
```

Ces identifiants décrivent la structure partagée, pas le contenu métier. Les tests e2e peuvent donc
vérifier qu’une page opérationnelle conserve ses blocs principaux après une évolution de design.
Les couleurs, espacements et typographies restent pilotés par les tokens `--tch-*` et les variables
locales `--comp-*`; les identifiants ne doivent jamais servir de sélecteurs CSS.

## Migration order

1. Formaliser les contrats d’inputs, les tokens locaux et les états des primitives partagées.
2. Migrer les détails seller terminal, ticket et tirage vers le même résumé latéral.
3. Migrer commission et rapports : source lisible, actions cohérentes, UUID masqués de la vue métier.
4. Migrer les tableaux et formulaires administratifs rencontrés dans les mêmes parcours.
5. Ajouter les tests de composant, les tests d’accessibilité essentiels et les scénarios e2e de
   navigation, action, erreur et responsive.
