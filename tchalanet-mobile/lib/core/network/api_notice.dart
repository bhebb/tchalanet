enum ApiNoticeSeverity { info, warning, error }

class ApiNotice {
  const ApiNotice({
    required this.code,
    required this.message,
    required this.severity,
    this.domain,
    this.kind,
    this.source,
    this.target,
    this.params = const {},
    this.meta = const {},
    this.requestId,
    this.traceId,
    this.spanId,
    this.errorId,
  });

  final String code;
  final String message;
  final String? domain;
  final ApiNoticeSeverity severity;
  final String? kind;
  final String? source;
  final String? target;
  final Map<String, Object?> params;
  final Map<String, Object?> meta;
  final String? requestId;
  final String? traceId;
  final String? spanId;
  final String? errorId;

  factory ApiNotice.fromJson(
    Map<String, dynamic> json, {
    String? traceId,
  }) => ApiNotice(
    code: json['code']?.toString() ?? 'UNKNOWN_NOTICE',
    message: json['message']?.toString() ?? '',
    domain: json['domain']?.toString(),
    severity: switch (json['severity']?.toString().toUpperCase()) {
      'ERROR' => ApiNoticeSeverity.error,
      'WARN' => ApiNoticeSeverity.warning,
      _ => ApiNoticeSeverity.info,
    },
    kind: json['kind']?.toString(),
    source: _sourceValue(json['source']) ?? _stringMeta(json['meta'], 'source'),
    target: json['target']?.toString() ?? _stringMeta(json['meta'], 'target'),
    params: Map<String, Object?>.from(json['params'] as Map? ?? const {}),
    meta: Map<String, Object?>.from(json['meta'] as Map? ?? const {}),
    requestId:
        _stringMeta(json['trace'], 'requestId') ??
        _stringMeta(json['meta'], 'requestId'),
    traceId:
        _stringMeta(json['trace'], 'traceId') ??
        _stringMeta(json['meta'], 'traceId') ??
        traceId,
    spanId:
        _stringMeta(json['trace'], 'spanId') ??
        _stringMeta(json['meta'], 'spanId'),
    errorId:
        _stringMeta(json['trace'], 'errorId') ??
        _stringMeta(json['meta'], 'errorId'),
  );
}

String? _sourceValue(Object? value) {
  if (value is Map && value['source'] != null)
    return value['source'].toString();
  return null;
}

String? _stringMeta(Object? value, String key) {
  if (value is! Map || value[key] == null) return null;
  final text = value[key].toString();
  return text.isEmpty ? null : text;
}
