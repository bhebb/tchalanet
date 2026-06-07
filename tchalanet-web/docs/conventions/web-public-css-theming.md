# Web Public CSS And Theming Conventions

Norme durable pour les pages publiques Angular Tchalanet.

## Objectif

Les pages publiques doivent rester cohérentes avec Material Design 3, réutilisables avec PageModel/widgets, compatibles light/dark mode, et portables entre plusieurs thèmes Material.

Le CSS public ne doit pas dépendre d'une palette locale, d'une configuration Tailwind/CDN, ni d'une structure DOM fragile.

## Nommage CSS

Utiliser un nommage scoped BEM-like dans les styles de composants :

```text
block
block__element
block--modifier
is-state
u-utility
```

Règles :

- utiliser un nom de bloc court et métier : `check`, `result-detail`, `receipt-preview`, `public-help`;
- utiliser `__` pour les parties internes : `check__form`, `result-detail__status`;
- utiliser `--` pour les variantes stables : `result-detail__status--payable`;
- utiliser `is-*` pour les états runtime : `is-loading`, `is-error`, `is-selected`;
- éviter les classes génériques non scopées : `card`, `button`, `title`, `section`, `container`, `active`;
- garder les sélecteurs courts et explicites, généralement une classe plus un état;
- ne pas styler via des sélecteurs fragiles comme `div > div > span`;
- ne pas utiliser `::ng-deep` pour les pages publiques.

## Tokens Et Variables

Les composants publics consomment des tokens sémantiques, pas des couleurs de marque directes.

Règles :

- utiliser les variables `--tch-*` pour couleurs, surfaces, bordures, focus, typographie, spacing, radius et statuts;
- garder un fallback Material quand utile : `var(--tch-color-primary, var(--mat-sys-primary))`;
- ne pas hardcoder les hex brand ou status dans les styles de composants;
- ne pas créer de variable locale opaque si elle remplace un token existant;
- les variables locales sont autorisées uniquement comme alias lisibles vers des tokens :

```css
.check {
  --check-card-bg: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
}
```

Quand un nouveau besoin sémantique est réel :

1. ajouter le fallback dans `runtime-root.scss`;
2. dériver la valeur depuis Material dans `runtime-vars.scss`;
3. mapper le token dans `theme-token-map.ts` si PageModel/backend peut l'envoyer.

## Material Design 3

Material Design 3 reste la base.

Règles :

- les composants publics doivent fonctionner avec `mode=light`, `mode=dark`, et `mode=system`;
- ne pas ajouter de constantes couleur dark-mode dans un composant;
- ne pas inverser manuellement les couleurs light;
- ne pas introduire Tailwind/CDN comme système de thème parallèle;
- vérifier le rendu sur le preset `tchalanet` et sur un autre preset Material disponible quand possible.

## I18n

Tout texte public doit être traduisible dès le départ, même temporaire.

Règles :

- utiliser les clés i18n existantes ou en ajouter dans `fr.json`, `en.json`, `ht.json`;
- préférer les labels PageModel déjà localisés quand ils existent;
- ne pas hardcoder de prose publique dans les templates Angular;
- les tests et fixtures peuvent contenir du texte direct si cela sert le cas de test.

## Validation

Pour chaque nouvelle page publique ou changement visuel significatif :

- vérifier mobile light;
- vérifier mobile dark;
- vérifier desktop light;
- vérifier desktop dark;
- vérifier le preset `tchalanet`;
- vérifier un autre preset Material si disponible localement;
- documenter dans la réponse finale si un preset alternatif n'est pas disponible.

