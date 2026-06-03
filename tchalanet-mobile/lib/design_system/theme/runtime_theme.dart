class ThemePreset {
  const ThemePreset({
    required this.id,
    required this.name,
    required this.primaryColor,
    this.secondaryColor,
    this.surfaceColor,
  });

  final String id;
  final String name;
  final int primaryColor;
  final int? secondaryColor;
  final int? surfaceColor;

  factory ThemePreset.fromJson(Map<String, dynamic> json) => ThemePreset(
    id: json['id'] as String? ?? '',
    name: json['name'] as String? ?? '',
    primaryColor: json['primaryColor'] as int? ?? 0xFF1A237E,
    secondaryColor: json['secondaryColor'] as int?,
    surfaceColor: json['surfaceColor'] as int?,
  );
}

class RuntimeTheme {
  const RuntimeTheme({required this.preset, this.isDefault = false});

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

  factory RuntimeTheme.fromJson(Map<String, dynamic> json) => RuntimeTheme(
    preset: ThemePreset.fromJson(
      json['preset'] as Map<String, dynamic>? ?? {},
    ),
    isDefault: json['isDefault'] as bool? ?? false,
  );
}
