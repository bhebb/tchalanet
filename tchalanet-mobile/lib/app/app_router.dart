import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../core/runtime/runtime_controller.dart';
import '../core/runtime/runtime_models.dart';
import '../features/auth/presentation/view_models/auth_controller.dart';
import '../features/auth/presentation/views/change_pin_page.dart';
import '../features/auth/presentation/views/forbidden_page.dart';
import '../features/auth/presentation/views/login_page.dart';
import '../features/cashier/home/presentation/view_models/cashier_home_providers.dart';
import '../features/cashier/home/presentation/views/cashier_home_page.dart';
import '../features/cashier/home/presentation/views/seller_terminal_draw_report_page.dart';
import '../features/cashier/home/presentation/views/seller_terminal_profile_page.dart';
import '../features/cashier/home/presentation/views/seller_terminal_stats_page.dart';
import '../features/cashier/tickets/presentation/views/cashier_history_page.dart';
import '../features/cashier/tickets/presentation/views/cashier_scan_page.dart';
import '../features/cashier/tickets/presentation/views/cashier_sell_page.dart';
import '../features/cashier/tickets/presentation/views/cashier_sell_success_page.dart';
import '../features/cashier/tickets/presentation/views/cashier_ticket_detail_page.dart';
import '../features/draw/presentation/views/seller_terminal_results_page.dart';
import '../features/notifications/presentation/views/notification_center_page.dart';

final _rootNavigatorKey = GlobalKey<NavigatorState>();

/// Pings go_router to re-run [redirect] on auth/runtime changes without
/// tearing down and rebuilding the GoRouter (and its Navigator) each time —
/// recreating GoRouter on every state change caused a blank screen churn
/// right after login (auth + runtime bootstrap fire several state updates
/// in quick succession).
///
/// A hand-rolled [Listenable] rather than Flutter's usual observable-state
/// base class: this isn't app state (the architecture guard reserves that
/// for Riverpod's Notifier/AsyncNotifier) — it only exists to satisfy
/// go_router's `refreshListenable` contract, which requires a [Listenable].
class _RouterRefreshListenable implements Listenable {
  _RouterRefreshListenable(Ref ref) {
    ref.listen(authControllerProvider, (_, _) => _notify());
    ref.listen(runtimeControllerProvider, (_, _) => _notify());
  }

  final _listeners = <VoidCallback>{};

  @override
  void addListener(VoidCallback listener) => _listeners.add(listener);

  @override
  void removeListener(VoidCallback listener) => _listeners.remove(listener);

  void _notify() {
    for (final listener in List<VoidCallback>.of(_listeners)) {
      listener();
    }
  }

  void dispose() => _listeners.clear();
}

final appRouterProvider = Provider<GoRouter>((ref) {
  final refreshListenable = _RouterRefreshListenable(ref);
  ref.onDispose(refreshListenable.dispose);

  return GoRouter(
    navigatorKey: _rootNavigatorKey,
    initialLocation: '/login',
    refreshListenable: refreshListenable,
    redirect: (context, state) {
      final authState = ref.read(authControllerProvider);
      final runtimeState = ref.read(runtimeControllerProvider);
      final isAuthenticated = authState is AuthAuthenticated;
      final isUnknown = authState is AuthUnknown;
      final isOnLogin = state.matchedLocation == '/login';
      final isOnForbidden = state.matchedLocation == '/forbidden';
      final isRuntimeBlocked =
          runtimeState.snapshot?.status == RuntimeStatus.blocked;

      if (isUnknown) return null;
      if (!isAuthenticated && !isOnLogin && !isOnForbidden) return '/login';
      if (isAuthenticated && isRuntimeBlocked && !isOnForbidden) {
        return '/forbidden';
      }
      if (isAuthenticated && isOnLogin) return '/pos';
      return null;
    },
    routes: [
      GoRoute(path: '/login', builder: (context, _) => const LoginPage()),
      GoRoute(
        path: '/change-pin',
        builder: (context, _) => const ChangePinPage(),
      ),
      GoRoute(path: '/pos', builder: (context, _) => const CashierHomePage()),
      // Bottom nav: Home | Tickets | Results | Reports. Profile belongs to the avatar.
      GoRoute(
        path: '/pos/history',
        builder: (context, _) => const CashierHistoryPage(),
      ),
      GoRoute(
        path: '/pos/scan',
        builder: (context, _) => const CashierScanPage(),
      ),
      GoRoute(
        path: '/pos/profile',
        builder: (context, _) => const SellerTerminalProfilePage(),
      ),
      GoRoute(
        path: '/pos/settings',
        builder: (context, _) => const SellerTerminalSettingsPage(),
      ),
      GoRoute(
        path: '/pos/notifications',
        builder: (context, _) => const _NotificationCenterRoutePage(),
      ),
      GoRoute(
        path: '/pos/reports',
        builder: (context, _) => const SellerTerminalStatsPage(),
      ),
      // Per-draw report drill-down. Optional extra keys: isoDate, drawLabel,
      // providerSchedule, providerZone, localSchedule, localZone.
      GoRoute(
        path: '/pos/reports/draw/:drawId',
        builder: (context, state) {
          final extra = state.extra as Map<String, String?>?;
          return SellerTerminalDrawReportPage(
            drawId: state.pathParameters['drawId']!,
            isoDate: extra?['isoDate'] ?? reportIsoDate(DateTime.now()),
            drawLabel: extra?['drawLabel'],
            providerSchedule: extra?['providerSchedule'],
            providerZone: extra?['providerZone'],
            localSchedule: extra?['localSchedule'],
            localZone: extra?['localZone'],
          );
        },
      ),
      GoRoute(
        path: '/pos/results',
        builder: (context, _) => const SellerTerminalResultsPage(),
      ),
      // Sell flow — /sell matches server-side HomeAction.route
      // Optional extra: {'drawId': String} to pre-select a specific draw.
      GoRoute(
        path: '/sell',
        builder: (context, state) {
          final extra = state.extra as Map<String, String?>?;
          return CashierSellPage(preselectedDrawId: extra?['drawId']);
        },
      ),
      // Ticket flows
      GoRoute(
        path: '/pos/sell/success',
        builder: (context, state) {
          final extra = state.extra as Map<String, String?>?;
          return CashierSellSuccessPage(
            ticketId: extra?['ticketId'] ?? '',
            ticketCode: extra?['ticketCode'] ?? '',
            publicCode: extra?['publicCode'],
            shareableText: extra?['shareableText'],
            drawId: extra?['drawId'],
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

class _NotificationCenterRoutePage extends ConsumerWidget {
  const _NotificationCenterRoutePage();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref
        .watch(posProfileProvider)
        .asData
        ?.value
        .settings
        .notifications;
    return NotificationCenterPage(
      notificationsEnabled: settings?.enabled ?? true,
      criticalOnly: settings?.criticalOnly ?? false,
    );
  }
}
