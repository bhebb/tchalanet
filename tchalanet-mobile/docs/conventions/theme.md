# Convention — Thème mobile Tchalanet

**Scope** : `tchalanet-mobile/`
**Status** : normative
**Source de vérité** : `lib/design_system/tokens/` et `lib/design_system/theme/`

## Règle

L'application mobile possède **un seul thème** : `tchalanet`, en mode clair.
Il est basé sur Material 3 et cohérent avec le thème Tchalanet du web.

Les widgets utilisent `Theme.of(context).colorScheme` et les tokens `Tch*`. Les
features ne choisissent jamais une palette ou un thème alternatif.

## Couleurs de marque

| Rôle | Material 3 | Valeur |
| --- | --- | --- |
| Bleu marine Tchalanet | `primary` | `#1A1B4B` |
| Texte sur primaire | `onPrimary` | `#FFFFFF` |
| Bleu conteneur | `primaryContainer` | `#2E3192` |
| Accent or | `tertiary` | `#FECB00` |
| Texte sur accent | `onTertiary` | `#241A00` |
| Fond | `surface` / background | `#F9F9FC` |
| Cartes | `surfaceContainerLowest` | `#FFFFFF` |
| Texte principal | `onSurface` | `#1A1C1E` |
| Texte secondaire | `onSurfaceVariant` | `#464652` |

L'or est un accent/CTA (`tertiary`), jamais `secondary`. Le rouge reste réservé aux
erreurs/destructions et le vert aux succès.

## Contrat complet Material 3

Le thème définit les familles sémantiques complètes nécessaires à Flutter :

- `primary`, `secondary`, `tertiary`, leurs `on*`, `*Container`, `*Fixed` et
  `*FixedDim` ;
- `surface`, `surfaceDim`, `surfaceBright` et tous les niveaux
  `surfaceContainerLowest` à `surfaceContainerHighest` ;
- `onSurface`, `onSurfaceVariant`, `outline`, `outlineVariant` ;
- `inverseSurface`, `onInverseSurface`, `inversePrimary`, `surfaceTint` ;
- `error`, `errorContainer` et leurs rôles de contraste.

Les états métier qui ne font pas partie de `ColorScheme` restent des tokens
sémantiques Tchalanet : `success`, `warning`, `missing`, `blocked`, `online`.

Un design Stitch peut révéler qu'un rôle manque ou illustrer son usage. Ses valeurs
générées ne sont jamais copiées automatiquement. La charte web Tchalanet et les rôles
Material 3 restent les références.

## Construction

`ThemeBuilder` construit le `ThemeData(useMaterial3: true)` depuis les tokens du thème
Tchalanet. Le thème local est disponible dès le premier rendu. Mobile V1 ignore les
données thème éventuellement présentes dans les bootstraps serveur.

## Interdit

- thème sombre ou preset alternatif sans nouveau change OpenSpec ;
- couleur de marque hardcodée dans une feature ;
- `Colors.blue`, `Colors.yellow`, etc. pour exprimer une décision de marque ;
- widget qui appelle directement l'API thème ;
- contournement de `ThemeData` pour un composant Material.
