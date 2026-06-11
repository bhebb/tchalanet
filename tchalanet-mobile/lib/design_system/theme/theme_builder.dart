import 'package:flutter/material.dart';

import '../tokens/tch_colors.dart';

/// Builds a Material 3 ThemeData from a token map returned by the server.
///
/// Only the validated Tchalanet semantic token contract is applied. The mobile app
/// has one preset; runtime values may override roles but cannot select another theme.
/// Token values: CSS hex strings (#RRGGBB) or font name strings.
abstract final class ThemeBuilder {
  static ThemeData buildFromTokens(Map<String, String> tokens) {
    final primary = _parseHex(tokens['color.primary']) ?? TchColors.primary;

    final seeded = ColorScheme.fromSeed(
      seedColor: primary,
      brightness: Brightness.light,
    );

    final scheme = seeded.copyWith(
      primary: primary,
      onPrimary: _color(tokens, 'color.onPrimary', TchColors.onPrimary),
      primaryContainer: _color(
        tokens,
        'color.primaryContainer',
        TchColors.primaryContainer,
      ),
      onPrimaryContainer: _color(
        tokens,
        'color.onPrimaryContainer',
        TchColors.onPrimaryContainer,
      ),
      primaryFixed: _color(
        tokens,
        'color.primaryFixed',
        TchColors.primaryFixed,
      ),
      primaryFixedDim: _color(
        tokens,
        'color.primaryFixedDim',
        TchColors.primaryFixedDim,
      ),
      onPrimaryFixed: _color(
        tokens,
        'color.onPrimaryFixed',
        TchColors.onPrimaryFixed,
      ),
      onPrimaryFixedVariant: _color(
        tokens,
        'color.onPrimaryFixedVariant',
        TchColors.onPrimaryFixedVariant,
      ),
      secondary: _color(tokens, 'color.secondary', TchColors.secondary),
      onSecondary: _color(tokens, 'color.onSecondary', TchColors.onSecondary),
      secondaryContainer: _color(
        tokens,
        'color.secondaryContainer',
        TchColors.secondaryContainer,
      ),
      onSecondaryContainer: _color(
        tokens,
        'color.onSecondaryContainer',
        TchColors.onSecondaryContainer,
      ),
      secondaryFixed: _color(
        tokens,
        'color.secondaryFixed',
        TchColors.secondaryFixed,
      ),
      secondaryFixedDim: _color(
        tokens,
        'color.secondaryFixedDim',
        TchColors.secondaryFixedDim,
      ),
      onSecondaryFixed: _color(
        tokens,
        'color.onSecondaryFixed',
        TchColors.onSecondaryFixed,
      ),
      onSecondaryFixedVariant: _color(
        tokens,
        'color.onSecondaryFixedVariant',
        TchColors.onSecondaryFixedVariant,
      ),
      tertiary: _color(tokens, 'color.tertiary', TchColors.tertiary),
      onTertiary: _color(tokens, 'color.onTertiary', TchColors.onTertiary),
      tertiaryContainer: _color(
        tokens,
        'color.tertiaryContainer',
        TchColors.tertiaryContainer,
      ),
      onTertiaryContainer: _color(
        tokens,
        'color.onTertiaryContainer',
        TchColors.onTertiaryContainer,
      ),
      tertiaryFixed: _color(
        tokens,
        'color.tertiaryFixed',
        TchColors.tertiaryFixed,
      ),
      tertiaryFixedDim: _color(
        tokens,
        'color.tertiaryFixedDim',
        TchColors.tertiaryFixedDim,
      ),
      onTertiaryFixed: _color(
        tokens,
        'color.onTertiaryFixed',
        TchColors.onTertiaryFixed,
      ),
      onTertiaryFixedVariant: _color(
        tokens,
        'color.onTertiaryFixedVariant',
        TchColors.onTertiaryFixedVariant,
      ),
      error: TchColors.error,
      onError: TchColors.onError,
      errorContainer: TchColors.errorContainer,
      onErrorContainer: TchColors.onErrorContainer,
      surface: _color(tokens, 'color.surface', TchColors.surface),
      surfaceBright: _color(
        tokens,
        'color.surfaceBright',
        TchColors.surfaceBright,
      ),
      surfaceDim: _color(tokens, 'color.surfaceDim', TchColors.surfaceDim),
      surfaceContainerLowest: _color(
        tokens,
        'color.surfaceContainerLowest',
        TchColors.surfaceContainerLowest,
      ),
      surfaceContainerLow: _color(
        tokens,
        'color.surfaceContainerLow',
        TchColors.surfaceContainerLow,
      ),
      surfaceContainer: _color(
        tokens,
        'color.surfaceContainer',
        TchColors.surfaceContainer,
      ),
      surfaceContainerHigh: _color(
        tokens,
        'color.surfaceContainerHigh',
        TchColors.surfaceContainerHigh,
      ),
      surfaceContainerHighest: _color(
        tokens,
        'color.surfaceContainerHighest',
        TchColors.surfaceContainerHighest,
      ),
      surfaceTint: _color(tokens, 'color.surfaceTint', TchColors.surfaceTint),
      onSurface: _color(tokens, 'color.onSurface', TchColors.onSurface),
      onSurfaceVariant: _color(
        tokens,
        'color.onSurfaceVariant',
        TchColors.onSurfaceVariant,
      ),
      outline: _color(tokens, 'color.outline', TchColors.outline),
      outlineVariant: _color(
        tokens,
        'color.outlineVariant',
        TchColors.outlineVariant,
      ),
      inverseSurface: _color(
        tokens,
        'color.inverseSurface',
        TchColors.inverseSurface,
      ),
      onInverseSurface: _color(
        tokens,
        'color.onInverseSurface',
        TchColors.onInverseSurface,
      ),
      inversePrimary: _color(
        tokens,
        'color.inversePrimary',
        TchColors.inversePrimary,
      ),
      shadow: TchColors.shadow,
      scrim: TchColors.scrim,
    );

    final fontFamily = _fontFamily(tokens['typography.fontFamily']);

    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      fontFamily: fontFamily,
      // POS terminals: surfaceContainerLow as page background so white cards
      // (surfaceContainerLowest) stand out clearly under ambient/glare conditions.
      scaffoldBackgroundColor: _color(
        tokens,
        'color.background',
        TchColors.background,
      ),
      // AppBar: white background = max legibility for terminal ID + status.
      appBarTheme: AppBarTheme(
        backgroundColor: scheme.surfaceContainerLowest,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 1,
      ),
      // Cards always white to contrast with the tinted page background.
      cardTheme: CardThemeData(
        color: scheme.surfaceContainerLowest,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: BorderSide(color: scheme.outlineVariant),
        ),
      ),
      // NavigationBar matches surfaceContainerLowest for bottom nav contrast.
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: scheme.surfaceContainerLowest,
        indicatorColor: scheme.primaryContainer,
      ),
    );
  }

  static Color? _parseHex(String? hex) {
    if (hex == null || hex.isEmpty) return null;
    final clean = hex.replaceAll('#', '').trim();
    if (clean.length != 6) return null;
    final value = int.tryParse('FF$clean', radix: 16);
    return value != null ? Color(value) : null;
  }

  static Color _color(Map<String, String> tokens, String key, Color fallback) =>
      _parseHex(tokens[key]) ?? fallback;

  static String? _fontFamily(String? token) {
    if (token == null || token == 'system') return null;
    return switch (token.toLowerCase()) {
      'inter' => 'Inter',
      'roboto' => 'Roboto',
      'poppins' => 'Poppins',
      'plus-jakarta-sans' => 'Plus Jakarta Sans',
      _ => null,
    };
  }
}
