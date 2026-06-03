class RuntimeSettings {
  const RuntimeSettings({
    this.featureFlags = const {},
    this.values = const {},
  });

  final Map<String, bool> featureFlags;
  final Map<String, Object?> values;

  static const empty = RuntimeSettings();

  bool isEnabled(String key, {bool defaultValue = false}) =>
      featureFlags[key] ?? defaultValue;

  T? value<T>(String key) => values[key] as T?;

  factory RuntimeSettings.fromJson(Map<String, dynamic> json) {
    final rawFlags = json['featureFlags'] as Map<String, dynamic>? ?? {};
    return RuntimeSettings(
      featureFlags: rawFlags.map(
        (k, v) => MapEntry(k, v as bool? ?? false),
      ),
      values: Map<String, Object?>.from(json['values'] as Map? ?? {}),
    );
  }
}
