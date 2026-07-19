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

/// Ordered, user-safe translation candidates for an API failure.
///
/// Raw exception messages remain diagnostic data. UI code resolves the first
/// available key from the active locale bundle and never renders them directly.
List<String> userErrorTranslationKeys(Object error) {
  if (error is! ApiException) return const ['common.error.unknown'];

  final keys = <String>[];
  if (error.code case final code? when code.isNotEmpty) {
    keys.add('common.errors.codes.$code.message');
  }
  if (error.category case final category? when category.isNotEmpty) {
    keys.add('common.errors.categories.$category.message');
  }

  switch (error.statusCode) {
    case 401:
      keys.add('common.error.auth_required');
    case 408:
      keys.add('common.error.timeout');
  }
  if (error.code?.startsWith('client.network.') ?? false) {
    keys.add('common.error.network');
  }
  keys.add('common.error.unknown');
  return keys;
}

@Deprecated('Resolve userErrorTranslationKeys through the active i18n bundle.')
String userMessage(Object error) =>
    error is ApiException ? 'Erreur serveur' : 'Erreur inattendue';
