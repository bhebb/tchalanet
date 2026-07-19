import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/features/cashier/home/presentation/views/seller_terminal_nav_bar.dart';

void main() {
  testWidgets('POS navigation keeps four primary destinations', (tester) async {
    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(home: SellerTerminalNavBar(currentIndex: 0)),
      ),
    );

    expect(find.byType(NavigationBar), findsOneWidget);
    expect(find.byType(NavigationDestination), findsNWidgets(4));
    expect(find.byIcon(Icons.menu_rounded), findsNothing);
  });
}
