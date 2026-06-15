import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

const _knownCoreFeatureImports = <String>{};

const _knownViewDataSourceImports = {
  'lib/features/cashier/tickets/presentation/views/cashier_history_page.dart|../../data/services/cashier_ticket_service.dart',
  'lib/features/cashier/tickets/presentation/views/cashier_scan_page.dart|../../data/services/cashier_ticket_service.dart',
  'lib/features/cashier/tickets/presentation/views/cashier_ticket_detail_page.dart|../../data/services/cashier_ticket_service.dart',
  'lib/features/cashier/tickets/presentation/views/send_receipt_sheet.dart|../../../../../core/storage/op_context_storage.dart',
  'lib/features/cashier/tickets/presentation/views/send_receipt_sheet.dart|../../data/services/cashier_ticket_service.dart',
};

const _knownViewModelDataSourceImports = {
  'lib/features/cashier/home/presentation/view_models/cashier_home_providers.dart|../../data/services/cashier_home_service.dart',
  'lib/features/cashier/operationalcontext/presentation/view_models/op_context_setup_controller.dart|../../data/services/cashier_op_context_service.dart',
  'lib/features/cashier/session/presentation/view_models/cashier_session_controller.dart|../../../../../core/storage/op_context_storage.dart',
  'lib/features/cashier/session/presentation/view_models/cashier_session_controller.dart|../../data/services/cashier_session_service.dart',
  'lib/features/cashier/tickets/presentation/view_models/sell_controller.dart|../../data/services/cashier_sell_catalog_service.dart',
  'lib/features/cashier/tickets/presentation/view_models/sell_controller.dart|../../data/services/cashier_ticket_service.dart',
  'lib/features/draw/presentation/view_models/draw_providers.dart|../../data/services/draw_result_service.dart',
};

const _knownCrossFeatureImports = {
  'lib/features/cashier/home/presentation/views/cashier_home_page.dart|../../../../auth/presentation/view_models/auth_controller.dart',
  'lib/features/cashier/home/presentation/views/cashier_home_page.dart|../../../../notifications/presentation/view_models/notification_summary_controller.dart',
  'lib/features/cashier/tickets/presentation/views/cashier_sell_success_page.dart|../../../../auth/presentation/view_models/auth_controller.dart',
  'lib/features/pos/presentation/views/pos_dashboard_page.dart|../../../auth/data/models/user_session.dart',
  'lib/features/pos/presentation/views/pos_dashboard_page.dart|../../../auth/presentation/view_models/auth_controller.dart',
};

const _knownProvidersDeclaredInViews = {
  'lib/features/cashier/tickets/presentation/views/cashier_history_page.dart|_historyProvider',
  'lib/features/cashier/tickets/presentation/views/cashier_scan_page.dart|verifyControllerProvider',
  'lib/features/cashier/tickets/presentation/views/cashier_ticket_detail_page.dart|_ticketDetailProvider',
};

const _knownHardcodedUiLiteralCounts = <String, int>{
  'lib/features/cashier/home/presentation/views/cashier_home_page.dart': 7,
  'lib/features/cashier/operationalcontext/presentation/views/cashier_setup_page.dart':
      7,
  'lib/features/cashier/session/presentation/views/cashier_session_open_page.dart':
      4,
  'lib/features/cashier/tickets/presentation/views/cashier_history_page.dart':
      5,
  'lib/features/cashier/tickets/presentation/views/cashier_scan_page.dart': 14,
  'lib/features/cashier/tickets/presentation/views/cashier_sell_page.dart': 12,
  'lib/features/cashier/tickets/presentation/views/cashier_sell_success_page.dart':
      11,
  'lib/features/cashier/tickets/presentation/views/cashier_ticket_detail_page.dart':
      23,
  'lib/features/cashier/tickets/presentation/views/send_receipt_sheet.dart': 2,
  'lib/features/pos/presentation/views/pos_dashboard_page.dart': 9,
  'lib/features/pos/presentation/views/pos_stub_page.dart': 1,
};

const _routedScreens = {
  '/login': 'LoginPage',
  '/pos': 'CashierHomePage',
  '/pos/setup': 'CashierSetupPage',
  '/pos/session/open': 'CashierSessionOpenPage',
  '/pos/history': 'CashierHistoryPage',
  '/pos/scan': 'CashierScanPage',
  '/pos/profile': 'PosStubPage',
  '/pos/notifications': 'NotificationCenterPage',
  '/sell': 'CashierSellPage',
  '/pos/sell/success': 'CashierSellSuccessPage',
  '/pos/tickets/:ticketId': 'CashierTicketDetailPage',
  '/forbidden': 'ForbiddenPage',
};

