import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../features/auth/presentation/view_models/auth_controller.dart';
import '../features/auth/presentation/views/forbidden_page.dart';
import '../features/auth/presentation/views/login_page.dart';
import '../features/cashier/home/presentation/views/cashier_home_page.dart';
import '../features/cashier/operationalcontext/presentation/views/cashier_setup_page.dart';
import '../features/cashier/session/presentation/views/cashier_session_open_page.dart';
import '../features/cashier/tickets/presentation/views/cashier_sell_success_page.dart';
import '../features/cashier/tickets/presentation/views/cashier_ticket_detail_page.dart';
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
        path: '/pos/session/open',
        builder: (context, _) => const CashierSessionOpenPage(),
      ),
      // Bottom nav: Ventes | Historique | Scanner | Profil
      GoRoute(
        path: '/pos/history',
        builder: (context, _) => const PosStubPage(title: 'Historique', index: 1),
      ),
      GoRoute(
        path: '/pos/scan',
        builder: (context, _) => const PosStubPage(title: 'Scanner', index: 2),
      ),
      GoRoute(
        path: '/pos/profile',
        builder: (context, _) => const PosStubPage(title: 'Profil', index: 3),
      ),
      // Ticket flows
      GoRoute(
        path: '/pos/sell/success',
        builder: (context, state) {
          final extra = state.extra as Map<String, String?>?;
          return CashierSellSuccessPage(
            ticketCode: extra?['ticketCode'] ?? '',
            publicCode: extra?['publicCode'],
            shareableText: extra?['shareableText'],
          );
        },
      ),
      GoRoute(
        path: '/pos/tickets/:ticketId',
        builder: (context, state) => CashierTicketDetailPage(
          ticketId: state.pathParameters['ticketId']!,
        ),
      ),
      GoRoute(
        path: '/forbidden',
        builder: (context, _) => const ForbiddenPage(),
      ),
    ],
  );
});
