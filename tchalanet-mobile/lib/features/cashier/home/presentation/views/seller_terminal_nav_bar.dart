import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../core/i18n/i18n_repository.dart';

class SellerTerminalNavBar extends ConsumerWidget {
  const SellerTerminalNavBar({super.key, required this.currentIndex});

  final int currentIndex;

  static const _destinations = [
    (
      icon: Icons.point_of_sale_rounded,
      labelKey: 'pos.dashboard.home',
      route: '/pos',
    ),
    (
      icon: Icons.history_rounded,
      labelKey: 'pos.dashboard.history',
      route: '/pos/history',
    ),
    (
      icon: Icons.emoji_events_outlined,
      labelKey: 'pos.dashboard.results',
      route: '/pos/results',
    ),
    (
      icon: Icons.bar_chart_rounded,
      labelKey: 'pos.dashboard.reports',
      route: '/pos/reports',
    ),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final translations = ref.watch(i18nBundleProvider);
    return NavigationBar(
      selectedIndex: currentIndex,
      onDestinationSelected: (i) {
        if (i != currentIndex) context.go(_destinations[i].route);
      },
      destinations: [
        for (final d in _destinations)
          NavigationDestination(
            icon: Icon(d.icon),
            label: translations.translate(d.labelKey),
          ),
      ],
    );
  }
}
