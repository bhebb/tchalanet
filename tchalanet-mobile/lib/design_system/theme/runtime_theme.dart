/// Matches server ThemeRuntimeView:
/// presetCode, mode, tokens (CSS token map), isDefault, version.
class RuntimeTheme {
  const RuntimeTheme({
    required this.presetCode,
    required this.mode,
    required this.tokens,
    this.isDefault = false,
    this.version = 0,
  });

  final String presetCode;
  final String mode;
  final Map<String, String> tokens;
  final bool isDefault;
  final int version;

  /// Tchalanet default — mirrors TchColors palette, used before backend responds.
  static const defaultTheme = RuntimeTheme(
    presetCode: 'tchalanet',
    mode: 'light',
    tokens: {
      'color.primary': '#1A1B4B',
      'color.onPrimary': '#FFFFFF',
      'color.primaryContainer': '#2E3192',
      'color.onPrimaryContainer': '#FFFFFF',
      'color.primaryFixed': '#E1E0FF',
      'color.primaryFixedDim': '#C1C1FC',
      'color.onPrimaryFixed': '#141545',
      'color.onPrimaryFixedVariant': '#404273',
      'color.secondary': '#5D5C72',
      'color.onSecondary': '#FFFFFF',
      'color.secondaryContainer': '#E2E0FA',
      'color.onSecondaryContainer': '#191A2D',
      'color.secondaryFixed': '#E2E0FA',
      'color.secondaryFixedDim': '#C6C4DE',
      'color.onSecondaryFixed': '#191A2D',
      'color.onSecondaryFixedVariant': '#45455A',
      'color.tertiary': '#FECB00',
      'color.onTertiary': '#241A00',
      'color.tertiaryContainer': '#FFE08B',
      'color.onTertiaryContainer': '#241A00',
      'color.tertiaryFixed': '#FFE08B',
      'color.tertiaryFixedDim': '#F1C100',
      'color.onTertiaryFixed': '#241A00',
      'color.onTertiaryFixedVariant': '#584400',
      'color.background': '#F9F9FC',
      'color.surface': '#F9F9FC',
      'color.surfaceBright': '#FFFFFF',
      'color.surfaceDim': '#DCD9DE',
      'color.surfaceContainerLowest': '#FFFFFF',
      'color.surfaceContainerLow': '#F6F2F7',
      'color.surfaceContainer': '#F0EDF2',
      'color.surfaceContainerHigh': '#EAE7EC',
      'color.surfaceContainerHighest': '#E5E1E6',
      'color.surfaceTint': '#1A1B4B',
      'color.onSurface': '#1A1C1E',
      'color.onSurfaceVariant': '#464652',
      'color.outline': '#777680',
      'color.outlineVariant': '#C8C5D0',
      'color.inverseSurface': '#313034',
      'color.onInverseSurface': '#F3EFF4',
      'color.inversePrimary': '#C1C1FC',
      'typography.fontFamily': 'plus-jakarta-sans',
    },
    isDefault: true,
  );

  factory RuntimeTheme.fromJson(Map<String, dynamic> json) => RuntimeTheme(
    presetCode: json['presetCode'] as String? ?? 'tchalanet',
    mode: json['mode'] as String? ?? 'light',
    tokens: Map<String, String>.from(json['tokens'] as Map? ?? {}),
    isDefault: json['isDefault'] as bool? ?? false,
    version: (json['version'] as num?)?.toInt() ?? 0,
  );
}
