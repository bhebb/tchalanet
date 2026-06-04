import 'package:dio/dio.dart';

/// Web build: no dart:io, nothing to override (browser handles TLS + CORS).
void applyDevCertOverride(Dio dio) {}
