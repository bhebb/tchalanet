class SupportReference {
  const SupportReference({
    required this.traceId,
    this.code,
    this.statusCode,
    this.errorId,
  });

  final String traceId;
  final String? code;
  final int? statusCode;
  final String? errorId;

  String toClipboardText() => [
    'Tchalanet support reference',
    'traceId: $traceId',
    if (errorId != null) 'errorId: $errorId',
    if (code != null) 'code: $code',
    if (statusCode != null) 'status: $statusCode',
  ].join('\n');
}
