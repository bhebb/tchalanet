# Admin Page Patterns — Tchalanet

Retour d'expérience issu de la construction de la page d'onboarding tenant (`feature/admin-ui-identity-v0`). Ce document est opérationnel : il décrit les décisions prises, les problèmes rencontrés, et les patterns retenus pour les pages admin — création, liste, et actions confirmées. À compléter au fil des prochains écrans.

---

## 1. Page de création / formulaire long

### Structure de référence

```
AdminPageShellComponent          ← titre + back-action
  └─ form[formGroup]             ← wrapper réactif, position: relative
       ├─ page-loader overlay    ← visible seulement pendant submitting()
       └─ AdminDetailLayoutComponent
            ├─ [main]            ← sections de formulaire empilées
            ├─ [aside]           ← rail droit sticky (identité + état)
            └─ [footer]          ← actions + erreur de soumission
```

**Règle fondamentale :** le `footer` est à l'intérieur de `__main`, pas en 3e colonne de la grille. Sinon il s'étend sur les deux colonnes et flotte sous le rail droit.

**Règle content-projection :** les wrappers `<div main>`, `<div aside>`, `<div footer>` sont projetés comme un seul enfant dans leur conteneur grid/flex. Ajouter systématiquement dans la SCSS de la page :

```scss
[main]   { display: grid;     gap: 2rem; }
[aside]  { display: grid;     gap: 1.5rem; }
[footer] { display: contents; }           /* devient transparent → enfants directs du __footer flex */
```

### États UI à gérer impérativement

| État | Signal | Comportement visible |
|---|---|---|
| Formulaire invalide | `form.invalid` | Hint dans le footer, submit désactivé |
| Preview en cours | `previewLoading()` | Spinner local dans la health card (rail droit) |
| Soumission en cours | `submitting()` | Overlay page entière + bouton en mode "chargement" |
| Erreur de soumission | `submitError()` | `TchErrorPanel` dans le footer (1 seul panneau) |
| Succès | `submitted()` | Formulaire remplacé par le résultat, footer avec actions post-succès |

**Ne jamais** afficher plusieurs panneaux d'erreur empilés. Une seule `TchErrorPanel` avec le message consolidé. Si plusieurs appels peuvent échouer, agréger au niveau de la logique TS.

### Overlay page-level vs spinner inline

Deux niveaux distincts :

- **`previewLoading()`** → spinner local dans la card concernée. L'utilisateur continue à remplir le formulaire.
- **`submitting()`** → overlay page entière avec `position: absolute; inset: 0` sur le wrapper `form`. Bloque tous les clics, fond semi-transparent, carte centrée avec spinner + message explicite.

```scss
.my-page {
  position: relative;  /* requis pour l'overlay enfant */
  display: block;
}

.my-page__page-loader {
  position: absolute;
  inset: 0;
  z-index: 50;            /* sous les overlays CDK (z-index 1000+) */
  display: grid;
  place-items: start center;
  padding-top: 8rem;
  background: color-mix(in oklab, var(--tch-color-surface) 72%, transparent);
  backdrop-filter: blur(2px);
  pointer-events: all;   /* bloque tous les clics en dessous */
}
```

L'overlay s'applique à **toute opération irréversible longue** (create, provision, archive). Pas pour un simple GET ou une prévisualisation.

### Rail droit sticky

Le rail droit (`[aside]`) contient :

1. **`TchIdentityCard` (compact)** — miroir vivant des champs du formulaire via `computed()`. Met à jour le nom, le code, le statut en temps réel. Permet à l'utilisateur de voir à quoi ressemblera l'objet sans scroller.
2. **Health card / Readiness card** — visible seulement pendant la phase de formulaire (`!submitted()`). Montre l'état de prévisualisation ou le statut calculé (profil, domaines, readiness).
3. **Next steps card** — visible seulement après succès (`submitted()`). Remplace la health card.
4. **Warnings card** — si l'API renvoie des avertissements (preview ou result).

Position sticky du rail : `top: calc(var(--tch-private-topbar-height, 64px) + 1rem)`. Sans ça, le rail scroll avec la page sur les formulaires longs.

### Anti-race sur les previews

Toute prévisualisation déclenchée par un `valueChanges` utilise `switchMap` + `EMPTY` sur erreur. Sinon les réponses arrivent dans le désordre et l'overlay flashe.

