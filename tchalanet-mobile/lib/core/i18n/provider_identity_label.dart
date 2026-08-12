const _officialProviderNames = <String, String>{
  'CA': 'California',
  'FL': 'Florida',
  'GA': 'Georgia',
  'IL': 'Illinois',
  'MI': 'Michigan',
  'MN': 'Minnesota',
  'MO': 'Missouri',
  'MS': 'Mississippi',
  'NJ': 'New Jersey',
  'NY': 'New York',
  'OH': 'Ohio',
  'PA': 'Pennsylvania',
  'TN': 'Tennessee',
  'TX': 'Texas',
  'WI': 'Wisconsin',
};

String officialProviderName(String? providerCode) {
  final code = providerCode?.trim().toUpperCase();
  if (code == null || code.isEmpty) return '';
  return _officialProviderNames[code] ?? code;
}

String? drawProviderCodeFrom({
  String? providerCode,
  String? slotKey,
  String? channelCode,
}) {
  final explicit = _cleanProviderCode(providerCode);
  if (explicit != null) return explicit;
  return _providerCodeFromStructuredCode(slotKey) ??
      _providerCodeFromStructuredCode(channelCode);
}

String? _providerCodeFromStructuredCode(String? value) {
  final parts = value
      ?.trim()
      .split(RegExp(r'[_-]'))
      .where((part) => part.trim().isNotEmpty)
      .toList(growable: false);
  if (parts == null || parts.isEmpty) return null;
  final first = parts.first.toUpperCase();
  if (first == 'HT' && parts.length > 1) return parts[1].toUpperCase();
  return first;
}

String? _cleanProviderCode(String? value) {
  final cleaned = value?.trim();
  if (cleaned == null || cleaned.isEmpty || cleaned == '—') return null;
  return cleaned.split(RegExp(r'[_-]')).first.toUpperCase();
}
