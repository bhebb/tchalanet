# Tasks

## 1. Manifeste de tokens + test de contrat à 4 voies

- [x] Extraire le vocabulaire `--tch-*` (couleur + typo + fonts + shape + densité) vers un manifeste
      canonique unique (`.ts` généré + committé). → `tools/generate-token-manifest.mjs` +
      `registry/token-manifest.generated.ts` (101 tokens) ; script `npm run tokens:generate`.
- [x] Implémenter un test (cible `ui-theme`) qui vérifie la cohérence entre le pont
      `runtime-vars.scss`, le fallback `runtime-root.scss`, les cibles de `theme-token-map.ts` et le
      manifeste. → `lib/theme-token-contract.spec.ts` (5 tests).
- [x] Le test échoue si : un token couleur du pont n'a pas son fallback `:root`, une cible du
      token-map n'existe pas dans le vocabulaire émis, ou le manifeste est périmé (vérifié négativement).
- [x] Cibler les *déclarations* `--tch-*:` (pas les usages) pour éviter les faux positifs.
- [x] Écart corrigé au passage : `--tch-color-accent`/`--tch-on-color-accent` manquaient dans le
      fallback `:root` → ajoutés à `runtime-root.scss`.

## 2. Conformité MD3 du pipeline de génération

- [x] Audit réalisé → findings consignés dans `design.md`.
- [x] Rôles couleur complets light+dark + tonal palettes M3 + `color-scheme` : **conformes** (`mat.theme()` correct).
- [x] Échelle typographique : **écart F2** — `--tch-font-size-*` figés dans `runtime-root`, non bridgés depuis `--mat-sys-*`.
- [x] Fonts : **écart F3** — triple déconnexion (mat.theme `Plus Jakarta Sans` vs seed `roboto`/`allowedFonts`) + valeur verbatim non-CSS.
- [x] Densité : **écart F4** — `density:0` figé, pas de `mat-density-*` appliqué, override droppé.
- [x] Ids registry == `code` plateforme : **12 == 12** ✅.
- [x] **Écart F1 (🔴 fidélité)** : primary `tchalanet` divergent sur 3 couches (`#134D9F`/`#1A1B4B`/`#006874`) → décision marque, hors socle.

## 3. Tokens manquants

- [x] Émettre `--tch-z-header/drawer/overlay/toast` dans `runtime-root.scss`.
- [x] Émettre `--tch-color-scrim` (lisible light/dark) dans `runtime-root.scss`.

## 4. Token-map ↔ rôles consommés

- [x] `color.onSurface` mappe désormais `--tch-color-on-surface` (rôle standard, était `--tch-color-foreground`).
- [x] `shape.radius.md` mappe désormais `--tch-radius-md` (était `--tch-radius-control`).
- [x] Compat préservée sans double-cible : les alias `--tch-color-foreground` et `--tch-radius-control`
      **dérivent** du rôle standard (`var(--tch-color-on-surface)` / `var(--tch-radius-md)`) dans
      `runtime-vars.scss` + `runtime-root.scss`, donc un override tenant cascade aux deux noms.
      Confirmé : foreground/radius-control sont largement consommés (dashboards, widgets, composants).
- [x] `theme-token-map.spec.ts` mis à jour ; `theme:generate` rejoue un registry identique (pas de régression).

## 5. Breakpoints — source unique

- [x] Confirmer `_breakpoints.scss` (`sm/md/lg/xl`) comme canon (source unique, valeurs inchangées).
- [x] Documenter le mapping vers les paliers runtime (`handset=<md`, `tablet=md..lg`, `desktop=≥lg`)
      dans `style.md §10`. (Re-dérivation effective de `TchBreakpointService` = slice composants.)

## 6. Frontières & qualité

- [x] Taguer `ui/theme` → `["scope:ui","type:theme"]` (parité réelle avec `ui-styles`/`ui-components` ;
      la cible `type:ui`/`scope:shared` de `nx-boundaries.md` est théorique → noté dans la doc).
- [~] Cibles `lint`/`test` sur `ui-styles` : **différé**. Lib SCSS pure, sans TS ni config eslint ; le
      test de contrat appartient à `ui-theme` (bonne ownership). À reprendre avec la décision stylelint.
- [~] `depConstraints` Nx : **différé**. Racine permissive (`* → *`) ; durcissement risqué pour le lint
      global → à valider séparément. Écart documenté dans `nx-boundaries.md`.
- [x] Réconcilier le mixin focus `_mixins.scss` ↔ `style.md §14` (doc alignée sur le mixin `max(...)` canonique).

## 7. Documentation

- [x] `style.md §5`/`theme.md "CSS Token Rules"` pointent le manifeste généré au lieu de recopier une liste.
- [x] Chaîne génération↔runtime↔catalog + périmètre thème documentés (`theme.md` pipeline + mapping).
- [x] Documenté que les overrides du thème courant sont appliqués par `ThemeDomApplier` (`theme.md`).
- [x] Écart `fontHref`/`--dark:` réconcilié : doc corrigée (comportement réel de `toOverrideCss` +
      marqué « future enhancement ») ; id `<style>` corrigé en `tch-theme-preset`.
- [x] `style.md §15` z-index/scrim marqués « émis » ; `nx-boundaries.md` reflète les tags réels + le
      non-enforcement des `depConstraints`.
- [x] `theme.md`/token-map alignés sur le nouveau mapping (rôles standard + alias dérivés).
- [x] Note explicite « application aux composants = slice suivant » ajoutée dans `ARCHITECTURE.md`.

## 8. Validation et delivery

- [x] `nx test ui-theme` (9 tests) vert ; échec confirmé négativement quand le manifeste est périmé.
- [x] `npm run theme:generate` régénère un registry **identique** (alias n'altèrent pas le CSS), light+dark.
- [x] `nx lint ui-theme` vert. (`ui-styles` lint différé — cf. tâche 6.)
- [ ] Inspecter le DOM `.tch-theme` : `--tch-z-overlay`, `--tch-color-scrim`, échelle typo/font (à faire app lancée).
- [x] `git diff --check` clean + `openspec validate --strict` valide.

## 9. Suivis (hors de ce change, à tracer)

- [ ] **F1** — décision marque + fidélité : trancher LA couleur primary `tchalanet`
      (teal `#006874` / indigo `#1A1B4B` / bleu `#134D9F`), régénérer la palette, retirer le raw-hex
      override de `runtime-vars`, aligner le seed backend. (Slice génération-pilotée.)
- [ ] **F2** — bridger les `--tch-font-size-*`/`--tch-line-height-*` depuis `--mat-sys-*` (typo suit M3).
- [ ] **F3** — mapper `allowedFonts` (mots-clés) vers de vraies stacks ; réconcilier famille générée
      (`Plus Jakarta Sans`) ↔ seed (`roboto`/`allowedFonts`) ↔ `--tch-font-family`.
- [ ] **F4** — décider du support densité runtime (`mat-density-*` appliqué) ou retirer de `editableTokens`.
- [ ] Web — génération pilotée par le set supporté plateforme + consommation de la liste/runtime backend.
- [ ] Backend — exposer/figer la liste des thèmes supportés consommable par la génération frontend.
- [ ] Style — passe de conformité des composants `ui/components`.
- [ ] (Futur) theme-builder custom tenant.
