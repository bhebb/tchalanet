import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/i18n/i18n_repository.dart';
import '../core/notifications/app_notification.dart';
import '../core/notifications/app_notification_controller.dart';
import '../core/runtime/runtime_controller.dart';
import '../features/auth/presentation/view_models/auth_controller.dart';
import '../features/notifications/presentation/view_models/notification_summary_controller.dart';

class RuntimePollingHost extends ConsumerStatefulWidget {
  const RuntimePollingHost({super.key, required this.child});

  final Widget child;

  @override
  ConsumerState<RuntimePollingHost> createState() => _RuntimePollingHostState();
}

class _RuntimePollingHostState extends ConsumerState<RuntimePollingHost>
    with WidgetsBindingObserver {
  Timer? _timer;
  bool? _authenticated;
  String? _publicLocale;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _timer?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state != AppLifecycleState.resumed || _authenticated != true) return;
    final controller = ref.read(runtimeControllerProvider.notifier);
    if (controller.shouldRefreshOnForeground(DateTime.now())) {
      unawaited(_refreshState());
    }
  }

  Future<void> _syncRuntime(bool authenticated, String locale) async {
    if (_authenticated == authenticated &&
        (authenticated || _publicLocale == locale)) {
      return;
    }
    _authenticated = authenticated;
    _timer?.cancel();

    if (!authenticated) {
      _publicLocale = locale;
      ref.read(runtimeControllerProvider.notifier).resetTenant();
      ref.read(notificationSummaryProvider.notifier).reset();
      await ref.read(runtimeControllerProvider.notifier).loadPublic(locale);
      return;
    }

    _publicLocale = null;
    await ref.read(runtimeControllerProvider.notifier).loadTenant();
    _applyRuntimeData();
    await _refreshState();
    _timer = Timer.periodic(runtimePollingInterval, (_) {
      unawaited(_refreshState());
    });
  }

  Future<void> _refreshState() async {
    final outcome = await ref
        .read(runtimeControllerProvider.notifier)
        .refreshTenantState();
    _applyRuntimeData();
    if (outcome == RuntimeRefreshOutcome.sessionExpired && mounted) {
      await ref.read(authControllerProvider.notifier).logout();
    }
  }

  void _applyRuntimeData() {
    final runtime = ref.read(runtimeControllerProvider);
    final summary =
        runtime.snapshot?.notifications ?? runtime.bootstrap?.notifications;
    if (summary != null) {
      ref
          .read(notificationSummaryProvider.notifier)
          .applyRuntimeSummary(
            unreadCount: summary.unreadCount,
            criticalCount: summary.criticalCount,
          );
    }
    final notices =
        runtime.snapshot?.notices ?? runtime.bootstrap?.notices ?? [];
    for (final notice in notices) {
      ref
          .read(appNotificationProvider.notifier)
          .show(
            kind: switch (notice.level) {
              'ERROR' => AppNotificationKind.error,
              'WARNING' => AppNotificationKind.warning,
              _ => AppNotificationKind.info,
            },
            messageKey: notice.code,
            messageFallback: notice.message,
            origin: AppNotificationOrigin.apiNotice,
          );
    }
  }

  @override
  Widget build(BuildContext context) {
    final authenticated =
        ref.watch(authControllerProvider) is AuthAuthenticated;
    final locale = ref.watch(localeProvider);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) unawaited(_syncRuntime(authenticated, locale));
    });
    return widget.child;
  }
}
