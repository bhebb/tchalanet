import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class SellerTerminalNavBar extends StatelessWidget {
  const SellerTerminalNavBar({super.key, required this.currentIndex});

  final int currentIndex;

  static const _destinations = [
    (icon: Icons.point_of_sale_rounded, label: 'Accueil', route: '/pos'),
    (icon: Icons.history_rounded, label: 'Historique', route: '/pos/history'),
    (icon: Icons.bar_chart_rounded, label: 'Stats', route: '/pos/stats'),
  ];

  @override
  Widget build(BuildContext context) {
    return NavigationBar(
      selectedIndex: currentIndex,
      onDestinationSelected: (i) {
        if (i != currentIndex) context.go(_destinations[i].route);
      },
      destinations: [
        for (final d in _destinations)
          NavigationDestination(icon: Icon(d.icon), label: d.label),
      ],
    );
  }
}
