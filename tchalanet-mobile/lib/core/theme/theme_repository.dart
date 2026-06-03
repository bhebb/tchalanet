import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../design_system/theme/runtime_theme.dart';
import '../../design_system/theme/theme_builder.dart';
import 'theme_service.dart';

/// Holds the active RuntimeTheme and exposes load methods.
/// Starts with [RuntimeTheme.defaultTheme]; updates silently on success.
class ThemeNotifier extends Notifier<RuntimeTheme> {
  @override
  RuntimeTheme build() {
    _loadPublic();
    return RuntimeTheme.defaultTheme;
  }

  Future<void> _loadPublic() async {
    try {
      final theme = await ref.read(themeServiceProvider).fetchPublicTheme();
      state = theme;
    } catch (_) {
      // Keep default — server unavailable at startup is non-fatal
    }
  }

  /// Call after auth to apply the tenant-specific theme.
  Future<void> loadTenantTheme() async {
    try {
      final theme = await ref.read(themeServiceProvider).fetchTenantTheme();
      state = theme;
    } catch (_) {
      // Keep current — tenant theme is cosmetic, not critical
    }
  }

  void reset() => state = RuntimeTheme.defaultTheme;
}

final themeNotifierProvider = NotifierProvider<ThemeNotifier, RuntimeTheme>(
  ThemeNotifier.new,
);

/// Ready-to-use ThemeData for MaterialApp — watches [themeNotifierProvider].
final runtimeThemeDataProvider = Provider<ThemeData>((ref) {
  final runtime = ref.watch(themeNotifierProvider);
  return ThemeBuilder.buildFromTokens(runtime.tokens);
});
