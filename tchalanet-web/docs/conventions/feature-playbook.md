# Feature Playbook — écrans console (admin-portal / platform-portal)

> **Statut** : ACTIF v1.0 — 2026-07-02
> **Scope** : toute page des consoles privées. Le public-portal suit `pagemodel.md`, pas ce guide.
> **But** : créer un écran sans réinventer. Choisis l'archétype, copie le squelette, utilise les
> briques listées. Si tu dois styler autre chose que des briques, c'est une brique manquante —
> elle va dans `libs/ui/console`, pas dans la page.
> **Pièges bas niveau** (content-projection SCSS, overlay de soumission, rail sticky) :
> `.agents/skills/tchalanet-screen-realization/references/admin-page-patterns.md`.

---

## 0. Décision rapide

| Tu construis… | Archétype | Référence dans le code |
| ------------- | --------- | ---------------------- |
| Une liste d'entités (filtres, table, pagination) | **A. Liste** | `platform/tenants/pages/list/platform-tenants.page` |
| La fiche d'une entité | **B. Détail** (+ aside droite) | `platform/tenants/pages/detail/platform-tenant-detail.page` |
| Un formulaire de création riche | **C. Création** (page routée `new/`) | `admin/seller-terminals/pages/new/admin-seller-terminal-new.page` |
| Modifier quelques champs d'une entité | **D. Édition inline** dans le détail | section « Identité » du tenant detail |
| Confirmer/exécuter une action de ligne | **E1. Confirm dialog** | actions statut de `platform-tenants.page` |
| Saisie contextuelle riche depuis une ligne | **E2. Drawer droite** | `admin/draws/components/draw-result-drawer` |
| Une page de configuration à sections | **F. Config/setup** | `admin/setup/pages/settings/admin-settings.page` |

---

## 1. Règles transverses (toutes pages)

Structure de fichiers (détail dans `structure.md`) :

```text
features/<surface>/<feature>/
  pages/<page>/<prefix>-<page>.page.{ts,html,scss}
  components/<bloc>/<bloc>.component.{ts,html,scss}
  data-access/<feature>-api.service.ts
  <feature>.routes.ts        (lazy via loadChildren depuis admin.routes / platform.routes)
```

Invariants :

