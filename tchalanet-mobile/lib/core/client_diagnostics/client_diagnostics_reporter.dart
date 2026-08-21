import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'client_diagnostics_models.dart';
import 'client_diagnostics_queue.dart';
import 'client_diagnostics_transport.dart';

class ClientDiagnosticsPolicyController
    extends Notifier<ClientDiagnosticsPolicy?> {
  @override
  ClientDiagnosticsPolicy? build() => null;

  void setPolicy(ClientDiagnosticsPolicy? policy) {
    state = policy;
  }
}

final clientDiagnosticsPolicyProvider =
    NotifierProvider<
      ClientDiagnosticsPolicyController,
      ClientDiagnosticsPolicy?
    >(ClientDiagnosticsPolicyController.new);

final clientDiagnosticsReporterProvider = Provider<ClientDiagnosticsReporter>((
  ref,
) {
  return ClientDiagnosticsReporter(
    ref,
    ClientDiagnosticsQueue(
      maxQueueSize: ClientDiagnosticsReporter.maxQueueSize,
      dedupeWindow: ClientDiagnosticsReporter.dedupeWindow,
    ),
  );
});

class ClientDiagnosticsReporter {
  ClientDiagnosticsReporter(this._ref, this._queue);

  static const _flushDelay = Duration(seconds: 2);
  static const dedupeWindow = Duration(seconds: 30);
  static const maxQueueSize = 100;
  static const _maxBatchSize = 20;

  final Ref _ref;
  final ClientDiagnosticsQueue _queue;
  Timer? _timer;
  bool _flushing = false;

  void record(ClientDiagnosticEvent event) {
    final policy = _ref.read(clientDiagnosticsPolicyProvider);
    final now = DateTime.now().toUtc();
    if (policy == null || !policy.allows(event.category, now)) {
      _queue.clear();
      return;
    }
    _queue.add(event, policyMaxEvents: policy.maxEvents);
    _timer ??= Timer(_flushDelay, () {
      _timer = null;
      unawaited(flush());
    });
  }

  Future<void> flush() async {
    if (_flushing || _queue.isEmpty) return;
    final policy = _ref.read(clientDiagnosticsPolicyProvider);
    final now = DateTime.now().toUtc();
    if (policy == null ||
        !policy.enabled ||
        policy.expiresAt == null ||
        !now.isBefore(policy.expiresAt!)) {
      _queue.clear();
      return;
    }
    _flushing = true;
    final batch = _queue.take(_maxBatchSize);
    try {
      final transport = _ref.read(clientDiagnosticsTransportProvider);
      await transport.submit(batch);
      _queue.removeFirst(batch.length);
    } catch (_) {
      // Diagnostics are best-effort and must not affect POS flows.
    } finally {
      _flushing = false;
      if (_queue.isNotEmpty) {
        _timer ??= Timer(_flushDelay, () {
          _timer = null;
          unawaited(flush());
        });
      }
    }
  }
}
