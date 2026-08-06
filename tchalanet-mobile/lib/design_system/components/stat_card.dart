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

    final radius = BorderRadius.circular(TchRadius.md);

    return Container(
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLowest,
        // Uniform border. The accent used to be a thicker left BorderSide, but
        // Flutter asserts that a non-uniform border cannot carry a borderRadius,
        // so any caller passing accentColor crashed at paint time. It is now an
        // inner bar clipped to the same radius.
        border: Border.all(color: scheme.outlineVariant),
        borderRadius: radius,
      ),
      child: ClipRRect(
        borderRadius: radius,
        // Stack, not a Row with stretch: the card is laid out in unbounded
        // height (inside a Column or ListView), where stretching every child
        // leaves nothing to determine the row height and the layout never
        // settles. The Stack takes its height from the text column and the
        // accent bar fills it.
        child: Stack(
          children: [
            Padding(
              padding: EdgeInsets.fromLTRB(
                accentColor == null ? TchSpacing.s16 : TchSpacing.s16 + 4,
                TchSpacing.s16,
                TchSpacing.s16,
                TchSpacing.s16,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
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
            ),
            if (accentColor != null)
              Positioned(
                left: 0,
                top: 0,
                bottom: 0,
                width: 4,
                child: ColoredBox(color: accentColor!),
              ),
          ],
        ),
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
