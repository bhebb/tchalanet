import 'package:flutter/material.dart';

import '../tokens/tch_radius.dart';
import '../tokens/tch_spacing.dart';

enum SurfaceCardEmphasis { lowest, low, normal, high }

class SurfaceCard extends StatelessWidget {
  const SurfaceCard({
    super.key,
    required this.child,
    this.emphasis = SurfaceCardEmphasis.lowest,
    this.padding = const EdgeInsets.all(TchSpacing.s16),
    this.onTap,
    this.backgroundColor,
    this.borderColor,
  });

  final Widget child;
  final SurfaceCardEmphasis emphasis;
  final EdgeInsetsGeometry padding;
  final VoidCallback? onTap;
  final Color? backgroundColor;
  final Color? borderColor;

  Color _background(ColorScheme scheme) => switch (emphasis) {
    SurfaceCardEmphasis.lowest => scheme.surfaceContainerLowest,
    SurfaceCardEmphasis.low => scheme.surfaceContainerLow,
    SurfaceCardEmphasis.normal => scheme.surfaceContainer,
    SurfaceCardEmphasis.high => scheme.surfaceContainerHigh,
  };

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;

    return Material(
      color: backgroundColor ?? _background(scheme),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(TchRadius.md),
        side: BorderSide(color: borderColor ?? scheme.outlineVariant),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(padding: padding, child: child),
      ),
    );
  }
}
