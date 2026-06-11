import 'package:flutter/material.dart';

import '../layout/screen_size.dart';

class AdaptiveNavigationDestination {
  const AdaptiveNavigationDestination({
    required this.icon,
    required this.label,
    this.selectedIcon,
  });

  final IconData icon;
  final IconData? selectedIcon;
  final String label;
}

class AdaptiveNavigationShell extends StatelessWidget {
  const AdaptiveNavigationShell({
    super.key,
    required this.currentIndex,
    required this.destinations,
    required this.onDestinationSelected,
    required this.body,
    this.appBar,
  });

  final int currentIndex;
  final List<AdaptiveNavigationDestination> destinations;
  final ValueChanged<int> onDestinationSelected;
  final Widget body;
  final PreferredSizeWidget? appBar;

  @override
  Widget build(BuildContext context) {
    if (context.isCompact) {
      return Scaffold(
        appBar: appBar,
        body: body,
        bottomNavigationBar: NavigationBar(
          selectedIndex: currentIndex,
          onDestinationSelected: onDestinationSelected,
          destinations: [
            for (final destination in destinations)
              NavigationDestination(
                icon: Icon(destination.icon),
                selectedIcon: Icon(
                  destination.selectedIcon ?? destination.icon,
                ),
                label: destination.label,
              ),
          ],
        ),
      );
    }

    return Scaffold(
      appBar: appBar,
      body: Row(
        children: [
          NavigationRail(
            selectedIndex: currentIndex,
            onDestinationSelected: onDestinationSelected,
            labelType: NavigationRailLabelType.all,
            destinations: [
              for (final destination in destinations)
                NavigationRailDestination(
                  icon: Icon(destination.icon),
                  selectedIcon: Icon(
                    destination.selectedIcon ?? destination.icon,
                  ),
                  label: Text(destination.label),
                ),
            ],
          ),
          const VerticalDivider(width: 1),
          Expanded(child: body),
        ],
      ),
    );
  }
}
