import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/network/api_client.dart'
    show apiClientProvider, mapDioException;
import '../models/pos_profile_models.dart';

class PosProfileService {
  const PosProfileService(this._dio);

  final Dio _dio;

  Future<PosProfileResponse> fetchProfile() async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/tenant/cashier/profile',
      );
      final data = response.data?['data'] as Map<String, dynamic>?;
      if (data == null) {
        throw const FormatException('empty POS profile payload');
      }
      return PosProfileResponse.fromJson(data);
    } on DioException catch (e) {
      throw mapDioException(e);
    }
  }
}

final posProfileServiceProvider = Provider<PosProfileService>(
  (ref) => PosProfileService(ref.watch(apiClientProvider)),
);
