import 'package:flutter/material.dart';

import '../layout/screen_size.dart';
import '../tokens/tch_radius.dart';
import '../tokens/tch_spacing.dart';

enum PosActionButtonSize { large, medium }

enum PosActionButtonTone { primary, secondary, tertiary }

/// Bold action button used for primary POS operations.
/// Large (h=128): primary sell action. Medium (h=112): secondary actions.
class PosActionButton extends StatelessWidget {
  const PosActionButton({
    super.key,
    required this.label,
    required this.icon,
    required this.onPressed,
    this.tone = PosActionButtonTone.primary,
    this.size = PosActionButtonSize.medium,
    this.enabled = true,
  });

  final String label;
  final IconData icon;
  final VoidCallback? onPressed;
  final PosActionButtonTone tone;
  final PosActionButtonSize size;
  final bool enabled;

  double _height(BuildContext context) {
    final scale = context.posScale;
    return size == PosActionButtonSize.large ? 128 * scale : 112 * scale;
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final (bg, fg) = switch (tone) {
      PosActionButtonTone.primary => (scheme.primary, scheme.onPrimary),
      PosActionButtonTone.secondary => (scheme.secondary, scheme.onSecondary),
      PosActionButtonTone.tertiary => (scheme.tertiary, scheme.onTertiary),
    };
    final effectiveOnPressed = enabled ? onPressed : null;

    return SizedBox(
      height: _height(context),
      child: Material(
        color: effectiveOnPressed != null
            ? bg
            : scheme.onSurface.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(TchRadius.md),
        child: InkWell(
          onTap: effectiveOnPressed,
          borderRadius: BorderRadius.circular(TchRadius.md),
          child: Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: TchSpacing.s16,
              vertical: TchSpacing.s12,
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  icon,
                  size: size == PosActionButtonSize.large ? 36 : 28,
                  color: effectiveOnPressed != null
                      ? fg
                      : scheme.onSurface.withValues(alpha: 0.38),
                ),
                const SizedBox(height: TchSpacing.s8),
                Text(
                  label.toUpperCase(),
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.labelLarge?.copyWith(
                    fontWeight: FontWeight.w800,
                    color: effectiveOnPressed != null
                        ? fg
                        : scheme.onSurface.withValues(alpha: 0.38),
                    letterSpacing: 0.5,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
