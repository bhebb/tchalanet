# Web Public CSS And Theming Conventions

> Status: DRAFT
> Scope: `tchalanet-web` public pages, public widgets, public shell components

Norme durable pour les pages publiques Angular Tchalanet.

## 1. Objectif

Les pages publiques doivent rester :

* cohérentes avec Material Design 3 ;
* compatibles avec light mode, dark mode et system mode ;
* réutilisables avec PageModel et les widgets ;
* portables entre plusieurs presets Material ;
* indépendantes d’une palette locale, d’un CDN Tailwind ou d’une structure DOM fragile.

Le CSS public doit consommer le système de thème runtime Tchalanet, pas créer un thème parallèle.

## 2. Périmètre

Cette convention s’applique à :

* public shell ;
* homepage publique ;
* widgets publics PageModel ;
* pages publiques de résultat, vérification ticket, aide, plans, contenu marketing ;
* composants publics réutilisés par ces pages.

Elle ne s’applique pas directement à :

* composants admin internes ;
* POS/cashier mobile-first privé ;
* sandbox dev ;
* tokens globaux eux-mêmes.

Les tokens globaux vivent dans la couche thème, par exemple :

```text
libs/ui/src/lib/theme/
```

Les pages publiques ne définissent pas les tokens globaux. Elles les consomment.

## 3. Nommage CSS

Utiliser un nommage scoped BEM-like dans les styles de composants :

```text
block
block__element
block--modifier
is-state
u-utility
```

Règles :

* utiliser un nom de bloc court et métier : `check`, `result-detail`, `receipt-preview`, `public-help`;
* utiliser `__` pour les parties internes : `check__form`, `result-detail__status`;
* utiliser `--` pour les variantes stables : `result-detail__status--payable`;
* utiliser `is-*` pour les états runtime : `is-loading`, `is-error`, `is-selected`;
* éviter les classes génériques non scopées : `card`, `button`, `title`, `section`, `container`, `active`;
* garder les sélecteurs courts et explicites ;
* ne pas styler via des sélecteurs fragiles comme `div > div > span`;
* ne pas utiliser `::ng-deep` pour les pages publiques.

Exemple recommandé :

```css
.check {
  display: grid;
  gap: var(--tch-space-4);
}

.check__card {
  border-radius: var(--tch-radius-lg);
  background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
}

.check__status--payable {
  color: var(--tch-status-success-fg);
}
```

## 4. Tokens et variables

Les composants publics consomment des tokens sémantiques, pas des couleurs de marque directes.

Règles :

* utiliser les variables `--tch-*` pour couleurs, surfaces, bordures, focus, typographie, spacing, radius et statuts ;
* garder un fallback Material quand utile : `var(--tch-color-primary, var(--mat-sys-primary))`;
* ne pas hardcoder les couleurs de marque ou de statut dans les styles de composants ;
* ne pas créer de variable locale opaque si elle remplace un token existant ;
* les variables locales sont autorisées uniquement comme alias lisibles vers des tokens.

Exemple autorisé :

```css
.check {
  --check-card-bg: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
}
```

Quand un nouveau besoin sémantique est réel :

1. ajouter le fallback dans `runtime-root.scss`;
2. dériver la valeur depuis Material dans `runtime-vars.scss`;
3. mapper le token dans `theme-token-map.ts` si PageModel ou le backend peut l’envoyer ;
4. documenter le token dans la convention thème.

## 5. Material Design 3

Material Design 3 reste la base.

Règles :

* les composants publics doivent fonctionner avec `mode=light`, `mode=dark` et `mode=system`;
* ne pas ajouter de constantes couleur dark-mode dans un composant ;
* ne pas inverser manuellement les couleurs light ;
* ne pas introduire Tailwind/CDN comme système de thème parallèle ;
* vérifier le rendu sur le preset `tchalanet` et sur un autre preset Material disponible quand possible.

## 6. Overrides Material

