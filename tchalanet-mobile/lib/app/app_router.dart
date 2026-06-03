import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../features/auth/presentation/view_models/auth_controller.dart';
import '../features/auth/presentation/views/forbidden_page.dart';
import '../features/auth/presentation/views/login_page.dart';
import '../features/cashier/home/presentation/views/cashier_home_page.dart';
import '../features/cashier/operationalcontext/presentation/views/cashier_setup_page.dart';
import '../features/pos/presentation/views/pos_stub_page.dart';

final _rootNavigatorKey = GlobalKey<NavigatorState>();

final appRouterProvider = Provider<GoRouter>((ref) {
  final authState = ref.watch(authControllerProvider);

  return GoRouter(
    navigatorKey: _rootNavigatorKey,
    initialLocation: '/login',
    redirect: (context, state) {
      final isAuthenticated = authState is AuthAuthenticated;
      final isUnknown = authState is AuthUnknown;
      final isOnLogin = state.matchedLocation == '/login';
      final isOnForbidden = state.matchedLocation == '/forbidden';

      if (isUnknown) return null;
      if (!isAuthenticated && !isOnLogin && !isOnForbidden) return '/login';
      if (isAuthenticated && isOnLogin) return '/pos';
      return null;
    },
    routes: [
      GoRoute(
        path: '/login',
        builder: (context, _) => const LoginPage(),
      ),
      GoRoute(
        path: '/pos',
        builder: (context, _) => const CashierHomePage(),
      ),
      GoRoute(
        path: '/pos/setup',
        builder: (context, _) => const CashierSetupPage(),
      ),
      GoRoute(
        path: '/pos/reports',
        builder: (context, _) => const PosStubPage(title: 'Reports', index: 1),
      ),
      GoRoute(
        path: '/pos/history',
        builder: (context, _) => const PosStubPage(title: 'History', index: 2),
      ),
      GoRoute(
        path: '/pos/settings',
        builder: (context, _) => const PosStubPage(title: 'Settings', index: 3),
      ),
      GoRoute(
        path: '/forbidden',
        builder: (context, _) => const ForbiddenPage(),
      ),
    ],
  );
});
