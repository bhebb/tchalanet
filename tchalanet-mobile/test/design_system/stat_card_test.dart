import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/design_system/components/stat_card.dart';

Widget _wrap(Widget child) => MaterialApp(
  home: Scaffold(body: Center(child: child)),
);

void main() {
  testWidgets('paints with an accent colour', (tester) async {
    // Regression: the accent used to be a thicker left BorderSide on a
    // BoxDecoration that also carried a borderRadius. Flutter asserts that a
    // non-uniform border cannot have a radius, so every caller passing
    // accentColor threw at paint time. Nothing used it, so it stayed latent.
    await tester.pumpWidget(
      _wrap(
        const SizedBox(
          width: 200,
          child: StatCard(
            label: 'Revni nèt',
            value: '13.50',
            unit: 'HTG',
            accentColor: Color(0xFF006C49),
          ),
        ),
      ),
    );

    expect(tester.takeException(), isNull);
    expect(find.text('REVNI NÈT'), findsOneWidget);
    expect(find.text('13.50'), findsOneWidget);
    expect(find.text('HTG'), findsOneWidget);
  });

  testWidgets('paints without an accent colour', (tester) async {
    await tester.pumpWidget(
      _wrap(
        const SizedBox(
          width: 200,
          child: StatCard(label: 'Tikè', value: '12'),
        ),
      ),
    );

    expect(tester.takeException(), isNull);
    expect(find.text('TIKÈ'), findsOneWidget);
    expect(find.text('12'), findsOneWidget);
  });
}
