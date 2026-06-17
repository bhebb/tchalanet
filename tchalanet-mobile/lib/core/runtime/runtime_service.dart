import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../network/api_client.dart';
import 'runtime_models.dart';

class RuntimeService {
  const RuntimeService(this._dio);

  final Dio _dio;

  Future<RuntimeBootstrap> fetchPublicBootstrap(String locale) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/public/runtime/bootstrap',
        queryParameters: {'locale': locale},
      );
      return RuntimeBootstrap.fromJson(
        _data(response),
        scope: RuntimeScope.public,
      );
    } on DioException catch (error) {
      throw mapDioException(error);
    }
  }

  Future<RuntimeBootstrap> fetchTenantBootstrap() async {
    try {
      final response = await _dio.get<Map<String, dynamic>>('/runtime/private');
      return RuntimeBootstrap.fromJson(
        _data(response),
        scope: RuntimeScope.tenant,
      );
    } on DioException catch (error) {
      throw mapDioException(error);
    }
  }

  Future<RuntimeStateSnapshot> fetchTenantState() async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '/tenant/runtime/state',
      );
      return RuntimeStateSnapshot.fromJson(_data(response));
    } on DioException catch (error) {
      throw mapDioException(error);
    }
  }

  Map<String, dynamic> _data(Response<Map<String, dynamic>> response) =>
      response.data?['data'] as Map<String, dynamic>? ?? const {};
}

final runtimeServiceProvider = Provider<RuntimeService>(
  (ref) => RuntimeService(ref.watch(apiClientProvider)),
);
