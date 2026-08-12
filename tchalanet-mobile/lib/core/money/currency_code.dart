const defaultCurrencyCode = 'HTG';

String resolveCurrencyCode(String? value) {
  final normalized = value?.trim().toUpperCase();
  return normalized == null || normalized.isEmpty
      ? defaultCurrencyCode
      : normalized;
}
