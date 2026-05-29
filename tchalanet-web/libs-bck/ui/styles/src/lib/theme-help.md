// On dit à notre fichier qu'on a besoin des outils de theming d'Angular Material.
// On les importera sous le nom "mat" pour y faire référence facilement.
@use '@angular/material' as mat;

// --- PARTIE 1 : DÉFINIR NOS PALETTES DE COULEURS ---
// Une "palette" n'est pas une seule couleur, mais toute une gamme de nuances (plus claires, plus foncées).

// On crée une variable SCSS (commence par $) nommée "$primary".
// On utilise la fonction "define-palette" de "mat" pour créer une palette complète.
// On lui dit de se baser sur la famille de couleurs bleues prédéfinie par Material ("mat.$blue-palette").
// Le "600" indique quelle nuance de ce bleu sera la couleur par défaut.
$primary: mat.define-palette(mat.$blue-palette, 600);

// On fait la même chose pour la couleur "accent". C'est la couleur utilisée pour
// mettre en évidence des éléments (boutons flottants, sliders, etc.).
$accent: mat.define-palette(mat.$indigo-palette, 600);

// Et enfin, la couleur "warn" pour les messages d'erreur ou les actions destructrices.
$warn: mat.define-palette(mat.$red-palette, 400);

// --- PARTIE 2 : CRÉER LES RECETTES DE THÈME COMPLÈTES ---
// Maintenant qu'on a nos ingrédients (les palettes), on crée les recettes.

// On crée une recette pour le thème clair.
// On utilise la fonction "define-light-theme" de "mat".
$tch-light: mat.define-light-theme((
// La partie la plus importante de la recette est la section "color".
color: (
// On dit à la recette :
// - Pour la couleur "primary", utilise notre palette $primary.
// - Pour la couleur "accent", utilise notre palette $accent.
// - Pour la couleur "warn", utilise notre palette $warn.
primary: $primary,
accent: $accent,
warn: $warn,
),
// "density" contrôle l'espacement des composants. 0 est la valeur par défaut.
density: 0
));

// On crée une deuxième recette pour le thème sombre.
// On utilise la fonction "define-dark-theme" qui est plus intelligente :
// elle sait que le texte doit être clair sur des fonds sombres.
$tch-dark: mat.define-dark-theme((
color: (
primary: $primary,
accent: $accent,
warn: $warn,
),
density: 0
));

// --- PARTIE 3 : GÉNÉRER LE CSS ---
// C'est ici que la magie opère. Les recettes sont prêtes, on lance la cuisson.

// On crée une "classe CSS" nommée ".mat-theme-tchalanet".
// Pensez-y comme un grand autocollant qu'on va coller sur notre application.
.mat-theme-tchalanet {

// C'est l'instruction la plus importante. On dit à Material :
// "Prends notre recette de thème clair ($tch-light) et génère TOUT le CSS
// nécessaire pour TOUS les composants Material (boutons, cartes, menus...)."
// Des centaines de lignes de CSS sont créées ici.
@include mat.all-component-themes($tch-light);

// Ensuite, on ajoute une règle spéciale.
// Le "&" signifie "quand on est déjà dans .mat-theme-tchalanet".
// Donc, "&.dark" veut dire "quand un élément a À LA FOIS la classe .mat-theme-tchalanet ET la classe .dark"...
&.dark {
// ... alors, ne régénère pas tout ! Simplement,
// "remplace les couleurs" en utilisant notre recette de thème sombre ($tch-dark).
// C'est plus efficace.
@include mat.all-component-colors($tch-dark);
}
}


------------------
_token.scss
// On a besoin des outils de Material ("mat") et de nos recettes de thème ("theme").
@use '@angular/material' as mat;
@use './themes' as theme;

