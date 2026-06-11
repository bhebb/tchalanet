import 'support_reference.dart';

enum AppNotificationKind { info, success, warning, error }

enum AppNotificationOrigin { local, apiNotice, apiError }

class AppNotification {
  const AppNotification({
    required this.id,
    required this.kind,
    required this.messageKey,
    this.messageFallback,
    this.origin = AppNotificationOrigin.local,
    this.titleKey,
    this.titleFallback,
    this.actionKey,
    this.onAction,
    this.supportReference,
    this.duration = const Duration(seconds: 5),
  }) : assert(
         (actionKey == null) == (onAction == null),
         'actionKey and onAction must be supplied together',
       );

  final int id;
  final AppNotificationKind kind;
  final AppNotificationOrigin origin;
  final String? titleKey;
  final String? titleFallback;
  final String messageKey;
  final String? messageFallback;
  final String? actionKey;
  final void Function()? onAction;
  final SupportReference? supportReference;
  final Duration duration;

  bool hasSameContentAs(AppNotification other) =>
      kind == other.kind &&
      origin == other.origin &&
      titleKey == other.titleKey &&
      messageKey == other.messageKey &&
      messageFallback == other.messageFallback &&
      actionKey == other.actionKey &&
      supportReference?.traceId == other.supportReference?.traceId;
}
