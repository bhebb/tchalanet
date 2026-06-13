class ApiException implements Exception {
  ApiException({
    required this.message,
    this.statusCode,
    this.requestId,
    this.traceId,
    this.spanId,
    this.errorId,
    this.code,
  });

  final String message;
  final int? statusCode;
  final String? requestId;
  final String? traceId;
  final String? spanId;
  final String? errorId;
  final String? code;

  @override
  String toString() => 'ApiException($statusCode): $message';
}
