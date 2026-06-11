import 'package:flutter/material.dart';

import '../tokens/tch_colors.dart';
import '../tokens/tch_radius.dart';
import '../tokens/tch_spacing.dart';

enum StatusBadgeKind { neutral, ready, warning, blocked, missing }

class StatusBadge extends StatelessWidget {
  const StatusBadge({
    super.key,
    required this.label,
    this.kind = StatusBadgeKind.neutral,
    this.icon,
  });

  final String label;
  final StatusBadgeKind kind;
  final IconData? icon;

  (Color, Color) _colors(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return switch (kind) {
      StatusBadgeKind.neutral => (
        scheme.surfaceContainerHighest,
        scheme.onSurfaceVariant,
      ),
      StatusBadgeKind.ready => (TchColors.successContainer, TchColors.success),
      StatusBadgeKind.warning => (
        TchColors.warningContainer,
        TchColors.warning,
      ),
      StatusBadgeKind.blocked => (
        scheme.errorContainer,
        scheme.onErrorContainer,
      ),
      StatusBadgeKind.missing => (
        scheme.surfaceContainerHigh,
        TchColors.missing,
      ),
    };
  }

  @override
  Widget build(BuildContext context) {
    final (background, foreground) = _colors(context);

    return Semantics(
      label: label,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: background,
          borderRadius: BorderRadius.circular(TchRadius.pill),
        ),
        child: Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: TchSpacing.s8,
            vertical: TchSpacing.s4,
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (icon != null) ...[
                Icon(icon, size: 14, color: foreground),
                const SizedBox(width: TchSpacing.s4),
              ],
              Text(
                label,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: foreground,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
