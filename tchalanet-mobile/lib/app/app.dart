import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/theme/theme_repository.dart';
import '../design_system/layout/screen_size.dart';
import 'app_router.dart';

/// Surface context provider — change to [SurfaceContext.posTerminal] when the
/// app is deployed on POS hardware (set via dart-define or runtime settings).
const bool _isPosDevice =
    bool.fromEnvironment('POS_DEVICE', defaultValue: false);

class App extends ConsumerWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(appRouterProvider);
    final theme = ref.watch(runtimeThemeDataProvider);

    return PosContextProvider(
      context: _isPosDevice ? SurfaceContext.posTerminal : SurfaceContext.mobile,
      child: MaterialApp.router(
        title: 'Tchalanet POS',
        theme: theme,
        routerConfig: router,
        debugShowCheckedModeBanner: false,
      ),
    );
  }
}
