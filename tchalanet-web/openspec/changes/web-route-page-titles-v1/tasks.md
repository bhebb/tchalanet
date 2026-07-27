# Tasks — web-route-page-titles-v1

## 1. Stratégie (`specs/web-page-title`)

- [x] `TchTitleStrategy` + `provideTchTitleStrategy(navigation?)` dans
      `libs/web/shell/src/lib/title/`, exporté par `@tch/web/shell`.
- [x] Résolution `data.titleKey` (route la plus profonde) puis modèle de navigation
      (destination la plus spécifique, `activeMatch: 'exact'` respecté).
- [x] Suffixe = `<title>` d'`index.html`, capturé à la construction — pas de clé i18n de portail.
- [x] Réapplication sur `onLangChange` : couvre le changement de langue **et** l'arrivée tardive
      des bundles, la première navigation ayant lieu avant leur chargement.
- [x] Traduction manquante → marque seule, jamais la clé brute.

## 2. Câblage

- [x] `admin-portal` : `provideTchTitleStrategy(TENANT_ADMIN_NAVIGATION)`.
- [x] `platform-portal` : `provideTchTitleStrategy(PLATFORM_NAVIGATION)`.
- [x] `public-portal` : `provideTchTitleStrategy()` — la nav publique vient du runtime PageModel,
      les titres passent donc par les routes.
- [x] `data.titleKey` sur les **16 routes publiques**, à partir de clés i18n existantes
      (`public.nav.*`, `public.check.title`, `public.footer.legal.*`…). Aucune clé nouvelle.

## 3. Tests

- [x] Vitest — clé de route, repli sur la navigation, destination la plus spécifique, marque seule,
      clé non traduite, changement de langue.
- [x] Vitest — garde sur les modèles réels : toute destination de route de
      `TENANT_ADMIN_NAVIGATION` / `PLATFORM_NAVIGATION` porte un `labelKey`, sinon la page qu'elle
      dessert resterait sans titre.
- [x] Playwright — titres réels du portail public : la page d'accueil est nommée, le titre change à
      la navigation, trois routes donnent trois titres distincts. Assertions **agnostiques de la
      langue** : le portail démarre en créole, ce n'est pas au test de navigation de figer ça.

## 4. Vérification

- [x] `pnpm run test` — 16 projets verts.
- [x] `pnpm run lint` — vert.
- [x] Build **production** des 3 portails — vert.
- [x] `nx e2e web-e2e -- --project=public-portal` — 7 tests verts.
- [x] Navigateur : `/` → `Akèy · Tchalanet` (créole, langue par défaut du portail).

## 5. Documentation

- [x] `libs/web/shell/README.md` — section « Titre du document » : les deux sources, le suffixe,
      le comportement au changement de langue.

## 6. Suites possibles

- [ ] `data.titleKey` sur les routes de détail/création/édition des consoles, au fil des écrans
      touchés — elles héritent aujourd'hui du titre de leur entrée de menu parente.
- [ ] Fil d'Ariane console, qui consommerait les mêmes clés.
