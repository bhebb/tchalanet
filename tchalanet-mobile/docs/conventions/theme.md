# Convention — Theme mobile

**Scope** : `tchalanet-mobile/`
**Status** : normative
**Source de vérité** : `lib/design_system/` — le code prime sur tout autre doc.

---

## 1. Structure du design system

```
lib/design_system/
  tokens/
    tch_colors.dart    ← palette de couleurs
    tch_radius.dart    ← rayons de bordure
    tch_spacing.dart   ← espacements
  theme/
    tch_theme.dart     ← ThemeData statique (défaut Tchalanet)
    runtime_theme.dart ← ThemePreset + RuntimeTheme (modèles runtime)
```

---

## 2. Couleurs — `TchColors`

Source : `lib/design_system/tokens/tch_colors.dart`

| Token | Valeur | Rôle |
|---|---|---|
| `primary` | `0xFF5E89EF` | Action principale |
| `primaryStrong` | `0xFF2457BA` | État pressé, accent fort |
| `onPrimary` | `0xFFFFFFFF` | Texte sur primary |
| `onPrimarySoft` | `0xFFF2F3F5` | Texte léger sur primary |
| `primaryContainer` | `0xFFDAE2FF` | Surface primary douce |
| `onPrimaryContainer` | `0xFF001847` | Texte sur primaryContainer |
| `secondary` | `0xFF6F5EEF` | Action secondaire |
| `onSecondary` | `0xFFFCFDFF` | Texte sur secondary |
| `secondaryContainer` | `0xFFE4DFFF` | Surface secondary douce |
| `onSecondaryContainer` | `0xFF160066` | Texte sur secondaryContainer |
| `tertiary` | `0xFFFF7844` | Accent chaud |
| `onTertiary` | `0xFFFCFDFF` | Texte sur tertiary |
| `tertiaryContainer` | `0xFFFFDBCF` | Surface tertiary douce |
| `onTertiaryContainer` | `0xFF380D00` | Texte sur tertiaryContainer |
| `background` | `0xFFF7F9FB` | Fond écran |
| `surface` | `0xFFF3EFF2` | Cartes, panneaux |
| `surfaceBright` | `0xFFFFFFFF` | Blanc pur (champs, modales) |
| `surfaceContainer` | `0xFFECEEF0` | Fond muet, zones basse emphase |
| `surfaceContainerHigh` | `0xFFE8E7F0` | Variante surfaceContainer |
| `onSurface` | `0xFF1D1B20` | Texte principal |
| `onSurfaceVariant` | `0xFF49454F` | Texte secondaire |
| `outline` | `0xFFCAC4D0` | Bordures légères |
| `outlineStrong` | `0xFF79767D` | Bordures marquées |
| `success` | `0xFF006C49` | Session ouverte, vente acceptée, OK |
| `successContainer` | `0xFFDDFBEA` | Badge succès, fond positif |
| `warning` | `0xFFB26A00` | Limite proche, offline, confirmation |
| `warningContainer` | `0xFFFFF2D6` | Fond avertissement |
| `error` | `0xFFBA1A1A` | Rejeté, bloqué, action destructive |
| `onError` | `0xFFFFFFFF` | Texte sur error |
| `errorContainer` | `0xFFFFEDEA` | Fond erreur doux |

**Règles :**
- Rouge (`error`) → uniquement erreurs bloquantes et actions destructives.
- Vert (`success`) → uniquement succès et état OK.
- Bleu primaire → actions principales.
- Jamais de couleurs hardcodées dans les widgets — toujours `Theme.of(context).colorScheme.*` ou un token `TchColors.*`.

---

## 3. Espacement — `TchSpacing`

Source : `lib/design_system/tokens/tch_spacing.dart`

Grille de 4 px.

| Token | Valeur |
|---|---|
| `s4` | 4 |
| `s8` | 8 |
| `s12` | 12 |
| `s16` | 16 |
| `s20` | 20 |
| `s24` | 24 |
| `s32` | 32 |
| `s40` | 40 |
| `s48` | 48 |
| `s64` | 64 |

Dimensions POS de référence :
- Marge écran : `s12` ou `s16`
- Gap standard : `s12`
- Hauteur header : `s48`–`s64`
- Hauteur bouton primaire : `s48`–`s64`
- Zone tactile minimum : 44 px (tout élément interactif)

---

## 4. Radius — `TchRadius`

Source : `lib/design_system/tokens/tch_radius.dart`

| Token | Valeur |
|---|---|
| `xs` | 4 |
| `sm` | 8 |
| `md` | 12 |
| `lg` | 16 |
| `xl` | 24 |
| `pill` | 999 |

Usage :
- Champs et cartes → `sm` (8) ou `md` (12)
- Badges et chips → `pill`
- Boutons primaires → `md` (12) ou `lg` (16)

---

## 5. Thème statique — `TchTheme`

Source : `lib/design_system/theme/tch_theme.dart`

```dart
ThemeData theme = TchTheme.light();
```

`TchTheme.light()` construit un `ThemeData` Material 3 câblé sur `TchColors` :
- `useMaterial3: true`
- `fontFamily: 'Inter'`
- `scaffoldBackgroundColor: TchColors.background`
- `ColorScheme` entièrement défini depuis les tokens

C'est le thème appliqué dans `app.dart`. Il ne change pas à runtime.

---

## 6. Thème runtime — `RuntimeTheme` / `ThemePreset`

Source : `lib/design_system/theme/runtime_theme.dart`

```dart
class ThemePreset {
  final String id;
  final String name;
  final int primaryColor;    // ARGB int ex: 0xFF5E89EF
  final int? secondaryColor;
  final int? surfaceColor;
}

class RuntimeTheme {
  final ThemePreset preset;
  final bool isDefault;

  static const defaultTheme = RuntimeTheme(
    preset: ThemePreset(
      id: 'tchalanet-default',
      name: 'Tchalanet Default',
      primaryColor: 0xFF1A237E,
    ),
    isDefault: true,
  );
}
```

`ThemePreset` est le modèle retourné par le backend (endpoint theme runtime). Il ne contient que les surcharges — `primaryColor`, `secondaryColor?`, `surfaceColor?`.

**Ce qui n'est pas encore implémenté (T8) :**
- Provider Riverpod `runtimeThemeProvider`
- Conversion `ThemePreset → ThemeData` (application des couleurs du preset sur `TchColors`)

---

## 7. Règles d'usage dans les widgets

```dart
// ✅ Correct — via ColorScheme Material 3
color: Theme.of(context).colorScheme.primary

// ✅ Correct — token sémantique direct si non disponible via ColorScheme
color: TchColors.success

// ✅ Correct — spacing via token
padding: EdgeInsets.all(TchSpacing.s16)

// ✅ Correct — radius via token
borderRadius: BorderRadius.circular(TchRadius.sm)

// ❌ Interdit — couleur hardcodée
color: Color(0xFF5E89EF)

// ❌ Interdit — valeur numérique brute
padding: EdgeInsets.all(16)
```

---

## 8. Incohérence documentaire connue

`docs/mobile/02_mobile_design_tokens.md` contient des valeurs de couleurs différentes du code Dart (ex. `primary` y est `#3525CD`, le code a `0xFF5E89EF`). **Le code fait foi.** La doc sera mise à jour quand la palette sera stabilisée.
