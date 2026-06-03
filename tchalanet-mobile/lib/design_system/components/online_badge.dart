import 'package:flutter/material.dart';

import '../tokens/tch_spacing.dart';

/// Animated pulse dot + label indicating connectivity.
class OnlineBadge extends StatefulWidget {
  const OnlineBadge({super.key, required this.online});

  final bool online;

  @override
  State<OnlineBadge> createState() => _OnlineBadgeState();
}

class _OnlineBadgeState extends State<OnlineBadge>
    with SingleTickerProviderStateMixin {
  late final AnimationController _ctrl;
  late final Animation<double> _fade;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 900),
    );
    _fade = Tween<double>(begin: 0.3, end: 1.0).animate(
      CurvedAnimation(parent: _ctrl, curve: Curves.easeInOut),
    );
    if (widget.online) _ctrl.repeat(reverse: true);
  }

  @override
  void didUpdateWidget(OnlineBadge old) {
    super.didUpdateWidget(old);
    if (widget.online && !_ctrl.isAnimating) {
      _ctrl.repeat(reverse: true);
    } else if (!widget.online && _ctrl.isAnimating) {
      _ctrl.stop();
    }
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final dotColor =
        widget.online ? const Color(0xFF22C55E) : const Color(0xFF9CA3AF);
    final label = widget.online ? 'Online' : 'Hors ligne';
    final labelColor = widget.online
        ? Theme.of(context).colorScheme.onSurfaceVariant
        : Theme.of(context).colorScheme.outline;

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        FadeTransition(
          opacity: widget.online ? _fade : const AlwaysStoppedAnimation(1.0),
          child: Container(
            width: 8,
            height: 8,
            decoration: BoxDecoration(color: dotColor, shape: BoxShape.circle),
          ),
        ),
        const SizedBox(width: TchSpacing.s4),
        Text(
          label.toUpperCase(),
          style: textTheme.labelSmall?.copyWith(
            color: labelColor,
            fontWeight: FontWeight.w700,
            fontSize: 10,
          ),
        ),
      ],
    );
  }
}
