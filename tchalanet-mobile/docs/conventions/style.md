# Convention — Styles et tokens Flutter

**Scope** : styles, layout et tokens  
**Status** : normative

Flutter utilise des tokens Dart typés, adaptés des responsabilités
`tchalanet-web/libs/ui/styles`.

## Responsabilités

```text
design_system/theme       thème Material 3 unique
design_system/tokens      couleurs sémantiques, spacing, radius, dimensions
design_system/components  composants visuels partagés
features/*/views           composition et layout spécifiques à l'écran
```

## Règles

- Utiliser `Theme.of(context).colorScheme` pour les rôles Material.
- Utiliser `TchSpacing`, `TchRadius` et les autres tokens typés.
- Les couleurs hors `ColorScheme` doivent être sémantiques (`success`, `warning`).
- Aucune couleur de marque ou taille répétée ne doit être hardcodée dans une feature.
- Une valeur ponctuelle purement géométrique est acceptable uniquement si elle ne
  représente pas une règle réutilisable.
- La typographie vient de `Theme.of(context).textTheme`.
- Les écrans adaptent le layout, pas la charte graphique.
- Choisir le rôle qui décrit la fonction visuelle, pas une couleur qui ressemble au
  résultat souhaité : carte = `surfaceContainerLowest`, fond secondaire =
  `surfaceContainerLow`, texte secondaire = `onSurfaceVariant`, accent = `tertiary`.
- Ne pas recréer des rôles comme `backgroundCard`, `lightGrey` ou `darkText` quand
  Material 3 fournit déjà le rôle sémantique.

## Adaptation du web

Le web utilise `--tch-*`; Flutter utilise les rôles Material 3 et tokens `Tch*`
équivalents. Il n'existe pas de variables CSS ni de BEM côté Flutter.

Les sorties Stitch/Tailwind servent uniquement à identifier composition et rôles
potentiels. Elles ne sont pas une source de vérité pour les hex, espacements ou noms
de tokens.
