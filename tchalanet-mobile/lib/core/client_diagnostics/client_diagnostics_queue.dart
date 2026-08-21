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

  List<ClientDiagnosticEvent> take(int maxBatchSize) =>
      _events.take(maxBatchSize).toList(growable: false);

  void removeFirst(int count) {
    if (count <= 0) return;
    _events.removeRange(0, count.clamp(0, _events.length));
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