```ts
this.form.valueChanges.pipe(
  debounceTime(400),
  filter(() => this.form.valid),
  tap(() => this.previewLoading.set(true)),
  switchMap(() =>
    this.api.preview(this.requestFromForm()).pipe(
      catchError(() => {
        this.previewLoading.set(false);
        return EMPTY; // absorbe l'erreur de preview, ne touche pas l'état submit
      })
    )
  ),
  takeUntil(this.destroy$)
).subscribe(res => {
  this.preview.set(res);
  this.previewLoading.set(false);
});
```

### Footer : layout et ordre

```html
<div footer>
  @if (submitError()) {
    <tch-error-panel [title]="..." [message]="submitError()!" />
  }

  @if (submitted()) {
    <!-- Post-succès : actions dans l'ordre de priorité décroissante -->
    <a tch-action variant="primary" ...>Voir l'objet</a>
    <a tch-action variant="secondary" ...>Action secondaire</a>
    <a tch-action variant="tertiary" ...>Retour à la liste</a>
    <button tch-action variant="tertiary" ...>Créer un autre</button>
  } @else {
    <!-- Hint à gauche via margin-right: auto -->
    @if (form.invalid && !submitting()) {
      <span class="my-page__submit-hint">Complétez les champs requis</span>
    }
    <a tch-action variant="tertiary" ...>Annuler</a>
    <tch-submit-button [label]="..." [loading]="submitting()" [disabled]="form.invalid" />
  }
</div>
```

```scss
.my-page__submit-hint {
  margin-right: auto;   /* pousse les boutons à droite */
  align-self: center;
  font-size: 0.8125rem;
  color: var(--tch-color-on-surface-variant);
}
```

### Gestion des erreurs HTTP

L'erreur brute d'un `HttpErrorResponse` ne doit jamais être passée directement à `String()` ou à un message — ça produit `[object Object]`.

Pattern à adopter :

```ts
private errorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    const pd = err.error as { title?: string; detail?: string } | null;
    return pd?.detail ?? pd?.title ?? err.message;
  }
  if (err instanceof Error) return err.message;
  return String(err);
}
```

Un seul `submitError = signal<string | null>(null)` suffit. Alimenté dans le bloc `error:` du subscribe.

### Guard sur les IDs de route

Avant tout appel API avec un ID extrait de la route ou d'un signal, valider que l'ID n'est pas `null`, `'null'`, ou `'undefined'` :

```ts
function isValidId(id: string | null | undefined): id is string {
  return !!id && id !== 'null' && id !== 'undefined';
}
```

Sans ça, un composant monté trop tôt génère un appel `GET /resource/null` qui part en 500 ou 404 backend.

### Mapping warnings backend → labels i18n

L'API backend renvoie des codes comme `INITIAL_ADMIN_EMAIL_MISSING`. Ne jamais afficher ces codes bruts. Mapper via un objet de constantes :

```ts
const WARNING_LABEL_KEYS: Record<string, string> = {
  INITIAL_ADMIN_EMAIL_MISSING: 'page.warning.initialAdminMissing',
  EXISTING_USER_ATTACHED: 'page.warning.existingUserAttached',
};

warningLabel(code: string): string {
  const key = WARNING_LABEL_KEYS[code] ?? 'page.warning.fallback';
  return this.translate.instant(key);
}
```

### Structure i18n d'une page de création

```json
{
  "page": {
    "title": "...",
    "description": "...",
    "section": { "identity": "...", "regional": "..." },
    "field": { "name": "...", "code": "..." },
    "hint": { "fieldX": "..." },
    "error": { "fieldX": "...", "submitTitle": "..." },
    "loading": { "title": "...", "message": "..." },
    "action": {
      "cancel": "...",
      "submit": "...",
      "submitting": "...",
      "completeRequired": "...",
      "viewObject": "...",
      "createAnother": "..."
    },
    "warning": { "codeA": "...", "fallback": "..." },
    "success": { "title": "...", "message": "..." },
    "identityCard": { "eyebrow": "...", "fallbackTitle": "..." },
    "health": { "title": "...", "loading": "..." },
    "result": { "nextSteps": "...", "warnings": "..." }
  }
}
```

### Scroll to top après succès

Après une soumission réussie, scroller en haut de page pour que le success state soit immédiatement visible :

```ts
next: (res) => {
  this.result.set(res);
  this.submitting.set(false);
  window.scrollTo({ top: 0, behavior: 'smooth' });
}
```

---

## 2. Page de liste / CRUD index

*À compléter — prochain écran : liste tenants (`/app/platform/tenants`).*

