# Design — web-async-state-resource-v1

## 1. Lecture : resources créés par `TchBackendClient`

Le client wrappe déjà `HttpClient` (URL logique, `TchRequestOptions`, unwrap `ApiResponse<T>`) —
les factories de resources vivent dedans, en réutilisant `get()`/`getPage()` :

```ts
// libs/api/src/lib/backend-client (extension)
getResource<T>(
  request: () => { path: string; options?: TchRequestOptions } | undefined,
): ResourceRef<T | undefined> {
  return rxResource({
    injector: this.injector,                    // création possible hors contexte d'injection
    params: request,                            // undefined ⇒ pas de chargement (lazy)
    stream: ({ params }) => this.get<T>(params.path, params.options),
  });
}
getPageResource<T>(…): ResourceRef<TchPage<T> | undefined>   // idem sur getPage()
```

Service métier — expose des **ResourceRefs typés**, le path logique reste dans `data-access/` :

```ts
// platform-tenants-api.service.ts (cible)
tenantsResource(filters: Signal<TenantFilters>) {
  return this.client.getPageResource<TenantSummaryView>(() => ({
    path: '/platform/tenants',
    options: { params: toHttpParams(filters()), suppressShellFeedback: true },
  }));
}
```

Page — consomme, ne construit rien :

```ts
// platform-tenants.page.ts (cible)
private readonly queryParams = toSignal(this.route.queryParamMap, {
  initialValue: this.route.snapshot.queryParamMap,
});
readonly filters = computed(() => toTenantFilters(this.queryParams()));   // URL = source de vérité
readonly tenants = this.api.tenantsResource(this.filters);
readonly tenantsError = resourceErrorVm(this.tenants, 'platform.tenants.list');
```

Obtenu gratuitement : `tenants.isLoading()`, `.value()`, `.error()`, `.reload()`, annulation
automatique de la requête précédente quand les filtres changent (remplace `Subject`+`switchMap`).

Règles :

- **Les features n'instancient jamais `rxResource`/`resource`/`httpResource` pour un appel
  backend** — monopole de `TchBackendClient` (layering `Page → service → client`). `resource()`
  nu reste permis pour des sources non-backend (Promise locale, etc.).
- `httpResource` inutile même en interne : il ne ferait ni l'unwrap `ApiResponse` ni la
  résolution `TchRequestOptions` — les factories composent `rxResource` + `get()`/`getPage()`.
- Chargements par section (setup) : un resource par section, pas un état de page monolithique.
- Chargement lazy (onglet) : la fonction `request` renvoie `undefined` tant que l'onglet n'est
  pas visité — un resource sans params ne charge pas.

## 2. Erreur : `resourceErrorVm`

```ts
// libs/web/async
export function resourceErrorVm(
  res: { error: Signal<unknown> },
  source: string,
): Signal<ErrorViewModel | null>;
```

