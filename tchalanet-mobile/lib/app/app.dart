import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/i18n/haitian_flutter_localizations.dart';
import '../core/i18n/i18n_repository.dart';
import '../design_system/layout/screen_size.dart';
import '../design_system/theme/tch_theme.dart';
import 'app_notification_host.dart';
import 'app_router.dart';
import 'runtime_polling_host.dart';

/// Surface context provider — change to [SurfaceContext.posTerminal] when the
/// app is deployed on POS hardware (set via dart-define or runtime settings).
const bool _isPosDevice = bool.fromEnvironment(
  'POS_DEVICE',
  defaultValue: false,
);

class App extends ConsumerWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(appRouterProvider);
    final localeCode = ref.watch(localeProvider);
    final translations = ref.watch(i18nBundleProvider);

    return PosContextProvider(
      context: _isPosDevice
          ? SurfaceContext.posTerminal
          : SurfaceContext.mobile,
      child: MaterialApp.router(
        onGenerateTitle: (_) =>
            translations.translate('app.name', fallback: 'Tchalanet POS'),
        theme: TchTheme.light(),
        locale: Locale(localeCode),
        supportedLocales: supportedLocaleCodes
            .map(Locale.new)
            .toList(growable: false),
        localizationsDelegates: const [
          HaitianMaterialLocalizationsDelegate(),
          HaitianCupertinoLocalizationsDelegate(),
          ...GlobalMaterialLocalizations.delegates,
        ],
        builder: (context, child) => RuntimePollingHost(
          child: AppNotificationHost(child: child ?? const SizedBox.shrink()),
        ),
        routerConfig: router,
        debugShowCheckedModeBanner: false,
      ),
    );
  }
}