Points à documenter :
- Structure avec `AdminCrudShellComponent` + toolbar + pagination
- Filters connectés à l'API via `switchMap` + query params
- États : loading initial, loading filtre, error, empty (`AdminEmptyState` + CTA), ready
- `TchStatusBadge` pour les statuts (`ACTIVE`, `SUSPENDED`, `ARCHIVED`, `DRAFT`)
- Row actions : inline vs menu contextuel
- Quand utiliser `TchConfirmDialog` vs action directe
- Reload de la liste après une action de ligne (invalidation locale vs re-fetch)
- Pagination backend : reset page à 0 quand les filtres changent

---

## 3. Actions confirmées (modale)

*À compléter — patterns émergents de `TchConfirmDialog`.*

Points à documenter :
- Simple destructif (`variant: 'destructive'`) vs sensible (`sensitive: true`)
- Quand exiger `requireReason` (archive, suppression d'accès, opération irréversible)
- `confirmCheckboxLabel` pour les actions à fort impact
- Bouton confirm navy, cancel neutre — ne jamais inverser
- Spinner pendant l'action post-confirmation (état `actionLoading`)
- Gestion de l'erreur post-confirmation (la modale reste ouverte si échec)

---

## 4. Tokens et composants à toujours vérifier avant de créer

| Besoin | Ce qui existe |
|---|---|
| Bouton submit avec loading | `TchSubmitButton` (`[loading]`, `[disabled]`, `[label]`, `[variant]`) |
| Bouton action générique | `TchActionButton` (`variant: primary / secondary / tertiary`) |
| Panneau d'erreur | `TchErrorPanel` (`[title]`, `[message]`) |
| Notice persistante (succès/avertissement) | `TchNotice` (`type: success / warning / info / error`) |
| Loading spinner | `TchLoading` ou `mat-spinner` |
| Card identité | `TchIdentityCard` (`variant: compact / default`) |
| Card section formulaire | `AdminSectionCard` (`[title]`, `[icon]`) |
| Layout create/edit | `AdminDetailLayout` (`[main]`, `[aside]`, `[footer]`) |
| Confirmation modale | `TchConfirmDialog` (via `MatDialog`) |
| Badge statut | `TchStatusBadge` |
| État vide | `AdminEmptyState` |
| Santé / Readiness | `AdminProvisioningHealthCard` |
| Prochaines étapes | `AdminNextStepsCard` |

Avant de créer quoi que ce soit, vérifier ce tableau. Un composant spécifique à une seule page reste dans le fichier de cette page (pas de lib partagée).

---

## 5. Pièges récurrents rencontrés

| Problème | Cause | Solution |
|---|---|---|
| `[object Object]` dans un message d'erreur | `String(httpErrorResponse)` | Mapper avec `errorMessage()` helper |
| Appel `GET /resource/null` | Signal d'ID pas encore résolu | Guard `isValidId()` avant tout appel |
| Plusieurs panneaux d'erreur empilés | Interceptor HTTP + AppErrorHandler tous deux en action | `AppErrorHandler` skipe les `HttpErrorResponse` (déjà gérées par l'interceptor) |
| Footer s'étale sur les deux colonnes | Footer en 3e item de grille 2 colonnes | Footer à l'intérieur de `__main`, pas frère du main/aside |
| Rail droit scroll avec la page | `position: sticky` sans `top` | `top: calc(var(--tch-private-topbar-height, 64px) + 1rem)` |
| Réponses preview dans le désordre | `mergeMap` sur `valueChanges` | `switchMap` + `EMPTY` sur erreur |
| Font serif dans dialogs / boutons | Tokens `--mdc-*` non émis par le preset | Ponts `!important` dans `_material-overrides.scss` |
| Thème blanc (no-op) en shell privé | `presetId: "tchalanet_default"` inconnu → preset non appliqué | `theme-store.ts` : fallback sur preset par défaut si clé inconnue |
| Sidenav parent non fermable si enfant actif | `isOpen()` renvoyait toujours `true` pour groupe actif | Signal `openOverride` avec 3 états : `undefined / null / id` |
| Gap footer invisible entre Annuler et Submit | `<div footer>` est le seul enfant de `__footer` ; le `gap` s'applique entre ce div et rien | `[footer] { display: contents; }` dans la page SCSS — les boutons deviennent directs flex items de `__footer` |
| Gap absent entre cartes `[main]` et `[aside]` | Même cause : `<div main>` et `<div aside>` sont des wrappers opaques sans `display: grid` | `[main] { display: grid; gap: 2rem; }` et `[aside] { display: grid; gap: 1.5rem; }` dans la page SCSS |