Un seul endroit fait `webAppErrorFromProblemDetail` + `resolveErrorFeedbackCopy` +
`toErrorViewModel` (supprime les 34 copies privées). L'injection de `TranslateService` se fait
dans la factory (contexte d'injection requis — appeler depuis un champ de classe).

## 3. Écriture : `tchMutation`

```ts
readonly saveLocale = tchMutation({
  run: (input: LocaleSettingsRequest) => this.api.saveLocale(input, { suppressShellFeedback: true }),
  source: 'admin.setup.locale',
  onSuccess: () => this.config.reload(),
});
// saveLocale.pending() : boolean          — désactive le submit
// saveLocale.pending(key) : boolean       — action de ligne (key = id)
// saveLocale.feedback() : { kind: 'success' | 'error'; vm: ErrorViewModel } | null
// saveLocale.execute(input, { key? })
// saveLocale.clearFeedback()
```

Angular n'a pas de primitive mutation : celle-ci reste volontairement minimale (~60 lignes).
Le feedback alimente `tch-section-error` / `tch-notice` ; les erreurs de champ serveur restent
sur `tch-field-error` via le helper `serverFieldMessage` (déplacé dans `@tch/web/async` ou
`@tch/web/errors`, une seule copie).

**Double-submit et idempotency :**

- `execute()` est **no-op si déjà `pending()`** (pour la même clé) — le double-clic est bloqué
  localement par défaut, sans que chaque page y pense.
- Hook optionnel pour les endpoints idempotents (vente, reset PIN, résultat manuel, override) :

```ts
tchMutation({
  run: …,
  idempotency?: { keyFactory: () => string },   // défaut : crypto.randomUUID() par execute()
});
```

La clé est fournie au `run` (header/champ selon le contrat de l'endpoint — le backend a déjà
la règle formelle : même clé + même payload rejoue la même ressource, même clé + payload
différent ⇒ conflit ; précédent web : `platform-ops-api.service.ts`). `tchMutation` ne
remplace **aucun** invariant backend — audit, permissions, idempotency, validation et
transitions restent serveur.

## 4. Template : `<tch-async-view>`

```html
<tch-async-view [resource]="draws" [error]="drawsError()"
                [loadingLabel]="'admin.draws.loading' | translate"
                (retry)="draws.reload()">
  <tch-admin-empty-state empty icon="confirmation_number" … />
  <ng-template tchAsyncReady let-draws>
    <tch-generated-draws-table [groups]="group(draws)" … />
  </ng-template>
</tch-async-view>
```

- États dérivés du `ResourceRef` : `isLoading` → `tch-loading` ; `error` → `tch-error-panel`
  (+ retry) ; valeur vide → slot `empty` ; sinon template `ready` avec la valeur en contexte.
- `empty` est déterminé par un prédicat input (`[isEmpty]`) avec défaut « array/items vide ».
- **Lecture seule et layout stable** : `tch-async-view` ne gère jamais les mutations — le
  pending par ligne/bouton reste sur `tchMutation` dans le template `ready`. En `reloading`,
  la valeur précédente reste rendue (le `ResourceRef` la conserve) — le composant ne
  démonte pas le template `ready`, il superpose l'indicateur discret.
- Le composant vit dans `libs/web/async` (il dépend d'`ErrorViewModel`) et compose les briques
  existantes de `@tch/ui/components`/`@tch/ui/console`.

## 5. Placement et frontières Nx

```text
libs/api        (extension backend-client)
  getResource / getPageResource                ← rxResource interne, seul endroit

libs/web/async  (@tch/web/async)
  resource-error-vm.ts                         ← dépend de web/errors (api ne peut pas)
  tch-mutation.ts
  server-field-message.ts
  tch-async-view.component.ts (+ directive tchAsyncReady)

web/async → api, web/errors, ui/components, ui/console (empty-state)
apps/*    → web/async, api
```

`resourceErrorVm` ne peut pas vivre dans `libs/api` : le mapping en `ErrorViewModel` dépend de
`web/errors` (+ translate), et `web/errors → api` existe déjà — l'inverse serait circulaire.

**`web/async` = primitives async, PAS une lib d'état.** N'y entrent jamais :

```text
TenantConfigStore · DrawsStore · CatalogCacheStore · AdminSetupStore · tout XxxStore/XxxCache
```

L'état métier reste placé par ownership (feature / service métier / lib runtime propriétaire),
conformément à `state-management.md`. Un helper de parsing/serialization des params URL
(`pagination-url.ts`) est acceptable ; un store ne l'est pas.

Pas de nouvelle dépendance npm. `rxResource` n'est référencé qu'à un seul endroit
(`TchBackendClient`) : si l'API Angular bouge à une montée de version, un seul fichier change
et le contrat des services/pages (ResourceRef) reste stable.

## 6. Contrat UX données : loading, erreurs, pagination, filtres, tri

Audit vs patterns industrie (2026-07-02) — ce qui est conforme reste, ce qui manque entre dans
ce change pour ne pas avoir à y revenir.

### Loading — 4 niveaux différenciés

Référence industrie : seuils de temps de réponse NN/g (0,1 s / 1 s / 10 s), skeleton screens,
et **stale-while-revalidate** (SWR/TanStack Query : on ne blanchit jamais des données déjà
affichées pendant un refetch). Le statut `reloading` des resources Angular mappe exactement ça.

| Niveau | Déclencheur | Rendu | Porté par |
| ------ | ----------- | ----- | --------- |
| Page/section initiale | `status === 'loading'` | `tch-loading` (spinner) ; slot `loading` surchargeable pour un skeleton spécifique | `tch-async-view` |
| Rechargement | `status === 'reloading'` (filtre changé, reload après action) | **les données précédentes restent affichées** + barre de progression discrète — jamais de blanchiment | `tch-async-view` |
| Action (bouton) | `mutation.pending()` | bouton désactivé + spinner inline + label « en cours » | `tchMutation` |
| Action (ligne) | `mutation.pending(key)` | spinner sur la ligne concernée seulement | `tchMutation` |

Anti-flash intégré à `tch-async-view` (pas par page) : l'indicateur n'apparaît qu'après
**300 ms** et reste au moins **500 ms** une fois montré (pas de flicker sur les réponses
rapides). Corrige l'existant : aujourd'hui chaque reload blanchit la page.

### Erreurs — verdict : conforme, deux renforts

Le modèle 3 niveaux (page `tch-error-panel`+retry / section `tch-section-error`+`tch-notice` /
champ `tch-field-error`) correspond au pattern industrie (hiérarchie type error boundaries).
Renforts dans ce change : (a) le retry est systématique et branché `resource.reload()` par
`tch-async-view` ; (b) le `traceId` du `ProblemDetail` est affiché dans l'état d'erreur page
(support) — porté par `resourceErrorVm`/`tch-async-view`, plus jamais recodé par page.

### Pagination — composant partagé, standard console

Référence : convention Material paginator / consoles admin (AWS, Stripe). Aujourd'hui chaque
liste recode son footer prev/next. Cible : **`tch-pagination`** dans `libs/ui/console`
(convergence avec le chantier C2 table) :

```text
« N–M sur Total » + prev/next + sélecteur de taille (10 / 20 / 50)
état dans l'URL (page, size) ; source serveur TchPage<T> (totalElements, hasNext/hasPrevious)
```

### Filtres — URL toujours, debounce déjà là

Conforme : URL source de vérité (deep-link, back, partage), debounce du search (existant dans
`AdminListSurface`, 300 ms par défaut), reset, empty-state filtré ≠ vide. Règles durcies :

- **tout** filtre passe par l'URL — aucun filtre en signal local (le pilote overview migre ses
  filtres locaux le cas échéant) ;
- noms de params standard : `q`, `status`, `sort`, `page`, `size` (+ params métier préfixés) ;
- valeur par défaut ⇒ param omis de l'URL (URLs propres, déjà le cas).

### Tri — verdict : conforme, à figer

`mat-sort` en-tête + direction, un seul critère, `sort=field,dir` dans l'URL, tri serveur pour
les listes paginées (pattern `platform-tenants`). À figer : tri par défaut explicite et
documenté par liste ; `matSortDisableClear` pour éviter l'état « sans tri ».

### Non-goals UX

Cursor pagination, virtual scroll, chips de filtres actifs auto-générées, toasts auto-dismiss,
librairie de skeletons — rien de tout ça tant qu'un besoin réel ne le justifie pas (le slot
`loading` de `tch-async-view` laisse la porte ouverte aux skeletons sans framework).

