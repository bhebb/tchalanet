import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/app/app_notification_host.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_models.dart';
import 'package:tchalanet_mobile/core/i18n/i18n_repository.dart';
import 'package:tchalanet_mobile/core/notifications/app_notification.dart';
import 'package:tchalanet_mobile/core/notifications/app_notification_controller.dart';
import 'package:tchalanet_mobile/core/notifications/support_reference.dart';
import 'package:tchalanet_mobile/design_system/theme/tch_theme.dart';

void main() {
  testWidgets('notification host resolves i18n and advances the queue', (
    tester,
  ) async {
    late WidgetRef appRef;
    var actionCalls = 0;
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          i18nBundleProvider.overrideWithValue(
            const I18nBundle(
              locale: 'ht',
              translations: {
                'notice.first': 'Premye mesaj',
                'notice.second': 'Dezyèm mesaj',
                'notice.retry': 'Eseye ankò',
                'common.notification.dismiss': 'Fèmen',
                'common.notification.copy_support_reference': 'Kopye pou sipò',
              },
            ),
          ),
        ],
        child: MaterialApp(
          theme: TchTheme.light(),
          home: Consumer(
            builder: (context, ref, _) {
              appRef = ref;
              return const AppNotificationHost(child: Scaffold());
            },
          ),
        ),
      ),
    );

    appRef.read(appNotificationProvider.notifier)
      ..show(
        kind: AppNotificationKind.error,
        messageKey: 'notice.first',
        actionKey: 'notice.retry',
        onAction: () => actionCalls++,
        supportReference: const SupportReference(traceId: 'hidden-trace'),
      )
      ..show(kind: AppNotificationKind.success, messageKey: 'notice.second');
    await tester.pump();

    expect(find.text('Premye mesaj'), findsOneWidget);
    expect(find.text('Dezyèm mesaj'), findsNothing);
    expect(find.textContaining('hidden-trace'), findsNothing);
    expect(find.byTooltip('Kopye pou sipò'), findsOneWidget);

    await tester.tap(find.text('Eseye ankò'));
    await tester.pumpAndSettle();

    expect(actionCalls, 1);
    expect(find.text('Premye mesaj'), findsNothing);
    expect(find.text('Dezyèm mesaj'), findsOneWidget);
  });
}