1. **`tch-admin-page-shell` est la racine de toute page** — `[title]`, `[description]`,
   slot `[meta]` (badges), slot `[actions]` (boutons d'en-tête).
2. **i18n obligatoire** — `TranslatePipe` + clés dans les bundles de la surface. Pas de texte
   FR codé en dur dans une nouvelle page (décision 2026-07-02).
3. **Lecture = resource, pas de signals loading/error à la main** (`@tch/web/async`, standard depuis
   `web-async-state-resource-v1`, pilotes : setup + overview draws). Le resource est créé **par
   `TchBackendClient`** (`getResource`/`getPageResource`, seul point de création) et exposé par le
   service data-access ; la page le consomme :
   ```ts
   // data-access : mapping DTO -> vue au niveau du client (param project)
   tenantsResource(query: () => TenantQuery) {
     return this.client.getPageResource<TenantView, TenantDto>(
       () => ({ path: '/platform/tenants', options: { params: toParams(query()), suppressShellFeedback: true } }),
       mapTenant,
     );
   }
   // page
   readonly tenants = this.api.tenantsResource(() => this.query());   // params dérivés de l'URL
   readonly tenantsError = resourceErrorVm(this.tenants, 'platform.tenants.list');
   ```
   Interdits : `loading`/`error`/`items` en signals manuels, enum `pageState()`, `Subject`+`switchMap`,
   et instancier `rxResource`/`httpResource` dans une feature (monopole du client).
4. **Squelette d'états = `<tch-async-view>`** (loading / erreur+retry / empty / ready) :
   ```html
   <tch-async-view [resource]="tenants" [error]="tenantsError()" [isEmpty]="isEmpty"
                   loadingLabel="…" (retry)="tenants.reload()">
     <tch-admin-empty-state empty … />
     <ng-template tchAsyncReady>
       @if (tenants.value(); as page) { <!-- table typée sur `page` --> }
     </ng-template>
   </tch-async-view>
   ```
   **Règle ready : lire `resource.value()` via `@if (…; as vm)`** pour obtenir la valeur typée —
   **ne pas** utiliser `let-vm` (le contexte est `unknown`, source de bugs).

   **Loading — 4 niveaux, gérés par `tch-async-view` (anti-flash ~300 ms / min ~500 ms intégré) :**

   | Situation | Indicateur |
   | --------- | ---------- |
   | Chargement initial (page/section) | `tch-loading` à la place du contenu |
   | **Rechargement** (filtre changé, reload après action) | **Données conservées** + barre discrète — **jamais de blanchiment** (stale-while-revalidate) |
   | Action en cours (submit) | `mutation.pending()` → bouton désactivé + spinner inline |
   | Action de ligne | `mutation.pending(key)` → indicateur sur la seule ligne concernée |
5. **Écriture = `tchMutation`, erreurs = `resourceErrorVm`** (`@tch/web/async`). Une mutation par
   opération : `pending()` (global/par clé), `feedback()` succès/erreur mappé, `onSuccess` (→
   `reload()`), `onError` (mapping champ serveur, renvoyer `true` = géré), double-submit bloqué,
   `idempotency.keyFactory` pour vente/reset PIN/résultat/override. Choix du composant d'affichage :

   | Portée | Composant | Source |
   | ------ | --------- | ------ |
   | La page ne peut pas charger | `tch-error-panel` (retry) | `tch-async-view` + `resourceErrorVm` |
   | Feedback d'une action | `tch-section-error` | `mutation.feedback()` |
   | Message ponctuel succès/erreur | `tch-notice` | mutation / signal local |
   | Erreur serveur d'un champ | `tch-field-error` | `serverFieldMessage(control)` |

   Ne plus recopier le mapping `webAppErrorFromProblemDetail` + `resolveErrorFeedbackCopy` +
   `toErrorViewModel` dans les pages : `resourceErrorVm` / `tchMutation` le font.
6. **API service dans `data-access/` de la feature**, `{ suppressShellFeedback: true }` quand la
   page gère elle-même son affichage d'erreur. Contrats génériques : `@tch/api`.
7. **Statuts métier** → `tch-status-badge` avec mapping explicite vers `BadgeStatus` :
   ```ts
   const map: Record<string, BadgeStatus> = {
     ACTIVE: 'ready', DRAFT: 'pending', SUSPENDED: 'warning',
     REJECTED: 'blocked', ARCHIVED: 'missing',
   };
   ```
8. Icônes : `<span class="material-symbols-outlined" aria-hidden="true">…</span>`.
9. **Baseline Angular moderne** (Angular 22) — tout nouveau composant utilise les APIs signal-first.
   Le détail de chaque API est dans les références de la skill `angular-developer`
   (`.agents/skills/angular-developer/references/`) :

   | API | Usage | Référence |
   | --- | ----- | --------- |
   | `input()` / `output()` / `model()` | Toujours — jamais `@Input`/`@Output` décorateurs | `inputs.md`, `outputs.md` |
   | `signal()` / `computed()` | État local et dérivé | `signals-overview.md` |
   | `linkedSignal()` | État dérivé mais réinitialisable (ex. sélection qui reset quand la liste change) | `linked-signal.md` |
   | `viewChild()` / `contentChild()` | Requêtes de vue signal-based — jamais `@ViewChild` décorateur | `components.md` |
   | `inject()` | Toujours — pas d'injection par constructeur | `di-fundamentals.md` |
   | `@if` / `@for` / `@switch` / `@defer` | Control flow natif — jamais `*ngIf`/`*ngFor` | `components.md` |
   | **Signal Forms** (`@angular/forms/signals`) | **Défaut pour tout nouveau formulaire** (précédent : `tch-login.page.ts`) | `signal-forms.md` |
   | `ReactiveFormsModule` | Accepté pour l'existant (65 fichiers) et les cas non couverts par signal forms ; migration opportuniste | `reactive-forms.md` |
   | Resources (`TchBackendClient.getResource`/`getPageResource`, param `project`) | **Standard de chargement** (§1.3) : le client crée les resources (rxResource interne + projection DTO→vue), les services exposent des ResourceRefs typés, la page consomme. Jamais de `rxResource`/`httpResource` dans une feature | `resource.md` |
   | `effect()` | Dernier recours — préférer `computed()`/`linkedSignal()` | `effects.md` |

   Toujours : `standalone: true` + `ChangeDetectionStrategy.OnPush`.

---

## 2. Archétype A — Liste

```html
<tch-admin-page-shell [title]="…" [description]="…">
  <div actions>
    <a tch-action variant="primary" routerLink="…/new">+ Créer</a>
  </div>

  <tch-admin-list-surface
    [searchValue]="…" [statusValue]="…" [statusOptions]="…"
    (searchChange)="onSearchFilter($event)" (statusChange)="onStatusFilter($event)"
    (resetFilters)="resetFilters()">

    <ng-container list-content>
      <!-- squelette d'états (§1.4), puis : -->
      <table mat-table [dataSource]="items()" matSort (matSortChange)="onSortChange($event)">
        <!-- colonnes ; dernière colonne = menu (…) mat-icon-button + mat-menu -->
        <tr mat-row *matRowDef="let row; columns: displayedColumns" [routerLink]="[rowId(row)]"></tr>
      </table>
    </ng-container>

    <ng-container list-footer>
      <!-- total + prev/next -->
    </ng-container>
  </tch-admin-list-surface>
</tch-admin-page-shell>
```

Règles :

- **L'URL est la source de vérité** pour `q`, `status`, `sort`, `page`, `size`. Les filtres font
  `router.navigate([], { queryParams, queryParamsHandling: 'merge' })` ; la page écoute
  `route.queryParamMap` et recharge.
- **Chargement anti-race** : un `Subject` de trigger + `switchMap` vers l'API (une réponse lente
  ne doit pas écraser un résultat filtré plus récent) + `takeUntilDestroyed`.
