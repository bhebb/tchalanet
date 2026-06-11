import 'package:flutter/material.dart';

import '../tokens/tch_spacing.dart';

class BottomActionBar extends StatelessWidget {
  const BottomActionBar({
    super.key,
    required this.primaryAction,
    this.secondaryAction,
  });

  final Widget primaryAction;
  final Widget? secondaryAction;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;

    return Material(
      color: scheme.surfaceContainerLowest,
      surfaceTintColor: Colors.transparent,
      elevation: 3,
      child: SafeArea(
        top: false,
        minimum: const EdgeInsets.fromLTRB(
          TchSpacing.s16,
          TchSpacing.s12,
          TchSpacing.s16,
          TchSpacing.s16,
        ),
        child: Row(
          children: [
            if (secondaryAction != null) ...[
              Expanded(child: secondaryAction!),
              const SizedBox(width: TchSpacing.s12),
            ],
            Expanded(flex: 2, child: primaryAction),
          ],
        ),
      ),
    );
  }
}
