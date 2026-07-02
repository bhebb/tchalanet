# web-async-state-resource-v1 — État async des pages via resource/rxResource

> Statut : PROPOSED — 2026-07-02
> Portée : admin-portal d'abord (pilotes setup + overview draws), puis standard pour toute page.
> Décision produit : **utiliser les APIs Angular les plus récentes (`resource`/`rxResource`),
> même encore en stabilisation** — pas de pipeline custom parallèle.

## Why

Mesures sur le code (2026-07-02) :

- `admin-config.page` (setup, une des pages les plus importantes) : **8 signals d'état** —
  1 chargement (`pageState`+`pageError`) + 2 sauvegardes × 3 signals (`saveState`/`formError`/`notice`).
- `admin-generated-draws.page` (overview) : **14 signals** — 1 chargement (5) + 2 canaux de
  mutation (5) + sélection + filtres.
- **34 pages** re-codent le mapping `ProblemDetail → ErrorViewModel` en méthode privée.
- ~58 pages recopient le pipeline `Subject` + `switchMap` + le squelette `@if loading/error/empty`.

C'est l'anti-pattern documenté « boolean explosion » (Kris Jenkins, *How Elm Slays a UI
Antipattern* ; David Khourshid, *Stop using isLoading booleans*). La réponse convergente de
l'industrie (TanStack Query, SWR, RTK Query) : séparer **query** (lecture : status+error+data+reload)
et **mutation** (écriture : pending+feedback+execute). Angular a intégré ce modèle nativement :
`resource()` / `rxResource()` sont l'implémentation RemoteData du framework (statut, erreur,
valeur, reload, annulation des requêtes obsolètes).

## What

1. **Lecture = resources créés par `TchBackendClient`.** Le client wrappe déjà `HttpClient`
   (résolution d'URL logique, options, unwrap `ApiResponse<T>`) — les factories de resources
   appartiennent à cette couche, pas aux pages :
   - `TchBackendClient.getResource<T>()` / `getPageResource<T>()` — `rxResource` interne
     construit sur `get()`/`getPage()` existants ;
   - les services métier (`data-access/`) exposent des **ResourceRefs typés**
     (`tenantsResource(filters)`) ;
   - les pages consomment le ResourceRef ; elles n'instancient jamais `rxResource`/`resource`
     pour un appel backend. Les paramètres viennent de l'URL
     (`toSignal(route.queryParamMap)`) — rechargement déclaratif, anti-race natif.
2. **Nouvelle lib fine `libs/web/async` (`@tch/web/async`)** — uniquement ce que ni le
   framework ni `libs/api` ne fournissent :
   - `resourceErrorVm(resource, source)` → `Signal<ErrorViewModel | null>` (mapping
     `ProblemDetail` centralisé, s'appuie sur `@tch/web/errors`) ;
   - `tchMutation()` — pending (global et par clé pour les actions de ligne), feedback
     succès/erreur en `ErrorViewModel`, `onSuccess` (typiquement `query.reload()`) ;
   - `<tch-async-view>` — composant template qui reçoit un `ResourceRef` et sélectionne
     loading / error(+retry) / empty / ready. Branche l'existant : `tch-loading`,
     `tch-error-panel`, `tch-admin-empty-state`.
3. **Pilotes** : `admin/setup/pages/settings/admin-config.page` (config multi-sections +
   éditions inline) puis `admin/draws/pages/overview/admin-generated-draws.page` (liste
   filtrée + actions de ligne + drawer). Ce sont les deux formes extrêmes : si les primitives
   tiennent sans cas particulier, elles tiennent partout.
4. **Contrat UX données complet** (pour ne pas y revenir) : loading différencié
   initial/rechargement/action avec anti-flash et **jamais de blanchiment au reload**
   (stale-while-revalidate via le statut `reloading`) ; erreurs avec retry + traceId ;
   `tch-pagination` partagé (« N–M sur Total », sélecteur de taille) ; filtres/tri **toujours**
   dans l'URL avec params standard `q`/`status`/`sort`/`page`/`size`. Détail : design §6.
5. **Codification** : playbook §1.3/§1.9/§2 (le standard de page devient resources + tchMutation
   + contrat UX), `state-management.md`, `AGENTS.md`. Nouvelle page = primitives obligatoires ;
   existant = migration opportuniste.

## Impact

- Nouvelle lib `libs/web/async` (dépend de `api` + `web/errors` ; `ui/components` inchangé).
- 2 pages pilotes migrées (aucun changement visuel/fonctionnel attendu).
- Docs conventions mises à jour (même PR que le code qui change la règle).
- Setup : 8 signals → ~3 déclarations. Overview : 14 → ~6 (filtres/sélection restent, c'est du
  vrai état UI).

## Non-goals

- **`libs/web/async` n'est pas une lib d'état** : primitives async uniquement — aucun store
  métier (`XxxStore`), aucun cache (`XxxCacheStore`) n'y entre ; l'état reste placé par
  ownership (feature / service métier), conformément à `state-management.md`.
- Pas de cache global ni d'invalidation par clés (données par page, pilotées par l'URL).
- Pas de retry/backoff automatique, pas d'optimistic updates.
- Pas de `httpResource` ni de `rxResource` créés dans les features pour des appels backend :
  la création de resources backend est le monopole de `TchBackendClient` (layering
  `Page → service métier → TchBackendClient`, AGENTS.md web). `httpResource` n'est même pas
  nécessaire en interne : les factories réutilisent `get()`/`getPage()` (unwrap `ApiResponse`).
- Pas de `BaseCrudPage`/génération d'écran — la page reste propriétaire de l'orchestration.
- Pas de cursor pagination, virtual scroll, chips de filtres auto, toasts auto-dismiss, ni
  librairie de skeletons (le slot `loading` de `tch-async-view` garde la porte ouverte).
- Pas de migration big-bang des ~90 pages.

## Risques et mitigations (statut API précis)

- Le contrat public `ResourceRef` / `ResourceStatus` (`idle`, `loading`, `reloading`,
  `resolved`, `error`, `local`) est documenté stable en v22 — **on ne dépend que de ce
  contrat**, jamais des internals Angular.
- `rxResource` est `@publicApi 22.0` (stable, vérifié dans les types installés) — il reste
  néanmoins **encapsulé dans `TchBackendClient`** : une évolution Angular se corrige en un
  point, les features ne voient que des `ResourceRef`.
- `httpResource` est explicitement expérimental **et** ne ferait ni l'unwrap `ApiResponse<T>`,
  ni les headers/options, ni le `ProblemDetail` : interdit tant que ces responsabilités vivent
  dans `TchBackendClient`.
- Les usages template passent par `tch-async-view` (contrat stable côté HTML) ; le mapping
  d'erreur est centralisé ; les pilotes sont couverts par les e2e existants setup/draws.
- Précédent bleeding-edge accepté en prod : signal forms dans `tch-login.page.ts`.
