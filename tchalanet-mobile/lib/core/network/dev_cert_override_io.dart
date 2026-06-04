import 'dart:io';

import 'package:dio/dio.dart';
import 'package:dio/io.dart';
import 'package:flutter/foundation.dart';

/// Dev-only (debug builds): accept the local mkcert CA cert for *.localtest.me.
/// Never active in release builds — the host check + kDebugMode gate ensure this
/// only loosens TLS for the local dev domain during development.
void applyDevCertOverride(Dio dio) {
  if (!kDebugMode) return;

  dio.httpClientAdapter = IOHttpClientAdapter(
    createHttpClient: () {
      final client = HttpClient();
      client.badCertificateCallback = (cert, host, port) {
        // Only bypass verification for the local dev domain.
        return host == 'localtest.me' || host.endsWith('.localtest.me');
      };
      return client;
    },
  );
}
