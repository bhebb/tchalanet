class ApiException implements Exception {
  ApiException({
    required this.message,
    this.statusCode,
    this.requestId,
    this.traceId,
    this.spanId,
    this.errorId,
    this.code,
    this.category,
    this.retryPolicy,
    this.retryable = false,
    this.params = const {},
  });

  final String message;
  final int? statusCode;
  final String? requestId;
  final String? traceId;
  final String? spanId;
  final String? errorId;
  final String? code;
  final String? category;
  final String? retryPolicy;
  final bool retryable;
  final Map<String, Object?> params;

  @override
  String toString() => 'ApiException($statusCode): $message';
}

/// User-facing message for any caught error.
///
/// For [ApiException] this returns the clean [ApiException.message] (already
/// localized by `mapDioException`), avoiding the `ApiException(400): …` prefix
/// leaking into the UI. Any other error falls back to `toString()`.
String userMessage(Object error) =>
    error is ApiException ? error.message : error.toString();
