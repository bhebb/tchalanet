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

  Future<PosProfileResponse> updateTerminal({
    required String displayName,
  }) async {
    return _patchProfile('/tenant/cashier/profile/terminal', {
      'displayName': displayName,
    });
  }

  Future<PosProfileResponse> updateSeller({
    String? firstName,
    String? lastName,
    String? email,
    String? phoneNumber,
    String? addressId,
  }) async {
    return _patchProfile('/tenant/cashier/profile/seller', {
      'firstName': firstName,
      'lastName': lastName,
      'email': email,
      'phoneNumber': phoneNumber,
      'addressId': addressId,
    });
  }

  Future<PosProfileResponse> updateCommercial({
    required num commissionRate,
  }) async {
    return _patchProfile('/tenant/cashier/profile/commercial', {
      'commissionRate': commissionRate,
    });
  }

  Future<PosProfileResponse> updateSettings({
    bool? receiptAutoPrint,
    int? receiptCopyCount,
    bool? notificationsEnabled,
    bool? notificationsCriticalOnly,
  }) async {
    return _patchProfile('/tenant/cashier/profile/settings', {
      'receiptAutoPrint': receiptAutoPrint,
      'receiptCopyCount': receiptCopyCount,
      'notificationsEnabled': notificationsEnabled,
      'notificationsCriticalOnly': notificationsCriticalOnly,
    });
  }

  Future<PosProfileResponse> _patchProfile(
    String path,
    Map<String, dynamic> body,
  ) async {
    try {
      final response = await _dio.patch<Map<String, dynamic>>(path, data: body);
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
