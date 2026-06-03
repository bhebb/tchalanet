import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../features/auth/presentation/view_models/auth_controller.dart';
import '../features/auth/presentation/views/forbidden_page.dart';
import '../features/auth/presentation/views/login_page.dart';
import '../features/pos/presentation/views/pos_dashboard_page.dart';

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
        builder: (context, _) => const PosDashboardPage(),
      ),
      GoRoute(
        path: '/forbidden',
        builder: (context, _) => const ForbiddenPage(),
      ),
    ],
  );
});
