import 'dart:async';
import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

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
  ClientDiagnosticsReporter(this._ref, this._queue) {
    _restoreFuture = _restorePersisted();
  }

  static const _flushDelay = Duration(seconds: 2);
  static const dedupeWindow = Duration(seconds: 30);
  static const maxQueueSize = 100;
  static const _maxBatchSize = 20;
  static const _storageKey = 'client_diagnostics.pending_events.v1';

  final Ref _ref;
  final ClientDiagnosticsQueue _queue;
  Timer? _timer;
  bool _flushing = false;
  Future<void>? _restoreFuture;

  void record(ClientDiagnosticEvent event) {
    final policy = _ref.read(clientDiagnosticsPolicyProvider);
    final now = DateTime.now().toUtc();
    if (policy != null && !policy.allows(event.category, now)) {
      _queue.clear();
      return;
    }
    _queue.add(event, policyMaxEvents: policy?.maxEvents ?? maxQueueSize);
    unawaited(_persist());
    if (policy == null) return;
    _timer ??= Timer(_flushDelay, () {
      _timer = null;
      unawaited(flush());
    });
  }

  Future<void> flush() async {
    if (_flushing) return;
    await _restoreFuture;
    if (_queue.isEmpty) return;
    final policy = _ref.read(clientDiagnosticsPolicyProvider);
    final now = DateTime.now().toUtc();
    if (policy == null) return;
    if (!policy.enabled ||
        policy.expiresAt == null ||
        !now.isBefore(policy.expiresAt!)) {
      _queue.clear();
      await _persist();
      return;
    }
    _queue.removeWhere((event) => !policy.allows(event.category, now));
    if (_queue.isEmpty) {
      await _persist();
      return;
    }
    _flushing = true;
    final batch = _queue.take(_maxBatchSize);
    try {
      final transport = _ref.read(clientDiagnosticsTransportProvider);
      await transport.submit(batch);
      _queue.removeFirst(batch.length);
      await _persist();
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

  Future<void> flushPublic({required String terminalCode}) async {
    final normalizedTerminalCode = terminalCode.trim();
    if (normalizedTerminalCode.isEmpty || _flushing) return;
    await _restoreFuture;
    if (_queue.isEmpty) return;
    _flushing = true;
    try {
      final transport = _ref.read(clientDiagnosticsPublicTransportProvider);
      final policy = await transport.loadPolicy(
        terminalCode: normalizedTerminalCode,
      );
      _ref.read(clientDiagnosticsPolicyProvider.notifier).setPolicy(policy);
      final now = DateTime.now().toUtc();
      if (!policy.enabled ||
          policy.expiresAt == null ||
          !now.isBefore(policy.expiresAt!)) {
        _queue.clear();
        await _persist();
        return;
      }
      _queue.removeWhere((event) => !policy.allows(event.category, now));
      if (_queue.isEmpty) {
        await _persist();
        return;
      }
      final batch = _queue.take(_maxBatchSize);
      await transport.submit(
        terminalCode: normalizedTerminalCode,
        events: batch,
      );
      _queue.removeFirst(batch.length);
      await _persist();
    } catch (_) {
      // Pre-login diagnostics are best-effort. If the API is unreachable, keep
      // the POS flow silent and lightweight.
    } finally {
      _flushing = false;
    }
  }

  Future<void> _restorePersisted() async {
    try {
      final preferences = await SharedPreferences.getInstance();
      final raw = preferences.getString(_storageKey);
      if (raw == null || raw.isEmpty) return;
      final decoded = jsonDecode(raw);
      if (decoded is! List) return;
      final events = decoded
          .whereType<Map>()
          .map((event) => ClientDiagnosticEvent.fromJson(event))
          .toList(growable: false);
      _queue.addAll(events, policyMaxEvents: maxQueueSize);
    } catch (_) {
      // Corrupt local diagnostics must not block the app.
    }
  }

  Future<void> _persist() async {
    try {
      final preferences = await SharedPreferences.getInstance();
      if (_queue.isEmpty) {
        await preferences.remove(_storageKey);
        return;
      }
      await preferences.setString(
        _storageKey,
        jsonEncode(_queue.all.map((event) => event.toJson()).toList()),
      );
    } catch (_) {
      // Diagnostics are best-effort and must not affect POS flows.
    }
  }
}
