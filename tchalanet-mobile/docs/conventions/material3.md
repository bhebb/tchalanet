# Convention — Material 3 mobile

**Scope** : tous les widgets Flutter  
**Status** : normative

Material 3 est le système UI du mobile. Les tokens Tchalanet spécialisent Material 3,
ils ne le remplacent pas.

## Règles

- `ThemeData.useMaterial3` reste activé.
- Utiliser les composants Material avant de créer un composant custom.
- Utiliser les rôles `ColorScheme`, `TextTheme`, `CardTheme`, `InputDecorationTheme`
  et les thèmes de composants.
- L'accent Tchalanet jaune/or correspond à `tertiary`.
- Toute action tactile mesure au moins 44 dp; une action POS principale vise 56 dp.
- Les layouts utilisent les classes de taille et le runtime surface mobile/POS.
- Les états disabled, loading, focused, error et pressed sont obligatoires pour les
  composants interactifs.
- Le contraste, le text scaling, SafeArea et la navigation clavier/lecteur d'écran
  font partie de la définition de terminé.

## Composition

Une feature compose des composants Material 3 et des composants partagés Tchalanet.
Elle ne redéfinit pas localement une famille de boutons, cartes ou champs.

