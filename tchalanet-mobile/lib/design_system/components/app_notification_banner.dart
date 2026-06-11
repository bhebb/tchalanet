import 'package:flutter/material.dart';

import '../../core/notifications/app_notification.dart';
import '../tokens/tch_colors.dart';
import '../tokens/tch_radius.dart';
import '../tokens/tch_spacing.dart';

class AppNotificationBanner extends StatelessWidget {
  const AppNotificationBanner({
    super.key,
    required this.kind,
    required this.message,
    required this.dismissTooltip,
    required this.onDismiss,
    this.title,
    this.actionLabel,
    this.onAction,
    this.copySupportTooltip,
    this.onCopySupport,
  });

  final AppNotificationKind kind;
  final String? title;
  final String message;
  final String dismissTooltip;
  final VoidCallback onDismiss;
  final String? actionLabel;
  final VoidCallback? onAction;
  final String? copySupportTooltip;
  final VoidCallback? onCopySupport;

  (Color, Color, IconData) _visuals(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return switch (kind) {
      AppNotificationKind.info => (
        scheme.primaryContainer,
        scheme.onPrimaryContainer,
        Icons.info_outline_rounded,
      ),
      AppNotificationKind.success => (
        TchColors.successContainer,
        TchColors.success,
        Icons.check_circle_outline_rounded,
      ),
      AppNotificationKind.warning => (
        TchColors.warningContainer,
        TchColors.warning,
        Icons.warning_amber_rounded,
      ),
      AppNotificationKind.error => (
        scheme.errorContainer,
        scheme.onErrorContainer,
        Icons.error_outline_rounded,
      ),
    };
  }

  @override
  Widget build(BuildContext context) {
    final (background, foreground, icon) = _visuals(context);
    final textTheme = Theme.of(context).textTheme;

    return Semantics(
      liveRegion: true,
      child: Material(
        color: background,
        elevation: 6,
        shadowColor: Theme.of(context).colorScheme.shadow,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(TchRadius.md),
        ),
        clipBehavior: Clip.antiAlias,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(
            TchSpacing.s16,
            TchSpacing.s12,
            TchSpacing.s8,
            TchSpacing.s12,
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(icon, color: foreground),
              const SizedBox(width: TchSpacing.s12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (title != null) ...[
                      Text(
                        title!,
                        style: textTheme.titleSmall?.copyWith(
                          color: foreground,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: TchSpacing.s4),
                    ],
                    Text(
                      message,
                      style: textTheme.bodyMedium?.copyWith(color: foreground),
                    ),
                  ],
                ),
              ),
              if (actionLabel != null && onAction != null)
                TextButton(
                  onPressed: onAction,
                  style: TextButton.styleFrom(foregroundColor: foreground),
                  child: Text(actionLabel!),
                ),
              if (copySupportTooltip != null && onCopySupport != null)
                IconButton(
                  onPressed: onCopySupport,
                  tooltip: copySupportTooltip,
                  visualDensity: VisualDensity.compact,
                  color: foreground,
                  icon: const Icon(Icons.content_copy_rounded),
                ),
              IconButton(
                onPressed: onDismiss,
                tooltip: dismissTooltip,
                visualDensity: VisualDensity.compact,
                color: foreground,
                icon: const Icon(Icons.close_rounded),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
