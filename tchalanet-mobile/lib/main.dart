import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app/app.dart';
import 'core/client_diagnostics/client_diagnostics_error_hooks.dart';
import 'core/config/firebase_config.dart';
import 'core/i18n/locale_repository.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeFirebaseAuth();
  final localeRepository = await loadLocaleRepository();
  final container = ProviderContainer(
    overrides: [localeRepositoryProvider.overrideWithValue(localeRepository)],
  );
  installClientDiagnosticsErrorHooks(container);
  runApp(UncontrolledProviderScope(container: container, child: const App()));
}
