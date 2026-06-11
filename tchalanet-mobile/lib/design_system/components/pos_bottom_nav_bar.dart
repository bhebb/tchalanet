import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class PosBottomNavBar extends StatelessWidget {
  const PosBottomNavBar({super.key, required this.currentIndex});

  final int currentIndex;

  static const _destinations = [
    (icon: Icons.point_of_sale_rounded, label: 'Ventes', route: '/pos'),
    (icon: Icons.history_rounded, label: 'Historique', route: '/pos/history'),
    (icon: Icons.qr_code_scanner_rounded, label: 'Scanner', route: '/pos/scan'),
    (icon: Icons.person_rounded, label: 'Profil', route: '/pos/profile'),
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
