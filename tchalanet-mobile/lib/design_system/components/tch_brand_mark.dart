import 'package:flutter/material.dart';

import '../tokens/tch_radius.dart';
import '../tokens/tch_spacing.dart';

/// Compact rendering of the existing Tchalanet shortcut mark: gold T on navy.
class TchBrandMark extends StatelessWidget {
  const TchBrandMark({super.key, this.size = 36, this.showWordmark = false});

  final double size;
  final bool showWordmark;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final mark = Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: scheme.primary,
        borderRadius: BorderRadius.circular(TchRadius.md),
      ),
      child: FittedBox(
        child: Text(
          'T',
          style: Theme.of(context).textTheme.headlineMedium?.copyWith(
            color: scheme.tertiary,
            fontWeight: FontWeight.w900,
            height: 1,
          ),
        ),
      ),
    );

    if (!showWordmark) return mark;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        mark,
        const SizedBox(width: TchSpacing.s8),
        Text(
          'Tchalanet',
          style: Theme.of(context).textTheme.titleLarge?.copyWith(
            color: scheme.primary,
            fontWeight: FontWeight.w900,
          ),
        ),
      ],
    );
  }
}
