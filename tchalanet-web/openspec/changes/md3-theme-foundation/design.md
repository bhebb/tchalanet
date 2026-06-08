# Design — Audit conformité MD3 du pipeline de génération (tâche 2)

Audit du pipeline `_theme-colors.scss → _generate-theme.scss → theme-presets.scss → registry`,
confronté aux bonnes pratiques Material 3 (`@angular/material` v21) et au seed backend `V203`.

## Conforme MD3 ✅

- **`mat.theme()` bien utilisé** : `theme-type` light/dark, `primary` + `tertiary` palettes,
  `typography` (familles + poids), `density: 0`, `color-scheme` posé par variante. Light **et** dark
  générés pour chaque preset.
- **Palette de marque** (`_theme-colors.scss`) = vraie palette tonale M3 générée par le schematic
  `ng generate @angular/material:theme-color` (steps 0–100, primary/secondary/tertiary/neutral/error).
- **Alignement des ids** : les **12 ids générés == 12 `code` du seed V203** (tchalanet, m3-blue,
  m3-indigo, m3-purple, m3-cyan, m3-teal, m3-green, m3-amber, m3-orange, m3-brown, m3-red, m3-pink).
  Un `presetCode` backend résout toujours vers un preset généré.

## Écarts à corriger (par sévérité)

### 🔴 F1 — Triple divergence du primary `tchalanet` (fidélité)
Le « primary tchalanet » a **trois valeurs contradictoires** selon la couche :
| Couche | Valeur | Alimente |
|---|---|---|
| `_theme-colors.scss` (palette mat.theme) | `#134D9F` (bleu) | `--mat-sys-primary` |
| `runtime-vars.scss` raw-hex `[data-preset='tchalanet']` | `#1A1B4B` (indigo) | `--tch-color-primary` |
| seed backend `V203` | `#006874` (teal) | notion backend du preset |

Conséquence : **incohérence visible dans le preset par défaut lui-même** — un composant Material
(`--mat-sys-primary`) rend bleu, un composant `--tch-color-primary` rend indigo, et le backend croit
teal. C'est une **décision produit/marque** (quelle est LA couleur tchalanet ?) — à trancher avant
toute régénération. Bloquant pour la fidélité, hors « foundation » (aucune valeur de marque changée
arbitrairement ici).

### 🟠 F2 — Typographie non bridgée depuis M3
Les couleurs suivent le modèle « fine indirection de marque sur M3 » (`runtime-vars` :
`--tch-color-* → var(--mat-sys-*)`, s'adapte par preset/dark). **La typographie ne le suit pas** :
`--tch-font-size-*` / `--tch-line-height-*` sont des **px figés dans `runtime-root.scss`**, sans lien
avec l'échelle typographique M3 (`--mat-sys-display-*`/`-headline-*`/`-title-*`/`-body-*`/`-label-*`)
que `mat.theme()` émet pourtant. Résultat : deux échelles typo parallèles, la `--tch` ne suit ni le
preset ni la densité. → bridger les `--tch` typo depuis `--mat-sys-*` (comme les couleurs).

### 🟠 F3 — Police : triple déconnexion + valeur non-CSS
- `mat.theme()` fige `Plus Jakarta Sans` (plain + brand) ;
- `runtime-root` fige `--tch-font-family: 'Plus Jakarta Sans, …'` ;
- seed backend : `typography.fontFamily: "roboto"`, `allowedFonts: [system, roboto, poppins, inter]`
  (**ne contient pas** Plus Jakarta Sans).

Un override tenant de police ne change donc **pas** la typo des composants Material (famille bakée).
De plus `theme-token-map` passe `typography.fontFamily` **verbatim** → `--tch-font-family: roboto`
(mot-clé nu, sans stack de secours). → mapper les `allowedFonts` (mots-clés) vers de vraies stacks et
réconcilier la liste avec ce qui est réellement supporté.

### 🟡 F4 — Densité non appliquée
`density: 0` est figé à la génération ; `ThemeDomApplier` **n'applique pas** de classe
`mat-density-*` ; l'override backend `density.default` est **droppé** par le token-map. La densité est
donc de fait fixe. `theme.md` prétend l'inverse (« `mat-density-*` ») → déjà partiellement corrigé,
reste à décider si la densité runtime est supportée (sinon retirer `allowedFonts`/`density` des
`editableTokens` côté discussion backend).

## Décisions / suites

- **F1 fidélité — RÉSOLU (web)** : couleur de marque retenue = **#1A1B4B (navy)**, sur référence design
  officielle. Palette `_theme-colors.scss` régénérée depuis #1A1B4B (+ tertiary #FECB00) via le
  schematic Material. Les hexes officiels (primary navy, primaryContainer #2E3192, accent gold,
  background #F9F9FC, surfaceContainerLowest #FFFFFF, onSurface #1A1C1E, onSurfaceVariant #464652,
  header blanc / footer navy) sont **épinglés sur `--mat-sys-*` ET `--tch-*`** dans `runtime-vars.scss`
  → Material et composants app rendent identiquement. Gold = accent (M3 tertiary) uniquement ;
  secondary dérive en indigo muté. **Suivi backend** : aligner le seed V203 (#006874 teal → #1A1B4B).
- **F2/F3/F4** : suites de la passe MD3 typographie/fonts/densité — touchent `runtime-root`,
  `runtime-vars`, `theme-token-map`, `_generate-theme` ; à valider visuellement (app lancée), donc
  hors du socle « sans régression visuelle » de ce change.
