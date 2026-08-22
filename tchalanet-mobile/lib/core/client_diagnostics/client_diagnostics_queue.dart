import 'client_diagnostics_models.dart';

class ClientDiagnosticsQueue {
  ClientDiagnosticsQueue({
    required this.maxQueueSize,
    required this.dedupeWindow,
  });

  final int maxQueueSize;
  final Duration dedupeWindow;
  final List<ClientDiagnosticEvent> _events = [];
  final Map<String, DateTime> _lastRecordedAtByFingerprint = {};

  bool get isEmpty => _events.isEmpty;
  bool get isNotEmpty => _events.isNotEmpty;
  List<ClientDiagnosticEvent> get all => List.unmodifiable(_events);

  void add(ClientDiagnosticEvent event, {required int policyMaxEvents}) {
    if (_isDuplicate(event)) return;
    final effectiveMax = policyMaxEvents <= 0 ? maxQueueSize : policyMaxEvents;
    _events.add(event);
    _lastRecordedAtByFingerprint[event.fingerprint] = event.occurredAtClient
        .toUtc();
    while (_events.length > effectiveMax || _events.length > maxQueueSize) {
      _events.removeAt(0);
    }
    _pruneFingerprints(event.occurredAtClient.toUtc());
  }

  void addAll(
    Iterable<ClientDiagnosticEvent> events, {
    required int policyMaxEvents,
  }) {
    for (final event in events) {
      add(event, policyMaxEvents: policyMaxEvents);
    }
  }

  List<ClientDiagnosticEvent> take(int maxBatchSize) =>
      _events.take(maxBatchSize).toList(growable: false);

  void removeFirst(int count) {
    if (count <= 0) return;
    _events.removeRange(0, count.clamp(0, _events.length));
  }

  void removeWhere(bool Function(ClientDiagnosticEvent event) test) {
    _events.removeWhere(test);
  }

  void clear() {
    _events.clear();
    _lastRecordedAtByFingerprint.clear();
  }

  bool _isDuplicate(ClientDiagnosticEvent event) {
    final occurredAt = event.occurredAtClient.toUtc();
    final previous = _lastRecordedAtByFingerprint[event.fingerprint];
    return previous != null && occurredAt.difference(previous) < dedupeWindow;
  }

  void _pruneFingerprints(DateTime now) {
    _lastRecordedAtByFingerprint.removeWhere(
      (_, recordedAt) => now.difference(recordedAt) >= dedupeWindow,
    );
  }
}
