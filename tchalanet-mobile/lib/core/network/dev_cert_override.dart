import 'package:dio/dio.dart';

// Conditional import: real implementation on dart:io (mobile/desktop),
// no-op stub on web (where dart:io and HttpClient are unavailable).
import 'dev_cert_override_stub.dart'
    if (dart.library.io) 'dev_cert_override_io.dart' as impl;

/// Dev-only: makes the Dio HTTP client trust the local mkcert CA used by
/// Traefik on *.localtest.me.
///
/// The Dart `dart:io` HttpClient has its own TLS stack (BoringSSL) and does NOT
/// honor Android's `network_security_config.xml`, so the mkcert CA trusted there
/// for the system network stack (Chrome Custom Tabs, OkHttp) is invisible to Dio.
/// Without this, every HTTPS API call to https://api.localtest.me:8443 fails the
/// TLS handshake before the request reaches the backend.
///
/// No-op in release builds and on web.
void applyDevCertOverride(Dio dio) => impl.applyDevCertOverride(dio);