## 7. Ce que les pilotes doivent prouver

| Pilote | Cas validés |
| ------ | ----------- |
| `admin-config.page` (setup) | resources par section, 2 mutations avec feedback distincts, édition inline, erreurs de champ serveur |
| `admin-generated-draws.page` | params depuis l'URL (filtres/presets), pending par ligne, drawer alimenté par mutation, reload après action |

Matrice de couverture minimale (chaque pilote, avant de cocher la tâche) :

```text
chargement initial · reload (stale data visible) · erreur initiale ·
erreur au reload avec données précédentes affichées · empty state ·
mutation succès · mutation erreur · pending par ligne · double-clic bloqué
```

Critère anti-usine à gaz : si un pilote exige un flag/option de plus sur les primitives pour un
besoin local, le besoin reste dans la page — on n'étend pas la primitive.

## 8. Garde-fous (checklist de revue)

- [ ] `libs/web/async` n'expose ni store métier ni cache — primitives async uniquement.
- [ ] Aucun appel backend ne contourne `TchBackendClient` (`HttpClient`, `httpResource`,
      `resource`/`rxResource` nus interdits dans les features pour le backend).
- [ ] Les features ne dépendent que du contrat public `ResourceRef`/`ResourceStatus` —
      `rxResource` (expérimental) n'apparaît que dans `TchBackendClient`.
- [ ] `tchMutation` bloque le double-submit localement et supporte une clé d'idempotence
      explicite pour les endpoints qui l'exigent ; les invariants (audit, permissions,
      validation, transitions) restent serveur.
- [ ] `tch-async-view` ne gère que la lecture ; les mutations page/ligne restent dehors.
- [ ] `reloading` ne blanchit jamais une zone déjà chargée.
- [ ] Filtres/tri/pagination canoniques dans l'URL : `q`, `status`, `sort`, `page`, `size`.
- [ ] Les erreurs affichées préservent `traceId`/`requestId`/`errorId` quand disponibles.
- [ ] La matrice de couverture des pilotes (§7) est complète.
