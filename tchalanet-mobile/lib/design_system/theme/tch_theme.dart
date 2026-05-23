import 'package:flutter/material.dart';
import '../tokens/tch_colors.dart';

abstract final class TchTheme {
  static ThemeData light() {
    final scheme = const ColorScheme(
      brightness: Brightness.light,
      primary: TchColors.primary,
      onPrimary: TchColors.onPrimary,
      primaryContainer: TchColors.primaryContainer,
      onPrimaryContainer: TchColors.onPrimaryContainer,
      secondary: TchColors.secondary,
      onSecondary: TchColors.onSecondary,
      secondaryContainer: TchColors.secondaryContainer,
      onSecondaryContainer: TchColors.onSecondaryContainer,
      tertiary: TchColors.tertiary,
      onTertiary: TchColors.onTertiary,
      tertiaryContainer: TchColors.tertiaryContainer,
      onTertiaryContainer: TchColors.onTertiaryContainer,
      error: TchColors.error,
      onError: TchColors.onError,
      errorContainer: TchColors.errorContainer,
      onErrorContainer: TchColors.error,
      surface: TchColors.surface,
      onSurface: TchColors.onSurface,
      surfaceContainerHighest: TchColors.surfaceContainerHigh,
      outline: TchColors.outline,
      outlineVariant: TchColors.outline,
      shadow: Colors.black,
      scrim: Colors.black,
      inverseSurface: Color(0xFF2E3037),
      onInverseSurface: Color(0xFFF0F0F9),
      inversePrimary: Color(0xFFB1C5FF),
    );

    return ThemeData(
      useMaterial3: true,
      fontFamily: 'Inter',
      colorScheme: scheme,
      scaffoldBackgroundColor: TchColors.background,
    );
  }
}
