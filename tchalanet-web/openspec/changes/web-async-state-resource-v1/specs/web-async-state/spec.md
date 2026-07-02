## ADDED Requirements

### Requirement: Resources de lecture créés par TchBackendClient

`TchBackendClient` SHALL exposer des factories de resources (`getResource<T>`,
`getPageResource<T>`) construites sur `rxResource` et ses méthodes `get()`/`getPage()`
existantes (URL logique, `TchRequestOptions`, unwrap `ApiResponse<T>`). Les services métier
SHALL exposer des ResourceRefs typés ; les pages les consomment, paramétrés par un signal
dérivé de l'URL quand la page est filtrable, au lieu du pipeline manuel `Subject` +
`switchMap` + signals `loading`/`error`/`data` séparés.

#### Scenario: Chargement d'une liste filtrée par l'URL

- **GIVEN** une page liste console dont les filtres vivent dans les query params
- **WHEN** un query param change (recherche, statut, page)
- **THEN** le resource exposé par le service relance le chargement avec les nouveaux params
- **AND** la requête précédente encore en vol est annulée (pas d'écrasement par une réponse obsolète)
- **AND** la page n'a déclaré aucun signal manuel `loading`/`error`/`items`.

#### Scenario: Création de resource backend hors du client interdite

- **GIVEN** un développeur ou un agent crée un chargement de données backend
- **WHEN** il choisit la primitive
- **THEN** il n'instancie ni `rxResource`, ni `resource()`, ni `httpResource` dans une page ou un service de feature pour cet appel
- **AND** il passe par une factory de `TchBackendClient` exposée via le service métier (layering `Page → service → TchBackendClient`).

### Requirement: Mapping d'erreur centralisé resourceErrorVm

La lib `@tch/web/async` SHALL exposer `resourceErrorVm(resource, source)` qui transforme
l'erreur d'un resource en `ErrorViewModel` affichable (via `@tch/web/errors`), supprimant les
copies privées de ce mapping dans les pages.

#### Scenario: Erreur backend ProblemDetail

- **GIVEN** un resource dont le chargement échoue avec un `ProblemDetail`
- **WHEN** la page lit `resourceErrorVm(res, source)()`
- **THEN** elle obtient un `ErrorViewModel` (titre, message, sévérité) issu du resolver de copy d'erreurs
- **AND** aucune page migrée ne contient de méthode privée `errorViewModel(...)`.

### Requirement: tchMutation pour les écritures

La lib `@tch/web/async` SHALL exposer `tchMutation()` couvrant l'état d'écriture : `pending()`
global et par clé, `feedback()` succès/erreur en `ErrorViewModel`, `execute()` et hook
`onSuccess` (rechargement du resource concerné).

#### Scenario: Sauvegarde d'une section de configuration

- **GIVEN** la page setup avec une mutation `saveLocale`
- **WHEN** l'utilisateur soumet la section
- **THEN** `saveLocale.pending()` pilote l'état du bouton
- **AND** en cas de succès `onSuccess` recharge le resource de la section et `feedback()` alimente une notice de succès
- **AND** en cas d'échec `feedback()` alimente `tch-section-error` sans signal ad hoc dans la page.

#### Scenario: Action de ligne avec pending par clé

- **GIVEN** une liste avec une action par ligne (ex. verrouiller un tirage)
- **WHEN** l'action est lancée pour la ligne `id`
- **THEN** `mutation.pending(id)` est vrai pour cette ligne seulement
- **AND** les autres lignes restent actionnables.

### Requirement: tch-async-view pour les états de template

La lib `@tch/web/async` SHALL exposer `<tch-async-view>` qui reçoit un resource (+ son
`ErrorViewModel`) et rend l'état correspondant — loading, erreur avec retry, vide, ou le
template ready — en composant les briques existantes (`tch-loading`, `tch-error-panel`,
`tch-admin-empty-state`).

#### Scenario: Page migrée sans squelette d'états manuel

- **GIVEN** une page pilote migrée
- **WHEN** son template rend les données
- **THEN** il n'y a plus de chaîne manuelle `@if (loading()) … @else if (error()) … @else if (vide) …`
- **AND** le retry de l'état d'erreur appelle `resource.reload()`
- **AND** l'état d'erreur page affiche le `traceId` du ProblemDetail quand il existe
- **AND** l'état vide est distinct de l'état chargé.

### Requirement: Loading différencié initial / rechargement / action

Le rendu de chargement SHALL distinguer le chargement initial (indicateur à la place du
contenu), le rechargement (`reloading` : les données précédentes restent affichées avec un
indicateur discret — jamais de blanchiment) et l'action en cours (indicateur sur le bouton ou
la ligne). Les indicateurs SHALL être anti-flash (apparition différée ~300 ms, durée minimale
~500 ms), géré par `tch-async-view` et non par chaque page.

#### Scenario: Changement de filtre sans blanchiment

- **GIVEN** une liste chargée à l'écran
- **WHEN** l'utilisateur change un filtre et que le resource passe en `reloading`
- **THEN** les lignes existantes restent visibles avec un indicateur de progression discret
- **AND** le contenu n'est pas remplacé par un spinner pleine zone.

#### Scenario: Réponse rapide sans flicker

- **GIVEN** un chargement qui répond en moins de ~300 ms
- **WHEN** la page charge ou recharge
- **THEN** aucun indicateur de chargement n'a flashé à l'écran.

### Requirement: Pagination, filtres et tri standardisés dans l'URL

Les listes paginées SHALL utiliser un composant de pagination partagé (« N–M sur Total »,
précédent/suivant, sélecteur de taille) et porter tout leur état de vue dans l'URL avec les
noms standard `q`, `status`, `sort`, `page`, `size` (params métier additionnels préfixés,
valeurs par défaut omises). Le tri SHALL être mono-critère `sort=field,dir` appliqué côté
serveur, avec un tri par défaut explicite.

#### Scenario: Deep-link restaure l'état complet de la liste

- **GIVEN** une URL de liste contenant `q`, `status`, `sort`, `page` et `size`
- **WHEN** l'utilisateur ouvre cette URL directement ou revient en arrière
- **THEN** la recherche, le filtre, le tri, la page et la taille de page sont restaurés
- **AND** aucun filtre de la page ne vit dans un signal local non reflété dans l'URL.

#### Scenario: Pagination partagée

- **GIVEN** une liste console migrée
- **WHEN** elle rend son pied de liste
- **THEN** elle utilise le composant de pagination partagé (total, plage affichée, sélecteur de taille)
- **AND** aucun footer de pagination recodé localement.

### Requirement: Pilotes setup et overview draws

Le change SHALL être validé par la migration de `admin-config.page` (setup) et
`admin-generated-draws.page` (overview) sans changement fonctionnel ni visuel.

#### Scenario: Setup migré

- **GIVEN** la page setup migrée sur rxResource + tchMutation
- **WHEN** un tenant admin charge, édite et sauvegarde chaque section
- **THEN** les comportements loading/erreur/succès sont identiques à l'existant
- **AND** la page ne déclare plus ses 8 signals d'état manuels.

#### Scenario: Overview draws migré

- **GIVEN** la page overview des tirages migrée
- **WHEN** l'admin filtre, exécute des actions de cycle de vie et saisit un résultat via le drawer
- **THEN** les comportements existants (dont e2e) restent verts
- **AND** le rechargement après action passe par `resource.reload()`.