Les pages publiques ne doivent pas cibler brutalement les classes internes Angular Material.

Interdit :

```css
.mat-mdc-button .mdc-button__label {
  color: red;
}
```

Préférer :

* les inputs officiels Material ;
* les tokens `--mat-sys-*` ;
* les tokens `--tch-*` ;
* un wrapper de composant Tchalanet quand le besoin devient récurrent.

Exemple :

```css
.public-hero__cta {
  --mdc-filled-button-container-shape: var(--tch-radius-pill);
}
```

Si un override Material devient nécessaire partout, il doit être déplacé dans la couche thème ou UI globale, pas rester dans une page publique.

## 7. PageModel et widgets

Les widgets publics doivent être stylés par leur contrat et leurs classes propres, pas par la position dans la page.

Règles :

* un widget ne dépend pas d’un parent spécifique pour être lisible ;
* un widget peut être rendu dans une autre page publique sans casser son style ;
* les variantes visuelles stables doivent venir du payload ou du type de widget, pas d’un sélecteur parent fragile ;
* les actions doivent utiliser les contrats communs `ActionItem` / `NavigationDestination` quand disponibles ;
* les images doivent passer par le contrat `ImageRef` quand disponible.

Exemple :

```html
<section class="public-results public-results--compact">
  ...
</section>
```

## 8. I18n

Tout texte public doit être traduisible dès le départ, même temporaire.

Règles :

* utiliser les clés i18n existantes ou en ajouter dans `fr.json`, `en.json`, `ht.json`;
* préférer les labels PageModel déjà localisés quand ils existent ;
* ne pas hardcoder de prose publique dans les templates Angular ;
* les tests et fixtures peuvent contenir du texte direct si cela sert le cas de test.

## 9. Responsive

Les pages publiques sont mobile-first.

Règles :

* commencer par le layout mobile ;
* utiliser des breakpoints simples ;
* éviter les largeurs fixes sauf pour des bornes explicites ;
* utiliser `min()`, `max()`, `clamp()` quand cela simplifie le responsive ;
* garder les sections lisibles sur petits écrans.

Exemple :

```css
.public-hero {
  padding: clamp(var(--tch-space-6), 6vw, var(--tch-space-12));
}
```

## 10. Accessibilité

Les styles publics ne doivent pas casser l’accessibilité.

Règles :

* conserver des états focus visibles ;
* utiliser `:focus-visible` plutôt que supprimer les outlines ;
* ne pas dépendre uniquement de la couleur pour exprimer un statut ;
* vérifier les contrastes sur light et dark mode ;
* respecter les composants Material quand ils fournissent déjà les bons comportements clavier.

Exemple :

```css
.check__link:focus-visible {
  outline: 2px solid var(--tch-color-focus-ring, var(--mat-sys-primary));
  outline-offset: 3px;
}
```

## 11. Validation

Pour chaque nouvelle page publique ou changement visuel significatif :

* vérifier mobile light ;
* vérifier mobile dark ;
* vérifier desktop light ;
* vérifier desktop dark ;
* vérifier le preset `tchalanet` ;
* vérifier un autre preset Material si disponible localement ;
* vérifier les états loading, empty, error ;
* vérifier le rendu sans données optionnelles ;
* documenter dans la réponse finale si un preset alternatif n’est pas disponible.

## 12. Checklist PR

Avant merge :

* [ ] classes scoped BEM-like ;
* [ ] pas de classes génériques non scopées ;
* [ ] pas de `::ng-deep` ;
* [ ] pas de couleurs hardcodées ;
* [ ] tokens `--tch-*` utilisés avec fallback Material si utile ;
* [ ] pas de Tailwind/CDN comme thème parallèle ;
* [ ] textes publics traduisibles ;
* [ ] light/dark vérifiés ;
* [ ] mobile/desktop vérifiés ;
* [ ] widgets publics portables hors page actuelle ;
* [ ] overrides Material déplacés dans la couche thème/UI si récurrents.
