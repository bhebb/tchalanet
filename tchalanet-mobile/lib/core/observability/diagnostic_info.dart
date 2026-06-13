class DiagnosticInfo {
  const DiagnosticInfo({
    this.requestId,
    this.traceId,
    this.spanId,
    this.route,
    this.operation,
    required this.occurredAt,
  });

  final String? requestId;
  final String? traceId;
  final String? spanId;
  final String? route;
  final String? operation;
  final DateTime occurredAt;

  bool get hasAny => requestId != null || traceId != null || spanId != null;

  /// Safe copy text — no PII, no tokens, no full payloads.
  String toCopyText() {
    final parts = <String>[
      if (requestId != null) 'requestId=$requestId',
      if (traceId != null) 'traceId=$traceId',
      if (spanId != null) 'spanId=$spanId',
      if (operation != null) 'operation=$operation',
      'time=${occurredAt.toUtc().toIso8601String()}',
    ];
    return parts.join(' ');
  }
}
