import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../design_system/theme/runtime_theme.dart';
import '../network/api_client.dart' show apiClientProvider, mapDioException;

/// Fetches the runtime theme from the server.
///
/// Two endpoints (both return ThemeRuntimeView shape):
/// - [fetchPublicTheme]  GET /public/theme/runtime  — no auth required
/// - [fetchTenantTheme]  GET /tenant/theme/runtime  — requires Bearer token (tenant resolved from JWT)
class ThemeService {
  const ThemeService(this._dio);

  final Dio _dio;

  Future<RuntimeTheme> fetchPublicTheme({String mode = 'light'}) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/public/theme/runtime',
        queryParameters: {'mode': mode},
      );
      return RuntimeTheme.fromJson(
        response.data?['data'] as Map<String, dynamic>? ?? {},
      );
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  Future<RuntimeTheme> fetchTenantTheme({String mode = 'light'}) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/tenant/theme/runtime',
        queryParameters: {'mode': mode},
      );
      return RuntimeTheme.fromJson(
        response.data?['data'] as Map<String, dynamic>? ?? {},
      );
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }
}

final themeServiceProvider = Provider<ThemeService>(
  (ref) => ThemeService(ref.watch(apiClientProvider)),
);
