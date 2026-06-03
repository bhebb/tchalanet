import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../network/api_client.dart';
import 'i18n_models.dart';

/// Fetches backend i18n overrides for a given locale.
/// Stateless — delegates caching to I18nRepository.
class I18nService {
  const I18nService(this._dio);

  final Dio _dio;

  // TODO: confirm endpoint path with backend team
  Future<I18nOverrides> fetchOverrides(String locale) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/i18n/overrides',
        queryParameters: {'locale': locale},
      );
      return I18nOverrides.fromJson(response.data ?? {});
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }
}

final i18nServiceProvider = Provider<I18nService>(
  (ref) => I18nService(ref.watch(apiClientProvider)),
);
