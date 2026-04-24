---
name: flutter
description: >
  Déclencher pour tout code Flutter, Dart, widgets, navigation, state management,
  appels API, ou configuration Android/POS dans apps/tchalanet-mobile.
  Indispensable si la tâche concerne : Riverpod, GoRouter, dio, Material 3,
  terminal POS Android, Motorola, ou l'application vendeur Tchalanet.
---

# Flutter — Tchalanet Mobile Vendeur

> Flutter (latest stable) · Dart · Android-first · POS + Mobile

## Contexte

Application vendeur Tchalanet — installée sur :

- Terminaux POS Android (Motorola, Sunmi, PAX…)
- Téléphones Android standards des vendeurs

**Repo** : `apps/tchalanet-mobile/` dans le monorepo (dossier indépendant — Nx ne gère pas Dart)

## Stack

| Item             | Choix                                     |
| ---------------- | ----------------------------------------- |
| State management | Riverpod (riverpod_annotation + code gen) |
| Navigation       | GoRouter                                  |
| HTTP             | Dio + Retrofit                            |
| Injection        | Riverpod providers                        |
| Sérialisation    | json_serializable + freezed               |
| Storage local    | Hive ou Isar (offline-first)              |
| UI               | Material 3                                |
| Tests            | flutter_test + mocktail                   |

## Structure projet

```
apps/tchalanet-mobile/
├─ lib/
│  ├─ core/
│  │  ├─ api/          ← Dio client, interceptors, auth
│  │  ├─ router/       ← GoRouter configuration
│  │  ├─ theme/        ← Material 3 theming
│  │  └─ l10n/         ← Localisation (fr, en, ht)
│  ├─ features/
│  │  ├─ auth/         ← Login vendeur, JWT Keycloak
│  │  ├─ sale/         ← Vente de ticket (flow principal)
│  │  ├─ draw/         ← Consultation tirages
│  │  └─ dashboard/    ← Tableau de bord vendeur
│  └─ main.dart
├─ android/
├─ test/
└─ pubspec.yaml
```

## Widgets — conventions

```dart
// ✅ StatelessWidget + Riverpod ConsumerWidget
class TicketSaleScreen extends ConsumerWidget {
  const TicketSaleScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final saleState = ref.watch(saleControllerProvider);

    return switch (saleState) {
      AsyncData(:final value) => TicketSaleForm(sale: value),
      AsyncError(:final error) => ErrorView(error: error),
      _                       => const LoadingIndicator(),
    };
  }
}

// ✅ ConsumerStatefulWidget si lifecycle nécessaire (rare)
// ❌ StatefulWidget seul — toujours Riverpod pour le state
```

## State management — Riverpod

```dart
// Provider avec code generation
@riverpod
class SaleController extends _$SaleController {

  @override
  FutureOr<Sale?> build() => null;

  Future<void> createSale(CreateSaleRequest request) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref.read(saleRepositoryProvider).create(request),
    );
  }
}

// Repository
@riverpod
SaleRepository saleRepository(SaleRepositoryRef ref) {
  return SaleRepository(ref.read(apiClientProvider));
}
```

## Appels API — Dio + interceptors

```dart
// Auth interceptor — ajoute le JWT Bearer automatiquement
class AuthInterceptor extends Interceptor {
  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final token = // récupérer depuis secure storage
    options.headers['Authorization'] = 'Bearer $token';
    handler.next(options);
  }
}

// ✅ Toujours gérer les erreurs réseau (POS offline fréquent)
try {
  final result = await saleRepo.create(request);
} on DioException catch (e) {
  if (e.type == DioExceptionType.connectionTimeout) {
    // Mode offline — sauvegarder localement
  }
}
```

## Navigation — GoRouter

```dart
final router = GoRouter(
  routes: [
    GoRoute(path: '/login',     builder: (_, __) => const LoginScreen()),
    GoRoute(path: '/dashboard', builder: (_, __) => const DashboardScreen()),
    GoRoute(path: '/sale',      builder: (_, __) => const SaleScreen()),
    GoRoute(path: '/draw/:id',  builder: (_, state) =>
        DrawScreen(drawId: state.pathParameters['id']!)),
  ],
  redirect: (context, state) {
    final isLoggedIn = // vérifier auth
    if (!isLoggedIn && state.matchedLocation != '/login') return '/login';
    return null;
  },
);
```

## POS Android — considérations

```dart
// ✅ Gérer les écrans de taille variable (POS vs téléphone)
class AdaptiveLayout extends StatelessWidget {
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;
    return screenWidth < 400
        ? CompactVendorLayout(...)   // petit écran POS
        : StandardVendorLayout(...); // téléphone standard
  }
}

// ✅ Boutons larges pour usage au doigt sur POS
ElevatedButton(
  style: ElevatedButton.styleFrom(
    minimumSize: const Size(double.infinity, 56), // hauteur min 56px
  ),
  onPressed: onTap,
  child: Text(label),
)

// ✅ Mode offline-first — les ventes doivent fonctionner sans réseau
// Hive/Isar pour stocker localement, sync quand réseau disponible
```

## Theming Material 3

```dart
ThemeData(
  useMaterial3: true,
  colorScheme: ColorScheme.fromSeed(
    seedColor: const Color(0xFF1D5799), // couleur Tchalanet
  ),
  // Tailles adaptées POS (touch targets généreux)
  textTheme: const TextTheme(
    bodyLarge: TextStyle(fontSize: 18),
    labelLarge: TextStyle(fontSize: 16),
  ),
)
```

## Commandes utiles

```bash
# Setup
flutter pub get
flutter pub run build_runner build --delete-conflicting-outputs

# Dev
flutter run                          # sur device connecté
flutter run -d <device-id>           # device spécifique
flutter devices                      # lister les devices

# Build Android
flutter build apk --release          # APK standard
flutter build apk --split-per-abi    # APK optimisé par architecture

# Tests
flutter test
flutter test --coverage

# Analyse
flutter analyze
dart fix --apply
```

## Checklist nouveau feature Flutter

- [ ] `ConsumerWidget` ou `ConsumerStatefulWidget` — jamais `StatefulWidget` seul
- [ ] Riverpod provider pour tout le state (pas de setState global)
- [ ] Gestion offline (DioException + stockage local)
- [ ] Boutons min 56px hauteur (touch targets POS)
- [ ] GoRouter pour la navigation (pas Navigator.push direct)
- [ ] Freezed pour les modèles de données
- [ ] Tests unitaires pour les controllers Riverpod
