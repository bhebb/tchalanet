import 'package:flutter/material.dart';

import '../tokens/tch_colors.dart';

/// Builds a Material 3 ThemeData from a token map returned by the server.
///
/// Token keys: color.primary, color.secondary, color.surface, color.onSurface,
///             typography.fontFamily
/// Token values: CSS hex strings (#RRGGBB) or font name strings.
abstract final class ThemeBuilder {
  static ThemeData buildFromTokens(Map<String, String> tokens) {
    final primary = _parseHex(tokens['color.primary']) ?? TchColors.primary;

    var scheme = ColorScheme.fromSeed(
      seedColor: primary,
      brightness: Brightness.light,
    );

    final secondary = _parseHex(tokens['color.secondary']);
    final surface = _parseHex(tokens['color.surface']);
    final onSurface = _parseHex(tokens['color.onSurface']);

    if (secondary != null || surface != null || onSurface != null) {
      scheme = scheme.copyWith(
        secondary: secondary,
        surface: surface,
        onSurface: onSurface,
      );
    }

    final fontFamily = _fontFamily(tokens['typography.fontFamily']);

    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      fontFamily: fontFamily,
      // POS terminals: surfaceContainerLow as page background so white cards
      // (surfaceContainerLowest) stand out clearly under ambient/glare conditions.
      scaffoldBackgroundColor: scheme.surfaceContainerLow,
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

  static String? _fontFamily(String? token) {
    if (token == null || token == 'system') return null;
    return switch (token.toLowerCase()) {
      'inter' => 'Inter',
      'roboto' => 'Roboto',
      'poppins' => 'Poppins',
      _ => null,
    };
  }
}
