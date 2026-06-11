import 'package:flutter/material.dart';

import '../tokens/tch_colors.dart';
import '../tokens/tch_spacing.dart';
import 'semantic_action_button.dart';

enum FeedbackStateKind { loading, empty, error, offline, blocked, success }

class FeedbackState extends StatelessWidget {
  const FeedbackState({
    super.key,
    required this.kind,
    required this.title,
    this.message,
    this.actionLabel,
    this.onAction,
    this.compact = false,
  });

  final FeedbackStateKind kind;
  final String title;
  final String? message;
  final String? actionLabel;
  final VoidCallback? onAction;
  final bool compact;

  IconData get _icon => switch (kind) {
    FeedbackStateKind.loading => Icons.hourglass_top_rounded,
    FeedbackStateKind.empty => Icons.inbox_outlined,
    FeedbackStateKind.error => Icons.error_outline_rounded,
    FeedbackStateKind.offline => Icons.cloud_off_rounded,
    FeedbackStateKind.blocked => Icons.block_rounded,
    FeedbackStateKind.success => Icons.check_circle_outline_rounded,
  };

  Color _color(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return switch (kind) {
      FeedbackStateKind.loading => scheme.primary,
      FeedbackStateKind.empty => scheme.onSurfaceVariant,
      FeedbackStateKind.error => scheme.error,
      FeedbackStateKind.offline => TchColors.warning,
      FeedbackStateKind.blocked => TchColors.blocked,
      FeedbackStateKind.success => TchColors.success,
    };
  }

  @override
  Widget build(BuildContext context) {
    final color = _color(context);
    final textTheme = Theme.of(context).textTheme;
    final action = actionLabel != null && onAction != null
        ? TonalActionButton(
            label: actionLabel!,
            onPressed: onAction,
            expanded: false,
          )
        : null;

    return Semantics(
      liveRegion: true,
      child: Padding(
        padding: EdgeInsets.all(compact ? TchSpacing.s16 : TchSpacing.s24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (kind == FeedbackStateKind.loading)
              SizedBox.square(
                dimension: compact ? 32 : 48,
                child: CircularProgressIndicator(color: color, strokeWidth: 3),
              )
            else
              Icon(_icon, color: color, size: compact ? 40 : 56),
            SizedBox(height: compact ? TchSpacing.s12 : TchSpacing.s16),
            Text(
              title,
              textAlign: TextAlign.center,
              style: textTheme.titleMedium?.copyWith(
                color: Theme.of(context).colorScheme.onSurface,
                fontWeight: FontWeight.w700,
              ),
            ),
            if (message != null) ...[
              const SizedBox(height: TchSpacing.s8),
              Text(
                message!,
                textAlign: TextAlign.center,
                style: textTheme.bodyMedium?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
            ],
            if (action != null) ...[
              const SizedBox(height: TchSpacing.s16),
              action,
            ],
          ],
        ),
      ),
    );
  }
}
