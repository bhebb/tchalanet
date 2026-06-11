import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/notifications/app_notification.dart';
import 'package:tchalanet_mobile/design_system/components/components.dart';
import 'package:tchalanet_mobile/design_system/theme/tch_theme.dart';

void main() {
  Widget app(Widget child) => MaterialApp(
    theme: TchTheme.light(),
    home: Scaffold(body: Center(child: child)),
  );

  Widget navigationApp(Size size) => MaterialApp(
    theme: TchTheme.light(),
    home: MediaQuery(
      data: MediaQueryData(size: size),
      child: AdaptiveNavigationShell(
        currentIndex: 0,
        destinations: const [
          AdaptiveNavigationDestination(icon: Icons.home, label: 'Home'),
          AdaptiveNavigationDestination(icon: Icons.person, label: 'Profile'),
        ],
        onDestinationSelected: (_) {},
        body: const Text('Body'),
      ),
    ),
  );

  testWidgets('semantic action buttons expose enabled and loading states', (
    tester,
  ) async {
    var presses = 0;
    await tester.pumpWidget(
      app(
        PrimaryActionButton(
          label: 'Action',
          icon: Icons.add,
          onPressed: () => presses++,
        ),
      ),
    );

    expect(
      tester.getSize(find.byType(FilledButton)).height,
      greaterThanOrEqualTo(56),
    );
    await tester.tap(find.byType(FilledButton));
    expect(presses, 1);

    await tester.pumpWidget(
      app(
        PrimaryActionButton(
          label: 'Action',
          loading: true,
          onPressed: () => presses++,
        ),
      ),
    );

    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    expect(
      tester.widget<FilledButton>(find.byType(FilledButton)).onPressed,
      isNull,
    );
  });

  testWidgets('feedback state renders supplied content and action', (
    tester,
  ) async {
    var retried = false;
    await tester.pumpWidget(
      app(
        FeedbackState(
          kind: FeedbackStateKind.offline,
          title: 'Offline title',
          message: 'Offline message',
          actionLabel: 'Retry',
          onAction: () => retried = true,
        ),
      ),
    );

    expect(find.text('Offline title'), findsOneWidget);
    expect(find.text('Offline message'), findsOneWidget);
    await tester.tap(find.text('Retry'));
    expect(retried, isTrue);
  });

  testWidgets('notification banner uses semantic content and dismiss action', (
    tester,
  ) async {
    var dismissed = false;
    await tester.pumpWidget(
      app(
        AppNotificationBanner(
          kind: AppNotificationKind.error,
          title: 'Error title',
          message: 'Error message',
          dismissTooltip: 'Dismiss',
          onDismiss: () => dismissed = true,
        ),
      ),
    );

    expect(find.text('Error title'), findsOneWidget);
    expect(find.text('Error message'), findsOneWidget);
    await tester.tap(find.byTooltip('Dismiss'));
    expect(dismissed, isTrue);
  });

  testWidgets('shared structural components compose without feature state', (
    tester,
  ) async {
    await tester.pumpWidget(
      app(
        const SurfaceCard(
          emphasis: SurfaceCardEmphasis.low,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              SectionHeader(title: 'Section', subtitle: 'Subtitle'),
              StatusBadge(label: 'Ready', kind: StatusBadgeKind.ready),
              FieldError(message: 'Invalid value'),
            ],
          ),
        ),
      ),
    );

    expect(find.text('Section'), findsOneWidget);
    expect(find.text('Ready'), findsOneWidget);
    expect(find.text('Invalid value'), findsOneWidget);
    expect(find.byType(InkWell), findsOneWidget);
  });

  testWidgets('online badge receives localized labels from caller', (
    tester,
  ) async {
    await tester.pumpWidget(
      app(
        const OnlineBadge(
          online: false,
          onlineLabel: 'Online localized',
          offlineLabel: 'Offline localized',
        ),
      ),
    );

    expect(find.text('OFFLINE LOCALIZED'), findsOneWidget);
    expect(find.text('Hors ligne'), findsNothing);
  });

  testWidgets('adaptive shell uses bottom navigation on compact width', (
    tester,
  ) async {
    await tester.pumpWidget(navigationApp(const Size(390, 844)));

    expect(find.byType(NavigationBar), findsOneWidget);
    expect(find.byType(NavigationRail), findsNothing);
  });

  testWidgets('adaptive shell uses navigation rail on expanded width', (
    tester,
  ) async {
    await tester.pumpWidget(navigationApp(const Size(1000, 800)));

    expect(find.byType(NavigationRail), findsOneWidget);
    expect(find.byType(NavigationBar), findsNothing);
  });
}
