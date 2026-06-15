import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app/app.dart';
import 'core/config/firebase_config.dart';
import 'core/i18n/locale_repository.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeFirebaseAuth();
  final localeRepository = await loadLocaleRepository();
  runApp(
    ProviderScope(
      overrides: [localeRepositoryProvider.overrideWithValue(localeRepository)],
      child: const App(),
    ),
  );
}
