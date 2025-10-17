# Guide de Theming (Angular Material v20, Sass, Architecture projet)

Ce document explique comment fonctionne le theming dans l’application, les fichiers à connaître, comment ajouter des
thèmes, comment brancher les personnalisations venant du backend, et les bonnes pratiques.

## Objectifs du theming

- Offrir plusieurs “presets” de thème Angular Material précompilés: `tchalanet` (par défaut), `ocean`, `forest`,
  `sunset`, `grape`, `teal`.
- Permettre un basculement Light/Dark à la volée (le “night” correspond à Dark).
- Autoriser des personnalisations dynamiques depuis le backend (ex: primaryColor, accentColor) sans recompiler Sass, via
  des variables CSS.

---

## Rappels: Angular Material v20 (M2) et Sass

- Import Sass Material (v20+):
  ```scss
  @use '@angular/material' as mat;
  ```
- API palettes/thèmes M2 (préfixe `m2-`):
    - Définir une palette: `mat.m2-define-palette(mat.$m2-blue-palette, 600)`
    - Thème clair/sombre: `mat.m2-define-light-theme(...)` / `mat.m2-define-dark-theme(...)`
    - Appliquer aux composants:
        - Light: `@include mat.all-component-themes($lightTheme);`
        - Dark (couleurs uniquement sous `.dark`): `@include mat.all-component-colors($darkTheme);`

Astuce: Ne pas utiliser l’ancienne API `@use '@angular/material/theming'`. En v20+, on reste sur
`@use '@angular/material' as mat` et on emploie les fonctions/mixins préfixées `m2-`.

---

## Vue d’ensemble de l’architecture styles

Point d’entrée global:

- `libs/shared/ui/styles/src/index.scss`
    - Aggrège les utilitaires et les classes de thèmes pour toute l’app.

Utilitaires Sass (réutilisables partout):

- `libs/shared/ui/styles/src/lib/_tokens.scss`
    - Expose des variables CSS “sémantiques” (ex: `--color-primary`, `--radius`) mappées sur les `--tch-*` injectées par
      le ThemeService.
- `libs/shared/ui/styles/src/lib/_mixins.scss`
    - Mixins Sass utiles (focus ring, elevation, rounded, surface, outline, text-ellipsis, container, etc.).
- `libs/shared/ui/styles/src/lib/_utilities.scss`
    - Classes utilitaires prêtes à l’emploi (ex: `.surface`, `.rounded`, `.elevate-2`, `.container`, `.sr-only`, etc.).

Agrégateur de thèmes:

- `libs/shared/ui/styles/src/lib/_themes.scss`
    - Importe chaque preset (`tchalanet.theme`, `ocean.theme`, …) et “émet” leurs classes `.mat-theme-…` via des mixins
      `apply-*-class()`.

Fichiers de thème (1 fichier par preset):

- `libs/shared/ui/styles/src/lib/themes/_tchalanet.theme.scss`
- `libs/shared/ui/styles/src/lib/themes/_ocean.theme.scss`
- `libs/shared/ui/styles/src/lib/themes/_forest.theme.scss`
- `libs/shared/ui/styles/src/lib/themes/_sunset.theme.scss`
- `libs/shared/ui/styles/src/lib/themes/_grape.theme.scss`
- `libs/shared/ui/styles/src/lib/themes/_teal.theme.scss`

Chaque fichier définit:

- 3 palettes M2: `primary`, `accent`, `warn`.
- 2 thèmes: `<preset>-light`, `<preset>-dark`.
- 1 mixin `apply-<preset>-class()` qui génère `.mat-theme-<preset>`, avec le support de `.dark`.

---

## Fonctionnement runtime: ThemeService

Le service applique dynamiquement:

- La classe `.mat-theme-<preset>` sur `<html>` pour activer les styles Angular Material précompilés du preset.
- La classe `.dark` si le mode sombre (night) est demandé.
- Les variables CSS `--tch-*` (ex: `--tch-primary`) pour vos composants applicatifs (hors Material), modifiables à la
  volée via la config backend.

