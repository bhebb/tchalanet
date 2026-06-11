import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/design_system/theme/runtime_theme.dart';
import 'package:tchalanet_mobile/design_system/theme/theme_builder.dart';
import 'package:tchalanet_mobile/design_system/tokens/tch_colors.dart';

void main() {
  test('default theme uses the Tchalanet Material 3 brand roles', () {
    final theme = ThemeBuilder.buildFromTokens(
      RuntimeTheme.defaultTheme.tokens,
    );
    final scheme = theme.colorScheme;

    expect(theme.useMaterial3, isTrue);
    expect(scheme.primary, TchColors.primary);
    expect(scheme.primaryContainer, TchColors.primaryContainer);
    expect(scheme.tertiary, TchColors.tertiary);
    expect(scheme.secondaryContainer, TchColors.secondaryContainer);
    expect(scheme.secondaryContainer, isNot(TchColors.tertiary));
    expect(scheme.surface, TchColors.surface);
    expect(scheme.surfaceDim, TchColors.surfaceDim);
    expect(scheme.surfaceBright, TchColors.surfaceBright);
    expect(scheme.surfaceContainerLowest, TchColors.surfaceContainerLowest);
    expect(scheme.surfaceContainerLow, TchColors.surfaceContainerLow);
    expect(scheme.surfaceContainer, TchColors.surfaceContainer);
    expect(scheme.surfaceContainerHigh, TchColors.surfaceContainerHigh);
    expect(scheme.surfaceContainerHighest, TchColors.surfaceContainerHighest);
    expect(scheme.onSurfaceVariant, TchColors.onSurfaceVariant);
    expect(scheme.inverseSurface, TchColors.inverseSurface);
    expect(scheme.onInverseSurface, TchColors.onInverseSurface);
    expect(theme.scaffoldBackgroundColor, TchColors.background);
  });

  test('validated runtime surface roles can be overridden independently', () {
    final theme = ThemeBuilder.buildFromTokens({
      ...RuntimeTheme.defaultTheme.tokens,
      'color.background': '#070809',
      'color.surfaceContainerLow': '#010203',
      'color.onSurfaceVariant': '#040506',
    });

    expect(theme.scaffoldBackgroundColor, const Color(0xFF070809));
    expect(theme.colorScheme.surfaceContainerLow, const Color(0xFF010203));
    expect(theme.colorScheme.onSurfaceVariant, const Color(0xFF040506));
    expect(
      theme.colorScheme.surfaceContainerLowest,
      TchColors.surfaceContainerLowest,
    );
  });

  test('unknown runtime tokens do not replace validated brand roles', () {
    final theme = ThemeBuilder.buildFromTokens({
      ...RuntimeTheme.defaultTheme.tokens,
      'color.notARealRole': '#FF00FF',
    });

    expect(theme.colorScheme.primary, TchColors.primary);
    expect(theme.colorScheme.tertiary, TchColors.tertiary);
  });
}
