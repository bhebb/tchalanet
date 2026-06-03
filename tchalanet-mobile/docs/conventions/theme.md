# Convention — Theme mobile

**Scope** : `tchalanet-mobile/`
**Status** : normative
**Source de vérité** : `lib/design_system/` + `lib/core/theme/` — le code prime sur tout autre doc.

---

## 1. Principe : défaut local, backend enrichit

```
RuntimeTheme.defaultTheme    ← appliqué immédiatement au démarrage
        +
GET /public/theme/runtime    ← chargé au démarrage, sans auth
        +
GET /tenant/theme/runtime    ← chargé après auth (thème tenant)
        ↓
ThemeBuilder.buildFromTokens(tokens)
        ↓
runtimeThemeDataProvider     ← ThemeData live dans MaterialApp
```

L'app démarre avec le thème par défaut Tchalanet. Le backend enrichit sans bloquer le rendu.

---

## 2. Endpoints serveur

| Endpoint | Auth | Usage |
|---|---|---|
| `GET /public/theme/runtime?mode=light` | Aucune | Thème public ou défaut platform |
| `GET /tenant/theme/runtime?mode=light` | Bearer | Thème spécifique au tenant connecté |

Réponse (`ThemeRuntimeView`) :

```json
{
  "status": "success",
  "data": {
    "presetCode": "tchalanet",
    "mode": "light",
    "tokens": {
      "color.primary":   "#006874",
      "color.secondary": "#4A6267",
      "color.surface":   "#FFFBFE",
      "color.onSurface": "#1C1B1F",
      "typography.fontFamily": "roboto"
    },
    "isDefault": true,
    "version": 1
  }
}
```

---

## 3. Tokens reconnus

| Clé | Type | Effet Flutter |
|---|---|---|
| `color.primary` | `#RRGGBB` | `ColorScheme.fromSeed(seedColor)` |
| `color.secondary` | `#RRGGBB` | `colorScheme.copyWith(secondary)` |
| `color.surface` | `#RRGGBB` | `colorScheme.copyWith(surface)` |
| `color.onSurface` | `#RRGGBB` | `colorScheme.copyWith(onSurface)` |
| `typography.fontFamily` | `inter` \| `roboto` \| `poppins` \| `system` | `ThemeData.fontFamily` |

Tokens non reconnus sont ignorés silencieusement.

---

## 4. Modèle `RuntimeTheme`

Source : `lib/design_system/theme/runtime_theme.dart`

```dart
RuntimeTheme(
  presetCode: 'tchalanet',
  mode:       'light',
  tokens:     {'color.primary': '#5E89EF', ...},
  isDefault:  true,
  version:    0,
)
```

`RuntimeTheme.defaultTheme` — thème Tchalanet intégré, appliqué avant toute réponse du backend.

---

## 5. Construction du ThemeData — `ThemeBuilder`

Source : `lib/design_system/theme/theme_builder.dart`

```dart
ThemeData theme = ThemeBuilder.buildFromTokens(tokens);
```

Logique :
1. Parse `color.primary` (#RRGGBB → `Color`)
2. `ColorScheme.fromSeed(seedColor: primary)` — génère la palette M3 complète
3. `copyWith(secondary, surface, onSurface)` si présents dans les tokens
4. `fontFamily` depuis `typography.fontFamily`
5. `scaffoldBackgroundColor = colorScheme.surface`

---

## 6. Providers Riverpod

```dart
// ThemeData live — à passer à MaterialApp.theme
ref.watch(runtimeThemeDataProvider)   // Provider<ThemeData>

// RuntimeTheme brut — si besoin du presetCode, mode, version
ref.watch(themeNotifierProvider)      // NotifierProvider<ThemeNotifier, RuntimeTheme>

// Déclencher le chargement tenant après auth
ref.read(themeNotifierProvider.notifier).loadTenantTheme();

// Réinitialiser au thème défaut (ex: à la déconnexion)
ref.read(themeNotifierProvider.notifier).reset();
```

**Règle** : seul `MaterialApp` consomme `runtimeThemeDataProvider`. Les widgets utilisent `Theme.of(context)`.

---

## 7. Utilisation dans les widgets

```dart
// ✅ Correct — via ColorScheme Material 3
color: Theme.of(context).colorScheme.primary
color: Theme.of(context).colorScheme.errorContainer

// ✅ Correct — token TchColors si le rôle n'est pas dans ColorScheme
color: TchColors.success
color: TchColors.warning

// ✅ Correct — spacing et radius via tokens
padding: EdgeInsets.all(TchSpacing.s16)
borderRadius: BorderRadius.circular(TchRadius.sm)

// ❌ Interdit — couleur hardcodée
color: Color(0xFF5E89EF)
color: Colors.blue

// ❌ Interdit — valeur numérique brute
padding: EdgeInsets.all(16)
borderRadius: BorderRadius.circular(8)
```

---

## 8. Tokens design system locaux

Pour les couleurs non couvertes par `ColorScheme` (succès, warning, statuts POS) :

| Token | Valeur | Usage |
|---|---|---|
| `TchColors.success` | `#006C49` | Session ouverte, vente OK |
| `TchColors.successContainer` | `#DDFBEA` | Badge succès |
| `TchColors.warning` | `#B26A00` | Offline, limite proche |
| `TchColors.warningContainer` | `#FFF2D6` | Fond avertissement |

Ces couleurs sont fixes — elles ne sont pas surchargées par le thème runtime.

---

## 9. Cycle de vie du thème

```
App start
  → ThemeNotifier.build()
  → fetchPublicTheme() en background
  → state = RuntimeTheme depuis serveur (ou defaultTheme si erreur)
  → runtimeThemeDataProvider se met à jour
  → MaterialApp re-render

Après login
  → AuthController → loadTenantTheme()
  → fetchTenantTheme() avec Bearer
  → state = thème tenant
  → MaterialApp re-render

Logout
  → themeNotifier.reset() → defaultTheme
```

---

## 10. Ce qui n'est pas implémenté en V1

- Mode sombre (`mode = 'dark'`) — les tokens dark existent côté serveur, le switch n'est pas câblé
- Réinitialisation automatique du thème au logout — à appeler explicitement depuis `AuthController.logout()`
- Tokens `shape.radius.md` et `density.default` — ignorés (câblage Material 3 non trivial en V1)