Exemple d’utilisation:
ts //
TypeScript themeService.applyPreset('forest', 'light');
// preset Material "forest" + light
themeService.applyPreset('teal', 'dark'); // preset "teal" + dark
themeService.applyFromBackend; // Material "ocean" + couleurs applicatives runtime


Important:
- Les composants Material consomment les styles compilés du preset (compile-time).
- Vos composants applicatifs consomment les variables CSS (runtime) exposées via `_tokens.scss`.

---

## Contrat Backend (thème)

Le backend peut renvoyer une configuration de thème. Recommandé:

- `themeId` (obligatoire pour changer le preset Material): 
  - valeurs autorisées: `tchalanet`, `ocean`, `forest`, `sunset`, `grape`, `teal`.
  - défaut côté client: `tchalanet`.
- `mode`: `light` ou `dark` (optionnel, défaut `light`).
- `theme`: personnalisations applicatives (runtime) — aujourd’hui:
  - `primaryColor`: `string` (ex: `#3f51b5`)
  - `accentColor`: `string` (ex: `#ff4081`)
  - extensible plus tard (ex: `warnColor`, `surfaceColor`, etc.)

---

## Bonnes pratiques

- Toujours utiliser l’API M2:
    - `@use '@angular/material' as mat;`
    - Palettes: `mat.$m2-*-palette`
    - Fonctions: `mat.m2-define-*`
    - Mixins d’inclusion: `mat.all-component-themes` (light) et `mat.all-component-colors` (sous `.dark`).
- Ne pas mélanger compile-time et runtime:
    - Presets Material = compile-time (classes `.mat-theme-*`).
    - Variables CSS applicatives = runtime (changées via le service).
- Consommer les tokens CSS (–color-*, –radius, –elev-*) dans vos composants app au lieu de couleurs “en dur”.
- Ajouter un preset:
    - Créez un `_nouveau.theme.scss`, exposez `@mixin apply-nouveau-class()`.
    - Ajoutez `@use` et `@include` dans `_themes.scss`.
    - Déclarez le preset dans le `ThemeService` (type + map de classe).
    - L’API backend peut alors renvoyer `"themeId": "nouveau"`.

---

## Resets et layout de page (footer collé en bas)

### Reset/normalisation conseillés
- Box-sizing universel:
  ```scss
  // SCSS
  *, *::before, *::after { box-sizing: border-box; }
  ```
- RàZ simples:
  ```scss
  // SCSS
  html, body { height: 100%; }
  body { margin: 0; }
  ```
- Typo de base:
  ```scss
  // SCSS
  body { font-family: Roboto, "Helvetica Neue", sans-serif; }
  ```

Ajoutez ces règles dans votre feuille de style globale (elles existent en partie dans `index.scss`).

### “Sticky footer” (pied de page collé en bas)
Objectif: le footer reste en bas de la fenêtre quand le contenu est court, et suit après le contenu quand il est long.

- Structure HTML de base:
  ```html
  <!-- HTML -->
  <body>
    <app-root class="page-root">
      <header>...</header>
      <main class="page-content">...</main>
      <tchl-footer></tchl-footer>
    </app-root>
  </body>
  ```
- CSS recommandé:
  ```scss
  // SCSS
  html, body { height: 100%; }
  body { margin: 0; }

  .page-root {
    min-height: 100%;
    display: flex;
    flex-direction: column;
  }

  .page-content {
    flex: 1 0 auto;    // prend l’espace restant
  }

  tchl-footer, .app-footer, footer {
    flex-shrink: 0;    // n’est pas compressé, reste collé en bas
  }
  ```

Avec Angular standalone:
- Appliquez `class="page-root"` sur le composant racine (ex: wrapper de shell) ou sur l’hôte de l’app.
- Assurez-vous que votre `<main>` (ou conteneur central) a bien `flex: 1 0 auto`.

Astuce:
- Évitez des marges/bordures qui “poussent” le footer en dehors du flux. Préférez du padding sur `main`.
- Pour une largeur max, utilisez le mixin `container` de `_mixins.scss` dans `main`.

---

## Exemple d’usage côté code

Initialiser depuis une page backend:
