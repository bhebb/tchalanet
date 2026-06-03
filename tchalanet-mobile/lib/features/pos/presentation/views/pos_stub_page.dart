import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../design_system/tokens/tch_spacing.dart';

/// Placeholder screen for bottom nav destinations not yet implemented.
class PosStubPage extends StatelessWidget {
  const PosStubPage({super.key, required this.title, required this.index});

  final String title;
  final int index;

  static const _routes = ['/pos', '/pos/reports', '/pos/history', '/pos/settings'];
  static const _icons = [
    Icons.point_of_sale_rounded,
    Icons.bar_chart_rounded,
    Icons.history_rounded,
    Icons.settings_rounded,
  ];
  static const _labels = ['Sales', 'Reports', 'History', 'Settings'];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              _icons[index],
              size: 48,
              color: Theme.of(context).colorScheme.outlineVariant,
            ),
            const SizedBox(height: TchSpacing.s16),
            Text(
              '$title — à venir',
              style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: index,
        onDestinationSelected: (i) {
          if (i != index) context.go(_routes[i]);
        },
        destinations: [
          for (int i = 0; i < _labels.length; i++)
            NavigationDestination(
              icon: Icon(_icons[i]),
              label: _labels[i],
            ),
        ],
      ),
    );
  }
}
