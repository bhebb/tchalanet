import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:tchalanet_mobile/core/client_diagnostics/client_diagnostics_models.dart';
import 'package:tchalanet_mobile/core/client_diagnostics/client_diagnostics_reporter.dart';
import 'package:tchalanet_mobile/core/client_diagnostics/client_diagnostics_transport.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test(
    'publishes pre-login diagnostics when public policy allows it',
    () async {
      SharedPreferences.setMockInitialValues({});
      final transport = _FakePublicTransport(
        policy: ClientDiagnosticsPolicy(
          enabled: true,
          expiresAt: DateTime.now().toUtc().add(const Duration(hours: 2)),
          maxEvents: 10,
          categories: {ClientDiagnosticCategory.connectivity},
        ),
      );
      final container = ProviderContainer(
        overrides: [
          clientDiagnosticsPublicTransportProvider.overrideWithValue(transport),
        ],
      );
      addTearDown(container.dispose);

      final reporter = container.read(clientDiagnosticsReporterProvider);
      reporter.record(
        ClientDiagnosticEvent(
          category: ClientDiagnosticCategory.connectivity,
          occurredAtClient: DateTime.parse('2026-08-22T11:40:00Z'),
          severity: ClientDiagnosticSeverity.error,
          operation: 'GET /tenant/cashier/tickets',
          errorCode: 'connectionError',
          message: "Failed host lookup: 'api.localtest.me'",
          platform: 'android',
        ),
      );

      await reporter.flushPublic(terminalCode: ' POS-001 ');

      expect(transport.loadedTerminalCodes, ['POS-001']);
      expect(transport.submittedTerminalCodes, ['POS-001']);
      expect(transport.submittedEvents, hasLength(1));
      expect(
        transport.submittedEvents.single.category,
        ClientDiagnosticCategory.connectivity,
      );
      expect(
        transport.submittedEvents.single.operation,
        'GET /tenant/cashier/tickets',
      );
    },
  );

  test('deduplicates repeated diagnostics before public flush', () async {
    SharedPreferences.setMockInitialValues({});
    final transport = _FakePublicTransport(
      policy: ClientDiagnosticsPolicy(
        enabled: true,
        expiresAt: DateTime.now().toUtc().add(const Duration(hours: 2)),
        maxEvents: 10,
        categories: {ClientDiagnosticCategory.connectivity},
      ),
    );
    final container = ProviderContainer(
      overrides: [
        clientDiagnosticsPublicTransportProvider.overrideWithValue(transport),
      ],
    );
    addTearDown(container.dispose);

    final reporter = container.read(clientDiagnosticsReporterProvider);
    final occurredAt = DateTime.parse('2026-08-22T11:40:00Z');
    for (var index = 0; index < 3; index += 1) {
      reporter.record(
        ClientDiagnosticEvent(
          category: ClientDiagnosticCategory.connectivity,
          occurredAtClient: occurredAt.add(Duration(seconds: index)),
          severity: ClientDiagnosticSeverity.error,
          operation: 'GET /tenant/cashier/tickets',
          errorCode: 'connectionError',
          message: "Failed host lookup: 'api.localtest.me'",
        ),
      );
    }

    await reporter.flushPublic(terminalCode: 'POS-001');

    expect(transport.submittedEvents, hasLength(1));
  });
}

class _FakePublicTransport extends ClientDiagnosticsPublicTransport {
  _FakePublicTransport({required this.policy}) : super(Dio());

  final ClientDiagnosticsPolicy policy;
  final List<String> loadedTerminalCodes = [];
  final List<String> submittedTerminalCodes = [];
  final List<ClientDiagnosticEvent> submittedEvents = [];

  @override
  Future<ClientDiagnosticsPolicy> loadPolicy({
    required String terminalCode,
  }) async {
    loadedTerminalCodes.add(terminalCode);
    return policy;
  }

  @override
  Future<void> submit({
    required String terminalCode,
    required List<ClientDiagnosticEvent> events,
  }) async {
    submittedTerminalCodes.add(terminalCode);
    submittedEvents.addAll(events);
  }
}
