import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/i18n/i18n_repository.dart';
import '../core/notifications/app_notification.dart';
import '../core/notifications/app_notification_controller.dart';
import '../design_system/components/app_notification_banner.dart';
import '../design_system/tokens/tch_spacing.dart';

class AppNotificationHost extends ConsumerStatefulWidget {
  const AppNotificationHost({super.key, required this.child});

  final Widget child;

  @override
  ConsumerState<AppNotificationHost> createState() =>
      _AppNotificationHostState();
}

class _AppNotificationHostState extends ConsumerState<AppNotificationHost> {
  Timer? _dismissTimer;
  int? _scheduledId;

  @override
  void dispose() {
    _dismissTimer?.cancel();
    super.dispose();
  }

  void _scheduleDismiss(AppNotification? notification) {
    if (notification == null || notification.id == _scheduledId) return;
    _dismissTimer?.cancel();
    _scheduledId = notification.id;
    _dismissTimer = Timer(notification.duration, () {
      if (!mounted) return;
      ref.read(appNotificationProvider.notifier).dismiss(notification.id);
    });
  }

  void _dismiss(AppNotification notification) {
    _dismissTimer?.cancel();
    _scheduledId = null;
    ref.read(appNotificationProvider.notifier).dismiss(notification.id);
  }

  @override
  Widget build(BuildContext context) {
    final notifications = ref.watch(appNotificationProvider);
    final notification = notifications.isEmpty ? null : notifications.first;
    final translations = ref.watch(i18nBundleProvider);

    WidgetsBinding.instance.addPostFrameCallback((_) {
      _scheduleDismiss(notification);
    });

    return Stack(
      children: [
        widget.child,
        SafeArea(
          child: Align(
            alignment: Alignment.topCenter,
            child: Padding(
              padding: const EdgeInsets.all(TchSpacing.s16),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 600),
                child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 200),
                  child: notification == null
                      ? const SizedBox.shrink()
                      : AppNotificationBanner(
                          key: ValueKey(notification.id),
                          kind: notification.kind,
                          title: notification.titleKey == null
                              ? null
                              : translations.translate(
                                  notification.titleKey!,
                                  fallback: notification.titleFallback,
                                ),
                          message: translations.translate(
                            notification.messageKey,
                            fallback: notification.messageFallback,
                          ),
                          actionLabel: notification.actionKey == null
                              ? null
                              : translations.translate(notification.actionKey!),
                          onAction: notification.onAction == null
                              ? null
                              : () {
                                  notification.onAction!();
                                  _dismiss(notification);
                                },
                          copySupportTooltip:
                              notification.supportReference == null
                              ? null
                              : translations.translate(
                                  'common.notification.copy_support_reference',
                                ),
                          onCopySupport: notification.supportReference == null
                              ? null
                              : () => Clipboard.setData(
                                  ClipboardData(
                                    text: notification.supportReference!
                                        .toClipboardText(),
                                  ),
                                ),
                          dismissTooltip: translations.translate(
                            'common.notification.dismiss',
                          ),
                          onDismiss: () => _dismiss(notification),
                        ),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
