import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/auth/auth_token_client.dart';
import 'package:tchalanet_mobile/core/network/auth_interceptor.dart';
import 'package:tchalanet_mobile/core/storage/token_storage.dart';

void main() {
  test(
    'tenant request refreshes Firebase token when storage is empty',
    () async {
      final storage = _FakeTokenStorage();
      final auth = _FakeAuthTokenClient(_validJwt());
      final adapter = _CaptureAdapter();
      var invalidated = false;
      final dio = Dio(BaseOptions(baseUrl: 'https://api.test'));
      dio.httpClientAdapter = adapter;
      dio.interceptors.add(
        AuthInterceptor(dio, storage, auth, () => invalidated = true),
      );

      await dio.post<dynamic>('/tenant/sales/preparations');

      expect(auth.refreshCount, 1);
      expect(storage.accessToken, auth.accessToken);
      expect(adapter.authorizationHeader, 'Bearer ${auth.accessToken}');
      expect(invalidated, isFalse);
    },
  );
}

class _FakeTokenStorage implements TokenStorage {
  String? accessToken;
  String? refreshToken;
  var cleared = false;

  @override
  Future<void> clear() async => cleared = true;

  @override
  Future<String?> readAccessToken() async => accessToken;

  @override
  Future<String?> readRefreshToken() async => refreshToken;

  @override
  Future<void> writeAccessToken(String token) async => accessToken = token;

  @override
  Future<void> writeRefreshToken(String token) async => refreshToken = token;
}

class _FakeAuthTokenClient implements AuthTokenClient {
  _FakeAuthTokenClient(this.accessToken);

  final String accessToken;
  var refreshCount = 0;

  @override
  Future<AuthTokenData> login(AuthCredentials credentials) async =>
      AuthTokenData(accessToken: accessToken);

  @override
  Future<void> logout() async {}

  @override
  Future<AuthTokenData> refresh([String? refreshToken]) async {
    refreshCount++;
    return AuthTokenData(accessToken: accessToken);
  }
}

class _CaptureAdapter implements HttpClientAdapter {
  String? authorizationHeader;

  @override
  void close({bool force = false}) {}

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<List<int>>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    authorizationHeader = options.headers['Authorization'] as String?;
    return ResponseBody.fromString(
      '{"data":{}}',
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }
}

String _validJwt() {
  const payload = 'eyJleHAiOjQxMDI0NDQ4MDB9';
  return 'header.$payload.signature';
}