- **Deux états vides distincts** : « aucun résultat pour ces filtres » (icône `search_off` +
  bouton reset) vs « aucune donnée » (CTA de création).
- Actions de ligne dans un `mat-menu` (…) ; clic sur la ligne = navigation détail.
- Ne pas utiliser `AdminDataTable`/`AdminListToolbar`/`AdminMobileCardList` (kit deprecated).
- Alternative toolbar : pour des filtres non-standard (chips de dates, selects multiples),
  utiliser `tch-admin-crud-shell` + `tch-admin-data-toolbar` (cf. `admin-generated-draws.page`).

### Pagination, tri, filtres — le standard (ne pas improviser)

**Params URL standard** — toute liste utilise ces noms, valeurs par défaut **omises** de l'URL :

```text
q        recherche texte (debounce ~300 ms — déjà géré par tch-admin-list-surface)
status   filtre de statut
sort     tri : "field,asc|desc" — UN seul critère, appliqué côté serveur
page     index 0-based
size     taille de page
```

Params métier additionnels : préfixés par le domaine (ex. `drawDate`, `channelId`).

**Filtres :**

- **Jamais de filtre en signal local** : l'état de vue complet vit dans l'URL
  (deep-link, bouton retour, partage). Lecture via `toSignal(route.queryParamMap)` +
  computeds ; écriture via `router.navigate({ queryParams, queryParamsHandling: 'merge' })`.
- **Parsing via `@tch/web/async`** — ne pas recopier de helpers par page :
  `numberParam(v, fallback)`, `dateParam(v)`, `textParam(v)`, `enumParam(v, allowed, fallback)`.
- Changer un filtre remet `page` à 0.
- Reset = navigation qui efface les params (pas un `form.reset()` seul).

**Tri :**

