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
      'color.primary': '#5E89EF',
      'color.secondary': '#6F5EEF',
      'color.surface': '#F3EFF2',
      'color.onSurface': '#1D1B20',
      'typography.fontFamily': 'inter',
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
