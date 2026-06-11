class ApiException implements Exception {
  ApiException({
    required this.message,
    this.statusCode,
    this.traceId,
    this.errorId,
    this.code,
  });

  final String message;
  final int? statusCode;
  final String? traceId;
  final String? errorId;
  final String? code;

  @override
  String toString() => 'ApiException($statusCode): $message';
}