- `matSort` sur les en-têtes + `matSortDisableClear` (pas d'état « sans tri »).
- Tri par défaut **explicite et documenté** par liste (ex. `updatedAt,desc`).
- Le tri est envoyé au serveur — jamais de `.sort()` client sur une liste paginée.

**Pagination :**

- Serveur via `TchPage<T>` (`totalElements`, `hasNext`, `hasPrevious`).
- Affichage : `tch-pagination` (`@tch/ui/console`) — « N–M sur Total » + prev/next + sélecteur de
  taille (10/20/50), piloté par les query params `page`/`size`. Ne pas recoder de footer de
  pagination (exception : une table à présentation bespoke qui possède déjà son pied, ex.
  `generated-draws-table` groupée par date).
- Au changement de page/filtre : **pas de blanchiment** — voir §1.4.

---

## 3. Archétype B — Détail (+ partie de droite)

```html
<tch-admin-page-shell [title]="…">
  <div meta><!-- tch-status-badge + chips type/devise/timezone --></div>
  <div actions>
    <a mat-button routerLink="…">← Retour à la liste</a>
    <!-- actions de statut conditionnelles -->
  </div>

  <!-- squelette d'états (§1.4), puis : -->
  <mat-tab-group> <!-- seulement si plusieurs domaines : Aperçu / Sous-entités / Config / Audit -->
    <mat-tab label="…">
      <tch-admin-detail-layout>
        <div main>
          <tch-admin-section-card title="…" icon="…">
            <dl><div><dt>Label</dt><dd>Valeur</dd></div>…</dl>
          </tch-admin-section-card>
          <!-- autres sections -->
        </div>
        <div aside>
          <tch-identity-card variant="compact" [title]="…" [code]="…" [status]="…" [meta]="…" />
        </div>
      </tch-admin-detail-layout>
    </mat-tab>
  </mat-tab-group>
</tch-admin-page-shell>
```

Règles :

- La « partie de droite » **statique** (résumé permanent) = slot `[aside]` de
  `tch-admin-detail-layout`. Pour un panneau **contextuel** ouvert à la demande → archétype E2.
- Une section = une `tch-admin-section-card` avec `dl/dt/dd` pour les faits.
- Onglets secondaires : chargement lazy au premier clic (`selectedIndexChange`), avec leurs
  propres signals `xxxLoading/xxxError`.
- Champ absent → `'common.not_available' | translate`, pas de chaîne vide.

---

## 4. Archétype C — Création

Page routée `pages/new/` (route `…/new` ou `…/onboarding`). **Pas de dialog de création** dès que
le formulaire dépasse 3–4 champs.

```html
<tch-admin-page-shell title="Nouveau …">
  <div actions><a mat-stroked-button routerLink="…">← Retour à la liste</a></div>

  @if (successResult(); as result) {
    <!-- carte de succès : récap + actions "voir" / "créer un autre" / "retour liste" -->
  } @else {
    <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
      <div class="form-col">
        @if (error(); as vm) { <tch-error-panel [title]="vm.title" [message]="vm.message" /> }

        <tch-admin-section-card title="Identité" icon="badge" description="…">
          <mat-form-field appearance="outline">…</mat-form-field>
          <tch-field-error [message]="serverFieldMessage(form.controls.xxx)" />
        </tch-admin-section-card>
        <!-- une section-card par groupe de champs -->

        <div class="footer">
          <a mat-stroked-button routerLink="…">Annuler</a>
          <button mat-flat-button color="primary" type="submit" [disabled]="saving()">…</button>
        </div>
      </div>

      <div class="preview-col"><!-- optionnel : aperçu live --></div>
    </form>
  }
</tch-admin-page-shell>
```

Règles :

- Un groupe de champs = une `tch-admin-section-card` (`title`, `icon`, `description`).
- **Nouveau formulaire = Signal Forms** (`@angular/forms/signals`, cf. §1.9) ; les squelettes
  ci-dessus montrent l'existant en ReactiveForms — adapter (`form()`, `[control]`) pour les
  nouvelles pages. Référence code : `libs/core/auth/src/lib/login/tch-login.page.ts`.
- **Double validation** : validators (schéma signal forms ou validators Angular/`mat-error`) +
  erreurs serveur par champ (`tch-field-error` alimenté depuis le `ProblemDetail`).
- Submit : `saving = signal(false)`, bouton désactivé + label « en cours » pendant l'appel.
- Après succès : **état de succès dans la page** (pas de redirect sec) avec les actions suivantes.
- Colonne d'aperçu live à droite : optionnelle, recommandée pour les entités « visuelles ».

---

## 5. Archétype D — Édition inline

Pas de page `edit/` dédiée : l'édition se fait **dans la section-card du détail**.

```ts
readonly showIdentityForm = signal(false);
readonly identityFormState = signal<'idle' | 'submitting' | 'success'>('idle');
```

```html
<tch-admin-section-card title="Identité" icon="badge">
  @if (!showIdentityForm()) {
    <dl>…faits…</dl>
    <button mat-stroked-button (click)="openIdentityForm()">{{ 'common.edit' | translate }}</button>
  } @else {
    <form [formGroup]="identityForm" (ngSubmit)="submitIdentity()">
      <!-- mat-form-field × N -->
      <div class="form-actions">
        <button mat-button type="button" (click)="cancelIdentityForm()">Annuler</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="…">Enregistrer</button>
      </div>
    </form>
  }
</tch-admin-section-card>
```

Après succès : recharger l'entité + `tch-notice type="success"` en haut de page.

---

## 6. Archétype E — Actions contextuelles

### E1. Confirm dialog (action de statut, action destructive)

```ts
const data: TchConfirmDialogData = {
  title: …, message: …, confirmLabel: …,
  destructive: true,        // action risquée
  sensitive: true,          // action sensible (audit)
  requireReason: true,      // exige une raison saisie
  auditLabel: …,
};
this.dialog.open(TchConfirmDialog, { data }).afterClosed().subscribe(result => {
  if (!result?.confirmed) return;
  this.runAction(…);        // helper commun : feedback signal + reload
});
```

Le helper `runAction` : reset du feedback → appel API → succès = `actionFeedback` info + reload ;
échec = `actionFeedback` erreur mappée. Cf. `runTenantAction` dans `platform-tenants.page.ts`.

### E2. Drawer à droite (saisie contextuelle)

Composant de feature `components/<x>-drawer/`, **contrôlé par la page** :

```html
@if (selectedItem(); as item) {
  <tch-xxx-drawer
    [item]="item" [canDoX]="…" [saveState]="…"
    (closed)="onDrawerClosed()" (saveRequested)="onSaveRequested($event)" />
}
```

Règles : le drawer reçoit tout par `input()`, émet par `output()`, **n'appelle pas l'API** —
la page orchestre. Modes internes du drawer (`saisie`/`lecture`/`modification`) = signals locaux.

---

## 7. Archétype F — Config/setup

Page de configuration = page détail sans entité listée : `tch-admin-page-shell` +
`tch-admin-section-card` par domaine de config, édition inline (archétype D) par section,
readiness/next-steps en `[aside]` si utile (`admin-next-steps-card`,
`admin-provisioning-health-card`). Référence : `admin/setup/pages/settings/`.

---

## 8. Table de décision des briques

| Besoin | Brique | Lib |
| ------ | ------ | --- |
| Racine + en-tête de page | `tch-admin-page-shell` | `@tch/ui/console` |
| Section de contenu / groupe de champs | `tch-admin-section-card` | `@tch/ui/console` |
| Layout détail avec aside droite | `tch-admin-detail-layout` | `@tch/ui/console` |
| Toolbar/contenu/footer d'une liste | `tch-admin-crud-shell` | `@tch/ui/console` |
| Recherche + filtre statut standard | `tch-admin-list-surface` | `@tch/ui/components` |
| Recherche inline simple | `tch-admin-data-toolbar` | `@tch/ui/console` |
| État vide | `tch-admin-empty-state` | `@tch/ui/console` |
| Carte identité (aside) | `tch-identity-card` | `@tch/ui/console` |
| Badge de statut | `tch-status-badge` | `@tch/ui/components` |
| Chargement | `tch-loading` | `@tch/ui/components` |
| Erreur de page (retry) | `tch-error-panel` | `@tch/ui/components` |
| Feedback d'action | `tch-section-error` | `@tch/ui/components` |
| Notice succès/erreur | `tch-notice` | `@tch/ui/components` |
| Erreur serveur de champ | `tch-field-error` | `@tch/ui/components` |
| Confirmation d'action | `TchConfirmDialog` | `@tch/ui/components` |
| CTA principal/secondaire | `tch-action` (`TchActionButton`) | `@tch/ui/components` |
| Table de données | `mat-table` à la main (brique console à venir — C2) | Material |
| **Lecture async (resource)** | `TchBackendClient.getResource`/`getPageResource` (param `project`) | `@tch/api` |
| **États de vue (loading/erreur/empty/ready)** | `tch-async-view` + `ng-template tchAsyncReady` | `@tch/web/async` |
| **Écriture (pending/feedback/idempotency)** | `tchMutation` | `@tch/web/async` |
| **Erreur resource → ViewModel** | `resourceErrorVm` | `@tch/web/async` |
| **Erreur serveur de champ (helper)** | `serverFieldMessage` | `@tch/web/async` |
| **Parsing query params (page/size/sort/date/enum)** | `numberParam`/`dateParam`/`textParam`/`enumParam` | `@tch/web/async` |
| **Pagination (N–M sur Total + taille)** | `tch-pagination` | `@tch/ui/console` |

---

## 9. Anti-patterns

```text
Kit admin-crud deprecated (AdminDataTable, AdminFormShell, AdminListToolbar,
  AdminMobileCardList, AdminFormActions, AdminStatusPill)   → ne plus utiliser
Texte FR codé en dur dans une nouvelle page                  → TranslatePipe obligatoire
Appel HTTP dans un component de feature ou un drawer         → la page/API service orchestre
Signals loading/error/items à la main pour un fetch          → resource + tch-async-view
Enum pageState() dans une nouvelle page                      → resource (status intégré)
Subject + switchMap pour un chargement                       → resource (anti-race natif)
rxResource/httpResource instancié dans une feature           → TchBackendClient.get*Resource
Mapping ProblemDetail recopié dans la page                   → resourceErrorVm / tchMutation
let-vm sur ng-template tchAsyncReady                         → @if (resource.value(); as vm)
Helpers numberParam/dateParam recopiés par page              → @tch/web/async
Filtres/pagination en état local sans passer par l'URL       → queryParams source de vérité
Footer de pagination recodé                                  → tch-pagination
Dialog de création pour un formulaire riche                  → page routée new/
Page edit/ dédiée pour 3 champs                              → édition inline (archétype D)
Styler la page au lieu d'enrichir la brique console          → la brique va dans ui/console
```