const _migratedScreenContracts = [
  (
    route: '/forbidden',
    view: 'lib/features/auth/presentation/views/forbidden_page.dart',
    viewModel:
        'lib/features/auth/presentation/view_models/forbidden_view_model.dart',
    provider: 'forbiddenViewModelProvider',
    stateClass: 'ForbiddenUiState',
  ),
  (
    route: '/pos/notifications',
    view:
        'lib/features/notifications/presentation/views/notification_center_page.dart',
    viewModel:
        'lib/features/notifications/presentation/view_models/notification_center_view_model.dart',
    provider: 'notificationCenterProvider',
    stateClass: 'NotificationCenterState',
  ),
];

void main() {
  group('dependency directions', () {
    test('data layer never imports presentation', () {
      expect(
        _importsUnder(
          'lib/features',
          sourceContains: '/data/',
        ).where((entry) => entry.importPath.contains('/presentation/')),
        isEmpty,
      );
    });

    test('core cannot gain feature dependencies', () {
      _expectNoNewDebt(
        actual: _importsUnder('lib/core')
            .where((entry) => entry.importPath.contains('features/'))
            .map((entry) => entry.key)
            .toSet(),
        knownDebt: _knownCoreFeatureImports,
        rule: 'core -> features imports',
      );
    });

    test('Views cannot gain Service, storage, or Dio imports', () {
      _expectNoNewDebt(
        actual:
            _importsUnder(
                  'lib/features',
                  sourceContains: '/presentation/views/',
                )
                .where(
                  (entry) =>
                      entry.importPath.contains('/services/') ||
                      entry.importPath.contains('/storage/') ||
                      entry.importPath == 'package:dio/dio.dart',
                )
                .map((entry) => entry.key)
                .toSet(),
        knownDebt: _knownViewDataSourceImports,
        rule: 'View -> data source imports',
      );
    });

    test('ViewModels cannot gain Service or storage imports', () {
      _expectNoNewDebt(
        actual:
            _importsUnder(
                  'lib/features',
                  sourceContains: '/presentation/view_models/',
                )
                .where(
                  (entry) =>
                      entry.importPath.contains('/services/') ||
                      entry.importPath.contains('/storage/') ||
                      entry.importPath == 'package:dio/dio.dart',
                )
                .map((entry) => entry.key)
                .toSet(),
        knownDebt: _knownViewModelDataSourceImports,
        rule: 'ViewModel -> data source imports',
      );
    });

    test('features cannot gain dependencies on other top-level features', () {
      _expectNoNewDebt(
        actual: _importsUnder(
          'lib/features',
        ).where(_isCrossFeatureImport).map((entry) => entry.key).toSet(),
        knownDebt: _knownCrossFeatureImports,
        rule: 'feature -> feature imports',
      );
    });
  });

  group('Riverpod and state policy', () {
    test('only approved Riverpod provider families are introduced', () {
      final forbidden = <String, List<String>>{};
      final patterns = {
        'StateNotifier': RegExp(r'\bStateNotifier(?:Provider)?\b'),
        'StateProvider': RegExp(r'\bStateProvider\b'),
        'ChangeNotifier': RegExp(r'\bChangeNotifier(?:Provider)?\b'),
        'Bloc/Cubit': RegExp(r'\b(?:Bloc|Cubit|BlocProvider)\b'),
      };

      for (final file in _dartFiles('lib')) {
        final content = file.readAsStringSync();
        for (final entry in patterns.entries) {
          if (entry.value.hasMatch(content)) {
            forbidden.putIfAbsent(entry.key, () => []).add(_relative(file));
          }
        }
      }

      expect(
        forbidden,
        isEmpty,
        reason:
            'Use Provider, NotifierProvider, AsyncNotifierProvider, '
            'FutureProvider, or StreamProvider according to state_management.md.',
      );
    });

    test('Views cannot gain screen provider declarations', () {
      final providerPattern = RegExp(r'final\s+(\w+Provider)\s*=');
      final declarations = <String>{};
      for (final file in _dartFiles(
        'lib/features',
      ).where((file) => _relative(file).contains('/presentation/views/'))) {
        final source = _relative(file);
        for (final match in providerPattern.allMatches(
          file.readAsStringSync(),
        )) {
          declarations.add('$source|${match.group(1)}');
        }
      }

      _expectNoNewDebt(
        actual: declarations,
        knownDebt: _knownProvidersDeclaredInViews,
        rule: 'provider declarations inside Views',
      );
    });

    test('migrated screen state is immutable and providers auto-dispose', () {
      for (final contract in _migratedScreenContracts) {
        final viewModel = File(contract.viewModel).readAsStringSync();
        expect(
          viewModel,
          contains('NotifierProvider.autoDispose<'),
          reason:
              '${contract.route} must use an auto-disposed screen provider.',
        );
        expect(
          _nonFinalFields(viewModel, contract.stateClass),
          isEmpty,
          reason: '${contract.stateClass} must expose final immutable fields.',
        );
      }
    });

    test('app and session state retain explicit reset behavior', () {
      final auth = File(
        'lib/features/auth/presentation/view_models/auth_controller.dart',
      ).readAsStringSync();
      final summary = File(
        'lib/features/notifications/presentation/view_models/notification_summary_controller.dart',
      ).readAsStringSync();
      final pollingHost = File(
        'lib/app/runtime_polling_host.dart',
      ).readAsStringSync();

      expect(auth, contains('Future<void> logout()'));
      expect(auth, contains('state = AuthUnauthenticated();'));
      expect(summary, contains('void reset()'));
      expect(
        pollingHost,
        contains('notificationSummaryProvider.notifier).reset()'),
      );
    });
  });

  group('routed screen MVVM contracts', () {
    test('every route remains explicitly inventoried', () {
      final router = File('lib/app/app_router.dart').readAsStringSync();
      final routes = RegExp(
        r"path:\s*'([^']+)'",
      ).allMatches(router).map((match) => match.group(1)!).toSet();

      expect(routes, _routedScreens.keys.toSet());
      for (final entry in _routedScreens.entries) {
        expect(
          router,
          contains(entry.value),
          reason: '${entry.key} must still resolve to ${entry.value}.',
        );
      }
    });

    test(
      'migrated routed screens have exactly one screen ViewModel contract',
      () {
        for (final contract in _migratedScreenContracts) {
          final view = File(contract.view).readAsStringSync();
          final viewModel = File(contract.viewModel).readAsStringSync();

          expect(view, contains("import '../view_models/"));
          expect(view, contains('ref.watch(${contract.provider})'));
          expect(viewModel, contains('class ${contract.stateClass}'));
          expect(
            viewModel,
            contains('extends Notifier<${contract.stateClass}>'),
          );
          expect(
            viewModel,
            isNot(contains('/data/services/')),
            reason: '${contract.route} ViewModel must depend on a Repository.',
          );
          expect(
            viewModel,
            isNot(contains('/data/storage/')),
            reason: '${contract.route} ViewModel must not access storage.',
          );
        }
      },
    );
  });

  group('presentation conventions', () {
    test(
      'feature Views cannot gain detectable hardcoded user-visible strings',
      () {
        final actual = <String, int>{};
        for (final file in _featureViewFiles()) {
          final count = _detectableUiLiterals(file.readAsStringSync()).length;
          if (count > 0) actual[_relative(file)] = count;
        }

        expect(
          actual,
          _knownHardcodedUiLiteralCounts,
          reason:
              'Resolve user-visible copy through i18n. When reducing legacy debt, '
              'lower or remove its explicit baseline count.',
        );
      },
    );

    test('feature Views cannot use raw or Material palette colors', () {
      final violations = <String, List<String>>{};
      final forbidden = [
        RegExp(r'\bColor\s*\(\s*0x[0-9A-Fa-f]+\s*\)'),
        RegExp(r'\bColor\.from(?:ARGB|RGBO)\s*\('),
        RegExp(r'#[0-9A-Fa-f]{6,8}'),
        RegExp(r'\bColors\.(?!transparent\b)[A-Za-z_]\w*'),
      ];

      for (final file in _featureViewFiles()) {
        final content = file.readAsStringSync();
        final matches = <String>[
          for (final pattern in forbidden)
            for (final match in pattern.allMatches(content)) match.group(0)!,
        ];
        if (matches.isNotEmpty) violations[_relative(file)] = matches;
      }

      expect(
        violations,
        isEmpty,
        reason:
            'Feature Views must use Material ColorScheme roles or approved '
            'semantic Tchalanet tokens, never raw or Material palette colors.',
      );
    });
  });
}

