import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../network/api_client.dart';
import 'i18n_models.dart';

/// I18n surfaces exposed by the server.
/// Must match com.tchalanet.server.catalog.i18n.api.model.I18nSurface.
enum I18nSurface {
  publicHome,
  publicResults,
  publicTicketCheck,
  commonPublicError,
  auth,
  cashier,
  tenantAdmin,
  platformAdmin,
  commonPrivateError,
}

extension I18nSurfaceParam on I18nSurface {
  String get param => switch (this) {
    I18nSurface.publicHome => 'PUBLIC_HOME',
    I18nSurface.publicResults => 'PUBLIC_RESULTS',
    I18nSurface.publicTicketCheck => 'PUBLIC_TICKET_CHECK',
    I18nSurface.commonPublicError => 'COMMON_PUBLIC_ERROR',
    I18nSurface.auth => 'AUTH',
    I18nSurface.cashier => 'CASHIER',
    I18nSurface.tenantAdmin => 'TENANT_ADMIN',
    I18nSurface.platformAdmin => 'PLATFORM_ADMIN',
    I18nSurface.commonPrivateError => 'COMMON_PRIVATE_ERROR',
  };
}

/// Fetches i18n overrides from the server.
///
/// Two endpoints exist:
/// - [fetchPublicBundle]: GET /public/i18n — no auth, public surfaces only.
///   Public surfaces: PUBLIC_HOME, PUBLIC_RESULTS, PUBLIC_TICKET_CHECK, COMMON_PUBLIC_ERROR.
///
/// - [fetchTenantBundle]: authenticated endpoint for CASHIER/COMMON_PRIVATE_ERROR surfaces.
///   TODO: backend endpoint not yet implemented — returns empty until available.
class I18nService {
  const I18nService(this._dio);

  final Dio _dio;

  /// Public bundle — no auth required.
  /// Returns [I18nOverrides.empty] silently on any error.
  Future<I18nOverrides> fetchPublicBundle(
    String locale, {
    List<I18nSurface> surfaces = const [
      I18nSurface.publicHome,
      I18nSurface.commonPublicError,
    ],
  }) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/public/i18n',
        queryParameters: {
          'locale': locale,
          'surface': surfaces.map((s) => s.param).toList(),
        },
      );
      final data = (response.data?['data'] as Map<String, dynamic>?) ?? {};
      return I18nOverrides.fromBundleView(data);
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }

  /// Authenticated tenant bundle for POS surfaces (CASHIER, COMMON_PRIVATE_ERROR).
  /// TODO: backend endpoint not yet implemented — returns [I18nOverrides.empty].
  Future<I18nOverrides> fetchTenantBundle(String locale) async {
    return I18nOverrides.empty;
  }
}

final i18nServiceProvider = Provider<I18nService>(
  (ref) => I18nService(ref.watch(apiClientProvider)),
);
