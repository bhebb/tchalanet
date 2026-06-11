enum ApiNoticeSeverity { info, warning, error }

class ApiNotice {
  const ApiNotice({
    required this.code,
    required this.message,
    required this.severity,
    this.domain,
    this.meta = const {},
    this.traceId,
  });

  final String code;
  final String message;
  final String? domain;
  final ApiNoticeSeverity severity;
  final Map<String, Object?> meta;
  final String? traceId;

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
    meta: Map<String, Object?>.from(json['meta'] as Map? ?? const {}),
    traceId: traceId,
  );
}