Iterable<_ImportEntry> _importsUnder(
  String root, {
  String? sourceContains,
}) sync* {
  final importPattern = RegExp(r"^import\s+'([^']+)';", multiLine: true);
  for (final file in _dartFiles(root)) {
    final source = _relative(file);
    if (sourceContains != null && !source.contains(sourceContains)) continue;
    for (final match in importPattern.allMatches(file.readAsStringSync())) {
      yield _ImportEntry(source, match.group(1)!);
    }
  }
}

Iterable<File> _dartFiles(String root) => Directory(root)
    .listSync(recursive: true)
    .whereType<File>()
    .where((file) => file.path.endsWith('.dart'));

Iterable<File> _featureViewFiles() => _dartFiles(
  'lib/features',
).where((file) => _relative(file).contains('/presentation/views/'));

String _relative(File file) =>
    file.path.replaceFirst('${Directory.current.path}/', '');

void _expectNoNewDebt({
  required Set<String> actual,
  required Set<String> knownDebt,
  required String rule,
}) {
  expect(
    actual.difference(knownDebt),
    isEmpty,
    reason:
        'New $rule are forbidden. Remove the dependency or deliberately update '
        'the migration inventory after architecture review.',
  );
  expect(
    knownDebt.difference(actual),
    isEmpty,
    reason:
        'Known $rule debt was removed. Delete the resolved entry from this '
        'baseline so it cannot return.',
  );
}

