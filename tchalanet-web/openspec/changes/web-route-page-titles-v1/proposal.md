# OpenSpec Change — Web Route Page Titles V1

## Status

Proposed — 2026-07-26

## Why

**Aucune route des trois portails ne déclare de titre**, et aucune `TitleStrategy` n'est enregistrée.
Chaque page de la console admin s'appelle « Tchalanet Admin » dans l'onglet — donc aussi dans
l'historique, dans les favoris et dans le sélecteur d'onglets.

Trois conséquences concrètes :

- **Onglets indiscernables.** Un utilisateur qui ouvre la liste des vendeurs, un rapport et la
  configuration dans trois onglets voit trois fois le même libellé.
- **Historique et favoris inutilisables.** Un favori sur `/app/platform/catalog/draw-channel-games`
  s'enregistre sous « Tchalanet Platform ».
- **Rien n'est annoncé au changement de page.** Une SPA ne recharge pas le document : sans mise à
  jour du titre, un lecteur d'écran n'a aucun signal que la page a changé.

La convention existe déjà à moitié : 20 routes portent un `data: { titleKey }`, consommé uniquement
par `PlaceholderPage` pour les écrans non implémentés. Le modèle de navigation, lui, associe déjà
~80 destinations à un `labelKey`.

## Decision (locked)

- **Deux sources, dans cet ordre** : `data.titleKey` de la route la plus profonde, puis le modèle de
  navigation. Une entrée de menu donne son titre à la page — pas de duplication à maintenir.
- **Le suffixe est le `<title>` d'`index.html`**, capturé au démarrage. C'est une marque
  (« Tchalanet Admin »), pas une chaîne à traduire : aucune clé i18n de portail à créer.
- **Format** : `Page · Marque`, ou la marque seule quand rien ne décrit la page.
- **Jamais de clé brute dans l'onglet.** Une traduction manquante retombe sur la marque seule.

## What Changes

- `TchTitleStrategy` + `provideTchTitleStrategy()` dans `@tch/web/shell`.
- Câblage des trois portails ; les consoles passent leur modèle de navigation.
- `data.titleKey` sur les 16 routes publiques, à partir de clés i18n **existantes** — aucune clé
  nouvelle.
- Tests unitaires sur la stratégie, e2e sur les titres réels du portail public.

## Impact

- `libs/web/shell` (nouveau primitive), `apps/*/src/app/app.config.ts`, routes publiques.
- Aucun changement de route, de garde ni de modèle de navigation.
- Les consoles héritent de ~80 titres sans toucher un seul fichier de routes.

## Non-goals

- Pas de fil d'Ariane (chantier distinct).
- Pas de `titleKey` sur les routes de détail/création/édition des consoles : elles héritent du titre
  de leur entrée de menu parente, ce qui est déjà un progrès net. Les préciser une par une viendra
  au fil des écrans touchés.
- Pas de titre dans la barre supérieure du shell privé — elle affiche le nom du portail, changer sa
  sémantique est une décision de design séparée.