// ":root" est un sélecteur spécial qui cible la racine de votre page (la balise <html>).
// Cela signifie que les variables définies ici seront disponibles PARTOUT dans votre application.
:root {
// On crée une variable CSS. La syntaxe est "--nom-de-la-variable".
// Cette variable sera utilisable directement dans le navigateur.
// Le "#__{...}" est une syntaxe SCSS pour insérer le résultat d'un calcul.
// "mat.get-color-from-palette" est une fonction qui extrait une couleur spécifique d'une palette.
// Ici, on dit : "Prends notre palette $primary (depuis le fichier 'theme'),
// extrais la nuance '500', et stocke sa valeur (ex: #225EC7) dans la variable CSS --mdc-theme-primary."
--mdc-theme-primary: #{mat.get-color-from-palette(theme.$primary, 500)};

// On fait la même chose pour la couleur du texte "sur" la couleur primaire.
// '500-contrast' est une nuance spéciale qui retourne automatiquement
// la meilleure couleur de contraste (blanc ou noir) pour la nuance 500.
--mdc-theme-on-primary: #{mat.get-color-from-palette(theme.$primary, '500-contrast')};

// On répète l'opération pour les couleurs "accent" et "warn".
--mdc-theme-accent: #{mat.get-color-from-palette(theme.$accent, 500)};
--mdc-theme-on-accent: #{mat.get-color-from-palette(theme.$accent, '500-contrast')};

--mdc-theme-warn: #{mat.get-color-from-palette(theme.$warn, 500)};
--mdc-theme-on-warn: #{mat.get-color-from-palette(theme.$warn, '500-contrast')};

🎨 Tchalanet Design System

1. Système de Theming

Base Angular Material 20 (MDC) + surcouche SCSS.

Un thème maître (tchalanet.theme.scss) qui définit :

Palettes primary, accent, warn.

Variante light et dark.

Palette spéciale pour l’accent-dot.

Exposition en CSS variables pour permettre :

des overrides par tenant,

une personnalisation runtime (theme builder).

Tokens (tokens.scss) centralisent :

couleurs sémantiques (--color-primary, --color-success, etc.),

typo responsive (--tch-h1, --tch-body, …),

rayons et élévations (--radius, --elev-2, …).

👉 Les tenants peuvent fournir un JSON d’override appliqué en runtime.
👉 Une page Theme Builder (admin tenant) permettra de modifier le thème visuellement.

2. Layout
   🟦 Container

Largeur max 1200px.

margin-inline: auto; pour centrer.

Mobile-first : 92vw avec padding latéral minimal (12px).

🟦 Header

Composé de : Logo + Nav + Lang Switcher + CTA.

Desktop : tout sur une ligne avec un spacer entre logo et nav.

Tablet / Mobile : 2 lignes →

Ligne 1 : Logo + Burger.

Ligne 2 : Lang switcher + CTA.

Responsive via media queries (768px, 1024px).

mat-toolbar utilisé mais height contrôlée par nos styles (auto / 64px / 80px).

🟦 Footer

Fond basé sur --footer-bg.

Colonnes dynamiques (grid).

Mobile : 1 colonne,

Tablette : 2 colonnes,

Desktop : 4 colonnes.

Bloc spécial : logo + réseaux sociaux + baseline.

3. Améliorations Futures (Polish)
   🎨 Theming

Créer une UI de Theme Builder tenant → live preview + export JSON.

Ajouter support density scale (0, -1, -2) pour Material.

Définir un jeu d’icônes par thème (optionnel).

📐 Layout

Header mobile :

affiner hauteur et marges (plus d’air).

uniformiser la taille du burger et du logo.

Nav desktop :

ajouter un gap plus clair entre logo et menu.

sur hover, transition plus douce (0.2s).

Footer :

garder la baseline toujours centrée.

ajouter option dark mode.

♿ Accessibilité

Vérifier contrastes (AAA).

S’assurer que chaque bouton/menu ait un aria-label.

Focus visible homogène sur tous les liens.

✅ En résumé :

Un thème central + overrides runtime → simple et multi-tenant.

Layout mobile-first clair et stable.

Les bases sont solides, les améliorations à faire sont du polish et de l’accessibilité.}

# Theming Guide (Tchalanet)

## Architecture

- **Source of truth (runtime)** : `ThemeDomApplier`
    - Écrit `:root { … }` et `:root[data-theme="dark"] { … }` (tokens Material `--mat-sys-*` + tokens globaux).
    - Toggle `.dark` sur le conteneur `.tch-theme` et sur `OverlayContainer`.
    - Applique `matClass` (Angular Material) et densité (`mat-density-0/1/2`).
    - (Option) charge la police définie dans le preset JSON.
- **Surfaces applicatives (compile-time)** : `@mixin tchl-surface-vars(light|dark)`
    - Dérive `--header-*`, `--footer-*`, `--hero-*`, `--crumb-*`, `--tch-nav-*`, etc. depuis `--mat-sys-*`.
    - Émis sur `.tch-theme` (light) et `.tch-theme.dark` (dark).
- **Presets (JSON)** : couleurs, typo, densité, overrides
    - `palette` → alimente `--mat-sys-*`
    - `typography` → `--tch-font-family`, `--tch-w-*`, `--tch-type-scale` (+ `loadHref`)
    - `cssVars` : overrides arbitraires (`"--dark:--header-bg": "#0f2a5a"`, etc.)
    - `tokens` (option) : `headerBg`, `headerFg`, …

## Règles d’or

1. **Jamais de couleurs/valeurs “en dur” dans les composants**  
   Utiliser *uniquement* des variables CSS (tokens).
2. **Un seul calcul des surfaces** (dans `tchl-surface-vars`)  
   L’applier **n’écrit pas** `--header-*` / `--footer-*` par défaut (sauf override preset).
3. **Exposer des variables par composant**  
   `--comp-*` avec fallback vers les tokens globaux → rethémable sans toucher au CSS.
4. **Navigation active**  
   Utiliser `routerLinkActive="active"` + `{ exact: path === '/' }`.  
   Styles via `--tch-nav-active-bg/fg`.
5. **Mapping Angular Material → tokens app**  
   Une seule fois en global (toolbar/menu/dialog/select/overlay/card/table).
6. **Typographie pilotée par preset**  
   Famille, poids, échelle via variables → pas de police “en dur” dans un composant.
7. **Dark mode**  
   `.tch-theme.dark` + `:root[data-theme="dark"]` → zéro style conditionnel local.

## Tokens globaux (exemples)

- **Couleurs document** : `--tch-color-surface`, `--tch-color-on-surface`, `--tch-color-primary`
- **Nav** : `--tch-nav-fg`, `--tch-nav-hover-bg`, `--tch-nav-active-bg`, `--tch-nav-active-fg`
- **Breadcrumb** : `--crumb-bg`, `--crumb-fg`, `--crumb-sep`, `--crumb-link`, `--crumb-current`
- **Card/Surfaces** : `--tch-surface-card`, `--tch-on-surface-card`, `--tch-sep`
- **Typo** : `--tch-font-family`, `--tch-w-regular/medium/semibold/bold`, `--tch-type-scale`
- **Rayon** : `--tch-shape-radius`

## Patterns de composant

### A. Consommer les tokens

```scss
.tch-card {
  background: var(--tch-surface-card);
  color: var(--tch-on-surface-card);
  border: 1px solid var(--tch-sep);
  border-radius: var(--tch-shape-radius, 12px);
}

