import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client.dart';
import 'change_pin_repository.dart';

class ChangePinRepositoryImpl implements ChangePinRepository {
  const ChangePinRepositoryImpl(this._dio);

  final Dio _dio;

  @override
  Future<void> changePin(String newPin) async {
    try {
      await _dio.post<void>(
        '/tenant/seller-terminal/me/change-pin',
        data: {'newPin': newPin},
      );
    } on DioException catch (error) {
      throw mapDioException(error);
    }
  }
}

final changePinRepositoryProvider = Provider<ChangePinRepository>((ref) {
  return ChangePinRepositoryImpl(ref.watch(apiClientProvider));
});
