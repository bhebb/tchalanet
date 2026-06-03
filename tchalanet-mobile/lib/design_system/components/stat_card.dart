import 'package:flutter/material.dart';

import '../tokens/tch_radius.dart';
import '../tokens/tch_spacing.dart';

/// Large numeric stat card used on the POS home screen.
class StatCard extends StatelessWidget {
  const StatCard({
    super.key,
    required this.value,
    required this.label,
    this.unit,
    this.accentColor,
  });

  final String value;
  final String label;
  final String? unit;
  // Non-null draws a left-border accent (e.g. for "Gagnants")
  final Color? accentColor;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    return Container(
      padding: const EdgeInsets.all(TchSpacing.s16),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLowest,
        border: Border(
          left: accentColor != null
              ? BorderSide(color: accentColor!, width: 4)
              : BorderSide.none,
          top: BorderSide(color: scheme.outlineVariant),
          right: BorderSide(color: scheme.outlineVariant),
          bottom: BorderSide(color: scheme.outlineVariant),
        ),
        borderRadius: BorderRadius.circular(TchRadius.md),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label.toUpperCase(),
            style: textTheme.labelSmall?.copyWith(
              color: scheme.outline,
              letterSpacing: 0.5,
            ),
          ),
          const SizedBox(height: TchSpacing.s4),
          Text(
            value,
            style: textTheme.headlineMedium?.copyWith(
              fontWeight: FontWeight.w700,
              color: scheme.onSurface,
            ),
          ),
          if (unit != null) ...[
            const SizedBox(height: 2),
            Text(
              unit!,
              style: textTheme.labelSmall?.copyWith(
                color: scheme.onSurfaceVariant,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

/// Full-width variant showing a large display-numeric value (e.g. total sales).
class StatCardLarge extends StatelessWidget {
  const StatCardLarge({
    super.key,
    required this.value,
    required this.label,
    this.unit,
  });

  final String value;
  final String label;
  final String? unit;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    return Container(
      padding: const EdgeInsets.all(TchSpacing.s24),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLowest,
        border: Border.all(color: scheme.outlineVariant),
        borderRadius: BorderRadius.circular(TchRadius.md),
      ),
      child: Column(
        children: [
          Text(
            label.toUpperCase(),
            style: textTheme.labelSmall?.copyWith(
              color: scheme.outline,
              letterSpacing: 0.5,
            ),
          ),
          const SizedBox(height: TchSpacing.s8),
          Text(
            value,
            style: textTheme.displaySmall?.copyWith(
              fontWeight: FontWeight.w700,
              color: scheme.primary,
              letterSpacing: -0.5,
            ),
          ),
          if (unit != null) ...[
            const SizedBox(height: 2),
            Text(
              unit!,
              style: textTheme.labelMedium?.copyWith(
                color: scheme.onSurfaceVariant,
              ),
            ),
          ],
        ],
      ),
    );
  }
}
