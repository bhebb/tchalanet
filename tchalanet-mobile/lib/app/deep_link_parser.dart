String? publicCodeFromTicketVerificationUri(Uri? uri) {
  if (uri == null) return null;

  final queryCode = uri.queryParameters['code'];
  if (queryCode != null && queryCode.trim().isNotEmpty) {
    return normalizeDeepLinkedPublicCode(queryCode);
  }

  final segments = uri.pathSegments;
  for (var i = 0; i < segments.length - 1; i++) {
    if (segments[i].toLowerCase() == 'ticket') {
      return normalizeDeepLinkedPublicCode(segments[i + 1]);
    }
  }

  if (uri.scheme == 'tchalanet' && uri.host.toLowerCase() == 'ticket') {
    final code = segments.isNotEmpty ? segments.last : null;
    return code == null ? null : normalizeDeepLinkedPublicCode(code);
  }

  return null;
}

String normalizeDeepLinkedPublicCode(String value) =>
    value.trim().toUpperCase().replaceAll(RegExp(r'\s+'), '');
