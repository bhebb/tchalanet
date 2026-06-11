import 'package:flutter/material.dart';

import '../tokens/tch_radius.dart';

const double _minimumActionHeight = 48;
const double _primaryPosActionHeight = 56;

abstract final class _ActionButtonContent {
  static Widget build({
    required String label,
    required bool loading,
    IconData? icon,
  }) {
    if (loading) {
      return const SizedBox.square(
        dimension: 20,
        child: CircularProgressIndicator(strokeWidth: 2),
      );
    }
    if (icon == null) return Text(label);
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon),
        const SizedBox(width: 8),
        Flexible(child: Text(label, overflow: TextOverflow.ellipsis)),
      ],
    );
  }
}

class PrimaryActionButton extends StatelessWidget {
  const PrimaryActionButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.loading = false,
    this.expanded = true,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool loading;
  final bool expanded;

  @override
  Widget build(BuildContext context) {
    final button = FilledButton(
      onPressed: loading ? null : onPressed,
      style: FilledButton.styleFrom(
        minimumSize: const Size(0, _primaryPosActionHeight),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(TchRadius.md),
        ),
      ),
      child: _ActionButtonContent.build(
        label: label,
        loading: loading,
        icon: icon,
      ),
    );
    return expanded ? SizedBox(width: double.infinity, child: button) : button;
  }
}

class SecondaryActionButton extends StatelessWidget {
  const SecondaryActionButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.loading = false,
    this.expanded = true,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool loading;
  final bool expanded;

  @override
  Widget build(BuildContext context) {
    final button = OutlinedButton(
      onPressed: loading ? null : onPressed,
      style: OutlinedButton.styleFrom(
        minimumSize: const Size(0, _minimumActionHeight),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(TchRadius.md),
        ),
      ),
      child: _ActionButtonContent.build(
        label: label,
        loading: loading,
        icon: icon,
      ),
    );
    return expanded ? SizedBox(width: double.infinity, child: button) : button;
  }
}

class TonalActionButton extends StatelessWidget {
  const TonalActionButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.loading = false,
    this.expanded = true,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool loading;
  final bool expanded;

  @override
  Widget build(BuildContext context) {
    final button = FilledButton.tonal(
      onPressed: loading ? null : onPressed,
      style: FilledButton.styleFrom(
        minimumSize: const Size(0, _minimumActionHeight),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(TchRadius.md),
        ),
      ),
      child: _ActionButtonContent.build(
        label: label,
        loading: loading,
        icon: icon,
      ),
    );
    return expanded ? SizedBox(width: double.infinity, child: button) : button;
  }
}

class DangerActionButton extends StatelessWidget {
  const DangerActionButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.loading = false,
    this.expanded = true,
  });

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool loading;
  final bool expanded;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final button = FilledButton(
      onPressed: loading ? null : onPressed,
      style: FilledButton.styleFrom(
        minimumSize: const Size(0, _minimumActionHeight),
        backgroundColor: scheme.error,
        foregroundColor: scheme.onError,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(TchRadius.md),
        ),
      ),
      child: _ActionButtonContent.build(
        label: label,
        loading: loading,
        icon: icon,
      ),
    );
    return expanded ? SizedBox(width: double.infinity, child: button) : button;
  }
}

class SemanticIconAction extends StatelessWidget {
  const SemanticIconAction({
    super.key,
    required this.icon,
    required this.tooltip,
    required this.onPressed,
    this.selected = false,
  });

  final IconData icon;
  final String tooltip;
  final VoidCallback? onPressed;
  final bool selected;

  @override
  Widget build(BuildContext context) {
    return IconButton.filledTonal(
      onPressed: onPressed,
      tooltip: tooltip,
      isSelected: selected,
      icon: Icon(icon),
    );
  }
}
