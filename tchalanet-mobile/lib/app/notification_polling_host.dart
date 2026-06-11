import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../features/auth/presentation/view_models/auth_controller.dart';
import '../features/notifications/presentation/view_models/notification_summary_controller.dart';

class NotificationPollingHost extends ConsumerStatefulWidget {
  const NotificationPollingHost({super.key, required this.child});

  final Widget child;

  @override
  ConsumerState<NotificationPollingHost> createState() =>
      _NotificationPollingHostState();
}

class _NotificationPollingHostState extends ConsumerState<NotificationPollingHost>
    with WidgetsBindingObserver {
  Timer? _timer;
  bool _authenticated = false;

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
    if (state == AppLifecycleState.resumed && _authenticated) {
      ref.read(notificationSummaryProvider.notifier).refresh();
    }
  }

  void _syncPolling(bool authenticated) {
    if (_authenticated == authenticated) return;
    _authenticated = authenticated;
    _timer?.cancel();

    if (!authenticated) {
      ref.read(notificationSummaryProvider.notifier).reset();
      return;
    }

    ref.read(notificationSummaryProvider.notifier).refresh(force: true);
    _timer = Timer.periodic(notificationPollingInterval, (_) {
      ref.read(notificationSummaryProvider.notifier).refresh();
    });
  }

  @override
  Widget build(BuildContext context) {
    final authenticated = ref.watch(authControllerProvider) is AuthAuthenticated;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _syncPolling(authenticated);
    });
    return widget.child;
  }
}