List<String> _nonFinalFields(String source, String className) {
  final classStart = source.indexOf('class $className');
  if (classStart < 0) return ['missing class $className'];
  final bodyStart = source.indexOf('{', classStart);
  var depth = 0;
  var bodyEnd = -1;
  for (var index = bodyStart; index < source.length; index++) {
    if (source[index] == '{') depth++;
    if (source[index] == '}') depth--;
    if (depth == 0) {
      bodyEnd = index;
      break;
    }
  }
  final body = source.substring(bodyStart + 1, bodyEnd);
  final fieldPattern = RegExp(
    r'^\s+(?!final\b|static\b|const\b|factory\b)([A-Za-z_][\w<>?, ]*)\s+([a-zA-Z_]\w*)\s*;',
    multiLine: true,
  );
  return fieldPattern
      .allMatches(body)
      .map((match) => match.group(0)!.trim())
      .toList();
}

bool _isCrossFeatureImport(_ImportEntry entry) {
  if (entry.importPath.startsWith('package:') ||
      !entry.importPath.startsWith('.')) {
    return false;
  }
  final sourceFeature = _featureKey(entry.source);
  final targetPath = File(
    entry.source,
  ).parent.uri.resolve(entry.importPath).toFilePath();
  final targetFeature = _featureKey(targetPath);
  return sourceFeature != null &&
      targetFeature != null &&
      sourceFeature != targetFeature;
}

String? _featureKey(String path) {
  final normalized = path.replaceAll(r'\', '/');
  const marker = 'lib/features/';
  final start = normalized.indexOf(marker);
  if (start < 0) return null;
  return normalized.substring(start + marker.length).split('/').first;
}

List<String> _detectableUiLiterals(String source) {
  final patterns = [
    RegExp(r'''\bText\s*\(\s*(['"])(.*?)\1''', dotAll: true),
    RegExp(
      r'''\b(?:tooltip|hintText|semanticsLabel|actionLabel)\s*:\s*(['"])(.*?)\1''',
      dotAll: true,
    ),
    RegExp(r'''\b(?:label|title|message)\s*:\s*(['"])(.*?)\1''', dotAll: true),
  ];
  final literals = <String>[];
  for (final pattern in patterns) {
    for (final match in pattern.allMatches(source)) {
      final literal = match.group(2)!.replaceAll(RegExp(r'\s+'), ' ').trim();
      if (_isUserVisibleLiteral(literal)) literals.add(literal);
    }
  }
  return literals;
}

bool _isUserVisibleLiteral(String literal) {
  if (literal.isEmpty ||
      literal == '—' ||
      literal == '99+' ||
      literal.startsWith('/') ||
      literal.startsWith('api.') ||
      literal.startsWith('auth.') ||
      literal.startsWith('common.') ||
      literal.startsWith('notifications.') ||
      literal.startsWith('pos.')) {
    return false;
  }
  return RegExp(r'[A-Za-zÀ-ÿ]').hasMatch(literal);
}

class _ImportEntry {
  const _ImportEntry(this.source, this.importPath);

  final String source;
  final String importPath;

  String get key => '$source|$importPath';
}
